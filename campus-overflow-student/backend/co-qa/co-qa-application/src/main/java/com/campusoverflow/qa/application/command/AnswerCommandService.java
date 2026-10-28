package com.campusoverflow.qa.application.command;

import com.campusoverflow.identity.api.IdentityApi;
import com.campusoverflow.qa.api.event.AnswerAcceptedEvent;
import com.campusoverflow.qa.api.event.AnswerEndorsedEvent;
import com.campusoverflow.qa.api.event.AnswerSubmittedEvent;
import com.campusoverflow.qa.api.event.ContentDeletedEvent;
import com.campusoverflow.qa.application.port.QuestionListCache;
import com.campusoverflow.qa.domain.Answer;
import com.campusoverflow.qa.domain.AnswerRepository;
import com.campusoverflow.qa.domain.MarkdownBody;
import com.campusoverflow.qa.domain.Question;
import com.campusoverflow.qa.domain.QuestionRepository;
import com.campusoverflow.shared.audit.AuditTrail;
import com.campusoverflow.shared.domain.NotFoundException;
import com.campusoverflow.shared.event.EventIds;
import com.campusoverflow.shared.event.IntegrationEventPublisher;
import com.campusoverflow.shared.security.Actor;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 用例：提交 / 编辑 / 删除回答，采纳最佳答案，教师认证。 */
@Service
public class AnswerCommandService {

    private final QuestionRepository questions;
    private final AnswerRepository answers;
    private final IdentityApi identity;
    private final IntegrationEventPublisher events;
    private final QuestionListCache listCache;
    private final AuditTrail audit;
    private final Clock clock;

    public AnswerCommandService(QuestionRepository questions, AnswerRepository answers, IdentityApi identity,
                                IntegrationEventPublisher events, QuestionListCache listCache, AuditTrail audit,
                                Clock clock) {
        this.questions = questions;
        this.answers = answers;
        this.identity = identity;
        this.events = events;
        this.listCache = listCache;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional
    public long submit(Actor actor, long questionId, String body) {
        Instant now = Instant.now(clock);
        Question q = loadQuestion(questionId);
        Answer answer = Answer.submit(q, actor.userId(), MarkdownBody.ofAnswer(body), now);
        Answer saved = answers.save(answer);
        questions.adjustAnswerCount(questionId, 1);
        events.publish(new AnswerSubmittedEvent(EventIds.next(), now, saved.id(), questionId,
                saved.authorId(), q.authorId(), q.title().value()));
        listCache.invalidateAll();
        audit.record("ANSWER_SUBMITTED", actor.userId(), "ANSWER", String.valueOf(saved.id()), null);
        return saved.id();
    }

    @Transactional
    public void edit(Actor actor, long answerId, String body) {
        Answer a = loadAnswer(answerId);
        Question q = loadQuestion(a.questionId());
        boolean manager = identity.canManageCourse(actor.userId(), q.courseId());
        Instant now = Instant.now(clock);
        a.edit(actor, manager, MarkdownBody.ofAnswer(body), now);
        answers.save(a);
        listCache.invalidateAll();
        audit.record("ANSWER_EDITED", actor.userId(), "ANSWER", String.valueOf(answerId), null);
    }

    @Transactional
    public void delete(Actor actor, long answerId) {
        Answer a = loadAnswer(answerId);
        Question q = loadQuestion(a.questionId());
        boolean manager = identity.canManageCourse(actor.userId(), q.courseId());
        Instant now = Instant.now(clock);
        boolean accepted = q.isAccepted(answerId);
        a.softDelete(actor, manager, accepted, now);
        answers.save(a);
        questions.adjustAnswerCount(a.questionId(), -1);
        events.publish(new ContentDeletedEvent(EventIds.next(), now, "ANSWER", answerId, a.questionId()));
        listCache.invalidateAll();
        audit.record("ANSWER_DELETED", actor.userId(), "ANSWER", String.valueOf(answerId), null);
    }

    /** 场景 R-2：采纳最佳答案。状态变更与 Outbox 事件在同一事务提交；声誉结算异步进行。 */
    @Transactional
    public void accept(Actor actor, long questionId, long answerId) {
        Question q = loadQuestion(questionId);
        Answer a = loadAnswer(answerId);
        boolean manager = identity.canManageCourse(actor.userId(), q.courseId());
        Instant now = Instant.now(clock);
        q.accept(a, actor, manager);
        questions.save(q);
        events.publish(new AnswerAcceptedEvent(EventIds.next(), now, answerId, questionId,
                a.authorId(), q.authorId(), actor.userId(), q.title().value()));
        listCache.invalidateAll();
        audit.record("ANSWER_ACCEPTED", actor.userId(), "ANSWER", String.valueOf(answerId), null);
    }

    @Transactional
    public void endorse(Actor actor, long answerId) {
        Answer a = loadAnswer(answerId);
        Question q = loadQuestion(a.questionId());
        boolean manager = identity.canManageCourse(actor.userId(), q.courseId());
        Instant now = Instant.now(clock);
        a.endorse(actor, manager);
        answers.save(a);
        events.publish(new AnswerEndorsedEvent(EventIds.next(), now, answerId, a.questionId(),
                a.authorId(), actor.userId(), q.title().value()));
        listCache.invalidateAll();
        audit.record("ANSWER_ENDORSED", actor.userId(), "ANSWER", String.valueOf(answerId), null);
    }

    private Question loadQuestion(long id) {
        return questions.findById(id)
                .orElseThrow(() -> new NotFoundException("QA-1000", "问题不存在: " + id));
    }

    private Answer loadAnswer(long id) {
        return answers.findById(id)
                .orElseThrow(() -> new NotFoundException("QA-2000", "回答不存在: " + id));
    }
}
