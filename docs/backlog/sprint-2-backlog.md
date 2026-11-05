# Sprint 2 Backlog（Sprint 待办列表）

> 所属实验：Lab 4 后端工程骨架与领域代码（第 7 周）｜ 交织 Lab 5 前端骨架（第 9 周）
> 维护人：PO（唐志烽）｜ 看板同步：SM（Flagg32）
> 版本：v1.0（Lab 4 课前定稿）
> 上游依据：`docs/backlog/product-backlog.md`、`docs/backlog/sprint-1-backlog.md`、`CampusOverflow实验部分.pdf`（Lab 4 步骤与提交物）、Lab 3 定稿 `docs/ddd/`（13 个聚合 / 42 条不变式，位于分支 `feature/lab3-ddd-modeling`，待合并）

---

## 一、Sprint 基本信息

| 项 | 内容 |
| --- | --- |
| Sprint | Sprint 2（后端骨架与领域代码） |
| Sprint 目标 | 后端成为**可独立构建、可连库启动、可端到端调用**的工程：`mvn clean install` 通过（口径见 D1）、`docker compose up` 后应用可启动、`POST /api/v1/questions` 打穿并落库，同时交付 Lab 4 四项提交物 |
| 周期 | 第 7–10 周（Lab 4 在第 7 周，Lab 5 在第 9 周） |
| 承载故事 | US-007 发布问题（后端实现闭环）、US-009 回答问题、US-012 采纳回答、US-004 浏览课程问答（读路径） |
| 估算 Velocity | 约 30 SP ＝ 承接故事 18 SP（US-007 5 + US-009 5 + US-012 3 + US-004 5）＋ 工程与文档任务 12 SP（以 Sprint 1 的 27 SP 为基线，本 Sprint 引入大量工程与文档任务） |
| 任务数 | 17 项（T-027 ~ T-043，接续 Sprint 1 的 T-001~T-026 编号） |
| 状态口径 | `Done` 已合并入 main；`Done(待验证)` 代码在库但未端到端验证；`To Do` 未开始；`待决` 等 PO 决策 |
| 前置依赖 | **Lab 3 四项提交物须先合并并取得教师认可**（PDF 将「Lab 3 产出物已提交并通过教师评审」列为 Lab 4 前置）。当前 Lab 3 定稿在 `feature/lab3-ddd-modeling`，**尚未合并入 main**；main 上的 `docs/ddd/` 仍是初版（4 个聚合、不变式未编号）。T-039 的不变式核查依赖该分支，须先合并（行动项 A-14） |

> **Sprint 2 与 Sprint 1 的关系**：Sprint 1 承诺的 8 个故事在 Sprint 1 内只完成了需求侧交付（故事地图、26 条任务卡、8 份 Gherkin）。截至 Sprint 2 启动，`sprint-1-backlog.md` 中 26 条任务**全部仍为 To Do**、未随实际开发更新。因此 Sprint 2 实际承接了 Sprint 1 的后端实现工作，本表不重复登记 Sprint 1 的任务编号，改为按「实验步骤 + 提交物」重新切分，Sprint 1 的任务卡由 SM 在本 Sprint 内按实际结果补记账（见行动项 A-09）。

---

## 二、任务拆分与责任人

### 2.1 后端骨架与领域代码（Lab 4 Step 1–2）

| 编号 | 任务 | 关联 | 责任人 | 状态 | 验收标准 |
| --- | --- | --- | --- | --- | --- |
| T-027 | 多模块 Maven 骨架：父 POM 统一版本（Spring Boot 3 / JDK 21 / JUnit 5），六上下文物理分包 | Lab4 Step1 | 江思达 | Done | 27 个模块可构建；`co-bootstrap` 为唯一启动入口 |
| T-028 | co-qa 核心聚合实现：`Question`、`Answer`、`Vote`、`Comment`、`Tags`、`MarkdownBody` | US-007/009/012 | 江思达 | Done | Setter 私有化、经工厂或聚合方法构造；不变式在聚合内校验 |
| T-029 | co-qa 应用服务：`QuestionCommandService`、`AnswerCommandService`、`VoteService`、`CommentService`、`QuestionQueryService` | US-007/004 | 江思达 | Done | 命令与查询分离；跨上下文仅经 `*-api` 包 |
| T-030 | Repository 接口定义在 domain，实现在 infrastructure | Lab4 Step2 | 江思达 | Done | domain 模块不出现 Spring / JPA import（`ArchitectureTest` FF-3 校验） |
| T-031 | 校验 Flyway 五个上下文的迁移脚本可在空库完整执行（`question` / `answer` / `user` 等表） | Lab4 Step3 | 何欣怡 | To Do | 空库启动后 `flyway_schema_history` 五条 version 全 SUCCESS；表结构与聚合字段一致 |
| T-032 | 全局异常处理 `@RestControllerAdvice` 统一错误响应 | Lab4 Step3 | 江思达 | Done | 参数校验失败 / 未登录 / 无权限 / 领域异常均返回统一错误体与正确状态码 |
| T-033 | Logback JSON 结构化日志 | Lab4 Step3 | 柯博 | To Do | 控制台与文件输出为 JSON；含 traceId、时间、级别、logger；`application.yml` 可切换 |
| T-034 | `POST /api/v1/questions` 端到端贯通并校验落库 | Lab4 Step4 | 何欣怡 | To Do | Postman 返回 201；`select * from question` 可见对应记录；标签关联表同步写入 |

### 2.2 提交物与验收证据（Lab 4 提交物 1–4）

| 编号 | 任务 | 关联 | 责任人 | 状态 | 验收标准 |
| --- | --- | --- | --- | --- | --- |
| T-035 | Postman Collection 成功用例实跑并留存证据 | 提交物④ | 何欣怡 | To Do | 集合已存在 16 个请求；补「运行结果截图 + 导出的 run report」入 `docs/lab4/` |
| T-036 | ADR-009 包结构与四层分层决策 | 提交物③（对应 PDF 的 ADR-002-package-structure） | 江思达 | To Do | 含背景 / 决策 / 后果 / 被否决方案（按上下文分包 vs 按技术分层）；说明 domain 不依赖框架的理由 |
| T-037 | 修复 4 个乱码 ADR 文件名，补齐 003/005/006 缺失文件或从 README 摘除 | 评分·文档 20% | SM（Flagg32） | Done | `campus-overflow-student/docs/adr/README.md` 中 8 条链接全部可点，无 404 |
| T-038 | 集成测试 `CampusOverflowFlowIT` 处置（D1 决策落地） | 提交物① | SM（Flagg32） | Done | 按 D1 执行 `@Disabled` 并写明恢复条件；README 记录构建口径 |
| T-039 | 不变式覆盖核查：Lab 3 定稿的 42 条不变式（`feature/lab3-ddd-modeling` 分支，待合并）× 现有 55 个测试 | 评分·不变式 25% | 柯博 | To Do | 产出一张「不变式 → 测试方法」对照表，标出未覆盖项并说明取舍 |
| T-040 | 《Lab 4 代码走查》文档，支撑全员答辩 | 评分·结构 30% | 江思达 | To Do | 按「请求进入 → 应用服务 → 聚合 → 仓储 → 返回」讲清一条链路，配模块依赖图 |

### 2.3 过程与决策（PO 职责）

| 编号 | 任务 | 关联 | 责任人 | 状态 | 验收标准 |
| --- | --- | --- | --- | --- | --- |
| T-041 | Sprint 2 Planning 纪要 | 过程记录 | PO（唐志烽） | Done | `sprint-2-planning-纪要.md` 含目标、范围、三项决策、分工、风险、行动项 |
| T-042 | Lab 4 提交物对照与验收口径 | 过程记录 | PO（唐志烽） | Done | `docs/lab4/提交物与验收口径.md`，可直接用于 Review 逐条打勾与向教师说明 |
| T-043 | 判定投票 / 评论 / 标签是否补故事与 Gherkin（D4） | 需求追溯 | PO（唐志烽） | 待决 | 见 D4：建议本轮只登记缺口、Sprint 3 统一补 ID，不新造故事 |

---

## 三、进入 Sprint 2 时的存量（已完成，不再重复投入）

以下内容在 Sprint 2 启动时已在 main 分支可用，本 Sprint 只需验证与补文档，不重新开发：

| 项 | 现状 | 来源 |
| --- | --- | --- |
| 六上下文 + 共享内核的 Maven 多模块结构 | 27 个模块，`mvn clean install -DskipITs` 实测 BUILD SUCCESS | 模板 + `ed4d212` |
| co-qa 全部 `TODO(S2)` | **0 个残留**，S2 范围代码已写完 | `ed4d212`（江思达，10-28 合并） |
| 单元测试 | 14 个测试类 / 55 个用例，0 失败 0 跳过 | 实测 |
| 架构约束测试 | `ArchitectureTest`（FF-1~FF-6）通过 | 实测 |
| Flyway 五上下文迁移脚本 | 脚本齐全（platform/identity/qa/reputation/discovery），未在空库实跑 | 模板 |
| Postman Collection | 16 个请求，含 `POST /api/v1/questions` 成功用例与 CSRF/登录前置 | `ed4d212` |
| `docker-compose.yml` | `mysql:8.0.39` + `redis:7.2` | `ed4d212` |
| ADR 001/002/004/007/008 | 已接受，但 4 个文件名乱码（见 T-037） | 模板 |

---

## 四、明确不在本 Sprint 范围（Out of Scope）

| 项 | 说明 | 去向 |
| --- | --- | --- |
| `TODO(S3)` 共 134 个（co-discovery 64 + co-reputation 70） | 搜索投影、通知中心、声誉结算与徽章、悬赏状态机 | Sprint 3 |
| 集成测试 `CampusOverflowFlowIT` 转绿 | 它断言了搜索命中、通知送达、采纳后声誉 +15，全部依赖 S3 能力，本 Sprint 无法转绿 | Sprint 3（按 D1 先禁用） |
| 投票 / 评论 / 标签的验收 | 代码已随 co-qa 落地，但产品 Backlog 与 Gherkin 中无对应故事 | 见 D4，Sprint 3 补 |
| 前端工程与前后端贯通 | React 18 + TS + Vite 骨架、路由、API 客户端 | Sprint 2 后半段（Lab 5）细化，本表暂不展开 |
| 课程管理、加精、教师管理视角 | Product Backlog 后续候选 | Sprint 3+ |

---

## 五、DoD（完成定义，沿用章程并补充 Lab 4 口径）

1. 代码经 PR 提交，**至少一名队友 review 并回复 LGTM**，由 SM 合并；
2. 单元测试全绿（当前基线 55 个用例，只增不减）；
3. 构建口径达标：`mvn clean install -DskipITs` BUILD SUCCESS（D1 约定）；
4. 涉及接口的，Postman 集合同步更新并留存运行证据；
5. 新增架构决策必须落 ADR，并在 `campus-overflow-student/docs/adr/README.md` 登记；
6. 看板卡片由责任人自行移入 Done，**状态与本文档同步**（Sprint 1 的教训：26 条全停 To Do）。

---

## 六、状态更新规约

- 每人每日站会后自行更新所负责任务的 `状态` 列，不再由 SM 代填；
- 状态只允许取值：`To Do` / `In Progress` / `In Review` / `Done` / `待决`；
- `待决` 项必须在 Planning 后 3 日内由 PO 给出结论并改为 `To Do` 或移出范围；
- 本表与 GitHub Projects 看板**同源**，若不一致以本表为准，由 SM 在站会提出差异。

---

**维护人**：PO（唐志烽）　|　**最后更新**：2026-11-04　|　**分发范围**：全体团队成员、课程指导教师
