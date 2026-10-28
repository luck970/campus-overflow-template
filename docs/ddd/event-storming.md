# CampusOverflow 事件风暴（Event Storming）全景

> 所属实验：Lab 3 领域驱动设计事件风暴（Sprint 1）
> 形式：Big Picture → Process Modeling → Software Design
> 便利贴约定：🟧 领域事件（过去时）　🟦 命令　🟨 角色　🟪 聚合　🟩 外部系统/读模型

---

## 一、Big Picture：领域事件时间线（🟧）

> 按业务流程顺序排列，事件一律使用「过去时 + 业务术语」，不接受抽象事件。

### 1. 身份与访问
- 🟧 User Registered（用户已注册）
- 🟧 User Logged In（用户已登录）
- 🟧 User Logged Out（用户已退出）

### 2. 课程与发现
- 🟧 Course Created（课程已创建）
- 🟧 Course Joined（课程已加入）
- 🟧 Course Left（课程已退出）

### 3. 提问
- 🟧 Question Posted（问题已发布）
- 🟧 Question Edited（问题已编辑）
- 🟧 Question Closed（问题已关闭）
- 🟧 Question Deleted（问题已删除，软删除）

### 4. 回答与互动
- 🟧 Answer Submitted（回答已提交）
- 🟧 Answer Edited（回答已编辑）
- 🟧 Answer Deleted（回答已删除）
- 🟧 Answer Accepted（回答已采纳）
- 🟧 Vote Cast（投票已投出）
- 🟧 Vote Changed（投票已变更）
- 🟧 Vote Retracted（投票已撤回）

### 5. 声誉
- 🟧 Reputation Awarded（声誉已授予）
- 🟧 Reputation Changed（声誉已变更）

### 热点（Hot Spot）
- ❓ 重复采纳如何防止（一致性问题）
- ❓ 问题删除后回答/投票如何级联（软删除可见性）
- ❓ 声誉最终一致性（跨上下文，通过领域事件）

---

## 二、Process Modeling：命令 → 事件（🟦 / 🟨 / 🟩）

| 🟨 角色 | 🟦 命令（动作） | 产生的 🟧 事件 | 备注 |
| --- | --- | --- | --- |
| 访客 | Register（注册） | User Registered | 校园身份 |
| 用户 | Log In（登录） | User Logged In | 学号/工号 |
| 用户 | Log Out（退出） | User Logged Out | 会话失效 |
| 教师 | Create Course（创建课程） | Course Created | 🟩 课程目录 |
| 学生 | Join Course（加入课程） | Course Joined | 幂等 |
| 学生 | Leave Course（退出课程） | Course Left | |
| 学生 | Post Question（发布问题） | Question Posted | 标题/正文/标签 |
| 学生 | Edit Question（编辑问题） | Question Edited | 仅作者 |
| 学生/教师 | Close/Delete Question | Question Closed / Deleted | 软删除 |
| 学生 | Submit Answer（提交回答） | Answer Submitted | 问题须开放 |
| 学生 | Edit/Delete Answer | Answer Edited / Deleted | 仅作者 |
| 提问者 | Accept Answer（采纳回答） | Answer Accepted | 每问题一次 |
| 学生 | Cast/Change/Retract Vote | Vote Cast/Changed/Retracted | 一人一票 |
| 系统（事件订阅者） | Award Reputation | Reputation Awarded/Changed | 监听 Answer Accepted |

### 外部系统（🟩）
- 🟩 校园身份提供方（OAuth / 学号目录）
- 🟩 邮件/通知服务
- 🟩 课程目录数据源

---

## 三、Software Design：命令-事件聚类为聚合（🟪）

| 聚合 | 相关命令 | 相关事件 | 所属限界上下文 |
| --- | --- | --- | --- |
| User（用户） | Register / Log In / Log Out | User Registered / Logged In / Logged Out | Identity 身份 |
| Course（课程） | Create / Join / Leave | Course Created / Joined / Left | Discovery 课程发现 |
| Question（问题） | Post / Edit / Close / Delete | Question Posted / Edited / Closed / Deleted | Q&A 问答 |
| Answer（回答） | Submit / Edit / Delete / Accept | Answer Submitted / Edited / Deleted / Accepted | Q&A 问答 |
| Vote（投票） | Cast / Change / Retract | Vote Cast / Changed / Retracted | Q&A 问答 |
| Reputation Ledger（声誉账本） | Award | Reputation Awarded / Changed | Reputation 声誉 |

### 读模型（Read Model）
- 课程问题列表（标题、标签、回答数、是否已解决）
- 问题详情（正文、标签、回答列表、采纳状态）
- 个人声誉摘要与贡献记录

---

## 四、关键结论

1. 事件全部使用业务语言，可直接映射为领域事件类（如 `QuestionPosted`）。
2. 跨上下文的声誉更新通过领域事件异步处理，保证最终一致性。
3. 问答上下文是核心域，聚合内强一致；声誉等跨上下文采用最终一致。
4. 聚合边界、不变式详见 [aggregates.md](aggregates.md)，上下文关系详见 [context-map.md](context-map.md)。

> 现场全景照片/数字白板截图可在使用 Miro/Mural/飞书白板按上述便利贴还原后导出，作为提交物第 1 项。
