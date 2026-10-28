# CampusOverflow 聚合识别清单

> 所属实验：Lab 3 领域驱动设计事件风暴（Sprint 1）
> 聚合 = 强一致的事务边界；聚合内强一致，跨聚合最终一致。

---

## 一、聚合总览

| 聚合 | 聚合根 | 实体 | 值对象 | 限界上下文 |
| --- | --- | --- | --- | --- |
| User | User | — | CampusId、Password | Identity |
| Course | Course | CourseMember | — | Discovery |
| Question | Question | Answer | Vote、Tag、QuestionTitle | Q&A |
| Reputation Ledger | ReputationLedger | ReputationEntry | ReputationPoints | Reputation |

---

## 二、聚合详细定义

### 1. User（用户聚合）— Identity
- **聚合根**：`User`
- **标识**：`UserId`（共享内核）
- **值对象**：`CampusId`（学号/工号）、`Password`（哈希存储）
- **关键不变式**：
  - 校园学号在系统内唯一；
  - 密码必须满足强度，且只存哈希；
  - 只有未删除用户可登录。

### 2. Course（课程聚合）— Discovery
- **聚合根**：`Course`
- **实体**：`CourseMember`（成员关系）
- **标识**：`CourseId`
- **关键不变式**：
  - 同一用户在同一课程内成员关系唯一（不可重复加入）；
  - 课程关闭后不可加入。

### 3. Question（问题聚合）— Q&A（核心域）
- **聚合根**：`Question`
- **实体**：`Answer`（归属于 Question，在聚合内管理）
- **值对象**：`Vote`（投票）、`Tag`（标签）、`QuestionTitle`（标题）
- **关键不变式**：
  - 标题非空且长度合法；
  - 问题必须归属于一门课程；
  - **每个问题最多采纳一个回答**（采纳后不可再采纳）；
  - 问题被软删除/关闭后，不允许新增回答或投票；
  - 同一用户对同一问题/回答**只能投一票**，可改票、撤回；
  - 只有问题作者可编辑/删除问题，只有回答作者可编辑/删除回答。

### 4. Reputation Ledger（声誉账本聚合）— Reputation
- **聚合根**：`ReputationLedger`
- **实体**：`ReputationEntry`（声誉流水）
- **值对象**：`ReputationPoints`
- **关键不变式**：
  - 声誉总分不允许为负（由流水累加保证）；
  - 同一来源事件（如某回答被采纳）**只能入账一次**（幂等）。

---

## 三、一致性边界

| 边界 | 一致性策略 |
| --- | --- |
| 单个聚合内部 | 强一致（一次事务），通过聚合根方法保护不变式 |
| Question ↔ Answer | Answer 作为 Question 聚合内实体，采纳/回答在同一聚合内强一致 |
| Q&A ↔ Reputation | 跨聚合/跨上下文，通过 `AnswerAccepted` 领域事件最终一致 |
| Question ↔ User/Course | 仅持有 `UserId`/`CourseId` 引用，不直接持有对方对象 |

---

## 四、设计要点

1. 聚合根是唯一外部入口，Setter 私有化，通过工厂方法与业务方法修改状态。
2. 聚合之间通过 ID 引用，避免对象图无限加载与跨聚合事务。
3. 采纳唯一性、投票唯一性、声誉幂等是三条最关键的不变式，需有单元测试覆盖。
4. 代码实现见 Lab 4 的 `co-qa-domain` 与 `co-reputation-domain`。
