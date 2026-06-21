# EduVoyage

[English](README.md) | 中文

EduVoyage 是一套面向高校在线教学场景的学习平台，产品形态对标“学习通”。平台围绕课程章节、知识点、知识图谱、作业测评、网盘、讨论通知和学习分析组织教学流程。

本仓库采用前后端同仓库管理：

- 后端：Java 25、Spring Boot 4.1、Spring WebFlux、响应式数据访问，包名 `cn.edu.shmtu.eduvoyage`。
- 前端：Vue 3、Vite、TypeScript、Tailwind CSS、Pinia、Vue Router。
- 数据与基础设施：MySQL 9、MongoDB 7、Redis 7.2、MinIO、可选 Elasticsearch。

## 当前 Docker 实例

当前环境已经启动 EduVoyage Docker 栈。

| 服务 | 地址 / 端口 | 说明 |
|---|---:|---|
| 前端 | `http://localhost:5173` | 主应用入口 |
| 后端 API | `http://localhost:8080` | REST/SSE API |
| 后端健康检查 | `http://localhost:8080/actuator/health` | 应返回 `UP` |
| Swagger UI | `http://localhost:5173/swagger-ui/index.html` | 经过 Nginx 代理 |
| OpenAPI JSON | `http://localhost:8080/v3/api-docs` | 原始 API schema |
| MinIO Console | `http://localhost:9001` | `minioadmin` / `minioadmin` |
| MySQL | `localhost:3306` | `eduvoyage` / `eduvoyage`，数据库 `eduvoyage` |
| MongoDB | `localhost:27017` | `root` / `secret`，数据库 `eduvoyage` |
| Redis | `localhost:6379` | 本地 Compose 无密码 |

默认种子账号：

| 角色 | 用户名 | 密码 |
|---|---|---|
| 管理员 | `admin` | `Admin@123` |
| 教师 | `teacher` | `Teacher@123` |
| 学生 | `student` | `Student@123` |

## 项目结构

```text
.
├── .github/workflows/ci.yml          # GitHub Actions CI
├── deploy/k8s/                       # Kubernetes 清单与 Kustomize 配置
├── docs/                             # 架构与前端设计文档
├── docker-compose.yml                # 本地完整 Docker 运行栈
├── eduvoyage/                        # Spring Boot 后端
│   ├── src/main/java/...             # identity/course/graph/assessment/drive/interaction/analytics
│   ├── src/main/resources/db/        # schema.sql 与 data.sql
│   └── Dockerfile
├── frontend/                         # Vue 前端
│   ├── src/
│   ├── nginx.conf
│   └── Dockerfile
└── README.md
```

## 功能模块

- 用户与组织：JWT access/refresh token、RBAC 权限、用户、角色、院系、专业、班级。
- 课程体系：课程、章节、知识点、课件、选课、学习进度。
- 知识图谱：图谱编辑 API、前置关系成环检测、拓扑排序、前置链路、学习路径。
- 作业测评：题库、作业试卷、作答、自动判分、教师批改、错题本。
- 网盘：个人/课程空间、目录树、SHA-256 去重、配额、MinIO 存储、分享链接。
- 互动通知：讨论、回复、点赞、公告、站内通知、SSE 未读推送。
- 学习分析：学生、教师、管理员仪表盘。

## Docker 启动

如果本机 Docker Buildx 可用，推荐：

```bash
docker compose up -d --build
```

当前这台机器缺少 Docker Buildx CLI 插件，因此使用兼容启动方式：

```bash
docker build -t eduvoyage/backend:local ./eduvoyage
docker build -t eduvoyage/frontend:local ./frontend
docker compose up -d --no-build
```

检查状态：

```bash
docker compose ps
curl -fsS http://localhost:8080/actuator/health
curl -I http://localhost:5173/
```

查看日志：

```bash
docker compose logs -f backend
docker compose logs -f frontend
```

停止但保留数据：

```bash
docker compose down
```

停止并删除本地数据卷：

```bash
docker compose down -v
```

### 可选 Elasticsearch

当前代码保留了 Elasticsearch 基础设施配置，但本地开发路径不依赖 Elasticsearch repository。默认 Docker 栈会跳过 Elasticsearch，避免首次启动时拉取超大镜像。

如需单独启动：

```bash
docker compose --profile search up -d elasticsearch
```

如果后续业务功能开始依赖 Elasticsearch，需要重新启用后端 repository/health 设置，并把 backend 对 Elasticsearch 的依赖加入相同 profile。

## 本地开发

只启动基础设施：

```bash
docker compose up -d mysql redis mongodb minio minio-init
```

启动后端：

```bash
cd eduvoyage
./gradlew bootRun
```

启动前端：

```bash
cd frontend
bun install
bun run dev
```

常用地址：

- 前端：`http://localhost:5173`
- 后端：`http://localhost:8080`
- Swagger UI：`http://localhost:8080/swagger-ui/index.html`

## 后端命令

```bash
cd eduvoyage
./gradlew compileJava
./gradlew compileTestJava
./gradlew test
./gradlew bootRun
```

集成测试按需使用 Testcontainers 启动 MySQL、MongoDB、Redis、MinIO 等依赖；无 Docker 环境时相关测试会自动跳过。

## 前端命令

```bash
cd frontend
bun install
bun run build
```

生产前端镜像使用 Bun 构建 Vite 应用，再通过 Nginx 提供静态文件。Nginx 会把 `/api`、`/api/sse`、`/v3/api-docs`、`/swagger-ui`、`/swagger-ui.html`、`/actuator` 代理到后端服务。

## API 使用

登录示例：

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"Admin@123"}'
```

统一返回体：

```json
{
  "code": 0,
  "message": "success",
  "data": {},
  "timestamp": "..."
}
```

主要 API 分组：

- 认证与用户：`/api/auth/**`、`/api/users/**`
- 组织架构：`/api/org/**`
- 课程与章节：`/api/courses/**`、`/api/chapters/**`
- 知识点与课件：`/api/nodes/**`、`/api/coursewares/**`
- 知识图谱：`/api/courses/{courseId}/graph/**`
- 作业测评：`/api/questions/**`、`/api/homeworks/**`、`/api/submissions/**`
- 网盘：`/api/drive/**`
- 讨论与通知：`/api/discussions/**`、`/api/notifications/**`、`/api/sse/notifications`
- 学习分析：`/api/analytics/**`
- 运维：`/actuator/health`、`/actuator/prometheus`

## 配置

本地 Docker Compose 使用仅适合开发的默认值，生产环境不要复用这些密码。

关键环境变量：

| 变量 | 作用 |
|---|---|
| `SPRING_PROFILES_ACTIVE` | 本地 Compose 用 `dev`，部署用 `prod` |
| `EDUVOYAGE_JWT_SECRET` | JWT 签名密钥，生产使用 32+ 字节随机值 |
| `R2DBC_URL`, `R2DBC_USERNAME`, `R2DBC_PASSWORD` | MySQL R2DBC 连接 |
| `MONGODB_URI`, `SPRING_MONGODB_URI`, `SPRING_DATA_MONGODB_URI` | MongoDB 连接 |
| `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD` | Redis 连接 |
| `MINIO_ENDPOINT`, `MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY`, `MINIO_BUCKET` | MinIO 对象存储 |
| `ELASTICSEARCH_URIS` | 可选 Elasticsearch 地址 |

## Kubernetes 部署

K8S 资源位于 `deploy/k8s`。

包含：

- Namespace、ResourceQuota、LimitRange。
- MySQL、Redis、MongoDB、MinIO、Elasticsearch StatefulSet。
- MySQL 初始化 Job。
- 后端与前端 Deployment/Service。
- Ingress。
- HPA。

本地渲染：

```bash
docker run --rm -v "$PWD":/work -w /work bitnami/kubectl:latest kustomize deploy/k8s
```

应用到集群：

```bash
kubectl apply -k deploy/k8s
```

部署到非本地集群前：

- 替换 `deploy/k8s/secret.yaml` 中所有示例密钥。
- 如果不使用默认 GHCR 仓库，修改镜像名。
- 后端 `schema.sql` 或 `data.sql` 变化后，同步更新 `deploy/k8s/db/`。
- 如需 HPA 生效，集群需安装 metrics-server。

默认 Ingress host：

```text
eduvoyage.local
```

本地 Ingress 测试可添加 hosts：

```text
127.0.0.1 eduvoyage.local
```

## GitHub Actions

工作流文件：`.github/workflows/ci.yml`

触发条件：

- push 到 `main`。
- pull request 指向 `main`。
- 手动 `workflow_dispatch`。

任务：

| Job | 内容 |
|---|---|
| `backend` | 安装 Java 25，执行 `compileJava`、`compileTestJava`、`test`。 |
| `frontend` | 安装 Bun 1.3.13，执行 `bun install --frozen-lockfile` 和 `bun run build`。 |
| `manifests` | 执行 `docker compose config`，并用 Kustomize 渲染 K8S 清单。 |
| `images` | 使用 Docker Buildx 构建后端和前端镜像。 |

每次 push 和 pull request 都会自动验证镜像构建，但默认不推送镜像。

发布镜像到 GHCR：

1. 打开 GitHub 仓库。
2. 进入 **Actions**。
3. 选择 **CI** workflow。
4. 点击 **Run workflow**。
5. 启用 `push_images`。
6. 运行 workflow。

会推送：

- `ghcr.io/mrnobody-zst/eduvoyage-backend:${GITHUB_SHA}`
- `ghcr.io/mrnobody-zst/eduvoyage-backend:latest`
- `ghcr.io/mrnobody-zst/eduvoyage-frontend:${GITHUB_SHA}`
- `ghcr.io/mrnobody-zst/eduvoyage-frontend:latest`

## 交接与开发约定

- 后端保持 controller -> service -> repository 分层。
- DTO 与 Entity 严格分离。
- 保持响应式契约：使用 `Mono`/`Flux`，避免在 event-loop 线程执行阻塞调用。
- 纯业务逻辑放在无 I/O 类中，并补聚焦单测。
- 业务错误使用 `BizException` 和 `BizErrorCode`。
- R2DBC 实体使用 Snowflake 风格 ID 时，继续用 `entityTemplate.insert(...).using(...)`。
- 修改 `schema.sql` 或 `data.sql` 后，同时更新 `deploy/k8s/db/`。
- 新增前端 API 调用时，沿用 `frontend/src/services` 中的 Axios 封装风格。
- 新增页面时，通过已有的角色/权限路由和 AppShell 导航接入。
- 本地 Docker 中优先访问 `/swagger-ui/index.html`，避免 `/swagger-ui.html` 的 SpringDoc WebFlux redirect 边界问题。

## 常见问题

本机缺少 Docker Buildx：

```bash
docker build -t eduvoyage/backend:local ./eduvoyage
docker build -t eduvoyage/frontend:local ./frontend
docker compose up -d --no-build
```

端口被占用：

```bash
ss -ltnp | rg ':(3306|6379|27017|9000|9001|8080|5173)\b'
```

重置本项目 Docker 数据：

```bash
docker compose down -v
```

查看后端日志：

```bash
docker compose logs -f backend
```

验证种子账号登录：

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"Admin@123"}'
```
