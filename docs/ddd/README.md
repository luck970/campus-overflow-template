# Lab 3 · 领域驱动设计事件风暴 —— 交付说明

> **课程**：《软件工程》· CampusOverflow 贯穿项目
> **实验**：Lab 3 领域驱动设计事件风暴（2 学时 · 第 5 周 · Sprint 1）
> **团队**：第 5 组　**PO**：唐志烽（[@luck970](https://github.com/luck970)）　**SM**：左燚之（[@Flagg32](https://github.com/Flagg32)）
> **Developer**：江思达（[@houhai6](https://github.com/houhai6)）、柯博（[@Ke-1226](https://github.com/Ke-1226)）、何心怡（[@hehe309](https://github.com/hehe309)）
> **仓库**：https://github.com/luck970/campus-overflow-template
> **状态**：✅ 四项提交物齐备，并与 `backend/` 代码逐条对齐

---

## 一、提交物对照（按实验要求逐项）

| # | 实验要求的提交物 | 本仓库文件 | 说明 |
| --- | --- | --- | --- |
| 1 | 事件风暴现场全景照片（墙面 / 数字白板截图） | [`event-storming-board.png`](event-storming-board.png) | 数字白板导出。6 条泳道 + 30 组「角色→命令→事件」，底部 8 条已决热点。**若教师要求现场照片，需在实验课现场补拍**；本图为同一内容的可核对版本，每张便利贴的文字与 `event-storming.md` 一一对应 |
| 2 | 上下文映射图（Context Map）PNG | [`context-map.png`](context-map.png) | 4 个限界上下文 + 共享内核 + 2 个外部系统，标注 5 类关系（客户-供应商 / 事件订阅 / 顺从者 / 防腐层 / 共享内核），并说明为何未采用伙伴关系 |
| 3 | 聚合识别清单 Markdown（聚合名 / 根实体 / 关键不变式） | [`aggregates.md`](aggregates.md) | 13 个聚合、42 条编号不变式、一致性边界表、读模型清单，并映射到具体类与现有测试 |
| 4 | 统一语言术语表 v1.0（中英对照） | [`glossary.md`](glossary.md) | 108 条术语（要求 ≥30），含**代码标识**列与 10 条反例清单 |

**配套过程记录**：[`event-storming.md`](event-storming.md)（Big Picture 38 个事件 / Process Modeling 34 条命令 / Software Design 聚类 / 8 个热点决策）、[`context-map.md`](context-map.md)（子域划分与关系论证）

---

## 二、实验步骤完成情况

| 步骤 | 要求 | 完成情况 |
| --- | --- | --- |
| Step 1 **Big Picture**（40 min） | 橙色便利贴罗列领域事件（过去时）、按时间线排列、识别热点与系统边界事件 | ✅ 38 个事件按 6 个业务阶段排列；8 个热点全部留结论；3 类系统边界事件单列 |
| Step 2 **Process Modeling**（30 min） | 蓝色命令、黄色角色、粉色外部系统 | ✅ 34 条命令全部标注触发角色与前置条件；6 类角色；3 个外部系统（校园身份 / 邮件通知 / 定时任务） |
| Step 3 **Software Design**（30 min） | 命令-事件聚类识别聚合、验证不变式与一致性边界、识别读模型 | ✅ 13 个聚合；每个聚合的不变式编号化并可映射到单元测试；7 个读模型 |
| Step 4 **上下文映射与术语表**（20 min） | 划定限界上下文、标注四类关系、术语表 ≥30 条 | ✅ 4 个上下文 + 共享内核；5 类关系标注（含对"伙伴关系"的显式取舍论证）；术语 108 条 |

---

## 三、评分要点自评

| 评分项 | 权重 | 我们的做法 | 对应材料 |
| --- | --- | --- | --- |
| 事件识别完整性 | 25% | 38 个事件覆盖注册→建课→提问→作答→评价→激励→沉淀全链路；每个事件均为业务语言过去时，无"用户操作完成"类抽象事件；单列 3 个系统边界事件 | event-storming.md §1、event-storming-board.png |
| 上下文划分合理性 | 30% | 严格按业务能力划分（非技术分层）；4 个上下文与 Maven 模块一一对应；`ArchitectureTest` FF-2 用自动化测试强制"只能经由 api 包或事件通信"；对未采用伙伴关系给出可验证理由 | context-map.md、context-map.png |
| 聚合边界与不变式定义清晰度 | 25% | 13 个聚合、42 条不变式均编号（如 I4-3「每问题最多采纳一个回答且不可更改」），并标注违反时的异常码；明确区分"强一致聚合内"与"最终一致跨聚合"；三处关键取舍写入 ADR | aggregates.md |
| 统一语言一致性与团队认同度 | 20% | 108 条术语含代码标识列，可直接作为命名依据；10 条反例清单强制区分易混词（回答 vs 评论、积分 vs 声誉）；术语变更须走评审、每 Sprint 检查一次 | glossary.md |

---

## 四、与代码的一致性（本版最重要的改进）

初版文档与实际代码存在三处**事实性偏差**，本版已全部修正。这几处若不改，会在 Lab 4 编码和评审时直接暴露：

| # | 初版说法 | 代码事实 | 本版修正 |
| --- | --- | --- | --- |
| 1 | 「Discovery 课程发现上下文」承载 `Course` 聚合，对应 `co-discovery` | `Course` 在 `co-identity`；`co-discovery` 实际承载**通知与全文检索** | Discovery 重新定义为「通知与检索」通用子域；课程归入 Identity |
| 2 | `Answer` 是 `Question` 聚合内实体，采纳与回答同事务强一致 | `Answer` 有独立仓储与独立主键，是**独立聚合**（注释明确"独立于 Question 聚合以降低并发冲突"） | 改为独立聚合，采纳时显式加载校验归属 |
| 3 | 声誉仅「账户 + 流水」两个概念 | 代码另有 `Bounty`（悬赏，状态机）、`BadgeAward`（6 类徽章）、`ReputationRulebook`（规则手册） | 补齐为 4 个聚合，并写明冻结/可用积分守恒 |

另外补齐了初版缺失的 `Comment`、`Tag` 聚合与 7 个读模型，使聚合总数由 4 个增加到 13 个。

**校验方式**：不变式全部对照源码与单元测试核验，例如
`BountyAndAccountTest` 中的 `bountyCanBeSettledOnlyOnce`（对应 I11-4）、`freezeSpendAndRefundKeepPointsConsistent`（对应 I9-3 / I9-4）。

---

## 五、如何查看

```bash
# 1. 直接看两张图（无需任何工具）
docs/ddd/context-map.png
docs/ddd/event-storming-board.png

# 2. 在 GitHub 上直接阅读（context-map.md 内含 Mermaid 版，GitHub 原生渲染）
docs/ddd/context-map.md
docs/ddd/event-storming.md
docs/ddd/aggregates.md
docs/ddd/glossary.md
```

---

## 六、遗留事项

| # | 事项 | 负责人 | 期限 |
| --- | --- | --- | --- |
| 1 | 若教师要求现场照片，实验课现场按 `event-storming-board.png` 复原墙面并补拍 | SM 左燚之 | 第 5 周实验课 |
| 2 | 热点 H9（教师认证是否加声誉分）、H10（每日上限时区公平性）进入 Sprint 3 Backlog | PO 唐志烽 | Sprint 3 Planning |
| 3 | `aggregates.md` 第五节列出的不变式测试缺口，由 Developer 在 Sprint 2 补齐 | 江思达 / 柯博 | Sprint 2 结束 |

---

**变更记录**

| 版本 | 日期 | 变更 |
| --- | --- | --- |
| v1.0 | 2026-11-04 | 首版：四份产物重写并与代码对齐，新增两张图与本文档 |
