package com.campusoverflow.qa.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campusoverflow.qa.api.event.AnswerAcceptedEvent;
import com.campusoverflow.qa.application.fake.QaTestFixtures.CountingCache;
import com.campusoverflow.qa.application.fake.QaTestFixtures.InMemoryAnswers;
import com.campusoverflow.qa.application.fake.QaTestFixtures.InMemoryQuestions;
import com.campusoverflow.qa.application.fake.QaTestFixtures.InMemoryVotes;
import com.campusoverflow.qa.application.fake.QaTestFixtures.RecordingAudit;
import com.campusoverflow.qa.application.fake.QaTestFixtures.RecordingEvents;
import com.campusoverflow.qa.application.fake.QaTestFixtures.StubIdentity;
import com.campusoverflow.qa.domain.MarkdownBody;
import com.campusoverflow.qa.domain.Question;
import com.campusoverflow.qa.domain.Tags;
import com.campusoverflow.qa.domain.Title;
import com.campusoverflow.shared.domain.BusinessRuleException;
import com.campusoverflow.shared.domain.ConflictException;
import com.campusoverflow.shared.domain.ForbiddenException;
import com.campusoverflow.shared.security.Actor;
import com.campusoverflow.shared.security.Role;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 柯博交叉测试：US-012 采纳回答（提问者/课程管理者权限、重复采纳、跨问题回答拒绝）。 */
class US012AcceptCrossTest {

    private static final Instant NOW = Instant.parse("2026-10-27T08:00:00Z");
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    private final InMemoryQuestions questions = new InMemoryQuestions();
    private final InMemoryAnswers answers = new InMemoryAnswers();
    private final InMemoryVotes votes = new InMemoryVotes();
    private final RecordingEvents events = new RecordingEvents();
    private final CountingCache cache = new CountingCache();
    private final RecordingAudit audit = new RecordingAudit();
    private final StubIdentity identity = new StubIdentity()
            .withUser(1, "柯博", Role.STUDENT).withUser(2, "小红", Role.STUDENT).withUser(9, "王老师", Role.TEACHER);

    private final Actor asker = new Actor(1, "柯博", Role.STUDENT);
    private final Actor hong = new Actor(2, "小红", Role.STUDENT);
    private final Actor teacher = new Actor(9, "王老师", Role.TEACHER);

    private AnswerCommandService answerService;
    private long questionId;

    @BeforeEach
    void setUp() {
        answerService = new AnswerCommandService(questions, answers, identity, events, cache, audit, clock);
        Question q = questions.save(Question.post(asker.userId(), 100L, new Title("什么是红黑树的黑高？"),
                MarkdownBody.ofQuestion("希望了解红黑树黑高的定义与作用。"), Tags.of(List.of("数据结构")), NOW));
        questionId = q.id();
    }

    @Test
    @DisplayName("US-012 采纳：提问者采纳后问题标记已采纳、发布事件并失效列表缓存")
    void askerCanAcceptAndQuestionMarkedSolved() {
        long answerId = answerService.submit(hong, questionId, "黑高是从节点到叶子路径上黑色节点的数量。");

        answerService.accept(asker, questionId, answerId);

        Question saved = questions.findById(questionId).orElseThrow();
        assertThat(saved.acceptedAnswerId()).isEqualTo(answerId);
        List<AnswerAcceptedEvent> accepted = events.ofType(AnswerAcceptedEvent.class);
        assertThat(accepted).hasSize(1);
        assertThat(accepted.get(0).answerId()).isEqualTo(answerId);
        assertThat(accepted.get(0).answerAuthorId()).isEqualTo(hong.userId());
        assertThat(accepted.get(0).acceptedById()).isEqualTo(asker.userId());
        assertThat(audit.actions).contains("ANSWER_ACCEPTED");
    }

    @Test
    @DisplayName("US-012 采纳：非提问者且非课程管理者被拒绝（Forbidden）")
    void nonAskerWithoutManageRightCannotAccept() {
        long answerId = answerService.submit(hong, questionId, "黑高是路径上的黑色节点数量。");

        assertThatThrownBy(() -> answerService.accept(hong, questionId, answerId))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("只有提问者或课程管理者");
    }

    @Test
    @DisplayName("US-012 采纳：课程管理者可以代采纳（教学场景）")
    void courseManagerCanAccept() {
        identity.withCourseManager(9);
        long answerId = answerService.submit(hong, questionId, "黑高是路径上的黑色节点数量。");

        answerService.accept(teacher, questionId, answerId);

        assertThat(questions.findById(questionId).orElseThrow().acceptedAnswerId()).isEqualTo(answerId);
    }

    @Test
    @DisplayName("US-012 采纳：一个问题只能采纳一个回答（重复采纳被拒绝）")
    void repeatAcceptRejected() {
        long first = answerService.submit(hong, questionId, "黑高是路径上的黑色节点数量。");
        long second = answerService.submit(asker, questionId, "补充：叶子节点也算黑色。");

        answerService.accept(asker, questionId, first);

        assertThatThrownBy(() -> answerService.accept(asker, questionId, second))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("只能采纳一个回答");
    }

    @Test
    @DisplayName("US-012 采纳：回答必须属于该问题（跨问题采纳被拒绝）")
    void answerFromAnotherQuestionCannotBeAccepted() {
        Question other = questions.save(Question.post(asker.userId(), 100L, new Title("另一道测试题目的完整标题内容"),
                MarkdownBody.ofQuestion("这是另一道题的内容，用于验证跨问题采纳被拒绝的边界。"), Tags.of(List.of("数据结构")), NOW));
        long foreignAnswerId = answerService.submit(hong, other.id(), "这是另一道题的回答。");

        assertThatThrownBy(() -> answerService.accept(asker, questionId, foreignAnswerId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("不属于此问题");
    }
}
