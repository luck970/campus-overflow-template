---
AIGC:
    Label: "1"
    ContentProducer: 001191440300708461136T1XGW3
    ProduceID: 31e7cf1207b8d082b268a2ac107cd18b_66ba22d0d7ec11f1b2d8525400393706
    ReservedCode1: 4gbpRl5hy/0ncT2Pnf/HBF7trqlc3hAQl16ulnrGVPyA4EujVwiiGj1csTgk/VD2QIPLWu6S625Af9nqX/+GnkXyj9IKmbQBTYbe3fWrUCX1anLhSv9vLGE+oSc3XKKct21DrfkEK8OlHSt0kC9IEB744QqlhKpb199/iKcAf/WH5bpMTLG8vFTieew=
    ContentPropagator: 001191440300708461136T1XGW3
    PropagateID: 31e7cf1207b8d082b268a2ac107cd18b_66ba22d0d7ec11f1b2d8525400393706
    ReservedCode2: 4gbpRl5hy/0ncT2Pnf/HBF7trqlc3hAQl16ulnrGVPyA4EujVwiiGj1csTgk/VD2QIPLWu6S625Af9nqX/+GnkXyj9IKmbQBTYbe3fWrUCX1anLhSv9vLGE+oSc3XKKct21DrfkEK8OlHSt0kC9IEB744QqlhKpb199/iKcAf/WH5bpMTLG8vFTieew=
---

# Lab 3 事件风暴工作坊 · 会议纪要

> 记录人：SM（Flagg32）　团队：Overflow Crew
> 会议性质：Lab 3 事件风暴工作坊（Sprint 1）
> 会议日期：2026-11-__　时段：__:__ – __:__　地点：实验室 / ________
> 领域专家：________（教师 / 助教）

---

## 一、参与情况

| 姓名 | 角色 | 出席 | 备注 |
| --- | --- | --- | --- |
| Flagg32 | SM（主持） | ☐ | 流程、时间盒、纪要与交付归档 |
| ________ | PO | ☐ | 业务主线与优先级裁决 |
| 江思达 | Developer | ☐ | 聚合边界与不变式 |
| 何欣怡 | Developer | ☐ | 聚合边界与不变式 |
| 柯博 | Developer | ☐ | 聚合边界与不变式 |
| ________ | 领域专家 | ☐ | 争议澄清 |

---

## 二、时间盒执行记录

| 阶段 | 计划 | 实际用时 | 执行情况 |
| --- | --- | --- | --- |
| Step 1 Big Picture | 40 min | ____ min | ☐ 按时收口　☐ 超时 ____ min |
| Step 2 Process Modeling | 30 min | ____ min | ☐ 按时收口　☐ 超时 ____ min |
| Step 3 Software Design / 聚合 | 30 min | ____ min | ☐ 按时收口　☐ 超时 ____ min |
| Step 4 上下文映射与术语表 | 20 min | ____ min | ☐ 按时收口　☐ 超时 ____ min |

---

## 三、建模结论（对应 4 项提交物）

### 1. Big Picture：领域事件

- 共产出 **19 个领域事件**，覆盖五段业务主线：身份与访问（3）、课程与发现（3）、提问（4）、回答与互动（7）、声誉（2）。
- 全部事件为"过去时 + 业务术语"，无抽象事件（现场退回的抽象卡已改写，见第五节）。
- 系统边界事件：`User Registered`、`Course Joined`、`Question Posted`、`Answer Accepted`、`Reputation Awarded`。

### 2. Process Modeling：命令 / 角色 / 外部系统

- 命令与角色一一对应关系见 `event-storming.md` 第二节，共 10 组角色-命令-事件链路。
- 识别外部系统 3 个：校园身份提供方（OAuth / 学号目录）、邮件/通知服务、课程目录数据源；另标记系统内定时任务（提醒摘要）。

### 3. Software Design：聚合与不变式

| 聚合 | 聚合根 | 所在上下文 | 关键不变式 |
| --- | --- | --- | --- |
| User | User | Identity | 校园学号唯一；密码仅存哈希 |
| Course | Course | Discovery | 同一课程内成员关系唯一；课程关闭后不可加入 |
| Question | Question（含 Answer 实体） | Q&A | **每问题至多一个被采纳回答**；**一人一票可改可撤**；软删除后不可新增回答/投票 |
| Reputation Ledger | ReputationLedger | Reputation | **同一来源事件仅入账一次（幂等）**；总分不为负 |

- 一致性边界：聚合内强一致（单事务）；Q&A → Reputation 通过 `AnswerAccepted` 领域事件最终一致。
- 读模型 3 个：课程问题列表、问题详情、个人声誉摘要与贡献记录。

### 4. 上下文映射与术语表

- 限界上下文 **4 个 + 共享内核 1 个**：Identity / Discovery / Q&A（核心域）/ Reputation / Shared Kernel。
- 关系：Identity→Discovery、Identity→Q&A、Discovery→Q&A 为客户-供应商（U/D）；Q&A→Reputation 为异步领域事件 + ACL；共享内核仅共享 UserId、CourseId 与事件契约。
- 术语表 v1.0 定稿 **50 条**，中英对照，见 `glossary.md`。

---

## 四、热点（Hot Spot）与裁决

| # | 热点 | 处理结论 | 落地位置 |
| --- | --- | --- | --- |
| 1 | 重复采纳如何防止 | 采纳唯一性放进 Question 聚合内强一致，采纳后其余回答不能再被采纳 | `aggregates.md` Question 不变式 |
| 2 | 问题删除后回答/投票如何级联 | 采用软删除：问题不可见但数据保留，关闭/软删除后禁止新增回答与投票 | `aggregates.md` Question 不变式 |
| 3 | 声誉跨上下文一致性 | Reputation 订阅 `AnswerAccepted` 事件异步入账，用来源事件 ID 做幂等 | `aggregates.md` Reputation 不变式 + `context-map.md` ACL |

---

## 五、术语定名分歧与裁决

| 分歧词 | 候选说法 | 裁决采用 | 理由 |
| --- | --- | --- | --- |
| 采纳 / 接受 | Accept / Accept Answer | **采纳回答（Accept Answer）** | 与 Sprint 1 用户故事 US-012 用词保持一致 |
| 赞 / 踩 | Like / Dislike | **赞成票 / 反对票（Upvote / Downvote）** | 与 Vote 值对象语义统一，避免与"点赞"混淆 |
| 问题 / 主题 | Topic / Thread | **问题（Question）** | 与 Backlog 与前端文案一致 |
| 已解决 | Resolved / Solved | **已解决问题（Solved Question）** | 与"已采纳回答"状态联动，避免一个概念两个词 |
| 声誉积分 | Score / Points / Reputation | **声誉（Reputation）/ 声誉流水（Reputation Entry）** | 数值明细统一叫流水，总分只在聚合根上暴露 |

---

## 六、现场纠偏记录（SM 主持动作）

| 现象 | 纠偏动作 | 结果 |
| --- | --- | --- |
| 出现抽象事件卡（如"用户操作完成"） | 当场退回，要求换成业务动作 | 改写为具体领域事件，共退回 __ 张 |
| 出现按技术切分上下文的倾向（数据库/前端） | 立即纠正为按业务能力划分 | 重新分组，最终 4 个上下文 |
| 单人贴卡过多 | 要求未贴卡成员补足，每人每段 ≥2 张 | 4 人全部参与 |

---

## 七、待确认事项

| # | 事项 | 责任人 | 期限 |
| --- | --- | --- | --- |
| 1 | 现场照片/白板导出图替换 `event-storming-board.png` | SM | 提交前 |
| 2 | `context-map.md` 中引用的 `docs/adr/ADR-002-package-structure.md` 与仓库实际 ADR 文件编号对齐 | SM | 提交前 |
| 3 | 术语表与 `product-backlog.md`、`stories/*.feature` 用词做一次全文比对 | PO | 提交前 |
| 4 | 聚合不变式三条（采纳唯一、投票唯一、声誉幂等）转为 Lab 4 单元测试项 | Developer | Lab 4 开始前 |

---

## 八、全员确认（统一语言认同度，评分项 20%）

> 术语表 v1.0 与本文第三节建模结论，已由全体成员过目并确认。

| 姓名 | 角色 | 确认签字 | 日期 |
| --- | --- | --- | --- |
| Flagg32 | SM | | |
| ________ | PO | | |
| 江思达 | Developer | | |
| 何欣怡 | Developer | | |
| 柯博 | Developer | | |

---

## 九、过程反思（供 Lab 6 回顾会）

1. 时间盒是否守住：____________
2. 参与度是否均衡：____________
3. 下次工作坊需要改进的一点：____________
*（内容由AI生成，仅供参考）*
