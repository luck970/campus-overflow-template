package com.campusoverflow.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campusoverflow.identity.api.event.UserRegisteredEvent;
import com.campusoverflow.identity.domain.Course;
import com.campusoverflow.identity.domain.CourseMember;
import com.campusoverflow.identity.domain.CourseRepository;
import com.campusoverflow.identity.domain.CourseRole;
import com.campusoverflow.identity.domain.PasswordHasher;
import com.campusoverflow.identity.domain.PasswordPolicy;
import com.campusoverflow.identity.domain.User;
import com.campusoverflow.identity.domain.UserRepository;
import com.campusoverflow.shared.audit.AuditTrail;
import com.campusoverflow.shared.domain.BusinessRuleException;
import com.campusoverflow.shared.domain.ConflictException;
import com.campusoverflow.shared.domain.NotFoundException;
import com.campusoverflow.shared.event.IntegrationEvent;
import com.campusoverflow.shared.event.IntegrationEventPublisher;
import com.campusoverflow.shared.page.PageRequest;
import com.campusoverflow.shared.page.PageResult;
import com.campusoverflow.shared.security.Actor;
import com.campusoverflow.shared.security.Role;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 柯博交叉测试：US-001 注册 / US-002 登录 / US-003 加入课程（应用服务编排 + 领域规则交叉验证）。 */
class CrossCutIdentityServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-27T08:00:00Z");
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    private final InMemoryUserRepository users = new InMemoryUserRepository();
    private final InMemoryCourseRepository courses = new InMemoryCourseRepository();
    private final PlainHasher hasher = new PlainHasher();
    private final RecordingEvents events = new RecordingEvents();
    private final RecordingAudit audit = new RecordingAudit();

    private UserRegistrationService registration;
    private CourseService courseService;

    @BeforeEach
    void setUp() {
        registration = new UserRegistrationService(users, hasher, events, audit, clock);
        courseService = new CourseService(courses, users, clock);
    }

    // ---- US-001 注册 ----

    @Test
    @DisplayName("US-001 注册成功：创建学生账号、发布注册事件并写审计")
    void registerHappyPathPublishesEventAndAudits() {
        long id = registration.register(new RegisterUserCommand("20230001", "柯博", "kebo@example.com",
                "pass1234", "计算机工程学院", "计科3班"));

        User saved = users.findById(id).orElseThrow();
        assertThat(saved.username()).isEqualTo("20230001");
        assertThat(saved.role()).isEqualTo(Role.STUDENT);
        List<UserRegisteredEvent> registered = events.ofType(UserRegisteredEvent.class);
        assertThat(registered).hasSize(1);
        assertThat(registered.get(0).userId()).isEqualTo(id);
        assertThat(audit.actions).contains("USER_REGISTERED");
    }

    @Test
    @DisplayName("US-001 注册冲突：学号已注册被拒绝（ID-2001）")
    void registerRejectsDuplicateUsername() {
        registration.register(new RegisterUserCommand("20230001", "柯博", "kebo@example.com",
                "pass1234", "计算机工程学院", "计科3班"));

        assertThatThrownBy(() -> registration.register(new RegisterUserCommand("20230001", "另一个昵称",
                "other@example.com", "pass1234", "计算机工程学院", "计科3班")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("学号");
    }

    @Test
    @DisplayName("US-001 注册冲突：昵称已被占用被拒绝（ID-2002）")
    void registerRejectsDuplicateDisplayName() {
        registration.register(new RegisterUserCommand("20230001", "柯博", "kebo@example.com",
                "pass1234", "计算机工程学院", "计科3班"));

        assertThatThrownBy(() -> registration.register(new RegisterUserCommand("20230002", "柯博",
                "kebo2@example.com", "pass1234", "计算机工程学院", "计科3班")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("昵称");
    }

    @Test
    @DisplayName("US-001 注册校验：纯数字弱密码被口令策略拒绝（ID-1004）")
    void registerRejectsWeakPassword() {
        assertThatThrownBy(() -> registration.register(new RegisterUserCommand("20230003", "弱口令用户",
                "weak@example.com", "12345678", "计算机工程学院", "计科3班")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("字母和数字");
    }

    // ---- US-002 登录（领域交叉：口令哈希与账户状态） ----

    @Test
    @DisplayName("US-002 登录凭证：注册后口令可校验，且哈希非明文存储")
    void registeredPasswordMatchesAndHashIsNotPlaintext() {
        registration.register(new RegisterUserCommand("20230004", "登录验证", "login@example.com",
                "pass1234", "计算机工程学院", "计科3班"));

        User saved = users.findByUsername("20230004").orElseThrow();
        assertThat(hasher.matches("pass1234", saved.passwordHash())).isTrue();
        assertThat(saved.passwordHash()).isNotEqualTo("pass1234");
    }

    @Test
    @DisplayName("US-002 登录状态：正常注册的账户可登录，禁用后不可登录")
    void registeredUserCanLoginUntilDisabled() {
        registration.register(new RegisterUserCommand("20230005", "状态验证", "status@example.com",
                "pass1234", "计算机工程学院", "计科3班"));
        User saved = users.findByUsername("20230005").orElseThrow();
        assertThat(saved.canLogin()).isTrue();

        saved.disable(new Actor(999, "管理员", Role.ADMIN));
        assertThat(saved.canLogin()).isFalse();
    }

    @Test
    @DisplayName("US-002 口令策略：字母+数字合法口令通过，纯数字口令被拒绝")
    void passwordPolicyAllowsLetterAndDigitOnly() {
        assertThatCode(() -> PasswordPolicy.validate("abc12345")).doesNotThrowAnyException();
        assertThatThrownBy(() -> PasswordPolicy.validate("12345678"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("字母和数字");
    }

    // ---- US-003 加入课程 ----

    @Test
    @DisplayName("US-003 加入课程：学生加入后以 STUDENT 身份成为成员")
    void joinCourseAddsStudentMember() {
        long courseId = newCourse("CS2001", "数据结构");
        long studentId = registerStudent("20230006", "加课验证");

        courseService.join(new Actor(studentId, "加课验证", Role.STUDENT), courseId);

        CourseMember member = courses.findMember(courseId, studentId).orElseThrow();
        assertThat(member.role()).isEqualTo(CourseRole.STUDENT);
        assertThat(courses.findMembershipsOfUser(studentId)).extracting(CourseMember::courseId)
                .containsExactly(courseId);
    }

    @Test
    @DisplayName("US-003 加入课程：重复加入被拒绝（ID-2005）")
    void joinCourseTwiceIsRejected() {
        long courseId = newCourse("CS2002", "操作系统");
        long studentId = registerStudent("20230007", "重复加课");
        Actor student = new Actor(studentId, "重复加课", Role.STUDENT);

        courseService.join(student, courseId);

        assertThatThrownBy(() -> courseService.join(student, courseId))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("已加入");
    }

    @Test
    @DisplayName("US-003 加入课程：课程不存在时报 NotFound")
    void joinUnknownCourseThrowsNotFound() {
        long studentId = registerStudent("20230008", "未知课程");
        assertThatThrownBy(() -> courseService.join(new Actor(studentId, "未知课程", Role.STUDENT), 9999L))
                .isInstanceOf(NotFoundException.class);
    }

    private long newCourse(String code, String name) {
        Actor teacher = new Actor(900, "王老师", Role.TEACHER);
        Course course = Course.create(teacher, code, name, "2026-秋", NOW);
        return courses.save(course).id();
    }

    private long registerStudent(String username, String displayName) {
        return registration.register(new RegisterUserCommand(username, displayName, username + "@example.com",
                "pass1234", "计算机工程学院", "计科3班"));
    }

    // ---- 测试替身 ----

    static class InMemoryUserRepository implements UserRepository {
        private final Map<Long, User> store = new LinkedHashMap<>();
        private final AtomicLong sequence = new AtomicLong();

        @Override
        public Optional<User> findById(long id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public Optional<User> findByUsername(String username) {
            return store.values().stream().filter(u -> u.username().equals(username)).findFirst();
        }

        @Override
        public Optional<User> findByDisplayName(String displayName) {
            return store.values().stream().filter(u -> u.displayName().equals(displayName)).findFirst();
        }

        @Override
        public boolean existsByUsername(String username) {
            return findByUsername(username).isPresent();
        }

        @Override
        public boolean existsByDisplayName(String displayName) {
            return findByDisplayName(displayName).isPresent();
        }

        @Override
        public boolean existsByEmail(String email) {
            return store.values().stream().anyMatch(u -> u.email().equals(email));
        }

        @Override
        public List<User> findAllById(Collection<Long> ids) {
            return ids.stream().map(store::get).filter(java.util.Objects::nonNull).collect(Collectors.toList());
        }

        @Override
        public PageResult<User> findByVerified(boolean verified, PageRequest page) {
            List<User> all = new ArrayList<>(store.values());
            return new PageResult<>(all, all.size(), page.page(), page.size());
        }

        @Override
        public User save(User user) {
            if (user.id() == null) {
                user.assignId(sequence.incrementAndGet());
            }
            store.put(user.id(), user);
            return user;
        }
    }

    static class InMemoryCourseRepository implements CourseRepository {
        private final Map<Long, Course> store = new LinkedHashMap<>();
        private final Map<String, CourseMember> members = new LinkedHashMap<>();
        private final AtomicLong sequence = new AtomicLong();

        @Override
        public Course save(Course course) {
            if (course.id() == null) {
                course.assignId(sequence.incrementAndGet());
            }
            store.put(course.id(), course);
            return course;
        }

        @Override
        public Optional<Course> findById(long id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public List<Course> findAll() {
            return new ArrayList<>(store.values());
        }

        @Override
        public boolean existsByCodeAndTerm(String code, String term) {
            return store.values().stream().anyMatch(c -> c.code().equals(code) && c.term().equals(term));
        }

        @Override
        public Optional<CourseMember> findMember(long courseId, long userId) {
            return Optional.ofNullable(members.get(courseId + ":" + userId));
        }

        @Override
        public CourseMember saveMember(CourseMember member) {
            members.put(member.courseId() + ":" + member.userId(), member);
            return member;
        }

        @Override
        public List<CourseMember> findMembers(long courseId) {
            return members.values().stream().filter(m -> m.courseId() == courseId).collect(Collectors.toList());
        }

        @Override
        public List<CourseMember> findMembershipsOfUser(long userId) {
            return members.values().stream().filter(m -> m.userId() == userId).collect(Collectors.toList());
        }
    }

    static class PlainHasher implements PasswordHasher {
        @Override
        public String hash(String raw) {
            return "plain:" + raw;
        }

        @Override
        public boolean matches(String raw, String hash) {
            return hash.equals("plain:" + raw);
        }
    }

    static class RecordingEvents implements IntegrationEventPublisher {
        public final List<IntegrationEvent> published = new ArrayList<>();

        @Override
        public void publish(IntegrationEvent event) {
            published.add(event);
        }

        public <T extends IntegrationEvent> List<T> ofType(Class<T> type) {
            return published.stream().filter(type::isInstance).map(type::cast).collect(Collectors.toList());
        }
    }

    static class RecordingAudit implements AuditTrail {
        public final List<String> actions = new ArrayList<>();

        @Override
        public void record(String action, Long actorId, String targetType, String targetId, String detail) {
            actions.add(action);
        }
    }
}
