# CampusOverflow 统一语言术语表 v1.0（Ubiquitous Language）

> 所属实验：Lab 3 领域驱动设计事件风暴（Sprint 1）
> 全员在代码、文档、对话中统一使用以下术语，避免一词多义。

## 一、身份与访问（Identity）

| 中文 | English | 定义 |
| --- | --- | --- |
| 用户 | User | 系统中的注册账号，聚合根 |
| 校园身份 | Campus Identity | 以学号/工号为核心的校内身份 |
| 学号/工号 | Campus Id | 校园身份的唯一编号，值对象 |
| 密码 | Password | 登录凭证，仅以哈希存储 |
| 认证 | Authentication | 验证用户身份的过程 |
| 会话 | Session | 登录后保持的状态 |
| 登录 | Log In | 建立会话的动作 |
| 退出 | Log Out | 注销会话的动作 |

## 二、课程与发现（Discovery）

| 中文 | English | 定义 |
| --- | --- | --- |
| 课程 | Course | 问答归属的教学单元，聚合根 |
| 课程成员 | Course Member | 加入某课程的用户 |
| 加入课程 | Enrollment / Join Course | 建立用户与课程的成员关系 |
| 课程目录 | Course Catalog | 可加入课程的列表 |

## 三、问答（Q&A，核心域）

| 中文 | English | 定义 |
| --- | --- | --- |
| 问题 | Question | 学生提出的待解答内容，聚合根 |
| 回答 | Answer | 对问题的回复，Question 聚合内实体 |
| 问题标题 | Question Title | 问题的简短概括，值对象 |
| Markdown 正文 | Markdown Body | 支持 Markdown 的问题/回答内容 |
| 标签 | Tag | 问题分类标记，值对象 |
| 问题状态 | Question Status | 开放/已关闭/已解决 |
| 开放问题 | Open Question | 可继续回答的问题 |
| 已关闭问题 | Closed Question | 不再接受回答的问题 |
| 已解决问题 | Solved Question | 已采纳回答的问题 |
| 软删除 | Soft Delete | 标记删除但保留数据，对外不可见 |
| 采纳回答 | Accept Answer | 提问者选定一个回答为最终答案 |
| 已采纳回答 | Accepted Answer | 被提问者采纳的回答 |
| 投票 | Vote | 对内容的评价，值对象 |
| 赞成票 | Upvote | 表示认可的投票 |
| 反对票 | Downvote | 表示不认可的投票 |
| 作者 | Author | 问题或回答的创建者 |
| 提问者 | Asker | 发起问题的用户 |

## 四、声誉（Reputation）

| 中文 | English | 定义 |
| --- | --- | --- |
| 声誉 | Reputation | 衡量用户贡献价值的分值 |
| 声誉账本 | Reputation Ledger | 记录用户声誉的聚合根 |
| 声誉流水 | Reputation Entry | 单次声誉变化记录 |
| 贡献 | Contribution | 用户产生的有价值内容（回答/采纳等） |

## 五、DDD 与过程通用术语

| 中文 | English | 定义 |
| --- | --- | --- |
| 限界上下文 | Bounded Context | 模型与语言一致的业务边界 |
| 聚合 | Aggregate | 强一致的对象簇 |
| 聚合根 | Aggregate Root | 聚合的唯一外部入口实体 |
| 实体 | Entity | 有唯一标识的对象 |
| 值对象 | Value Object | 以属性定义、无唯一标识的对象 |
| 领域事件 | Domain Event | 领域中已发生的事实（过去时） |
| 仓储 | Repository | 聚合的持久化抽象 |
| 防腐层 | Anti-Corruption Layer (ACL) | 隔离外部模型的转换层 |
| 上下文映射 | Context Map | 限界上下文之间关系的视图 |
| 统一语言 | Ubiquitous Language | 团队共享的领域语言 |
| 核心域 | Core Domain | 最具业务价值、重点投入的领域 |
| 读模型 | Read Model | 为查询优化的视图模型 |
| 走骨架 | Walking Skeleton | 贯穿全链路的最薄可运行实现 |
| 产品待办列表 | Product Backlog | 按优先级排序的全部需求 |
| 用户故事 | User Story | 以用户视角描述的需求 |
| 故事点 | Story Point | 工作量相对估算单位 |
| 完成的定义 | Definition of Done (DoD) | 判定工作完成的标准 |

---

> 本术语表共 50 个术语，随项目演进在回顾会补充修订。
