# EduVoyage 前端设计规范（Frontend Design Spec）

> 适用阶段：第四阶段（前端）。本文件先行确立设计系统与组件约定，**不含实现代码**——
> 第四阶段开工时按本规范落地。
> 设计方法采用 **[MRNOBODY-ZST/TailwindCSS-DesignSkill](https://github.com/MRNOBODY-ZST/TailwindCSS-DesignSkill)**
> 的工作流与适配规则（详见 §1）。

---

## 0. 与已有决策的衔接

- 三端形态：沿用 Phase 1 结论 —— **单应用 + 角色路由 + 布局复用**。
- 技术栈：Vue 3 `<script setup>` + Vite + TypeScript + TailwindCSS + Pinia + Vue Router +
  Axios + ECharts + AntV G6（见 Phase 1 §1）。
- 后端契约：所有响应为统一 `Result<T>` / `PageResult<T>`，错误码见后端 `BizErrorCode`；
  鉴权为双 Token（Access 15m / Refresh 7d），权限以**权限码**（如 `course:create`）驱动前端菜单与按钮级控制。

---

## 1. 设计方法：采用 TailwindCSS-DesignSkill

该 Skill 的核心是一套 657 个 Vue + Tailwind 4.2 区块（blocks），作为**起点模板**适配到本项目。落地工作流：

1. **先读宿主项目**：确认框架、路由、样式约定、Tailwind 版本、图标库、数据形状、暗色策略——
   本项目即：Vue3 + Vite + Tailwind 4 + 自有 Pinia/Router + 暗色用 `class` 策略（见 §3）。
2. **按产品域选模板**：
   - `marketing` → 登录页/落地页/平台介绍（学校门户首页、登录注册）。
   - `application-ui` → **主力**：仪表盘、应用外壳(app shell)、导航栏、表单、表格、弹窗、通知——
     学生/教师/管理三端的工作界面几乎全部来自此类。
   - `ecommerce` → 本项目基本不用（无电商场景）。
3. **优先 `target_compliant=true` 模板**（支持 auto theme + dark mode）。
4. **复制后适配**：删除示例数据/占位链接/假 logo/示例文案，绑定真实 props、state、route、API。
5. **惯用化翻译**：区块是 Vue 片段，直接对齐本项目的 Headless UI / Heroicons 依赖（见 §6）。
6. **本地验证**：build/lint/typecheck 通过；桌面+移动、亮色+暗色都检查。

**适配铁律（来自 Skill）**：
- 保留区块的结构与间距节奏，但移除通用演示内容。
- 优先使用宿主项目的基础组件与图标库。
- Vue 片段常引用 `@headlessui/vue`、`@heroicons/vue/24/outline`，按需安装映射。
- 清理 JSX 残留（`className`/`htmlFor`/异常自闭合标签）。
- 避免：嵌套卡片、脆弱的宽度假设、易溢出文本、单色配色。

**Tailwind 4 类约定**（区块用到，需保持/按需 backport）：
`size-*`、`text-balance`、`text-pretty`、`shadow-xs`、`bg-linear-to-*`、`inset-ring`、
`not-dark:*`、`group-has-*`、`has-*`。

> 工具：仓库 `frontend-tailwind-css/scripts/tailwind_templates.py` 提供 `list / search / inspect / copy`，
> 第四阶段用 `search` + 业务名词（dashboard、table、modal、sidebar、form、stats、auth）检索区块。

---

## 2. 设计原则（EduVoyage 专属）

围绕 Phase 0 的产品理念"**以知识点为最小单元、以图谱为骨架、以数据驱动教学**"：

1. **数据密集界面要"密而可扫"**（dense & scannable）——教师/管理端的表格、看板优先信息密度；
   学生端学习页适度留白、降低认知负荷。
2. **图谱是一等公民**——知识图谱（G6）在学生端是导航主入口之一，在教师端是编辑主场景，
   需要全屏/大画布布局与清晰的掌握度配色（见 §4.3）。
3. **三态齐全**——每个数据视图都必须有 loading / empty / error 三态组件。
4. **权限即视图**——菜单、路由、按钮都由后端权限码驱动；无权限内容不渲染（而非仅禁用）。
5. **可访问性达标**——保留 `aria-label`、`sr-only`、焦点态、对话框语义、键盘可达；表格/列表语义化。

---

## 3. 设计令牌（Design Tokens）

> Skill 本身不规定固定调色板，依赖项目约定。以下为 EduVoyage 自定义令牌，
> 在 `tailwind.config.ts` 的 `theme.extend` 与 CSS 变量中落地，暗色用 `class` 策略
> （`<html class="dark">`，支持 auto/system 跟随）。

### 3.1 颜色
| 角色 | Token | 亮色 | 暗色 | 用途 |
|---|---|---|---|---|
| 品牌主色 | `brand` | `#2563eb` (blue-600) | `#3b82f6` | 主按钮、链接、激活态。呼应"航行/Voyage"的蓝 |
| 主色弱 | `brand-soft` | `#eff6ff` | `#1e293b` | 选中背景、标签底 |
| 成功 | `success` | `#16a34a` | `#22c55e` | 已掌握、通过 |
| 警示 | `warning` | `#d97706` | `#f59e0b` | 薄弱点、临近截止 |
| 危险 | `danger` | `#dc2626` | `#ef4444` | 删除、错误、未通过 |
| 中性底 | `surface` | `#ffffff` | `#0f172a` | 卡片/面板背景 |
| 页面底 | `bg` | `#f8fafc` | `#020617` | 页面背景 |
| 文本主 | `fg` | `#0f172a` | `#e2e8f0` | 正文 |
| 文本次 | `fg-muted` | `#64748b` | `#94a3b8` | 辅助说明 |
| 边框 | `border` | `#e2e8f0` | `#1e293b` | 分隔线、描边 |

知识图谱掌握度专用色（见 §4.3）单列，保证语义跨主题一致。

### 3.2 字体 / 字号
- 字体族：`Inter` / 系统无衬线 + 中文 `"PingFang SC","Microsoft YaHei",sans-serif`；等宽 `JetBrains Mono`（编程题/代码）。
- 字阶（Tailwind 默认 + 语义别名）：`text-xs`(标签) `text-sm`(表格/正文密) `text-base`(正文) `text-lg`(卡片标题) `text-xl`/`text-2xl`(页面标题) `text-3xl`(大盘数字)。
- 标题用 `text-balance`，长段落用 `text-pretty`。

### 3.3 间距 / 圆角 / 阴影
- 间距节奏：沿用 Tailwind 4/8 间距刻度；页面容器 `px-4 sm:px-6 lg:px-8`，区块纵向 `space-y-6`。
- 圆角：控件 `rounded-md`，卡片 `rounded-xl`，弹窗 `rounded-2xl`，头像/标签 `rounded-full`。
- 阴影：卡片 `shadow-xs` 静态、`hover:shadow-sm`；弹窗/悬浮 `shadow-lg`。避免重阴影。

### 3.4 断点
沿用 Tailwind 默认 `sm 640 / md 768 / lg 1024 / xl 1280 / 2xl 1536`；
侧边栏在 `lg` 以下折叠为抽屉（Headless UI Dialog）。

---

## 4. 布局与关键界面

### 4.1 应用外壳（App Shell）
三端共用 `AppShell` 骨架（来自 application-ui 区块适配），按角色注入不同导航：
```
┌───────────────────────────────────────────────┐
│ TopBar: logo · 全局搜索 · 通知(未读角标,SSE) · 主题切换 · 头像菜单 │
├──────────┬────────────────────────────────────┤
│ Sidebar  │  Router-View (页面区, max-w 容器)        │
│ (权限码   │   - 面包屑                              │
│  动态菜单)│   - 页面标题 + 操作区                    │
│          │   - 内容(三态)                          │
└──────────┴────────────────────────────────────┘
```
- 侧边栏菜单项由后端权限码过滤；`lg` 以下折叠为抽屉。
- 布局复用：`StudentLayout / TeacherLayout / AdminLayout` 仅配置导航与主题强调色，复用同一 `AppShell`。

### 4.2 各端核心页面（模板映射）
| 端 | 页面 | 选用区块类型 |
|---|---|---|
| 公共 | 登录/注册/找回密码 | marketing 的 auth / sign-in forms |
| 学生 | 我的学习(仪表盘) | application-ui dashboard + stats + progress |
| 学生 | 课程详情 / 知识点学习 | app shell + media + G6 图谱浏览器 |
| 学生 | 作业作答 | form controls + 倒计时 + 防切屏提示 |
| 学生 | 个人学情 | ECharts 雷达/折线/进度环 |
| 教师 | 课程管理 / 章节树 | tables + tree + slide-over 编辑 |
| 教师 | 图谱编辑器 | 全屏 G6 画布 + 属性侧栏(slide-over) |
| 教师 | 组卷 / 批改队列 | tables + filters + modal |
| 教师 | 教学分析 | stats + 热力图 + 排名表 |
| 管理 | 运营大盘 | stats grid + 趋势图 |
| 管理 | 用户/组织/课程审核 | data tables + bulk actions + dialog |

### 4.3 知识图谱（G6）配色——掌握度语义（跨主题一致）
| 状态 | 含义 | 节点色 |
|---|---|---|
| 未开始 | mastery_level=0 | 中性灰 `#94a3b8` |
| 学习中 | =1 | 品牌蓝 `#3b82f6` |
| 薄弱 | =2 | 警示橙 `#f59e0b` |
| 已掌握 | =3 | 成功绿 `#22c55e` |
边按 `type` 区分线型：`PREREQUISITE` 实线箭头、`SUCCESSOR` 虚线、`CONTAINS` 粗线、`RELATED` 点线。

---

## 5. 通用组件清单（`src/components/`）

三态与表单是复用核心，第四阶段先建这些再拼页面：
- `state/`：`LoadingState`、`EmptyState`、`ErrorState`、`AsyncBoundary`（统一包裹三态 + 重试）。
- `data/`：`DataTable`（排序/分页/选择/列配置，对接 `PageResult`）、`FilterBar`、`StatCard`、`Pagination`。
- `form/`：`FormField`（label+错误+帮助文本）、`TextInput`、`Select`、`DatePicker`、`Uploader`（分片上传，对接 §0 预签名/断点续传）。
- `overlay/`：`Modal`、`SlideOver`、`ConfirmDialog`、`Toast`（全局，挂 Axios 拦截器）、`Drawer`。
- `feedback/`：`Badge`、`Tag`、`ProgressRing`、`Skeleton`。
- `nav/`：`Sidebar`、`TopBar`、`Breadcrumb`、`UserMenu`、`NotificationBell`（SSE 未读计数）。
- `charts/`：`EChart`（按需注册）、`RadarChart`、`HeatmapChart`、`LineTrend`、`ProgressDonut`。
- `graph/`：`G6GraphViewer`（学生只读+着色）、`G6GraphEditor`（教师拖拽连线）。
- `auth/`：`PermissionGuard` 组件 + `v-permission` 指令（权限码控制渲染）。

---

## 6. 依赖与图标
- 图标库：**Heroicons**（`@heroicons/vue/24/outline` + `/24/solid`），与 Skill 区块默认一致，零额外映射成本。
- 无障碍交互原语：**Headless UI**（`@headlessui/vue`）—— Dialog/Menu/Listbox/Disclosure/Switch，
  区块直接可用，且自带键盘可达与焦点管理。
- 不引入重型 UI 框架（如 Element Plus），以保持 Tailwind 区块的视觉一致性与可控性。

---

## 7. 暗色模式与主题
- 策略：`darkMode: 'class'`；提供 **亮 / 暗 / 跟随系统(auto)** 三态切换，存 `localStorage` + 监听 `prefers-color-scheme`。
- 所有令牌成对定义（§3.1）；优先选 `target_compliant=true` 区块，减少暗色补色工作。
- 验收：每个页面亮/暗双跑，对比度达 WCAG AA。

---

## 8. 第四阶段落地顺序（预告，待批准后执行）
1. 项目骨架 + 公共封装（Vite/TS/Tailwind 配置、Axios 拦截器、Pinia stores、Router 守卫、`AppShell` 与三套 Layout、§5 通用组件库、设计令牌）。
2. 学生端 → 3. 教师端 → 4. 管理后台。

每一步用 Skill 的 `search`/`copy` 选区块 → 适配 → 接 API → 亮暗+响应式验证 → 提交。

---

## 待确认
1. 上面的设计令牌（主色蓝、字体、圆角/阴影尺度）是否符合你的预期？是否要换主色或加入学校 VI 色？
2. 图标库 Heroicons + Headless UI 是否接受（与 Skill 区块最契合）？
3. 是否现在就把 `frontend-tailwind-css/` 这套 Skill 资源（657 区块 + 脚本）拉入仓库作为 `frontend/.design/` 离线素材，便于第四阶段直接检索？
