# Sprint 1 Backlog（Sprint 待办列表）

> 所属实验：Lab 2 需求建模工作坊（Sprint 1）
> 主持：PO + SM　|　团队：Overflow Crew（5 人）

---

## 一、Sprint 基本信息

| 项 | 内容 |
| --- | --- |
| Sprint | Sprint 1（Walking Skeleton） |
| Sprint 目标 | 一名真实学生可在课程内完成「注册→登录→加入课程→提问→回答→采纳→看到声誉」的完整闭环，端到端可演示 |
| 估算 Velocity | 约 27 故事点 |
| 故事点合计 | 27 点（US-001/002/003/004/007/009/012/015） |
| 周期 | 第 3–6 周 |

---

## 二、故事 → 任务拆分与责任人

> **状态补记账说明（SM / 行动项 A-09，2026-11-05 补记）**：本表 26 条任务自 Sprint 1 定稿后一直停留在 `To Do`，未随实际开发更新（见 `sprint-2-backlog.md` 第一节「Sprint 2 与 Sprint 1 的关系」）。现由 SM 依仓库实际代码与测试证据回填真实状态，判定口径如下：
>
> - `Done`：代码已合并 main，且存在可点名的单元测试或接口证据；
> - `Done(待验证)`：代码已在库，但缺端到端验证（Docker、Flyway、Postman 打穿）或前端交互未在真实浏览器验证；
> - `In Progress`：仅后端或仅前端一侧落地；
> - `To Do`：尚未实现，已明确移交 Sprint 3。
>
> 汇总（26 条）：`Done` 14 条、`Done(待验证)` 9 条、`In Progress` 2 条（T-007 前端课程入口、T-024 个人声誉页）、`To Do` 1 条（T-025 采纳后声誉反馈，依赖 Sprint 3 声誉结算）。

| 故事 | 任务 | 责任人 | 状态 | 证据 / 去向 |
| --- | --- | --- | --- | --- |
| US-001 注册 | T-001 注册表单与前端校验 | 柯博 | Done(待验证) | `frontend/src/pages/LoginPage.tsx` 含注册表单与登录/注册切换；未走真实浏览器端到端 |
| US-001 注册 | T-002 注册接口错误码与重复账号处理 | 柯博 | Done | `POST /api/v1/auth/register`；`UserRegistrationService`；`CrossCutIdentityServiceTest.registerRejectsDuplicateUsername / DuplicateDisplayName / WeakPassword` |
| US-001 注册 | T-003 QA：注册成功与异常路径测试 | 江思达 | Done | `CrossCutIdentityServiceTest` 5 个注册用例（成功发布事件 + 三类拒绝 + 哈希非明文） |
| US-002 登录 | T-004 登录页、错误提示与跳转 | 柯博 | Done(待验证) | `LoginPage.tsx`；`POST /api/v1/auth/login` 统一错误体 |
| US-002 登录 | T-005 会话保持、退出与权限拦截 | 柯博 | Done | `GET /api/v1/auth/me`、`/auth/logout`、`SecurityConfig` 权限拦截；`registeredUserCanLoginUntilDisabled` |
| US-002 登录 | T-006 QA：登录失败与退出测试 | 江思达 | Done(待验证) | 已覆盖「登录成功至禁用」与 `PasswordPolicy`；失败计数/限流（`LoginThrottle`）与退出通路缺专门用例 |
| US-003 加入课程 | T-007 课程列表与加入操作 | 何欣怡 | In Progress | 后端 `GET /api/v1/courses/mine`、`POST /api/v1/courses/{id}/join` 已就绪；前端无课程页与加入入口 |
| US-003 加入课程 | T-008 重复加入与异常处理 | 何欣怡 | Done | `CourseService` 已加入分支；`joinCourseTwiceIsRejected`、`joinUnknownCourseThrowsNotFound` |
| US-003 加入课程 | T-009 QA：加入课程测试 | 何欣怡 | Done | `CrossCutIdentityServiceTest` 加入课程 3 例 + `CourseTest` 3 例 |
| US-004 浏览 | T-010 课程问题列表与分页 | 何欣怡 | Done(待验证) | `GET /api/v1/questions?courseId&page&size` + `QuestionListPage.tsx` |
| US-004 浏览 | T-011 问题详情、正文与标签展示 | 何欣怡 | Done(待验证) | `QuestionDetailView` + `QuestionDetailPage.tsx`（含回答、评论、投票区） |
| US-004 浏览 | T-012 已删除问题不可访问提示 | 何欣怡 | Done | `QuestionQueryService` 软删除分支 + `QuestionQueryServiceTest` |
| US-004 浏览 | T-013 QA：列表、详情与空状态测试 | 何欣怡 | Done | `QuestionQueryServiceTest`（318 行）覆盖列表、详情、软删除、空态 |
| US-007 提问 | T-014 提问表单与 Markdown 输入 | 江思达 | Done(待验证) | `POST /api/v1/questions` + `AskPage.tsx` |
| US-007 提问 | T-015 标题、标签规则与错误提示 | 江思达 | Done | `QuestionRequest` `@NotBlank @Size` 标题 150 / 正文 30000 / 标签 1–5；`QuestionTest` 覆盖 |
| US-007 提问 | T-016 发布成功后的列表刷新 | 江思达 | Done(待验证) | `AskPage.tsx` 提交后 `useNavigate` 跳转 |
| US-007 提问 | T-017 QA：提问边界与异常测试 | 江思达 | Done | `QuestionTest`（142 行）标题、标签、正文边界 |
| US-009 回答 | T-018 回答输入与提交 | 江思达 | Done(待验证) | `POST /api/v1/questions/{id}/answers` + 详情页回答区 |
| US-009 回答 | T-019 已删除问题禁止回答 | 江思达 | Done | `AnswerCommandService` 校验 `DELETED`；`AnswerAndVoteServiceTest` |
| US-009 回答 | T-020 QA：回答成功与异常测试 | 江思达 | Done | `AnswerTest`、`AnswerAndVoteServiceTest` |
| US-012 采纳 | T-021 采纳按钮与权限显示 | 柯博 | Done(待验证) | `QuestionDetailView` 可执行操作 + 详情页采纳入口 |
| US-012 采纳 | T-022 采纳接口与重复采纳处理 | 柯博 | Done | `PUT /api/v1/questions/{id}/accepted-answer`；`repeatAcceptRejected` |
| US-012 采纳 | T-023 QA：提问者/教师/无关学生测试 | 柯博 | Done | `US012AcceptCrossTest`：提问者、无权限者、课程管理者、跨题回答 4 类 |
| US-015 声誉 | T-024 个人声誉摘要展示 | 柯博 | In Progress | `GET /api/v1/users/{id}/reputation` 与 `profileOwnViewShowsPointsAndBadges` 已就绪；前端仅有 `LeaderboardPage`，无个人声誉页 |
| US-015 声誉 | T-025 采纳后声誉反馈可见 | 柯博 | To Do | 采纳后的声誉结算未实现：`co-reputation` 尚有 70 处 `TODO(S3)`，移交 Sprint 3（见 sprint-2-backlog 第四节 Out of Scope） |
| US-015 声誉 | T-026 QA：本人/他人可见范围测试 | 江思达 | Done | `ReputationQueryServiceTest`：`profileOtherViewHidesPoints`、`ledgerOtherViewForbidden`、`ledgerAdminAllowed` |

---

## 三、容量平衡（按责任人）

| 责任人 | 主要任务数 | 负责模块 |
| --- | --- | --- |
| 江思达（houhai6） | 10 | 提问、回答、QA |
| 何欣怡（hehe309） | 7 | 加入课程、浏览、QA |
| 柯博（Ke-1226） | 9 | 注册、登录、采纳、声誉、QA |
| Flagg32（SM） | 过程护航 | 看板、站会、移除障碍 |
| PO | 验收口径 | 优先级、验收、Review |

> 任务卡片按 To Do → In Progress → In Review → Done 流转；上述状态已于 2026-11-05 由 SM 按实际结果回填（原为清一色 To Do）。

---

## 四、Sprint 验收与演示

- 演示脚本：注册 → 登录 → 加入课程 → 发布问题 → 换账号回答 → 采纳 → 查看声誉。
- 每个故事满足对应 Gherkin 场景与 DoD 后，方可移入 Done。
- Sprint 结束召开 Review 与 Retrospective，结果归档。
