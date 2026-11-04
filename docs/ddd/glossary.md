# CampusOverflow 统一语言术语表 v1.0（Ubiquitous Language）

> 所属实验：**Lab 3 领域驱动设计事件风暴**（第 5 周 / Sprint 1）
> 适用范围：需求文档、领域模型、代码标识符、接口契约、UI 文案、团队沟通——**五处必须用同一个词**。
> 版本：v1.0　维护人：PO（唐志烽）　评审：全员　下次修订：Sprint 3 回顾会
> 本表已与 `backend/` 代码中的类名、枚举值、字段名逐一核对，可直接作为命名依据。

---

## 一、身份与课程（Identity 上下文）

| 中文 | English | 代码标识 | 定义 |
| --- | --- | --- | --- |
| 用户 | User | `User` / `userId` | 系统中的注册账号，**聚合根**；一个自然人一个账号 |
| 校园身份 | Campus Identity | — | 以学号/工号为核心的校内实名身份，来源为校园统一身份认证 |
| 学号/工号 | Campus Id | `campusId` | 校园身份唯一编号，**值对象**，全校唯一 |
| 密码哈希 | Password Hash | `Password` | 登录凭证，**只存不可逆哈希**，任何日志不得出现明文 |
| 用户状态 | User Status | `UserStatus` | `ACTIVE`（可登录）/ `DISABLED`（禁用） |
| 实名认证 | Verification | `UserVerified` | 管理员核验学号真实性后置为已认证 |
| 角色（系统级） | Role | `Role` | 系统级身份：`STUDENT` / `TA` / `TEACHER` / `ADMIN` |
| 注册 | Register | `RegisterUserCommand` | 访客创建账号，产生 `UserRegistered` |
| 登录 | Log In | `LogIn` | 建立服务端会话（Redis），非 JWT |
| 退出 | Log Out | `LogOut` | 使会话失效 |
| 会话 | Session | — | 登录后保持的认证状态，存于 Redis |
| 登录限流 | Login Throttle | `LoginThrottle` | 连续失败即锁定，防暴力破解 |
| 课程 | Course | `Course` / `courseId` | 课程专属问答区的载体，**聚合根** |
| 课程代码 | Course Code | `code` | 2–20 位字母/数字/连字符，入库大写 |
| 学期 | Academic Term | `term` | 如 `2026-FALL`；课程代码在同一学期内唯一 |
| 课程成员 | Course Member | `CourseMember` | 用户与课程的成员关系，**独立聚合** |
| 课程内角色 | Course Role | `CourseRole` | `STUDENT` / `TA` / `TEACHER`，决定课程内权限 |
| 加入课程 | Join Course | `CourseJoined` | 建立成员关系，重复加入幂等 |
| 课程管理者 | Course Manager | `canManage()` | 该课程的 `TA` 或 `TEACHER`；可采纳、认证、删除任意内容、发起悬赏 |
| 创建课程 | Create Course | `CourseCreated` | 仅教师或管理员可执行 |

---

## 二、问答（Q&A 上下文 · 核心域）

| 中文 | English | 代码标识 | 定义 |
| --- | --- | --- | --- |
| 问题 | Question | `Question` / `questionId` | 学生在某课程下提出的待解答内容，**聚合根**。**不得叫"帖子""主题"** |
| 回答 | Answer | `Answer` / `answerId` | 对问题的正式解答，**独立聚合根**。与"评论"严格区分 |
| 标题 | Title | `Title` | 问题的一句话概括，**值对象** |
| 正文 | Body / Markdown Body | `MarkdownBody` | 支持 Markdown 的问题/回答正文，**值对象** |
| 标签 | Tag | `Tag` / `TagName` | 问题分类标记，**值对象** |
| 标签集合 | Tags | `Tags` | 去重后 **1–5 个**标签的集合，**值对象** |
| 问题状态 | Question Status | — | 开放（可作答）/ 已解决（已采纳）/ 已删除（软删除） |
| 开放问题 | Open Question | `ensureOpenForAnswers()` | 未被删除、可继续作答的问题 |
| 已解决问题 | Solved Question | `acceptedAnswerId != null` | 已采纳唯一回答的问题 |
| 采纳回答 | Accept Answer | `Question.accept()` | 提问者或课程管理者选定唯一最佳回答，**每问题仅一次、不可更改** |
| 已采纳回答 | Accepted Answer | `acceptedAnswerId` | 被采纳的那一条回答；**唯一事实来源在 Question 上** |
| 教师认证 | Endorsement | `Answer.endorse()` | 课程教师/助教为优质回答盖章，每回答仅一次 |
| 投票 | Vote | `Vote` / `voteId` | 对问题或回答的单项评价，**聚合根**，取值仅 +1 / −1 |
| 赞成票 | Upvote | `value = 1` | 表示认可的投票 |
| 反对票 | Downvote | `value = -1` | 表示不认可的投票 |
| 改投 | Change Vote | `Vote.change()` | 变更已有投票方向，按「新效果 − 旧效果」结算声誉 |
| 撤回投票 | Retract Vote | `VoteRetracted` | 删除投票记录，产生负向声誉流水 |
| 投票目标类型 | Target Type | `TargetType` | `QUESTION` / `ANSWER`，决定投票与评论指向哪种内容 |
| 得分 | Score | `score` | 内容净票数，**反规范化计数器**，由仓储原子 SQL 维护 |
| 回答数 | Answer Count | `answerCount` | 问题的回答数量，**反规范化计数器** |
| 浏览量 | View Count | `viewCount` | 问题被浏览次数，**反规范化计数器** |
| 评论 | Comment | `Comment` / `commentId` | 挂在问题或回答下的短讨论，**聚合根**。**不得与"回答"混用** |
| 回复 | Reply | `parentId` | 对某条评论的回复，**仅允许一层** |
| 作者 | Author | `authorId` | 内容创建者；可编辑/删除自己的内容 |
| 提问者 | Asker | `Question.authorId` | 发起问题的人；拥有采纳权 |
| 软删除 | Soft Delete | `deletedAt` / `ContentDeleted` | 打删除标记、对外不可见、数据保留 |
| 内容删除事件 | Content Deleted | `ContentDeletedEvent` | 问题或回答被软删除时发布的集成事件 |

---

## 三、声誉与激励（Reputation 上下文）

| 中文 | English | 代码标识 | 定义 |
| --- | --- | --- | --- |
| 声誉 | Reputation | `reputation` | 衡量用户贡献价值的**只读荣誉值**，由账本累加得出。**不是积分** |
| 声誉账户 | Reputation Account | `ReputationAccount` | 用户声誉与积分的账户，**聚合根**，一用户一账户 |
| 声誉流水 | Ledger Entry | `LedgerEntry` | 单条声誉变化记录，**只追加、不可修改** |
| 声誉原因 | Reputation Reason | `ReputationReason` | `ANSWER_ACCEPTED` / `QUESTION_VOTE` / `ANSWER_VOTE` / `BOUNTY_AWARDED` |
| 声誉规则手册 | Rulebook | `ReputationRulebook` | 规则集中处：采纳 +15、被赞 +10、被踩 −2、每日正向上限 200 |
| 账本对账 | Reconciliation | `ReconciliationPassed` | 校验「账户声誉 = 流水累加」，不一致即告警（FF-8） |
| 积分 | Points | `points` | 可用于发起悬赏的"货币"，**与声誉是两种不同的量** |
| 可用积分 | Available Points | `availablePoints` | 当前可自由支配的积分 |
| 冻结积分 | Frozen Points | `frozenPoints` | 已锁入悬赏、结算或到期后释放的积分 |
| 悬赏 | Bounty | `Bounty` / `bountyId` | 为问题追加积分奖励的机制，**聚合根** |
| 悬赏状态 | Bounty Status | `BountyStatus` | `OPEN` → `AWARDED` / `EXPIRED`，**终态不可变** |
| 悬赏发起人 | Sponsor | `sponsorId` | 出资发起悬赏的课程管理者；**不得赢得自己的悬赏** |
| 悬赏结算 | Award Bounty | `Bounty.award()` | 问题被采纳时把冻结积分转给答主，**仅一次** |
| 悬赏过期 | Expire Bounty | `BountyExpired` | 到期未结算，释放冻结积分 |
| 徽章 | Badge | `BadgeAward` / `BadgeType` | 达成里程碑后授予的荣誉标记，同一徽章仅授予一次 |
| 徽章策略 | Badge Policy | `BadgePolicy` | 按累计指标判定应授予哪些徽章 |
| 排行榜 | Leaderboard | `LeaderboardService` | 按日/周/学期聚合的声誉排名（**读模型**） |
| 每日上限 | Daily Cap | `DAILY_VOTE_CAP = 200` | 反作弊：单日（UTC）因被投票获得的正向声誉上限 |

### 六类徽章（统一译名）

| 代码 | 中文名 | 达成条件 |
| --- | --- | --- |
| `FIRST_ANSWER` | 初来乍到 | 提交第一个回答 |
| `FIRST_ACCEPTED` | 初露锋芒 | 第一个回答被采纳 |
| `HELPER_10` | 解惑达人 | 累计 10 个回答被采纳 |
| `TEACHER_ENDORSED` | 名师认可 | 回答获得教师认证 |
| `REPUTATION_100` | 小有名气 | 声誉达到 100 |
| `REPUTATION_1000` | 德高望重 | 声誉达到 1000 |

---

## 四、发现（Discovery 上下文）

| 中文 | English | 代码标识 | 定义 |
| --- | --- | --- | --- |
| 通知 | Notification | `Notification` | 面向单个接收人的站内消息，**聚合根** |
| 通知类型 | Notification Type | `NotificationType` | 被回答 / 被采纳 / 被认证 / 被 @ / 获奖 / 悬赏结算 |
| 未读计数 | Unread Count | `/notifications/unread-count` | 顶栏红点数据（**读模型**） |
| 提及 | Mention | `MentionParser` | `@昵称` 解析，触发定向通知 |
| 搜索索引 | Search Index | `SearchIndex` / `SearchDocument` | 基于 MySQL ngram 的中文全文索引（**读模型**） |
| 相关问题 | Related Question | `RelatedQuestionPolicy` | 详情页的相似问题推荐（**读模型**） |

---

## 五、DDD 与过程术语

| 中文 | English | 定义 |
| --- | --- | --- |
| 限界上下文 | Bounded Context | 模型与语言保持一致的业务边界；边界内一个词只有一个含义 |
| 子域 | Subdomain | 问题空间的业务划分，与限界上下文（解决方案空间）区分 |
| 核心域 | Core Domain | 最具差异化竞争力、投入最强的子域（本项目为 Q&A） |
| 支撑子域 | Supporting Subdomain | 业务重要但可复用现成方案（Reputation、Identity） |
| 通用子域 | Generic Subdomain | 业界成熟能力，优先采购/复用（Discovery） |
| 聚合 | Aggregate | 强一致的对象簇，一个事务只改一个聚合 |
| 聚合根 | Aggregate Root | 聚合的唯一外部入口实体，保护不变式 |
| 实体 | Entity | 有唯一标识、生命周期内标识不变的对象 |
| 值对象 | Value Object | 由属性定义、无独立标识、不可变的对象（`Title`、`Tags`、`Vote` 值） |
| 不变式 | Invariant | 任何时刻都必须成立的业务规则，由聚合根方法守护 |
| 领域事件 | Domain Event | 领域中**已经发生**的事实，用过去时命名 |
| 集成事件 | Integration Event | 跨上下文发布的事件契约，位于 `*-api/event` 包 |
| 命令 | Command | 表达意图的祈使动作，可能被拒绝 |
| 发布语言 | Published Language | 上下文对外暴露的只读 API 与事件契约（`*-api` 包），FF-5 强制其零业务依赖 |
| 防腐层 | Anti-Corruption Layer (ACL) | 隔离外部模型的翻译层，防止外部概念污染领域 |
| 上下文映射 | Context Map | 限界上下文之间关系的全局视图 |
| 共享内核 | Shared Kernel | 多个上下文共同拥有的最小稳定模型子集，变更需全员同意 |
| 客户-供应商 | Customer-Supplier (U/D) | 下游是上游的客户，上游需照顾下游需求 |
| 顺从者 | Conformist | 下游完全遵从上游模型，不做翻译 |
| 伙伴关系 | Partnership | 上下游互相依赖、必须同步演进（**本项目未采用**，理由见 context-map.md） |
| 仓储 | Repository | 聚合的持久化抽象，接口在 domain 层、实现于 infrastructure 层 |
| 读模型 | Read Model | 为查询优化的投影视图，可反规范化、可重建 |
| 走骨架 | Walking Skeleton | 贯穿全链路的最薄可运行实现，Sprint 1 的目标形态 |
| 事务性发件箱 | Transactional Outbox | 业务数据与事件同事务落库、异步中继投递的模式（ADR-002） |
| 幂等消费 | Idempotent Consumer | 重复投递同一事件不产生重复副作用 |
| 最终一致 | Eventual Consistency | 跨聚合/上下文允许短暂不一致，但最终收敛 |
| 架构适应度函数 | Architecture Fitness Function | 把架构约束写成自动化测试（本项目 FF-1 ~ FF-8） |
| 产品待办列表 | Product Backlog | 按价值排序的全部需求 |
| Sprint 待办 | Sprint Backlog | 本 Sprint 承诺的故事与任务 |
| 完成的定义 | Definition of Done (DoD) | 判定"做完了"的统一标准 |
| 故事点 | Story Point | 相对工作量的估算单位 |

---

## 六、易混淆词的强制区分（反例清单）

| ❌ 禁止说法 | ✅ 正确说法 | 为什么必须区分 |
| --- | --- | --- |
| 帖子 / 主题 / 提问帖 | **问题**（Question） | 论坛语汇会引入"版块/置顶/精华"等不属于本领域的隐含概念 |
| 回复 | 作答用 **回答**（Answer）；讨论用 **评论**（Comment） | 两者是不同的聚合，权限与声誉规则完全不同 |
| 点赞 / 顶 | **赞成票**（Upvote） | 只支持 ±1，不存在"多赞" |
| 采纳答案 | **采纳回答**（Accept Answer） | 统一动作名，避免与"答案"这一英文直译混淆 |
| 积分 = 声誉 | **积分**（可消费）与**声誉**（只读荣誉）是两种量 | 混用会导致悬赏扣分把荣誉值扣成负数 |
| 称号 / 等级 | **徽章**（Badge） | 本项目无等级体系 |
| 删除帖子 | **软删除**（Soft Delete） | 物理删除会破坏声誉流水的可审计性 |
| 用户操作完成 / 数据处理完毕 | 具体业务事件，如 **回答已提交**（`AnswerSubmitted`） | 抽象事件无法用于建模，是事件风暴的第一号陷阱 |
| 课程管理员 | **课程管理者**（Course Manager = TA/TEACHER） | 与系统级 `ADMIN` 区分 |
| 老师 / 教师 / 讲师 | 统一 **教师**（TEACHER） | UI、文档、代码三处必须一致 |

---

## 七、术语使用规约

1. **代码即文档**：类名、方法名、枚举值、字段名必须与本表 `代码标识` 列一致，评审时逐条比对。
2. **禁止同义词**：同一概念只允许一个中文名 + 一个英文名，出现第二种写法即为缺陷。
3. **新增术语须走评审**：任何新业务词先入表、后写代码，避免"代码写完再补文档"。
4. **本表是活文档**：每个 Sprint 回顾会检查一次，过期术语当场删除，不做历史累积。

> 术语共 **108 条**（含六类徽章专名，不含反例清单 10 条），远超实验要求的 30 条下限。跨上下文关系见 [context-map.md](context-map.md)，聚合与不变式见 [aggregates.md](aggregates.md)。
