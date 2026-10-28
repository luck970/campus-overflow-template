package com.campusoverflow.reputation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campusoverflow.identity.api.CourseSummary;
import com.campusoverflow.identity.api.IdentityApi;
import com.campusoverflow.identity.api.UserSummary;
import com.campusoverflow.reputation.domain.BadgeAward;
import com.campusoverflow.reputation.domain.BadgeType;
import com.campusoverflow.reputation.domain.LedgerEntry;
import com.campusoverflow.reputation.domain.ReputationAccount;
import com.campusoverflow.reputation.domain.ReputationReason;
import com.campusoverflow.reputation.domain.ReputationAccountRepository;
import com.campusoverflow.reputation.domain.LedgerRepository;
import com.campusoverflow.reputation.domain.BadgeRepository;
import com.campusoverflow.shared.domain.ForbiddenException;
import com.campusoverflow.shared.domain.NotFoundException;
import com.campusoverflow.shared.page.PageRequest;
import com.campusoverflow.shared.page.PageResult;
import com.campusoverflow.shared.security.Actor;
import com.campusoverflow.shared.security.Role;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 声誉查询用例测试：验证档案隐私投影、徽章与流水权限。 */
class ReputationQueryServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-08T06:00:00Z");

    private final InMemoryAccounts accounts = new InMemoryAccounts();
    private final InMemoryLedger ledger = new InMemoryLedger();
    private final InMemoryBadges badges = new InMemoryBadges();
    private final FakeIdentity identity = new FakeIdentity()
            .withUser(1, "小明", Role.STUDENT)
            .withUser(2, "小红", Role.STUDENT)
            .withUser(9, "管理员", Role.ADMIN);

    private ReputationQueryService service;
    private final Actor ming = new Actor(1, "小明", Role.STUDENT);
    private final Actor hong = new Actor(2, "小红", Role.STUDENT);
    private final Actor admin = new Actor(9, "管理员", Role.ADMIN);

    @BeforeEach
    void setUp() {
        service = new ReputationQueryService(accounts, ledger, badges, identity);
    }

    @Test
    @DisplayName("档案：本人查看时返回声誉、积分与徽章")
    void profileOwnViewShowsPointsAndBadges() {
        accounts.add(account(1, 150, 80, 20));
        badges.award(new BadgeAward(1, BadgeType.REPUTATION_100, NOW));

        ReputationProfileView p = service.profile(1, ming);

        assertThat(p.userId()).isEqualTo(1);
        assertThat(p.displayName()).isEqualTo("小明");
        assertThat(p.reputation()).isEqualTo(150);
        assertThat(p.availablePoints()).isEqualTo(80);
        assertThat(p.frozenPoints()).isEqualTo(20);
        assertThat(p.badges()).hasSize(1);
        assertThat(p.badges().get(0).code()).isEqualTo("REPUTATION_100");
        assertThat(p.badges().get(0).name()).isEqualTo("小有名气");
        assertThat(p.badges().get(0).description()).isEqualTo("声誉达到 100");
        assertThat(p.badges().get(0).awardedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("档案：他人或匿名查看时积分不可见")
    void profileOtherViewHidesPoints() {
        accounts.add(account(1, 150, 80, 20));

        ReputationProfileView other = service.profile(1, hong);
        assertThat(other.reputation()).isEqualTo(150);
        assertThat(other.availablePoints()).isNull();
        assertThat(other.frozenPoints()).isNull();

        ReputationProfileView anon = service.profile(1, null);
        assertThat(anon.availablePoints()).isNull();
        assertThat(anon.frozenPoints()).isNull();
    }

    @Test
    @DisplayName("档案：账户不存在时抛出 NotFoundException")
    void profileUnknownAccountThrows() {
        assertThatThrownBy(() -> service.profile(999L, ming))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("流水：本人查看时返回分页流水")
    void ledgerOwnViewReturnsPage() {
        accounts.add(account(1, 10, 10, 0));
        ledger.append(LedgerEntry.of(1, "evt-1", ReputationReason.ANSWER_ACCEPTED, 10, "ANSWER", 5, NOW));

        PageResult<LedgerEntryView> r = service.ledger(1, ming, new PageRequest(1, 20));

        assertThat(r.total()).isEqualTo(1);
        LedgerEntryView e = r.items().get(0);
        assertThat(e.reason()).isEqualTo("ANSWER_ACCEPTED");
        assertThat(e.reasonLabel()).isEqualTo("回答被采纳");
        assertThat(e.delta()).isEqualTo(10);
        assertThat(e.refType()).isEqualTo("ANSWER");
        assertThat(e.refId()).isEqualTo(5);
        assertThat(e.createdAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("流水：他人查看时抛出 ForbiddenException")
    void ledgerOtherViewForbidden() {
        accounts.add(account(1, 10, 10, 0));

        assertThatThrownBy(() -> service.ledger(1, hong, new PageRequest(1, 20)))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("流水：管理员可以查看任意用户流水")
    void ledgerAdminAllowed() {
        accounts.add(account(1, 10, 10, 0));
        ledger.append(LedgerEntry.of(1, "evt-1", ReputationReason.QUESTION_VOTE, 5, "QUESTION", 8, NOW));

        PageResult<LedgerEntryView> r = service.ledger(1, admin, new PageRequest(1, 20));

        assertThat(r.total()).isEqualTo(1);
        assertThat(r.items().get(0).reason()).isEqualTo("QUESTION_VOTE");
    }

    @Test
    @DisplayName("流水：账户不存在时抛出 NotFoundException")
    void ledgerUnknownAccountThrows() {
        assertThatThrownBy(() -> service.ledger(999L, ming, new PageRequest(1, 20)))
                .isInstanceOf(NotFoundException.class);
    }

    private static ReputationAccount account(long userId, int reputation, int available, int frozen) {
        return ReputationAccount.reconstitute(userId, reputation, available, frozen, 0);
    }

    // ---- fakes ----

    static class InMemoryAccounts implements ReputationAccountRepository {
        final Map<Long, ReputationAccount> store = new LinkedHashMap<>();

        void add(ReputationAccount a) {
            store.put(a.userId(), a);
        }

        @Override
        public Optional<ReputationAccount> findByUserId(long userId) {
            return Optional.ofNullable(store.get(userId));
        }

        @Override
        public List<ReputationAccount> findAllByUserId(Collection<Long> userIds) {
            return userIds.stream().map(store::get).filter(Objects::nonNull).toList();
        }

        @Override
        public List<ReputationAccount> topByReputation(int limit) {
            return store.values().stream()
                    .sorted(Comparator.comparingInt(ReputationAccount::reputation).reversed())
                    .limit(limit).toList();
        }

        @Override
        public List<ReputationAccount> findAll() {
            return List.copyOf(store.values());
        }

        @Override
        public ReputationAccount save(ReputationAccount account) {
            store.put(account.userId(), account);
            return account;
        }
    }

    static class InMemoryLedger implements LedgerRepository {
        final List<LedgerEntry> entries = new ArrayList<>();

        @Override
        public boolean exists(String eventId, long userId, ReputationReason reason) {
            return entries.stream()
                    .anyMatch(e -> e.eventId().equals(eventId) && e.reason() == reason && e.userId() == userId);
        }

        @Override
        public void append(LedgerEntry entry) {
            entries.add(new LedgerEntry((long) entries.size() + 1, entry.userId(), entry.eventId(), entry.reason(),
                    entry.delta(), entry.refType(), entry.refId(), entry.createdAt()));
        }

        @Override
        public PageResult<LedgerEntry> findByUser(long userId, PageRequest page) {
            List<LedgerEntry> all = entries.stream().filter(e -> e.userId() == userId).toList();
            int from = page.offset();
            int to = Math.min(from + page.size(), all.size());
            return new PageResult<>(from >= all.size() ? List.of() : all.subList(from, to), all.size(),
                    page.page(), page.size());
        }

        @Override
        public long countByUserAndReason(long userId, ReputationReason reason) {
            return entries.stream().filter(e -> e.userId() == userId && e.reason() == reason).count();
        }

        @Override
        public int sumPositiveVoteDeltaSince(long userId, Instant since) {
            return entries.stream()
                    .filter(e -> e.userId() == userId && e.reason().isVote() && e.delta() > 0
                            && !e.createdAt().isBefore(since))
                    .mapToInt(LedgerEntry::delta).sum();
        }

        @Override
        public Map<Long, Long> sumByUser() {
            Map<Long, Long> result = new HashMap<>();
            entries.forEach(e -> result.merge(e.userId(), (long) e.delta(), Long::sum));
            return result;
        }
    }

    static class InMemoryBadges implements BadgeRepository {
        final List<BadgeAward> awards = new ArrayList<>();

        @Override
        public boolean has(long userId, BadgeType type) {
            return awards.stream().anyMatch(a -> a.userId() == userId && a.type() == type);
        }

        @Override
        public void award(BadgeAward award) {
            awards.add(award);
        }

        @Override
        public List<BadgeAward> findByUser(long userId) {
            return awards.stream().filter(a -> a.userId() == userId).toList();
        }
    }

    static class FakeIdentity implements IdentityApi {
        final Map<Long, UserSummary> users = new HashMap<>();

        FakeIdentity withUser(long id, String name, Role role) {
            users.put(id, new UserSummary(id, name, role, true, "计算机工程学院"));
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
            return Optional.empty();
        }

        @Override
        public boolean canManageCourse(long userId, Long courseId) {
            return false;
        }
    }
}
