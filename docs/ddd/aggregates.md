# CampusOverflow 聚合识别清单 v1.0

> 所属实验：**Lab 3 领域驱动设计事件风暴**（第 5 周 / Sprint 1）
> 产出方式：事件风暴 Step 3「Software Design 与聚合识别」——按**命令-事件聚类**识别聚合，再以不变式校验边界。
> 聚合的定义：**强一致的事务边界**。聚合内强一致（单事务），跨聚合一律最终一致。
> 本清单与 `backend/` 实际代码逐条对应，可作为 Lab 4 编码与单元测试的验收依据。
>
> **分工与主责（Lab 3 成员分工表）**：问答域 **A4 Question / A5 Answer / A6 Vote / A7 Comment** 四个聚合由
> **江思达（houhai6）主笔**，负责命令/角色卡片与四个聚合的根实体、不变式编号；本文件由其校对并留一次校对提交。
> 验收标准：每个聚合有根实体与不变式编号，且不变式能在 `QuestionTest` 等测试类中找到对应用例（见第五节精确映射）。

---

## 一、聚合总览

| # | 聚合（中/英） | 聚合根 | 标识 | 所在上下文 | 物理模块 | 一致性级别 |
| --- | --- | --- | --- | --- | --- | --- |
| A1 | 用户 / User | `User` | `userId` | Identity | `co-identity-domain` | 强一致 |
| A2 | 课程 / Course | `Course` | `courseId` | Identity | `co-identity-domain` | 强一致 |
| A3 | 课程成员 / CourseMember | `CourseMember` | (`courseId`,`userId`) | Identity | `co-identity-domain` | 强一致 |
| A4 | 问题 / Question | `Question` | `questionId` | Q&A（核心域） | `co-qa-domain` | 强一致 |
| A5 | 回答 / Answer | `Answer` | `answerId` | Q&A（核心域） | `co-qa-domain` | 强一致 |
| A6 | 投票 / Vote | `Vote` | `voteId` | Q&A（核心域） | `co-qa-domain` | 强一致 |
| A7 | 评论 / Comment | `Comment` | `commentId` | Q&A（核心域） | `co-qa-domain` | 强一致 |
| A8 | 标签字典 / Tag | `Tag` | `tagId` / `TagName` | Q&A（核心域） | `co-qa-domain` | 最终一致（计数） |
| A9 | 声誉账户 / ReputationAccount | `ReputationAccount` | `userId` | Reputation | `co-reputation-domain` | 强一致 |
| A10 | 声誉流水 / LedgerEntry | `LedgerEntry` | `entryId` | Reputation | `co-reputation-domain` | 只追加 |
| A11 | 悬赏 / Bounty | `Bounty` | `bountyId` | Reputation | `co-reputation-domain` | 强一致 |
| A12 | 徽章 / BadgeAward | `BadgeAward` | (`userId`,`badgeCode`) | Reputation | `co-reputation-domain` | 强一致（幂等） |
| A13 | 通知 / Notification | `Notification` | `notificationId` | Discovery | `co-discovery-domain` | 最终一致 |

> **读模型不是聚合**：`QaReadModel`、`LeaderboardService`、`SearchIndex` 均为投影视图，见第四节。

---

## 二、聚合详细定义与关键不变式

### A1 用户 / User（Identity）

| 项 | 内容 |
| --- | --- |
| 聚合根 | `User`（`co-identity-domain/.../domain/User.java`） |
| 标识 | `userId`（技术标识）、`campusId`（业务标识，学号/工号） |
| 值对象 | `CampusId`、`Password`（仅存哈希）、`UserStatus`（ACTIVE / DISABLED） |
| 仓储 | `UserRepository`（domain 层接口，infrastructure 层适配） |

**关键不变式**

| 编号 | 不变式 | 违反时行为 |
| --- | --- | --- |
| I1-1 | 学号/工号在全校范围内唯一 | 抛 `ConflictException(ID-1001)` |
| I1-2 | 密码必须满足强度策略，且**只存哈希**、不可逆 | `PasswordPolicy` 拒绝弱口令 |
| I1-3 | 仅 `ACTIVE` 状态用户可登录，禁用用户登录即失败 | `ForbiddenException` |
| I1-4 | 角色（STUDENT / TA / TEACHER / ADMIN）变更仅管理员可执行 | `ForbiddenException` |

### A2 课程 / Course（Identity）

| 项 | 内容 |
| --- | --- |
| 聚合根 | `Course` |
| 标识 | `courseId` |
| 值对象 | `term`（学期，如 `2026-FALL`）、`code`（课程代码） |
| 仓储 | `CourseRepository` |

**关键不变式**

| 编号 | 不变式 | 说明 |
| --- | --- | --- |
| I2-1 | 课程代码为 2–20 位字母/数字/连字符，入库统一大写 | 正则 `^[A-Za-z0-9-]{2,20}$` |
| I2-2 | **同一学期内课程代码唯一** | `existsByCodeAndTerm` + DB 唯一键 |
| I2-3 | 仅教师或管理员可创建课程（学生不可建课） | `ForbiddenException(ID-3002)` |
| I2-4 | 课程名称 2–50 字、学期 2–20 字 | `Guard.requireText` |

### A3 课程成员 / CourseMember（Identity）

| 项 | 内容 |
| --- | --- |
| 聚合根 | `CourseMember` |
| 标识 | 复合标识 (`courseId`,`userId`) |
| 值对象 | `CourseRole`（STUDENT / TA / TEACHER，`canManage() = TA \| TEACHER`） |

**关键不变式**

| 编号 | 不变式 | 说明 |
| --- | --- | --- |
| I3-1 | 同一用户在同一课程内**成员关系唯一**，重复加入幂等不报错 | `findMember` 先查后写 |
| I3-2 | 建课时创建者自动成为该课程的 `TEACHER` 成员 | `CourseService.create` |
| I3-3 | 只有课程管理者（TA / TEACHER）能变更他人角色 | `changeRole` 前校验调用者 |

> **边界说明**：`CourseMember` 虽由 `CourseRepository` 统一提供存取方法，但以独立对象、独立主键保存，
> 不随 `Course` 整体加载。故建模为**独立聚合、以 ID 引用 Course**，避免课程成员数量增长撑大课程聚合的锁范围。

### A4 问题 / Question（Q&A 核心域）

| 项 | 内容 |
| --- | --- |
| 聚合根 | `Question`（`co-qa-domain/.../domain/Question.java`） |
| 标识 | `questionId` |
| 值对象 | `Title`、`MarkdownBody`、`Tags`（内含 `TagName`，1–5 个且去重） |
| 外部引用 | `authorId`（→User）、`courseId`（→Course），**只存 ID，不持有对象** |
| 仓储 | `QuestionRepository` |

**关键不变式（本聚合是核心域的守护者）**

| 编号 | 不变式 | 违反时行为 |
| --- | --- | --- |
| I4-1 | 标题、正文非空且长度合法 | `BusinessRuleException(QA-1001/1002)` |
| I4-2 | 标签去重后必须为 **1–5 个** | `BusinessRuleException(QA-1005)` |
| I4-3 | **每个问题最多采纳一个回答，且采纳后不可更改** | `ConflictException(QA-1008)` |
| I4-4 | 只能采纳**属于本问题且未被删除**的回答 | `BusinessRuleException(QA-1008)` |
| I4-5 | 仅提问者或课程管理者（TA/TEACHER）可采纳 | `ForbiddenException(QA-1008)` |
| I4-6 | 仅作者或课程管理者可编辑问题 | `ForbiddenException(QA-1007)` |
| I4-7 | **问题软删除后不可编辑、不可再被回答、不可采纳** | `BusinessRuleException(QA-1009)` |
| I4-8 | 提问者只能删除**尚无回答**的问题；课程管理者可删任意问题 | `BusinessRuleException(QA-1009)` |

> **反规范化说明**：`score` / `answerCount` / `viewCount` 是**读优化计数器**，由仓储以原子 SQL 维护，
> 不属于聚合内的强一致状态（见 `Question` 类注释）。把它们留在聚合内会与投票/回答聚合形成跨聚合事务，必须放弃强一致。

### A5 回答 / Answer（Q&A 核心域）

| 项 | 内容 |
| --- | --- |
| 聚合根 | `Answer`（**独立聚合**，非 Question 的内部实体） |
| 标识 | `answerId` |
| 值对象 | `MarkdownBody` |
| 外部引用 | `questionId`、`authorId`、`endorsedBy` |

**关键不变式**

| 编号 | 不变式 | 说明 |
| --- | --- | --- |
| I5-1 | 提交回答时问题必须存在且未被删除 | `Answer.submit` 内调 `question.ensureOpenForAnswers()` |
| I5-2 | 仅作者或课程管理者可编辑/删除回答 | `ForbiddenException(QA-1007)` |
| I5-3 | 每个回答最多获得**一次**教师认证（endorsement） | `ConflictException` |
| I5-4 | 「是否被采纳」的**唯一事实来源是 `Question.acceptedAnswerId`**，Answer 不重复存储 | 避免双写不一致 |

> **为什么 Answer 独立而不是 Question 的内部实体？**
> 一个问题可有上百条回答，若作为内部实体，任何一次「提交回答」都会锁住整个问题聚合（悲观锁/乐观锁版本冲突），
> 且问题详情查询要加载全部回答。拆为独立聚合后，并发提交回答互不阻塞；采纳时由应用服务先加载 Question，
> 再传入 Answer 校验归属，仍在**单个 Question 事务内**完成采纳（I4-3/I4-4 未被削弱）。这是本次事件风暴最重要的一次边界调整。

### A6 投票 / Vote（Q&A 核心域）

| 项 | 内容 |
| --- | --- |
| 聚合根 | `Vote` |
| 标识 | `voteId`；唯一约束 (`voterId`,`targetType`,`targetId`) |
| 值对象/枚举 | `TargetType`（QUESTION / ANSWER），投票值 ∈ {+1, −1} |

**关键不变式**

| 编号 | 不变式 | 说明 |
| --- | --- | --- |
| I6-1 | 投票值只能是 **+1 或 −1** | `BusinessRuleException(QA-1006)` |
| I6-2 | **不能给自己的内容投票** | `BusinessRuleException(QA-1006)` |
| I6-3 | 同一用户对同一内容**只能有一票**（可改投，不可重复投） | 聚合内校验 + DB 唯一键兜底 |
| I6-4 | 撤销投票 = 删除该 Vote；改投以「新效果 − 旧效果」计算声誉增量 | `Vote.change` 返回旧值 |

### A7 评论 / Comment（Q&A 核心域）

| 项 | 内容 |
| --- | --- |
| 聚合根 | `Comment` |
| 标识 | `commentId` |
| 外部引用 | `targetType`+`targetId`（问题或回答）、`questionId`、`parentId` |

**关键不变式**

| 编号 | 不变式 | 说明 |
| --- | --- | --- |
| I7-1 | 评论内容 1–1000 字 | `Guard.requireText(QA-1007)` |
| I7-2 | **最多一层二级回复**：被回复的评论本身不能是回复 | `BusinessRuleException(QA-1007)` |
| I7-3 | 回复必须挂在与父评论**相同的内容目标**上 | 防止跨问题串楼 |

### A8 标签字典 / Tag（Q&A）

| 项 | 内容 |
| --- | --- |
| 聚合根 | `Tag`（标签字典条目，`TagRepository`） |
| 值对象 | `TagName`（规范化后的标签名） |
| 不变式 | 标签名规范化后唯一；使用计数（popular）为**最终一致的读模型**，不参与事务 |

### A9 声誉账户 / ReputationAccount（Reputation）

| 项 | 内容 |
| --- | --- |
| 聚合根 | `ReputationAccount` |
| 标识 | `userId`（一个用户一个账户） |
| 状态 | `reputation`（声誉值）、`availablePoints`（可用积分）、`frozenPoints`（冻结积分） |
| 领域服务 | `ReputationRulebook`（规则集中一处：采纳 +15、被赞 +10、被踩 −2、每日正向上限 200） |

**关键不变式**

| 编号 | 不变式 | 说明 |
| --- | --- | --- |
| I9-1 | **`reputation` 恒等于该用户全部账本流水 `delta` 之和** | 由对账任务（FF-8 / `admin/reputation/reconciliation`）持续验证 |
| I9-2 | 声誉只能通过 `applyReputation(delta)` 改变，且**必须先写流水再改账户** | 账户自身不是事实来源，账本才是 |
| I9-3 | **悬赏积分 = 可用 + 冻结**；冻结额不得超过可用额 | `freezePoints(901)` 在可用 700 时被拒绝 |
| I9-4 | 冻结的消耗与退还必须守恒（`spendFrozen` / `refundFrozen` 后总额不变） | 单元测试 `freezeSpendAndRefundKeepPointsConsistent` |
| I9-5 | 每日（UTC 自然日）因被投票获得的正向声誉**不超过 200** | 反作弊，`applyDailyCap` |

> 积分（points）与声誉（reputation）是**两种不同的"货币"**：声誉是只读的荣誉值，积分可用于发起悬赏。

### A10 声誉流水 / LedgerEntry（Reputation）

| 项 | 内容 |
| --- | --- |
| 聚合根 | `LedgerEntry`（**只追加**，不允许更新/删除） |
| 标识 | `entryId`；唯一键 (`eventId`,`userId`,`reason`) |
| 值对象 | `ReputationReason`（ANSWER_ACCEPTED / QUESTION_VOTE / ANSWER_VOTE / BOUNTY_AWARDED） |

**关键不变式**

| 编号 | 不变式 | 说明 |
| --- | --- | --- |
| I10-1 | 流水**只追加**，任何修改都以新增冲正流水表达 | 可审计、可回溯 |
| I10-2 | **同一来源事件对同一用户同一原因只入账一次**（幂等） | 唯一键 `uk_ledger_event` 由 DB 兜底 |

### A11 悬赏 / Bounty（Reputation）

| 项 | 内容 |
| --- | --- |
| 聚合根 | `Bounty` |
| 标识 | `bountyId`；关联 `questionId`、`sponsorId`、`answerId` |
| 状态机 | `OPEN` →（问题采纳）→ `AWARDED`；`OPEN` →（到期）→ `EXPIRED` |

**关键不变式**

| 编号 | 不变式 | 说明 |
| --- | --- | --- |
| I11-1 | 积分区间 **10–500**，悬赏期 **≤ 14 天** | `MIN_POINTS` / `MAX_POINTS` / `MAX_DAYS` |
| I11-2 | 只有具备课程管理权限者对**开放中的问题**可发起悬赏 | `Bounty.open` 前置校验 |
| I11-3 | **发起人不能赢得自己的悬赏** | `BusinessRuleException` |
| I11-4 | **结算只能发生一次**，终态（AWARDED / EXPIRED）不可再变 | `ConflictException` |
| I11-5 | 发起时冻结积分，结算或到期时释放/扣除，积分守恒 | 与 A9 的 I9-3/I9-4 联动 |

### A12 徽章 / BadgeAward（Reputation）

| 项 | 内容 |
| --- | --- |
| 聚合根 | `BadgeAward`（`record(userId, badgeType, awardedAt)`） |
| 标识 | (`userId`,`badgeCode`) |
| 领域策略 | `BadgePolicy`（阈值判定）、`BadgeType`（6 类徽章） |
| 不变式 | 同一用户同一徽章**只能授予一次**（DB 唯一键 `uk_badge` 兜底）；徽章由策略按累计指标判定，不可手工臆造 |

### A13 通知 / Notification（Discovery）

| 项 | 内容 |
| --- | --- |
| 聚合根 | `Notification` |
| 标识 | `notificationId` |
| 值对象/枚举 | `NotificationType`、`MentionParser`（@ 提及解析） |
| 不变式 | 同一接收人对**同一来源事件**不重复生成通知（幂等）；已读状态单向不可逆 |

---

## 三、一致性边界

| 边界 | 一致性策略 | 实现手段 |
| --- | --- | --- |
| 单个聚合内部 | **强一致**（一个本地事务） | 聚合根方法校验 + 乐观锁 `version` |
| Question ↔ Answer | **最终一致**（各自独立事务） | `questionId` 引用；采纳时在同一 Question 事务内校验归属 |
| Question ↔ Vote | **最终一致** | `score` 由仓储原子 SQL 自增/自减 |
| Q&A ↔ Reputation | **最终一致** | `AnswerAccepted` / `VoteCast` 等领域事件 → 事务性发件箱 → 消费端幂等入账 |
| Q&A ↔ Discovery | **最终一致** | 事件投影出通知与搜索索引 |
| Reputation ↔ Q&A | **无反向依赖** | 声誉不回调问答，避免双向耦合 |
| 跨上下文查询 | **同步只读** | 通过 `IdentityApi` / `QaQueryApi` / `ReputationApi` 发布语言，禁止跨库直读 |

**判定原则**：一次业务操作若必须"同时成立才合法"，才放进同一聚合；
只要"稍后成立也可以接受"，就拆到两个聚合用事件对齐。

---

## 四、读模型（Read Model）识别

> 读模型不是聚合，可自由反规范化、可重建，允许最终一致。

| 读模型 | 提供方 | 支撑的界面 | 数据来源 |
| --- | --- | --- | --- |
| 课程问题列表 `QaReadModel` | Q&A | 课程问答列表（标题、标签、回答数、是否已解决、得分） | 问题/回答/投票投影 |
| 问题详情视图 | Q&A + Identity | 问题详情页（含作者摘要，经 `IdentityApi` 补全） | 聚合 + 跨上下文只读接口 |
| 热门标签 | Q&A | 侧边标签云 | 标签使用计数 |
| 排行榜 `LeaderboardService` | Reputation | 声誉排行榜（日/周/学期维度） | 事件驱动的累计榜 |
| 个人声誉摘要与流水 | Reputation | 个人主页、贡献记录 | `ReputationApi` |
| 搜索索引 `SearchIndex` | Discovery | 全文检索（MySQL ngram） | `QuestionPosted` 等事件投影 |
| 相关推荐 `RelatedQuestionPolicy` | Discovery | 详情页「相关问题」 | 标签/向量相似度 |
| 未读通知计数 | Discovery | 顶栏红点 | 通知状态聚合计数 |

---

## 五、问答域不变式 → 测试方法精确映射（江思达主责，Lab 3 验收基线）

> 验收要求「不变式能在 QuestionTest 找到对应用例」。下表把问答域四个聚合（A4–A7）的每条不变式
> 逐条对到真实测试类与方法；测试均位于 `co-qa-domain/src/test`，可直接运行核对。

### A4 Question → `QuestionTest`（11 个用例，3 个嵌套分组）

| 不变式 | 测试方法（`QuestionTest`） | 断言要点 |
| --- | --- | --- |
| I4-1 标题/正文合法 | `Posting.titleMustBeBetween10And150Chars` | 短标题抛 `BusinessRuleException` |
| I4-2 标签 1–5 个 | `Posting.requiresOneToFiveTags` | 空标签与 6 个标签均被拒 |
| I4-2 标签去重/规范化 | `Posting.tagsAreNormalizedAndDeduplicated` | `#Java/java/ Spring-Boot ` → `java, spring-boot` |
| I4-2 标签字符合法 | `Posting.rejectsIllegalTagCharacters` | `<script>` 被拒，`C++` 合法 |
| I4-3 最多采纳一个 | `Accepting.askerCanAcceptAnAnswerOnce` | 第二次采纳抛 `ConflictException` |
| I4-4 回答须属本问题 | `Accepting.answerMustBelongToQuestion` | 外来回答抛 `BusinessRuleException` |
| I4-4 已删除回答不可采纳 | `Accepting.deletedAnswerCannotBeAccepted` | 软删后采纳被拒 |
| I4-5 仅提问者/管理者可采纳 | `Accepting.otherStudentsCannotAccept` / `courseTeacherCanAcceptOnBehalfOfAsker` | 路人抛 `ForbiddenException`，教师可代采纳 |
| I4-6 仅作者/管理者可编辑 | `EditingAndDeleting.onlyAuthorOrManagerCanEdit` | 路人被拒，教师可改 |
| I4-7 软删后禁答/禁采纳 | `EditingAndDeleting.deletedQuestionCannotBeAnswered` | 删除后 `Answer.submit` 被拒 |
| I4-8 有回答不可删/管理者可删 | `EditingAndDeleting.askerCannotDeleteAnsweredQuestionButManagerCan` | 提问者被拒，教师可删 |

### A5 Answer → `AnswerTest`（3 个）+ `QuestionTest`

| 不变式 | 测试方法 | 断言要点 |
| --- | --- | --- |
| I5-1 问题须开放 | `QuestionTest.EditingAndDeleting.deletedQuestionCannotBeAnswered` | 对已删除问题提交回答被拒 |
| I5-2 仅作者/管理者可编辑删除 | `AnswerTest.strangersCannotEditOrDelete` | 路人编辑/删除均抛 `ForbiddenException` |
| I5-3 认证仅一次 | `AnswerTest.onlyCourseManagerCanEndorseAndOnlyOnce` | 学生被拒，教师认证后再认证抛 `ConflictException` |
| I5-4 采纳事实来源唯一 | `QuestionTest.Accepting.askerCanAcceptAnAnswerOnce` | 只写 `Question.acceptedAnswerId`，Answer 不双写 |
| 采纳后不可删 | `AnswerTest.acceptedAnswerCannotBeDeletedByItsAuthor` | 作者删除被采纳回答被拒，教师可删 |

### A6 Vote → `VoteAndCommentTest`

| 不变式 | 测试方法 | 断言要点 |
| --- | --- | --- |
| I6-1 投票值 ±1 | `voteValueMustBePlusOrMinusOne` | 值 2 被拒；`change(-1)` 增量为 1 |
| I6-2 不自投 | `cannotVoteOnOwnContent` | 给自己投票抛异常且含「自己」 |
| I6-4 改投增量 | `voteValueMustBePlusOrMinusOne` | `Vote.change` 返回新旧差值 |

### A7 Comment → `VoteAndCommentTest`

| 不变式 | 测试方法 | 断言要点 |
| --- | --- | --- |
| I7-2 最多一层回复 | `repliesAreLimitedToOneLevelOnSameTarget` | 对回复再回复被拒 |
| I7-3 同目标挂载 | `repliesAreLimitedToOneLevelOnSameTarget` | 挂错目标（QUESTION/ANSWER 不一致）被拒 |

> 其余域（Reputation 对应 `BountyAndAccountTest`、Identity 对应 `CourseTest`/`UserTest`）由相应主责人维护，不在本主责范围内。

---

## 六、本次事件风暴的三处关键取舍

1. **Answer 从 Question 内部实体提升为独立聚合** —— 换并发吞吐，代价是采纳时需显式加载并校验归属。
2. **计数器（score/answerCount/viewCount）移出聚合强一致范围** —— 换写入性能，代价是列表页可能短暂滞后（秒级）。
3. **声誉彻底事件化 + 只追加账本** —— 换可审计与解耦，代价是需要对账任务兜底（FF-8）。

> 三处取舍均已写入 ADR（见 `campus-overflow-student/docs/adr/002-事务性发件箱.md`、`004-声誉账本.md`），
> 并在 `ArchitectureTest`（FF-1 ~ FF-6）中以自动化测试固化。
