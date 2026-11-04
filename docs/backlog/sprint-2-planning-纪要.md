# Sprint 2 Planning 会议纪要

> 所属实验：Lab 4 后端工程骨架与领域代码（第 7 周）
> 会议类型：Sprint 2 Planning（Sprint Planning Meeting）
> 主持人：PO（唐志烽）+ SM（Flagg32）
> 记录人：PO（唐志烽）
> 归档位置：`docs/backlog/sprint-2-planning-纪要.md`
> 状态：**草案** —— 由 PO 于 Lab 4 课前起草，会后按实际讨论结论定稿（定稿前不作为团队承诺）

---

## 一、会议基本信息

| 项 | 内容 |
| --- | --- |
| 会议主题 | 确定 Sprint 2 目标、Lab 4 提交物口径与任务分工 |
| 时间 | 2026-11-04（周三）09:00–10:30，共 90 分钟 |
| 形式 | 团队同步会议（Lab 4 课前，共享屏幕过 backlog 与代码现状） |
| 主持人 | PO（唐志烽）主持目标与范围；SM（Flagg32）主持流程与分工 |
| 参会人 | 唐志烽（PO）、Flagg32（SM）、江思达（houhai6）、何欣怡（hehe309）、柯博（Ke-1226），共 5 人 |
| 会议目标 | ① 核对 Sprint 1 真实完成度；② 对齐 Lab 4 四项提交物的口径；③ 就集成测试、ADR 编号、提交物路径三项拍板；④ 完成任务认领 |
| 议程 | 1) Sprint 1 回顾与 Sprint 2 目标（20 min）② Lab 4 提交物核对与决策（30 min）③ 任务拆分与认领（25 min）④ 风险与行动项（15 min） |

**输入材料**

- `CampusOverflow实验部分.pdf`：Lab 4 实验目标、4 个 Step、4 项提交物、4 条评分要点
- `docs/backlog/sprint-2-backlog.md`：本 Sprint 任务表（T-027 ~ T-043）
- `campus-overflow-student/docs/瀛︾敓浠诲姟娓呭崟.md`（《学生任务清单》）：107 个待实现方法按 S2 / S3 划分
- 代码实测：`mvn clean install -DskipITs` BUILD SUCCESS（27 模块 / 55 单测 0 失败）；`mvn clean install` 失败于 `co-bootstrap` 集成测试
- `docs/ddd/aggregates.md`：13 个聚合、42 条编号不变式（Lab 3 定稿，位于分支 `feature/lab3-ddd-modeling`，**尚未合并入 main**；main 上仍是初版 4 个聚合）
- `docs/team/charter-v1.0.md`：分支、PR、DoD 规约

---

## 二、议程一：Sprint 1 回顾与 Sprint 2 目标（20 min）

**Sprint 1 真实完成度核对（PO 通报）**

| 检查项 | 结论 |
| --- | --- |
| 需求侧交付 | 完成：故事地图、Product Backlog（8 故事 / 27 SP）、8 份 Gherkin、Team Charter |
| 代码侧交付 | 承诺的 8 个故事（US-001/002/003/004/007/009/012/015）在 Sprint 1 内**未落地**，实际由 `ed4d212`（10-28）一并实现，进入 Sprint 2 时已合入 main |
| 任务卡状态 | `sprint-1-backlog.md` 26 条任务**全部仍为 To Do**，未随实际开发流转 |
| 结论 | Sprint 1 的 DoD “卡片由责任人移入 Done” 未被遵守，属于过程执行缺口，不涉及代码质量 |

**整改措施（记入行动项）**：Sprint 2 起，任务状态由责任人每日站会后自更新；SM 在 Sprint 末按实际结果回填 Sprint 1 的 26 条任务，不再留全清一色 To Do。

**Sprint 2 目标（提请确认）**

> 后端成为一个**可独立构建、可连库启动、可端到端调用**的工程：`mvn clean install` 通过（口径见 D1）、`docker compose up` 后应用可启动，`POST /api/v1/questions` 打穿并落库；同时交付 Lab 4 四项提交物。

**范围取舍**

1. **纳入**：Lab 4 全部 4 个 Step；US-007 / US-009 / US-012 的后端实现闭环；US-004 的读路径；四项提交物与配套文档。
2. **不纳入**：`TODO(S3)` 的 134 个方法（co-discovery 64 / co-reputation 70）、投票与评论的验收补齐、前端工程（Lab 5 在第 9 周，于 Sprint 2 中段细化）。
3. **不因 Lab 4 增加故事**：Lab 4 是工程骨架实验，不对应新用户故事；不新造 US 编号以免打乱 Product Backlog 的追溯关系。

---

## 三、议程二：Lab 4 提交物核对与决策（30 min）

### 3.1 四项提交物现状（PO 逐条核对，结论以代码实测为准）

| # | PDF 要求 | 当前状态 | 处置 |
| --- | --- | --- | --- |
| ① | 多模块工程 `mvn clean install` 全绿 | `-DskipITs` 时 BUILD SUCCESS（27 模块 / 55 单测）；不加参数时失败于 `CampusOverflowFlowIT` | 见 **D1** |
| ② | `application.yml` 即可启动并连库 | 配置齐全（dev/prod profile、5 个上下文独立 Flyway 目录、Redis 会话），但**无人实跑过** | T-031 / T-034 |
| ③ | `docs/adr/ADR-002-package-structure.md` | **不存在**；且 002 号已被「事务性发件箱」占用 | 见 **D2** |
| ④ | Postman Collection 含 `POST /api/v1/questions` 成功用例 | 集合已存在，16 个请求，含成功用例与 CSRF/登录前置；**缺运行证据** | T-035 |

### 3.2 决策 D1：集成测试与「全绿」的口径

**问题**：`CampusOverflowFlowIT` 断言「提问 → 回答 → 采纳 → 声誉 +15 → 搜索命中 → 收到通知」，后三步全部依赖 `TODO(S3)`。即使装好 Docker，它在 Sprint 2 内也**不可能转绿**。

| 方案 | 做法 | 代价 | 评价 |
| --- | --- | --- | --- |
| A | `@Disabled` 该 IT，注释写明「待 S3 能力就绪后恢复」，README 记录构建口径 | 需向教师说明 | **采纳** |
| B | 不加注解，构建统一加 `-DskipITs` | 掩盖问题，`mvn clean install` 仍红 | 不采纳 |
| C | 先实现 S3 让 IT 转绿 | 134 个方法，远超 2 学时 | 不采纳 |

**决策（A）**：由 SM 对 `CampusOverflowFlowIT` 加 `@Disabled("依赖 Sprint 3 的搜索/通知/声誉能力，S3 交付后恢复")`，并在 `campus-overflow-student/README.md` 写明：

- 本 Sprint 构建口径为 `mvn clean install`（单元测试全绿，55 个用例）；
- 集成测试暂时禁用，原因与恢复条件如上；
- 反向要求：**S3 完成时，第一件事是移除该注解并让 IT 转绿**，列入 Sprint 3 的 DoD。

**影响**：提交物① 的验收口径由「所有测试通过」调整为「单元测试全绿 + 集成测试有明确禁用理由与恢复计划」。此口径需 PO 在 Review 前向教师书面确认一次（行动项 A-08）。

### 3.3 决策 D2：ADR 编号冲突

**问题**：PDF 要求 `ADR-002-package-structure.md`，但仓库 ADR-002 已是「事务性发件箱」，README 已登记 001–008。

**决策**：**不重排既有编号**（会产生大量失效链接与历史断层）。新增 **`009-包结构分层.md`**，并在 `campus-overflow-student/docs/adr/README.md` 增加一行映射说明：

> 009 对应课程 PDF 中要求的 `ADR-002-package-structure`；本仓库编号连续递增，不复用 002。

**同时修正**：README 中 003 / 005 / 006 三个条目指向的文件**实际不存在**，由 SM 一并处理（补齐内容或从表中摘除并注明「未成文」），保证 8 条链接全部可点。

### 3.4 决策 D3：提交物路径口径

**问题**：PDF 写 `docs/adr/`，实际 ADR 在 `campus-overflow-student/docs/adr/`；而 Lab 3 的 DDD 产物放在仓库根 `docs/ddd/`。

**决策**：**不搬迁既有文件**（避免打断 8 个 ADR 的历史链接）。约定：

1. 工程内文档（ADR）跟随工程目录 `campus-overflow-student/docs/adr/`；
2. 实验提交物（Lab 3 的 `docs/ddd/`、本次的 `docs/lab4/`）放仓库根 `docs/`；
3. 两者的对应关系写入 `docs/lab4/提交物与验收口径.md` 的路径映射表，供教师检索。

### 3.5 决策 D4：投票 / 评论 / 标签的需求追溯缺口

**问题**：`co-qa` 中已实现投票、评论、标签、教师认证四类能力，但 Product Backlog 与 Gherkin 中**没有对应故事**（Sprint 1 已明确排除）。属「有代码、无需求、无验收」。

**决策（建议）**：本 Sprint **不新造故事**，只在 `product-backlog.md` 第四节「后续 Backlog」中把这四项标注为「**已实现（Sprint 2 随 co-qa 落地），验收待补**」，Sprint 3 统一补 US 编号与 Gherkin。

理由：现在补故事会连带要求补 Gherkin 与验收记录，挤占 Lab 4 的 2 学时；而留痕标注已足以保证追溯不断。

---

## 四、议程三：任务拆分与认领（25 min）

按「一个任务不超过 1 人 1 天」拆分，共 **17 项（T-027 ~ T-043）**，接续 Sprint 1 的 T-001~T-026 编号，明细见 `docs/backlog/sprint-2-backlog.md`。

| 责任人 | 任务 | 核心产出 | 认领说明 |
| --- | --- | --- | --- |
| 江思达（houhai6） | T-027~T-030、T-032、T-036、T-040（7 项） | co-qa 实现（已完成）、ADR-009 包结构、代码走查文档 | co-qa 全部由他实现，架构与分层最熟，ADR-009 与走查文档非他莫属；**同时承担知识转移责任** |
| 何欣怡（hehe309） | T-031、T-034、T-035（3 项） | 空库迁移验证、端到端贯通、Postman 运行证据 | 三项串成一条「起库 → 迁移 → 调接口 → 查库」链路，放同一人手上避免交接损耗 |
| 柯博（Ke-1226） | T-033、T-039（2 项） | Logback JSON 日志、不变式覆盖对照表 | 日志是基础设施横切件；不变式核查直接对应 25% 评分项，由非作者独立核对更有说服力 |
| Flagg32（SM） | T-037、T-038（2 项） | ADR 乱码与死链修复、集成测试处置、看板与回填 | 文档完整性与构建口径属过程质量，归 SM；并负责 Sprint 1 任务卡回填 |
| 唐志烽（PO） | T-041~T-043（3 项） | Planning 纪要、提交物验收口径、D4 结论 | 负责口径与对外沟通，不做代码 |

**认领结论**

1. 17 项任务**全部有唯一责任人**，无悬空项；
2. **交叉验证约定**：T-039（不变式核查）与 T-035（Postman 证据）必须由**未参与该功能编写的人**执行，主开发不验收自己的代码；
3. **单点风险提示**：江思达一人承担 7 项，其中 T-036 / T-040 属文档产出，若延期，优先保 T-036（直接对应提交物③），T-040 可延至 Sprint Review 前。

---

## 五、议程四：风险识别与应对（15 min）

| 编号 | 风险 | 影响 | 应对措施 | 责任人 |
| --- | --- | --- | --- | --- |
| R-06 | 成员机器未装 Docker，`mysql:8.0.39` 镜像需在线拉取 | 提交物② 无法验证，2 学时耗在环境上 | 会前一日由何欣怡在实验室机器完成 `docker compose pull` 并启动成功一次 | 何欣怡 |
| R-07 | co-qa 全部代码由江思达一人所写，其余 4 人未参与 | 答辩或教师随机提问时无法解释结构，直接损失 30%+25% | 执行 T-040 走查文档 + 每人认领一个讲解切片（见验收口径第五节） | 江思达 / PO |
| R-08 | 「`mvn clean install` 全绿」与 S3 未实现存在硬冲突 | 提交物① 可能被判不合要求 | 按 D1 执行并**提前书面确认口径**，不等到 Review 才解释 | PO |
| R-09 | ADR 文件名乱码（GBK/UTF-8 错位），在 Windows 下重命名可能引入新的编码问题 | 链接继续失效，20% 文档分受损 | 用 `git mv` 而非系统重命名；改完在 Linux/macOS 视角校验一次；一次 PR 只做这一件事 | SM |
| R-10 | Sprint 1 的 26 张任务卡全为 To Do，若继续不维护 | 过程记录与 DoD 被打问号 | 落实「责任人每日自更新」+ SM 回填，Sprint 末双向对账 | SM |
| R-11 | **Lab 3 尚未合并**：定稿停在 `feature/lab3-ddd-modeling`（+851/−247），main 上 `docs/ddd/` 仍为初版（4 个聚合、不变式未编号） | ① PDF 明确将「Lab 3 通过教师评审」列为 Lab 4 前置，前置未闭环直接做 Lab 4 有被扣分风险；② T-039 的不变式核查在 main 上没有基准 | Sprint 2 启动第一件事是推 Lab3 的 PR 合并（A-14）；T-039 在合并后启动，不提前开跑 | PO |

---

## 六、Sprint 2 承诺与完成定义

**Sprint 承诺**：完成 Lab 4 四项提交物，后端达成「可构建、可启动、可调用」；承接 US-007 / US-009 / US-012 的后端闭环与 US-004 的读路径。

**完成定义（DoD）**

1. 代码经 PR 提交，至少一名队友 review 并回复 LGTM，由 SM 合并；
2. 单元测试全绿（基线 55 个用例，只增不减）；
3. 构建口径达标：`mvn clean install -DskipITs` BUILD SUCCESS，且 README 已写明集成测试禁用理由；
4. 涉及接口的，Postman 集合同步更新并留存运行证据；
5. 新增架构决策落 ADR 并在 `campus-overflow-student/docs/adr/README.md` 登记；
6. 看板卡片由责任人自行移入 Done，状态与 `sprint-2-backlog.md` 同步。

**Sprint Review 演示脚本**：`docker compose up -d` → 启动应用 → Postman 登录（CSRF）→ 发布问题返回 201 → 库中查得该记录 → 列表接口可检索到该问题。

---

## 七、行动项（Action Items）

| 编号 | 行动项 | 责任人 | 期限 |
| --- | --- | --- | --- |
| A-07 | 将 T-027~T-043 录入看板，状态按 `sprint-2-backlog.md` 初始值设置 | SM（Flagg32） | 会后 24 小时内 |
| A-08 | 向教师书面确认提交物① 的口径（单测全绿 + IT 有理由禁用），留存回复 | PO（唐志烽） | Lab 4 课前 |
| A-09 | 按实际结果回填 Sprint 1 的 26 张任务卡状态，不再留全 To Do | SM（Flagg32） | 第 7 周末 |
| A-10 | 完成 `docker compose pull` 并成功启动一次，截图入群 | 何欣怡 | Lab 4 课前一日 |
| A-11 | 在 `product-backlog.md` 第四节标注投票/评论/标签「已实现待验收」，落实 D4 | PO（唐志烽） | 会后 24 小时内 |
| A-12 | 完成 `campus-overflow-student/docs/adr/README.md` 链接体检，8 条全部可点 | SM（Flagg32） | 第 7 周末 |
| A-13 | 组织 Sprint 2 Review 与 Retrospective，纪要归档 `docs/backlog/` | SM（Flagg32） | Sprint 结束 |
| A-14 | 为 Lab 3 定稿建 PR（`feature/lab3-ddd-modeling`），取得一名队友 LGTM 后由 SM 合并，并请教师确认评审 | PO（唐志烽） | **Lab 4 之前** |

---

## 八、会议结论

1. Sprint 1 的真实情况是「需求侧完成、代码侧顺延、任务卡未流转」，Sprint 2 承接后端实现与 Lab 4 提交物，不新增用户故事。
2. 三项决策已定：**D1 集成测试禁用并写明恢复条件**、**D2 ADR 新增 009 不复用 002**、**D3 文档路径不搬迁但建映射表**；D4 建议只留痕不补故事。
3. 17 项任务全部认领到人，最重的单点在江思达（7 项），以「保 ADR-009、走查文档可延」为降级策略。
4. 最大风险是环境（Docker）与知识集中（co-qa 只有一人写过），分别由 A-10 与 T-040 应对。
5. **执行顺序约束**：先合并 Lab 3（A-14），再开 Lab 4 的文档类任务；Lab 4 的代码类任务不受影响，可立即开始。

---

**记录人**：唐志烽（PO）　|　**记录时间**：2026-11-04　|　**状态**：草案，待会议确认　|　**分发范围**：全体团队成员、课程指导教师
