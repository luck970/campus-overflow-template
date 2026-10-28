package com.campusoverflow.qa.application.command;

import com.campusoverflow.qa.api.event.VoteCastEvent;
import com.campusoverflow.qa.domain.Answer;
import com.campusoverflow.qa.domain.AnswerRepository;
import com.campusoverflow.qa.domain.Question;
import com.campusoverflow.qa.domain.QuestionRepository;
import com.campusoverflow.qa.domain.TargetType;
import com.campusoverflow.qa.domain.Vote;
import com.campusoverflow.qa.domain.VoteRepository;
import com.campusoverflow.shared.domain.BusinessRuleException;
import com.campusoverflow.shared.domain.NotFoundException;
import com.campusoverflow.shared.event.EventIds;
import com.campusoverflow.shared.event.IntegrationEventPublisher;
import com.campusoverflow.shared.security.Actor;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用例：投票 / 改投 / 撤销（value ∈ {-1, 0, 1}，0 表示撤销）。
 * 发布的事件同时携带旧值与新值，下游据此计算声誉增量。
 */
@Service
public class VoteService {

    private final VoteRepository votes;
    private final QuestionRepository questions;
    private final AnswerRepository answers;
    private final IntegrationEventPublisher events;
    private final Clock clock;

    public VoteService(VoteRepository votes, QuestionRepository questions, AnswerRepository answers,
                       IntegrationEventPublisher events, Clock clock) {
        this.votes = votes;
        this.questions = questions;
        this.answers = answers;
        this.events = events;
        this.clock = clock;
    }

    public record VoteResult(int myVote, int scoreDelta) {
    }

    private record Target(long authorId, long questionId) {
    }

    @Transactional
    public VoteResult cast(Actor actor, TargetType type, long targetId, int value) {
        Target target = resolve(type, targetId);
        if (target.authorId == actor.userId()) {
            throw new BusinessRuleException("QA-1006", "不能给自己的内容投票");
        }
        if (value < -1 || value > 1) {
            throw new BusinessRuleException("QA-1006", "投票值只能是 -1、0、1");
        }
        Optional<Vote> existing = votes.find(actor.userId(), type, targetId);
        int oldValue = existing.map(Vote::value).orElse(0);
        Instant now = Instant.now(clock);
        if (value == 0) {
            existing.ifPresent(votes::delete);
        } else {
            Vote vote;
            if (existing.isEmpty()) {
                vote = Vote.cast(actor.userId(), target.authorId, type, targetId, value, now);
            } else {
                vote = existing.get();
                vote.change(value, now);
            }
            votes.save(vote);
        }
        int scoreDelta = value - oldValue;
        if (type == TargetType.QUESTION) {
            questions.adjustScore(targetId, scoreDelta);
        } else {
            answers.adjustScore(targetId, scoreDelta);
        }
        if (scoreDelta != 0) {
            events.publish(new VoteCastEvent(EventIds.next(), now, type.name(), targetId, target.authorId,
                    actor.userId(), target.questionId, oldValue, value));
        }
        return new VoteResult(value, scoreDelta);
    }

    private Target resolve(TargetType type, long targetId) {
        if (type == TargetType.QUESTION) {
            Question q = questions.findById(targetId)
                    .orElseThrow(() -> new NotFoundException("QA-1000", "问题不存在: " + targetId));
            return new Target(q.authorId(), q.id());
        }
        Answer a = answers.findById(targetId)
                .orElseThrow(() -> new NotFoundException("QA-2000", "回答不存在: " + targetId));
        return new Target(a.authorId(), a.questionId());
    }
}
