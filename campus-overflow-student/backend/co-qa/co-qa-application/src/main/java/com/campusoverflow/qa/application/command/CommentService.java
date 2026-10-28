package com.campusoverflow.qa.application.command;

import com.campusoverflow.qa.api.event.CommentPostedEvent;
import com.campusoverflow.qa.domain.Answer;
import com.campusoverflow.qa.domain.AnswerRepository;
import com.campusoverflow.qa.domain.Comment;
import com.campusoverflow.qa.domain.CommentRepository;
import com.campusoverflow.qa.domain.Question;
import com.campusoverflow.qa.domain.QuestionRepository;
import com.campusoverflow.qa.domain.TargetType;
import com.campusoverflow.shared.domain.NotFoundException;
import com.campusoverflow.shared.event.EventIds;
import com.campusoverflow.shared.event.IntegrationEventPublisher;
import com.campusoverflow.shared.security.Actor;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 用例：评论与二级回复。评论中的 @昵称 由 Discovery 解析并发送提醒。 */
@Service
public class CommentService {

    private final CommentRepository comments;
    private final QuestionRepository questions;
    private final AnswerRepository answers;
    private final IntegrationEventPublisher events;
    private final Clock clock;

    public CommentService(CommentRepository comments, QuestionRepository questions, AnswerRepository answers,
                          IntegrationEventPublisher events, Clock clock) {
        this.comments = comments;
        this.questions = questions;
        this.answers = answers;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public long post(Actor actor, PostCommentCommand cmd) {
        Instant now = Instant.now(clock);
        Comment parent = cmd.parentId() == null ? null
                : comments.findById(cmd.parentId())
                        .orElseThrow(() -> new NotFoundException("QA-1007", "父评论不存在: " + cmd.parentId()));
        long questionId;
        if (cmd.targetType() == TargetType.QUESTION) {
            Question q = questions.findById(cmd.targetId())
                    .orElseThrow(() -> new NotFoundException("QA-1000", "问题不存在: " + cmd.targetId()));
            q.ensureOpenForAnswers();
            questionId = q.id();
        } else {
            Answer a = answers.findById(cmd.targetId())
                    .orElseThrow(() -> new NotFoundException("QA-2000", "回答不存在: " + cmd.targetId()));
            questionId = a.questionId();
        }
        Comment comment = Comment.post(cmd.targetType(), cmd.targetId(), questionId, actor.userId(), parent,
                cmd.body(), now);
        Comment saved = comments.save(comment);
        events.publish(new CommentPostedEvent(EventIds.next(), now, saved.id(), questionId,
                actor.userId(), cmd.body()));
        return saved.id();
    }
}
