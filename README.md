# EduVoyage

EduVoyage 是一套面向高校在线教学场景的学习平台，产品形态对标“学习通”，核心组织方式是“课程章节 + 知识图谱 + 学习分析”。项目采用前后端分离但同仓库管理：

- 后端：Spring Boot WebFlux 响应式模块化单体，包名 `cn.edu.shmtu.eduvoyage`
- 前端：Vue 3 + Vite + TypeScript + Tailwind CSS
- 数据：MySQL 9、MongoDB 7、Redis 7.2、MinIO、Elasticsearch

## 项目结构

```text
.
├── docs/                         # 架构与前端设计文档
├── deploy/k8s/                   # Kubernetes manifests + kustomization
├── eduvoyage/                    # Spring Boot 后端
│   ├── src/main/java/...         # identity/course/graph/assessment/drive/interaction/analytics
│   ├── src/main/resources/db/    # schema.sql + data.sql
│   └── Dockerfile
├── frontend/                     # Vue 前端
│   ├── src/
│   ├── nginx.conf
│   └── Dockerfile
├── docker-compose.yml            # 一键本地完整栈
└── .github/workflows/ci.yml      # CI：后端测试、前端构建、镜像构建
```

## 默认账号

开发种子数据会创建三类账号：

| 角色 | 用户名 | 密码 |
|---|---|---|
| 管理员 | `admin` | `Admin@123` |
| 教师 | `teacher` | `Teacher@123` |
| 学生 | `student` | `Student@123` |

## 本地开发

环境要求：

- JDK 25
- Bun 1.3+
- Docker / Docker Compose

启动基础设施：

```bash
docker compose up -d mysql redis mongodb minio minio-init elasticsearch
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

访问：

- 前端：`http://localhost:5173`
- 后端：`http://localhost:8080`
- Swagger UI：`http://localhost:8080/swagger-ui.html`
- MinIO Console：`http://localhost:9001` (`minioadmin` / `minioadmin`)

## Docker 一键启动

根目录提供完整 `docker-compose.yml`，会启动 MySQL、Redis、MongoDB、MinIO、Elasticsearch、后端和前端。后端在 compose 中使用 `dev` profile，启动时会自动执行 `schema.sql` 和 `data.sql`。

```bash
docker compose up --build
```

访问：

- 前端：`http://localhost:5173`
- 后端 API：`http://localhost:8080`
- Swagger UI：`http://localhost:5173/swagger-ui.html` 或 `http://localhost:8080/swagger-ui.html`

停止并保留数据：

```bash
docker compose down
```

停止并清理本地卷：

```bash
docker compose down -v
```

## Kubernetes 部署

K8S 清单位于 `deploy/k8s`，包含：

- Namespace、ResourceQuota、LimitRange
- MySQL / Redis / MongoDB / MinIO / Elasticsearch StatefulSet
- MySQL 初始化 Job（通过 Kustomize 将 `schema.sql`、`data.sql` 打入 ConfigMap）
- 后端、前端 Deployment + Service
- Ingress
- HPA

`deploy/k8s/db/` 内随附一份初始化 SQL 副本，用于 Kustomize 生成数据库初始化 ConfigMap；后端 schema 或种子数据变更后请同步更新这里。

部署前请先修改 `deploy/k8s/secret.yaml` 中所有占位密钥，并根据镜像仓库调整：

```bash
kubectl kustomize deploy/k8s | less
kubectl apply -k deploy/k8s
```

如需使用自己的镜像：

```bash
kubectl -n eduvoyage set image deployment/eduvoyage-backend backend=your-registry/eduvoyage-backend:tag
kubectl -n eduvoyage set image deployment/eduvoyage-frontend frontend=your-registry/eduvoyage-frontend:tag
```

默认 Ingress host 为 `eduvoyage.local`。本地测试可添加 hosts：

```text
127.0.0.1 eduvoyage.local
```

资源建议：

- 单环境最小：4 CPU / 8 GiB RAM
- 生产建议：将 MySQL、MongoDB、Redis、MinIO、Elasticsearch 托管化或独立高可用部署
- HPA 依赖 metrics-server

## API 概览

所有接口统一返回 `Result<T>`，分页返回 `PageResult<T>`。主要路径：

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

Swagger 分组在 `application.yml` 中配置，启动后访问 `/swagger-ui.html`。

## 验证命令

后端：

```bash
cd eduvoyage
./gradlew compileJava compileTestJava test
```

前端：

```bash
cd frontend
bun install
bun run build
```

镜像：

```bash
docker build -t eduvoyage/backend:local ./eduvoyage
docker build -t eduvoyage/frontend:local ./frontend
```

## CI

`.github/workflows/ci.yml` 在 push / pull request 时执行：

1. 后端 `compileJava compileTestJava test`
2. 前端 `bun install --frozen-lockfile` + `bun run build`
3. 后端/前端 Docker image build

默认 CI 只构建镜像验证，不推送到 registry。生产镜像推送可在此 workflow 基础上增加 registry login 与 `push: true`。

## 配置与安全

后端生产 profile 使用环境变量注入敏感配置：

- `EDUVOYAGE_JWT_SECRET`
- `R2DBC_URL` / `R2DBC_USERNAME` / `R2DBC_PASSWORD`
- `MONGODB_URI`
- `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD`
- `MINIO_ENDPOINT` / `MINIO_ACCESS_KEY` / `MINIO_SECRET_KEY` / `MINIO_BUCKET`
- `ELASTICSEARCH_URIS` / `ELASTICSEARCH_USERNAME` / `ELASTICSEARCH_PASSWORD`

不要在生产环境复用 compose 或 K8S 示例中的默认密码。
