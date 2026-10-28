package com.campusoverflow.qa.application.query;

import com.campusoverflow.identity.api.IdentityApi;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 问答查询用例（读路径）。 */
@Service
@Transactional(readOnly = true)
public class QuestionQueryService {

    private static final int EXCERPT_LENGTH = 160;
    private static final int MAX_TAGS = 50;

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
        if (courseId != null && identity.findCourse(courseId).isEmpty()) {
            throw new NotFoundException("COURSE_NOT_FOUND", "课程不存在");
        }
        Optional<PageResult<QuestionSummaryView>> cached = cache.get(courseId, page);
        if (cached.isPresent()) {
            return cached.get();
        }
        PageResult<QuestionRow> rows = readModel.listQuestions(courseId, page);
        Map<Long, com.campusoverflow.identity.api.UserSummary> users =
                identity.findUsers(rows.items().stream().map(QuestionRow::authorId).collect(Collectors.toSet()));
        List<QuestionSummaryView> summaries = rows.items().stream()
                .map(r -> new QuestionSummaryView(
                        r.id(),
                        r.title(),
                        renderer.toPlainText(r.body(), EXCERPT_LENGTH),
                        r.tags(),
                        r.courseId(),
                        AuthorView.of(r.authorId(), users.get(r.authorId())),
                        r.score(),
                        r.answerCount(),
                        r.viewCount(),
                        r.acceptedAnswerId() != null,
                        r.createdAt()))
                .toList();
        PageResult<QuestionSummaryView> result = new PageResult<>(summaries, rows.total(), page.page(), page.size());
        cache.put(courseId, page, result);
        return result;
    }

    public QuestionDetailView detail(long questionId, Actor actorOrNull) {
        QuestionRow row = readModel.findQuestion(questionId)
                .orElseThrow(() -> new NotFoundException("QUESTION_NOT_FOUND", "问题不存在或已删除"));
        viewCounter.increment(questionId);

        List<AnswerRow> answerRows = new ArrayList<>(readModel.findAnswers(questionId));
        answerRows.sort(Comparator.comparingLong(AnswerRow::id));
        List<CommentRow> commentRows = readModel.findComments(questionId);

        List<Long> authorIds = new ArrayList<>();
        authorIds.add(row.authorId());
        answerRows.forEach(a -> authorIds.add(a.authorId()));
        commentRows.forEach(c -> authorIds.add(c.authorId()));
        answerRows.stream().map(AnswerRow::endorsedBy).filter(java.util.Objects::nonNull).forEach(authorIds::add);
        Map<Long, com.campusoverflow.identity.api.UserSummary> users =
                identity.findUsers(authorIds.stream().collect(Collectors.toSet()));

        AuthorView author = AuthorView.of(row.authorId(), users.get(row.authorId()));

        CourseRefView course = null;
        if (row.courseId() != null) {
            Optional<com.campusoverflow.identity.api.CourseSummary> cs = identity.findCourse(row.courseId());
            if (cs.isPresent()) {
                course = new CourseRefView(cs.get().id(), cs.get().code(), cs.get().name());
            }
        }

        Map<String, Integer> myVotes = actorOrNull == null
                ? Map.of()
                : readModel.findMyVotes(actorOrNull.userId(), questionId,
                        answerRows.stream().map(AnswerRow::id).collect(Collectors.toSet()));
        int questionVote = myVotes.getOrDefault("QUESTION:" + questionId, 0);

        boolean manage = actorOrNull != null && row.courseId() != null
                && identity.canManageCourse(actorOrNull.userId(), row.courseId());
        boolean own = actorOrNull != null && actorOrNull.is(row.authorId());
        PermissionsView permissions = actorOrNull == null
                ? PermissionsView.NONE
                : new PermissionsView(own, own || manage, own || manage, manage);

        List<CommentView> questionComments = commentRows.stream()
                .filter(c -> "QUESTION".equals(c.targetType()))
                .map(c -> new CommentView(c.id(), AuthorView.of(c.authorId(), users.get(c.authorId())),
                        c.parentId(), c.body(), c.createdAt()))
                .toList();

        List<AnswerView> answers = answerRows.stream()
                .map(a -> {
                    List<CommentView> answerComments = commentRows.stream()
                            .filter(c -> "ANSWER".equals(c.targetType()) && c.targetId() == a.id())
                            .map(c -> new CommentView(c.id(),
                                    AuthorView.of(c.authorId(), users.get(c.authorId())),
                                    c.parentId(), c.body(), c.createdAt()))
                            .toList();
                    AuthorView endorsedBy = a.endorsedBy() == null ? null
                            : AuthorView.of(a.endorsedBy(), users.get(a.endorsedBy()));
                    return new AnswerView(a.id(), a.body(), renderer.toSafeHtml(a.body()),
                            AuthorView.of(a.authorId(), users.get(a.authorId())), a.score(),
                            row.acceptedAnswerId() != null, endorsedBy,
                            myVotes.getOrDefault("ANSWER:" + a.id(), 0),
                            actorOrNull != null && actorOrNull.is(a.authorId()),
                            a.createdAt(), a.updatedAt(), answerComments);
                })
                .toList();

        return new QuestionDetailView(row.id(), row.title(), row.body(), renderer.toSafeHtml(row.body()), row.tags(),
                course, author, row.score(), row.answerCount(), row.viewCount() + 1, row.acceptedAnswerId(),
                questionVote, row.createdAt(), row.updatedAt(), permissions, questionComments, answers);
    }

    public List<TagView> popularTags(int limit) {
        return readModel.popularTags(Math.min(Math.max(limit, 1), MAX_TAGS));
    }
}
