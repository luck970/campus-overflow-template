package com.campusoverflow.qa.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campusoverflow.identity.api.CourseSummary;
import com.campusoverflow.identity.api.IdentityApi;
import com.campusoverflow.identity.api.UserSummary;
import com.campusoverflow.qa.application.port.ContentRenderer;
import com.campusoverflow.qa.application.port.QaReadModel;
import com.campusoverflow.qa.application.port.QuestionListCache;
import com.campusoverflow.qa.application.port.ViewCounter;
import com.campusoverflow.shared.domain.NotFoundException;
import com.campusoverflow.shared.page.PageRequest;
import com.campusoverflow.shared.page.PageResult;
import com.campusoverflow.shared.security.Actor;
import com.campusoverflow.shared.security.Role;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 问答查询用例测试：验证列表缓存、详情组装、权限投影与浏览计数。 */
class QuestionQueryServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-08T06:00:00Z");

    private final InMemoryReadModel readModel = new InMemoryReadModel();
    private final FakeIdentity identity = new FakeIdentity()
            .withUser(1, "小明", Role.STUDENT)
            .withUser(2, "小红", Role.STUDENT)
            .withUser(9, "王老师", Role.TEACHER)
            .withCourse(100L);
    private final FakeRenderer renderer = new FakeRenderer();
    private final FakeViewCounter viewCounter = new FakeViewCounter();
    private final FakeCache cache = new FakeCache();

    private QuestionQueryService service;
    private final Actor ming = new Actor(1, "小明", Role.STUDENT);

    @BeforeEach
    void setUp() {
        service = new QuestionQueryService(readModel, identity, renderer, viewCounter, cache);
    }

    @Test
    @DisplayName("列表：未命中缓存时从读模型查询并组装摘要，随后写入缓存")
    void listAssemblesSummariesAndFillsCache() {
        long q1 = readModel.addQuestion(1, 100L, "红黑树插入后为什么必须旋转？", "变色为什么不够？希望有直观解释。",
                List.of("数据结构", "红黑树"), 3, 2, 10, true);

        PageResult<QuestionSummaryView> result = service.list(100L, new PageRequest(1, 20));

        assertThat(result.total()).isEqualTo(1);
        QuestionSummaryView s = result.items().get(0);
        assertThat(s.id()).isEqualTo(q1);
        assertThat(s.title()).isEqualTo("红黑树插入后为什么必须旋转？");
        assertThat(s.excerpt()).isEqualTo("excerpt:变色为什么不够？希望有直观解释。");
        assertThat(s.tags()).containsExactly("数据结构", "红黑树");
        assertThat(s.author().displayName()).isEqualTo("小明");
        assertThat(s.score()).isEqualTo(3);
        assertThat(s.answerCount()).isEqualTo(2);
        assertThat(s.viewCount()).isEqualTo(10);
        assertThat(s.accepted()).isTrue();
        assertThat(cache.putCount).isEqualTo(1);

        service.list(100L, new PageRequest(1, 20));
        assertThat(readModel.listCalls).isEqualTo(1);
    }

    @Test
    @DisplayName("列表：课程不存在时抛出 NotFoundException")
    void listRejectsUnknownCourse() {
        assertThatThrownBy(() -> service.list(999L, new PageRequest(1, 20)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("详情：组装正文渲染、评论、回答、投票与权限，并递增浏览计数")
    void detailAssemblesFullViewAndCountsView() {
        long q1 = readModel.addQuestion(1, 100L, "红黑树插入后为什么必须旋转？", "## 变色为什么不够", List.of("数据结构"),
                3, 1, 10, true);
        long a1 = readModel.addAnswer(q1, 2, "关键在于黑高这个不变量。", 5, null);
        readModel.addComment(q1, "QUESTION", q1, 2, null, "同问");
        readModel.addComment(q1, "ANSWER", a1, 1, null, "讲得清楚");
        readModel.addVote("QUESTION:" + q1, 1);
        readModel.addVote("ANSWER:" + a1, -1);

        QuestionDetailView d = service.detail(q1, ming);

        assertThat(d.bodyHtml()).isEqualTo("<h2>变色为什么不够</h2>");
        assertThat(d.course().name()).isEqualTo("数据结构");
        assertThat(d.myVote()).isEqualTo(1);
        assertThat(d.permissions().canEdit()).isTrue();
        assertThat(d.permissions().canDelete()).isTrue();
        assertThat(d.permissions().canAccept()).isTrue();
        assertThat(d.viewCount()).isEqualTo(11);
        assertThat(viewCounter.increments).containsExactly(q1);
        assertThat(d.comments()).hasSize(1);
        assertThat(d.comments().get(0).body()).isEqualTo("同问");
        assertThat(d.answers()).hasSize(1);
        AnswerView av = d.answers().get(0);
        assertThat(av.accepted()).isTrue();
        assertThat(av.myVote()).isEqualTo(-1);
        assertThat(av.bodyHtml()).isEqualTo("<p>关键在于黑高这个不变量。</p>");
        assertThat(av.author().displayName()).isEqualTo("小红");
        assertThat(av.comments()).hasSize(1);
    }

    @Test
    @DisplayName("详情：匿名访问时无编辑权限、无我的投票")
    void detailAnonymousHasNoPermissions() {
        long q1 = readModel.addQuestion(1, 100L, "红黑树插入后为什么必须旋转？", "正文", List.of("数据结构"), 0, 0, 0, false);

        QuestionDetailView d = service.detail(q1, null);

        assertThat(d.myVote()).isZero();
        assertThat(d.permissions().canEdit()).isFalse();
        assertThat(d.permissions().canDelete()).isFalse();
        assertThat(d.permissions().canAccept()).isFalse();
        assertThat(d.permissions().canManage()).isFalse();
    }

    @Test
    @DisplayName("详情：问题不存在或已删除时抛出 NotFoundException")
    void detailMissingQuestionThrows() {
        assertThatThrownBy(() -> service.detail(404L, ming))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("热门标签：返回读模型的热门标签")
    void popularTagsDelegatesToReadModel() {
        readModel.tags.add(new TagView("数据结构", 12));
        readModel.tags.add(new TagView("算法", 8));

        List<TagView> tags = service.popularTags(10);

        assertThat(tags).hasSize(2);
        assertThat(tags.get(0).name()).isEqualTo("数据结构");
        assertThat(tags.get(0).questionCount()).isEqualTo(12);
    }

    // ---- fakes ----

    static class InMemoryReadModel implements QaReadModel {
        final Map<Long, QuestionRow> questions = new LinkedHashMap<>();
        final Map<Long, List<AnswerRow>> answers = new LinkedHashMap<>();
        final Map<Long, List<CommentRow>> comments = new LinkedHashMap<>();
        final Map<String, Integer> votes = new HashMap<>();
        final List<TagView> tags = new ArrayList<>();
        int listCalls;

        long addQuestion(long authorId, Long courseId, String title, String body, List<String> tags,
                         int score, int answerCount, int viewCount, boolean accepted) {
            long id = questions.size() + 1;
            questions.put(id, new QuestionRow(id, authorId, courseId, title, body, tags,
                    accepted ? 999L : null, score, answerCount, viewCount, NOW, NOW));
            return id;
        }

        long addAnswer(long questionId, long authorId, String body, int score, Long endorsedBy) {
            long id = answers.computeIfAbsent(questionId, k -> new ArrayList<>()).size() + 1;
            answers.get(questionId).add(new AnswerRow(id, authorId, body, score, endorsedBy, NOW, NOW));
            return id;
        }

        void addComment(long questionId, String targetType, long targetId, long authorId, Long parentId, String body) {
            comments.computeIfAbsent(questionId, k -> new ArrayList<>()).add(
                    new CommentRow(comments.size() + 1L, targetType, targetId, authorId, parentId, body, NOW));
        }

        void addVote(String key, int value) {
            votes.put(key, value);
        }

        @Override
        public PageResult<QuestionRow> listQuestions(Long courseId, PageRequest page) {
            listCalls++;
            List<QuestionRow> rows = questions.values().stream()
                    .filter(q -> courseId == null || courseId.equals(q.courseId())).toList();
            return new PageResult<>(rows, rows.size(), page.page(), page.size());
        }

        @Override
        public Optional<QuestionRow> findQuestion(long questionId) {
            return Optional.ofNullable(questions.get(questionId));
        }

        @Override
        public List<AnswerRow> findAnswers(long questionId) {
            return List.copyOf(answers.getOrDefault(questionId, List.of()));
        }

        @Override
        public List<CommentRow> findComments(long questionId) {
            return List.copyOf(comments.getOrDefault(questionId, List.of()));
        }

        @Override
        public Map<String, Integer> findMyVotes(long voterId, long questionId, Collection<Long> answerIds) {
            Map<String, Integer> result = new HashMap<>();
            votes.forEach((k, v) -> {
                if (k.equals("QUESTION:" + questionId)
                        || answerIds.contains(Long.parseLong(k.substring("ANSWER:".length())))) {
                    result.put(k, v);
                }
            });
            return result;
        }

        @Override
        public List<TagView> popularTags(int limit) {
            return tags;
        }
    }

    static class FakeRenderer implements ContentRenderer {
        @Override
        public String toSafeHtml(String markdown) {
            if (markdown.startsWith("## ")) {
                return "<h2>" + markdown.substring(3) + "</h2>";
            }
            return "<p>" + markdown + "</p>";
        }

        @Override
        public String toPlainText(String markdown, int maxLength) {
            return "excerpt:" + markdown;
        }
    }

    static class FakeViewCounter implements ViewCounter {
        final List<Long> increments = new ArrayList<>();

        @Override
        public void increment(long questionId) {
            increments.add(questionId);
        }
    }

    static class FakeCache implements QuestionListCache {
        final Map<String, PageResult<QuestionSummaryView>> store = new HashMap<>();
        int putCount;

        @Override
        public Optional<PageResult<QuestionSummaryView>> get(Long courseId, PageRequest page) {
            return Optional.ofNullable(store.get(key(courseId, page)));
        }

        @Override
        public void put(Long courseId, PageRequest page, PageResult<QuestionSummaryView> value) {
            putCount++;
            store.put(key(courseId, page), value);
        }

        @Override
        public void invalidateAll() {
        }

        private static String key(Long courseId, PageRequest page) {
            return courseId + "|" + page.page() + "|" + page.size();
        }
    }

    static class FakeIdentity implements IdentityApi {
        final Map<Long, UserSummary> users = new HashMap<>();
        final List<Long> courses = new ArrayList<>();

        FakeIdentity withUser(long id, String name, Role role) {
            users.put(id, new UserSummary(id, name, role, true, "计算机工程学院"));
            return this;
        }

        FakeIdentity withCourse(long id) {
            courses.add(id);
            return this;
        }

        @Override
        public Optional<UserSummary> findUser(long userId) {
            return Optional.ofNullable(users.get(userId));
        }

        @Override
        public Map<Long, UserSummary> findUsers(Collection<Long> userIds) {
            Map<Long, UserSummary> result = new HashMap<>();
            userIds.forEach(id -> findUser(id).ifPresent(u -> result.put(id, u)));
            return result;
        }

        @Override
        public Optional<UserSummary> findByDisplayName(String displayName) {
            return users.values().stream().filter(u -> u.displayName().equals(displayName)).findFirst();
        }

        @Override
        public Optional<CourseSummary> findCourse(long courseId) {
            if (courses.contains(courseId)) {
                return Optional.of(new CourseSummary(courseId, "CS2001", "数据结构", "2026-秋", 100L));
            }
            return Optional.empty();
        }

        @Override
        public boolean canManageCourse(long userId, Long courseId) {
            UserSummary u = users.get(userId);
            return u != null && u.role() == Role.TEACHER;
        }
    }
}
