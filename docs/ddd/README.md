---
AIGC:
    Label: "1"
    ContentProducer: 001191440300708461136T1XGW3
    ProduceID: 31e7cf1207b8d082b268a2ac107cd18b_64969fdbd7ec11f1b2d8525400393706
    ReservedCode1: Z3p2ml726BW5jiqL/BCHZDf/julEz973XLK+JDyoeKU1/IHOyHu8YiXa1M7raxgH0SWwYphn5PPEfTcrcKUeO5jCy3EkgXdbZbLmvJYS13wMVDsl1b9fIrQq56U5phHLJz+DTdQmMOfed7A3fiTq1cJA7m6sNj/1T5CVG4ixFFbmzNvj48d0UuV0TfA=
    ContentPropagator: 001191440300708461136T1XGW3
    PropagateID: 31e7cf1207b8d082b268a2ac107cd18b_64969fdbd7ec11f1b2d8525400393706
    ReservedCode2: Z3p2ml726BW5jiqL/BCHZDf/julEz973XLK+JDyoeKU1/IHOyHu8YiXa1M7raxgH0SWwYphn5PPEfTcrcKUeO5jCy3EkgXdbZbLmvJYS13wMVDsl1b9fIrQq56U5phHLJz+DTdQmMOfed7A3fiTq1cJA7m6sNj/1T5CVG4ixFFbmzNvj48d0UuV0TfA=
---

# Lab 3 领域驱动设计事件风暴 · 交付索引

> 所属实验：Lab 3（2 学时，第 5 周，Sprint 1）
> 团队：Overflow Crew　Scrum Master：Flagg32
> 汇总人：SM　最后更新：2026-11-04

本目录存放 Lab 3 事件风暴的全部建模产物，对应实验指导书的 4 项提交物。

---

## 一、提交物对照表

| # | 指导书要求 | 本仓库文件 | 状态 |
| --- | --- | --- | --- |
| 1 | 事件风暴现场全景照片（墙面 / 数字白板截图） | `docs/ddd/event-storming-board.png` | ⚠️ 待替换为真实现场照片或数字白板导出图（当前为按 `event-storming.md` 内容还原的布局图，供核对完整性用） |
| 2 | 上下文映射图（Context Map）PNG | `docs/ddd/context-map.png`（源：`context-map.md` 中的 mermaid） | ✅ 已完成 |
| 3 | 聚合识别清单 Markdown（聚合名 / 根实体 / 关键不变式） | `docs/ddd/aggregates.md` | ✅ 已完成（4 个聚合、4 组不变式） |
| 4 | 统一语言术语表 v1.0，中英对照 | `docs/ddd/glossary.md` | ✅ 已完成（50 个术语，要求 ≥30） |

过程材料（SM 归档）：

| 文件 | 用途 |
| --- | --- |
| `event-storming.md` | Step 1–3 的事件 / 命令 / 聚合全量清单，白板照片的文字底稿 |
| `lab3-workshop-主持脚本.md` | 课前准备、四段时间盒、现场引导与陷阱纠偏清单 |
| `lab3-event-storming-纪要.md` | 会议纪要、热点裁决、术语定名分歧记录、全员确认签字 |

---

## 二、四项提交物与评分要点对应

| 评分要点 | 权重 | 本目录中的支撑材料 |
| --- | --- | --- |
| 事件识别完整性 | 25% | `event-storming.md` 第一节：19 个领域事件，覆盖身份、课程、提问、回答互动、声誉 5 段业务主线 |
| 上下文划分合理性 | 30% | `context-map.md` + `context-map.png`：4 个限界上下文 + 共享内核，全部按业务能力划分 |
| 聚合边界与不变式清晰度 | 25% | `aggregates.md`：Question 聚合锁定"每问题至多一个被采纳回答""一人一票"，Reputation 锁定入账幂等 |
| 统一语言一致性与全员认同度 | 20% | `glossary.md`（50 条）+ 纪要中的全员确认签字表 |

---

## 三、与前后实验的衔接

- **上游（Lab 2）**：事件与命令全部落在 Sprint 1 的 8 条用户故事（US-001/002/003/004/007/009/012/015）范围内，未引入 Backlog 之外的新功能；五段骨干活动（建立身份 → 进入课程 → 提出问题 → 回答互动 → 成长激励）与 `docs/backlog/user-story-map.md` 一致。
- **下游（Lab 4）**：`context-map.md` 中每个限界上下文已映射到 Maven 模块（co-identity / co-discovery / co-qa / co-reputation / co-shared-kernel），可直接作为 Lab 4 分包依据；`aggregates.md` 中标出的三条关键不变式（采纳唯一、投票唯一、声誉幂等）是 Lab 4 单元测试的必测项。

---

## 四、待补事项（SM 跟踪）

1. 提交物 1 替换为真实现场照片（或 Miro / 飞书白板导出图），需体现橙/蓝/黄/粉/紫五色便利贴与虚线圈边界。
2. 术语表全员确认签字（4 名成员）完成后再提交评审。
3. 编号引用修正：`context-map.md` 末尾引用的 `docs/adr/ADR-002-package-structure.md` 在当前仓库 `docs/adr/` 下不存在，需改为模板 ADR 的实际文件名或补一份 ADR。
*（内容由AI生成，仅供参考）*
