# EduVoyage

English | [中文](README.zh-CN.md)

EduVoyage is a university-oriented online learning platform inspired by Chaoxing Learning. It organizes courses around chapters, knowledge points, knowledge graphs, assessments, cloud drive files, discussion, notification, and learning analytics.

The repository is a frontend/backend monorepo:

- Backend: Java 25, Spring Boot 4.1, Spring WebFlux, reactive persistence, package `cn.edu.shmtu.eduvoyage`.
- Frontend: Vue 3, Vite, TypeScript, Tailwind CSS, Pinia, Vue Router.
- Data and infrastructure: MySQL 9, MongoDB 7, Redis 7.2, MinIO, optional Elasticsearch.

## Current Docker Instance

The Docker stack is running locally in this environment.

| Service | URL / Port | Notes |
|---|---:|---|
| Frontend | `http://localhost:5173` | Main web app |
| Backend API | `http://localhost:8080` | REST/SSE API |
| Backend health | `http://localhost:8080/actuator/health` | Should return `UP` |
| Swagger UI | `http://localhost:5173/swagger-ui/index.html` | Proxied through Nginx |
| OpenAPI JSON | `http://localhost:8080/v3/api-docs` | Raw API schema |
| MinIO Console | `http://localhost:9001` | `minioadmin` / `minioadmin` |
| MySQL | `localhost:3306` | `eduvoyage` / `eduvoyage`, database `eduvoyage` |
| MongoDB | `localhost:27017` | `root` / `secret`, database `eduvoyage` |
| Redis | `localhost:6379` | No password in local compose |

Default seed accounts:

| Role | Username | Password |
|---|---|---|
| Admin | `admin` | `Admin@123` |
| Teacher | `teacher` | `Teacher@123` |
| Student | `student` | `Student@123` |

## Project Structure

```text
.
├── .github/workflows/ci.yml          # GitHub Actions CI
├── deploy/k8s/                       # Kubernetes manifests and Kustomize config
├── docs/                             # Architecture and frontend design notes
├── docker-compose.yml                # Local full-stack Docker runtime
├── eduvoyage/                        # Spring Boot backend
│   ├── src/main/java/...             # identity/course/graph/assessment/drive/interaction/analytics
│   ├── src/main/resources/db/        # schema.sql and data.sql
│   └── Dockerfile
├── frontend/                         # Vue frontend
│   ├── src/
│   ├── nginx.conf
│   └── Dockerfile
└── README.zh-CN.md
```

## Feature Map

- Identity and organization: JWT access/refresh tokens, RBAC permissions, users, roles, departments, majors, classes.
- Course system: courses, chapters, knowledge points, courseware, enrollments, learning progress.
- Knowledge graph: graph editing APIs, prerequisite cycle detection, topological order, prerequisite chain, learning path.
- Assessment: question bank, homework papers, attempts, auto grading, manual grading, wrong-book records.
- Drive: personal/course space, directory tree, deduplication by SHA-256, quota, MinIO storage, share links.
- Interaction: discussions, replies, likes, announcements, notifications, SSE unread updates.
- Analytics: student, teacher, and admin dashboards backed by learning logs and aggregate queries.

## Run With Docker

Recommended command when Docker Buildx is available:

```bash
docker compose up -d --build
```

This environment currently lacks the Docker Buildx CLI plugin, so use the compatible fallback below:

```bash
docker build -t eduvoyage/backend:local ./eduvoyage
docker build -t eduvoyage/frontend:local ./frontend
docker compose up -d --no-build
```

Check status:

```bash
docker compose ps
curl -fsS http://localhost:8080/actuator/health
curl -I http://localhost:5173/
```

View logs:

```bash
docker compose logs -f backend
docker compose logs -f frontend
```

Stop the stack but keep data:

```bash
docker compose down
```

Stop and remove local data volumes:

```bash
docker compose down -v
```

### Optional Elasticsearch

The current application code keeps Elasticsearch as infrastructure-ready configuration but does not rely on Elasticsearch repositories for the local development path. The default Docker stack therefore skips Elasticsearch to avoid a very large image pull during first setup.

To include it:

```bash
docker compose --profile search up -d elasticsearch
```

If backend features later require Elasticsearch at runtime, enable repository/health settings and add the backend dependency back to the compose profile.

## Local Development Without Full Docker App Images

Start infrastructure:

```bash
docker compose up -d mysql redis mongodb minio minio-init
```

Run backend:

```bash
cd eduvoyage
./gradlew bootRun
```

Run frontend:

```bash
cd frontend
bun install
bun run dev
```

Local dev URLs are the same:

- Frontend: `http://localhost:5173`
- Backend: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`

## Backend Commands

```bash
cd eduvoyage
./gradlew compileJava
./gradlew compileTestJava
./gradlew test
./gradlew bootRun
```

Integration tests use Testcontainers for MySQL/MongoDB/Redis/MinIO where needed. They are configured to skip automatically when Docker is unavailable.

## Frontend Commands

```bash
cd frontend
bun install
bun run build
```

The production frontend image builds the Vite app with Bun and serves it through Nginx. Nginx proxies `/api`, `/api/sse`, `/v3/api-docs`, `/swagger-ui`, `/swagger-ui.html`, and `/actuator` to the backend service.

## API Usage

Login:

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"Admin@123"}'
```

All API responses use the shared envelope:

```json
{
  "code": 0,
  "message": "success",
  "data": {},
  "timestamp": "..."
}
```

Important API groups:

- Auth and users: `/api/auth/**`, `/api/users/**`
- Organization: `/api/org/**`
- Courses and chapters: `/api/courses/**`, `/api/chapters/**`
- Knowledge nodes and courseware: `/api/nodes/**`, `/api/coursewares/**`
- Knowledge graph: `/api/courses/{courseId}/graph/**`
- Assessment: `/api/questions/**`, `/api/homeworks/**`, `/api/submissions/**`
- Drive: `/api/drive/**`
- Discussion and notifications: `/api/discussions/**`, `/api/notifications/**`, `/api/sse/notifications`
- Analytics: `/api/analytics/**`
- Operations: `/actuator/health`, `/actuator/prometheus`

## Configuration

Local Docker Compose uses safe local defaults. Do not reuse these values in production.

Key environment variables:

| Variable | Purpose |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `dev` for local compose, `prod` for deployment |
| `EDUVOYAGE_JWT_SECRET` | JWT signing secret, use 32+ random bytes |
| `R2DBC_URL`, `R2DBC_USERNAME`, `R2DBC_PASSWORD` | MySQL R2DBC connection |
| `MONGODB_URI`, `SPRING_MONGODB_URI`, `SPRING_DATA_MONGODB_URI` | MongoDB connection |
| `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD` | Redis connection |
| `MINIO_ENDPOINT`, `MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY`, `MINIO_BUCKET` | MinIO object storage |
| `ELASTICSEARCH_URIS` | Optional Elasticsearch endpoint |

## Kubernetes Deployment

Kubernetes assets live in `deploy/k8s`.

Included resources:

- Namespace, ResourceQuota, LimitRange.
- MySQL, Redis, MongoDB, MinIO, Elasticsearch StatefulSets.
- MySQL initialization Job.
- Backend and frontend Deployments/Services.
- Ingress.
- HPA.

Render locally:

```bash
docker run --rm -v "$PWD":/work -w /work bitnami/kubectl:latest kustomize deploy/k8s
```

Apply to a cluster:

```bash
kubectl apply -k deploy/k8s
```

Before applying to a non-local cluster:

- Replace every value in `deploy/k8s/secret.yaml`.
- Update image names if you do not use the default GHCR repository.
- Sync `deploy/k8s/db/schema.sql` and `deploy/k8s/db/data.sql` whenever backend DB scripts change.
- Make sure metrics-server is installed if HPA is expected to scale workloads.

Default ingress host:

```text
eduvoyage.local
```

For local ingress testing, add:

```text
127.0.0.1 eduvoyage.local
```

## GitHub Actions

Workflow: `.github/workflows/ci.yml`

Triggers:

- Push to `main`.
- Pull request targeting `main`.
- Manual `workflow_dispatch`.

Jobs:

| Job | What It Does |
|---|---|
| `backend` | Sets up Java 25, runs `compileJava`, `compileTestJava`, and `test`. |
| `frontend` | Sets up Bun 1.3.13, runs `bun install --frozen-lockfile`, then `bun run build`. |
| `manifests` | Runs `docker compose config` and renders K8S manifests with Kustomize. |
| `images` | Builds backend and frontend Docker images with Docker Buildx. |

Automatic image validation happens on every push and pull request, but images are not pushed by default.

To publish images to GHCR:

1. Open the repository on GitHub.
2. Go to **Actions**.
3. Select the **CI** workflow.
4. Click **Run workflow**.
5. Enable `push_images`.
6. Run the workflow.

The workflow pushes:

- `ghcr.io/mrnobody-zst/eduvoyage-backend:${GITHUB_SHA}`
- `ghcr.io/mrnobody-zst/eduvoyage-backend:latest`
- `ghcr.io/mrnobody-zst/eduvoyage-frontend:${GITHUB_SHA}`
- `ghcr.io/mrnobody-zst/eduvoyage-frontend:latest`

## Handoff Notes

- Keep backend layers separated: controller -> service -> repository.
- Keep DTOs separate from entities.
- Preserve reactive contracts: use `Mono`/`Flux`; avoid blocking calls on event-loop threads.
- Keep pure business logic in I/O-free classes and cover it with focused tests.
- Use `BizException` and `BizErrorCode` for domain errors.
- For Snowflake-style IDs with R2DBC entities, keep using `entityTemplate.insert(...).using(...)`.
- When changing `schema.sql` or `data.sql`, also update `deploy/k8s/db/`.
- When adding frontend API calls, use the shared Axios client and service wrapper style already in `frontend/src/services`.
- When adding pages, wire routes through the role/permission-aware router and AppShell navigation.
- Prefer `/swagger-ui/index.html` over `/swagger-ui.html` in local Docker, because the direct index path avoids a SpringDoc WebFlux redirect edge case.

## Troubleshooting

Buildx missing locally:

```bash
docker build -t eduvoyage/backend:local ./eduvoyage
docker build -t eduvoyage/frontend:local ./frontend
docker compose up -d --no-build
```

Port already in use:

```bash
ss -ltnp | rg ':(3306|6379|27017|9000|9001|8080|5173)\b'
```

Reset all local Docker data for this project:

```bash
docker compose down -v
```

Check backend logs:

```bash
docker compose logs -f backend
```

Check seeded login:

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"Admin@123"}'
```
