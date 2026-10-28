package com.campusoverflow.qa.application.query;

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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 问答查询用例（读路径）。 */
@Service
@Transactional(readOnly = true)
public class QuestionQueryService {

    private static final int EXCERPT_LENGTH = 160;

    private final QaReadModel readModel;
    private final IdentityApi identity;
    private final ContentRenderer renderer;
    private final ViewCounter viewCounter;
    private final QuestionListCache cache;

    public QuestionQueryService(QaReadModel readModel, IdentityApi identity, ContentRenderer renderer,
                                ViewCounter viewCounter, QuestionListCache cache) {
        this.readModel = readModel;
        this.identity = identity;
        this.renderer = renderer;
        this.viewCounter = viewCounter;
        this.cache = cache;
    }

    public PageResult<QuestionSummaryView> list(Long courseId, PageRequest page) {
        Optional<PageResult<QuestionSummaryView>> cached = cache.get(courseId, page);
        if (cached.isPresent()) {
            return cached.get();
        }
        PageResult<QuestionRow> rows = readModel.listQuestions(courseId, page);
        Set<Long> authorIds = rows.items().stream().map(QuestionRow::authorId)
                .collect(Collectors.toCollection(HashSet::new));
        Map<Long, UserSummary> users = identity.findUsers(authorIds);
        PageResult<QuestionSummaryView> result = rows.map(r -> {
            AuthorView author = AuthorView.of(r.authorId(), users.get(r.authorId()));
            String excerpt = renderer.toPlainText(r.body(), EXCERPT_LENGTH);
            return new QuestionSummaryView(r.id(), r.title(), excerpt, r.tags(), r.courseId(), author,
                    r.score(), r.answerCount(), r.viewCount(), r.acceptedAnswerId() != null, r.createdAt());
        });
        cache.put(courseId, page, result);
        return result;
    }

    public QuestionDetailView detail(long questionId, Actor actorOrNull) {
        QuestionRow q = readModel.findQuestion(questionId)
                .orElseThrow(() -> new NotFoundException("QA-1000", "问题不存在: " + questionId));
        List<AnswerRow> answerRows = readModel.findAnswers(questionId);
        List<CommentRow> commentRows = readModel.findComments(questionId);

        Set<Long> authorIds = new HashSet<>();
        authorIds.add(q.authorId());
        answerRows.forEach(a -> authorIds.add(a.authorId()));
        commentRows.forEach(c -> authorIds.add(c.authorId()));
        Map<Long, UserSummary> users = identity.findUsers(authorIds);

        List<Long> answerIds = answerRows.stream().map(AnswerRow::id).toList();
        Map<String, Integer> myVotes = actorOrNull == null ? Map.of()
                : readModel.findMyVotes(actorOrNull.userId(), questionId, answerIds);

        viewCounter.increment(questionId);

        CourseRefView courseRef = null;
        if (q.courseId() != null) {
            CourseSummary course = identity.findCourse(q.courseId()).orElse(null);
            if (course != null) {
                courseRef = new CourseRefView(course.id(), course.code(), course.name());
            }
        }

        List<AnswerView> answerViews = answerRows.stream().map(a -> {
            AuthorView author = AuthorView.of(a.authorId(), users.get(a.authorId()));
            AuthorView endorser = null;
            if (a.endorsedBy() != null) {
                endorser = AuthorView.of(a.endorsedBy(), identity.findUser(a.endorsedBy()).orElse(null));
            }
            int myVote = myVotes.getOrDefault("ANSWER:" + a.id(), 0);
            boolean canEdit = actorOrNull != null && actorOrNull.is(a.authorId());
            return new AnswerView(a.id(), a.body(), renderer.toSafeHtml(a.body()), author, a.score(),
                    q.acceptedAnswerId() != null && q.acceptedAnswerId() == a.id(), endorser, myVote,
                    canEdit, a.createdAt(), a.updatedAt(), List.of());
        }).toList();

        List<CommentView> commentViews = commentRows.stream().map(c -> {
            AuthorView author = AuthorView.of(c.authorId(), users.get(c.authorId()));
            return new CommentView(c.id(), author, c.parentId(), c.body(), c.createdAt());
        }).toList();

        int myQuestionVote = myVotes.getOrDefault("QUESTION:" + questionId, 0);
        boolean canManage = actorOrNull != null && identity.canManageCourse(actorOrNull.userId(), q.courseId());
        boolean canEdit = actorOrNull != null && actorOrNull.is(q.authorId());
        PermissionsView permissions = new PermissionsView(canEdit, canEdit, canManage || canEdit, canManage);

        return new QuestionDetailView(q.id(), q.title(), q.body(), renderer.toSafeHtml(q.body()), q.tags(),
                courseRef, AuthorView.of(q.authorId(), users.get(q.authorId())), q.score(), q.answerCount(),
                q.viewCount(), q.acceptedAnswerId(), myQuestionVote, q.createdAt(), q.updatedAt(),
                permissions, commentViews, answerViews);
    }

    public List<TagView> popularTags(int limit) {
        return readModel.popularTags(limit);
    }
}
