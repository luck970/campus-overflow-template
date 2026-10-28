package com.campusoverflow.reputation.application;

import com.campusoverflow.identity.api.IdentityApi;
import com.campusoverflow.identity.api.UserSummary;
import com.campusoverflow.reputation.application.ReputationProfileView.BadgeView;
import com.campusoverflow.reputation.domain.BadgeAward;
import com.campusoverflow.reputation.domain.BadgeRepository;
import com.campusoverflow.reputation.domain.LedgerEntry;
import com.campusoverflow.reputation.domain.LedgerRepository;
import com.campusoverflow.reputation.domain.ReputationAccount;
import com.campusoverflow.reputation.domain.ReputationAccountRepository;
import com.campusoverflow.shared.domain.ForbiddenException;
import com.campusoverflow.shared.domain.NotFoundException;
import com.campusoverflow.shared.page.PageRequest;
import com.campusoverflow.shared.page.PageResult;
import com.campusoverflow.shared.security.Actor;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReputationQueryService {

    private final ReputationAccountRepository accounts;
    private final LedgerRepository ledger;
    private final BadgeRepository badges;
    private final IdentityApi identity;

    public ReputationQueryService(ReputationAccountRepository accounts, LedgerRepository ledger,
                                  BadgeRepository badges, IdentityApi identity) {
        this.accounts = accounts;
        this.ledger = ledger;
        this.badges = badges;
        this.identity = identity;
    }

    public ReputationProfileView profile(long userId, Actor viewerOrNull) {
        ReputationAccount account = accounts.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("ACCOUNT_NOT_FOUND", "声誉账户不存在"));
        String displayName = identity.findUser(userId).map(UserSummary::displayName).orElse("已注销用户");
        List<BadgeView> badgeViews = badges.findByUser(userId).stream()
                .map(a -> new BadgeView(a.type().name(), a.type().displayName(), a.type().description(),
                        a.awardedAt()))
                .toList();
        boolean own = viewerOrNull != null && viewerOrNull.is(userId);
        return new ReputationProfileView(userId, displayName, account.reputation(),
                own ? account.availablePoints() : null,
                own ? account.frozenPoints() : null,
                badgeViews);
    }

    /** 声誉流水属于个人数据：仅本人与管理员可查看。 */
    public PageResult<LedgerEntryView> ledger(long userId, Actor viewer, PageRequest page) {
        accounts.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("ACCOUNT_NOT_FOUND", "声誉账户不存在"));
        boolean allowed = viewer != null && (viewer.is(userId) || viewer.isAdmin());
        if (!allowed) {
            throw new ForbiddenException("FORBIDDEN", "仅本人或管理员可查看声誉流水");
        }
        PageResult<LedgerEntry> entries = ledger.findByUser(userId, page);
        List<LedgerEntryView> views = entries.items().stream()
                .map(e -> new LedgerEntryView(e.id(), e.reason().name(), e.reason().label(), e.delta(),
                        e.refType(), e.refId(), e.createdAt()))
                .toList();
        return new PageResult<>(views, entries.total(), page.page(), page.size());
    }
}
