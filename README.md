# AI 面试官（ai_mianshi_ljj）

毕业设计项目：基于大模型的智能模拟面试系统，支持 AI 面试对话、刷题练习、社区论坛、个人中心与会员订阅，并提供完整管理后台（用户管控、知识库、提示词与系统配置）。

仓库地址：https://github.com/ljj520zzz/ai_mianshi

---

## 功能概览

### 用户端

| 模块 | 说明 |
|------|------|
| 注册 / 登录 | JWT + Redis 会话；支持登出；账号禁用后立即失效 |
| AI 面试 | 同步 / SSE 流式对话，面试历史，PDF 综合评价报告 |
| 刷题练习 | 题目列表、提交判题、练习记录与统计 |
| 论坛 | 帖子列表 / 详情 / 发帖、点赞、收藏（列表与详情可游客浏览） |
| 个人中心 | 资料与头像、仪表盘、活跃度热力图、对话历史 |
| 会员 | Stripe 订阅下单、状态查询、Webhook 回调 |

### 管理端

| 模块 | 说明 |
|------|------|
| 管理员登录 | JWT + Redis，请求头 `X-Admin-Token` |
| 用户管理 | 分页、新增/编辑、逻辑删除、**禁用/启用**、**强制下线** |
| 会话 / 消息 | 分页查询、逻辑删除 |
| 提示词 / 配置 | CRUD；可配置后台站点标题等 |
| 知识库 | Markdown 文档管理、批量导入、刷新 PGVector 向量库 |

---

## 技术栈

### 后端

- Java 17、Spring Boot 3.4.4
- Spring AI 1.0.0 / Spring AI Alibaba 1.0.0.2
- 大模型：阿里云 DashScope（默认 `qwen-plus`），可选 Ollama
- MyBatis 3.0.3、MySQL（业务库）、PostgreSQL + PGVector（向量检索）
- Redis（JWT 会话白名单）、JJWT 0.12.6
- Knife4j 4.4.0、Stripe、iText PDF

### 前端

| 工程 | 技术 | 开发端口 |
|------|------|----------|
| `ai-agent-frontend` | Vue 3 + Vite + axios + markdown-it | **3000** |
| `ai-agent-admin-frontend` | Vue 3 + Vite + Ant Design Vue | **3001** |

### 可选附属

- `image-search-mcp-server`：图片搜索 MCP 服务（默认端口 **8127**）

---

## 目录结构

```text
ai_mianshi_ljj/
├── pom.xml
├── src/main/java/com/ljj/aiagent/
│   ├── auth/           # JWT + Redis 认证拦截
│   ├── user/           # 用户注册登录、个人资料
│   ├── interview/      # AI 面试
│   ├── conversation/   # 会话、报告导出
│   ├── forum/          # 论坛
│   ├── practice/       # 刷题
│   ├── payment/        # Stripe 会员
│   ├── admin/          # 管理端 API、知识库
│   ├── rag/            # 向量库 / 文档加载
│   └── ...
├── src/main/resources/
│   ├── application.yml
│   ├── application-local.yml   # 本地覆盖（勿提交密钥）
│   └── application-prod.yml
├── ai-agent-frontend/          # 用户端
├── ai-agent-admin-frontend/    # 管理端
└── image-search-mcp-server/    # 可选 MCP
```

---

## 环境依赖

启动前请准备：

1. **JDK 17+**、**Maven 3.8+**、**Node.js 18+**
2. **Redis**（默认 `127.0.0.1:6379`）
3. **MySQL**（业务数据）
4. **PostgreSQL + pgvector**（RAG / 知识库向量，不用 RAG 也可先关掉）
5. **阿里云 DashScope API Key**（对话必需）

---

## 配置说明

主配置：`src/main/resources/application.yml`

| 配置项 | 说明 |
|--------|------|
| `server.port` | 后端端口，默认 **682** |
| `server.servlet.context-path` | 固定为 `/api` |
| `spring.data.redis.*` | Redis 地址 / 密码（可用环境变量 `REDIS_HOST` 等） |
| `spring.datasource.*` | PostgreSQL（向量库） |
| `app.datasource.mysql.*` | MySQL（业务库） |
| `spring.ai.dashscope.api-key` | DashScope 密钥 |
| `spring.ai.dashscope.chat.options.model` | 默认 `qwen-plus` |
| `app.auth.jwt-secret` | JWT 密钥（≥32 字符，建议用 `JWT_SECRET`） |
| `app.auth.token-expire-seconds` | Token 有效期，默认 7200 秒 |
| `app.interview.rag-enabled` | 面试是否走知识库 RAG，默认 `false` |
| `app.knowledge-dir` | 知识库 Markdown 根目录 |
| `stripe.*` | 会员支付相关（可选） |

**建议：** 本地密钥写在 `application-local.yml` 或环境变量中，不要把真实 Key 提交到 GitHub。

常用环境变量：

```text
REDIS_HOST / REDIS_PORT / REDIS_PASSWORD
JWT_SECRET / JWT_EXPIRE_SECONDS
STRIPE_SECRET_KEY / STRIPE_WEBHOOK_SECRET / STRIPE_PRICE_ID
APP_FRONTEND_PUBLIC_URL
```

前端可选：

```text
VITE_API_BASE_URL          # 生产环境 API 基址，如 http://host:682/api
VITE_DEV_PROXY_TARGET      # 开发代理目标，默认 http://localhost:682
```

---

## 本地启动

### 1. 后端

```bash
# 项目根目录
mvn spring-boot:run
# 或
mvn clean package -DskipTests
java -jar target/ai_mianshi_ljj-0.0.1-SNAPSHOT.jar
```

- 服务地址：`http://localhost:682/api`
- 接口文档：`http://localhost:682/api/doc.html`

### 2. 用户前端

```bash
cd ai-agent-frontend
npm install
npm run dev
```

访问：http://localhost:3000  
开发环境 `/api` 已代理到 `http://localhost:682`。

### 3. 管理端前端

```bash
cd ai-agent-admin-frontend
npm install
npm run dev
```

访问：http://localhost:3001

### 4. 可选 MCP（图片搜索）

```bash
cd image-search-mcp-server
mvn spring-boot:run
```

在 `application.yml` 中取消 `spring.ai.mcp` 相关注释后生效。

---

## 认证机制

### 用户端

1. `POST /api/user/login` 校验账号密码（MD5），签发 JWT（含 `jti`、`scope=user`）
2. Redis 双向绑定：
   - `auth:tk:user:{jti}` → `userId`
   - `auth:uu:user:{userId}` → 该用户全部 `jti`（Set）
3. 业务请求头：`Authorization: Bearer {token}`
4. SSE 流式也可通过 Query 传 `token`
5. `POST /api/user/logout` 删除当前会话
6. 账号禁用或强制下线时，清理该用户 Redis 中全部 token

### 管理端

1. `POST /api/admin-api/auth/login`（需 `admin` 角色）
2. 请求头：`X-Admin-Token: {token}`
3. 禁用用户：`POST /api/admin-api/user/status`（`status`：1 启用 / 0 禁用）
4. 强制下线：`POST /api/admin-api/user/force-logout`

> 当前实现为**多设备可同时在线**（同一账号可持有多个 jti）。若需「单设备登录」，登录成功前先对该用户执行 `revokeAllByUser` 即可。

---

## 主要 API 一览

全局前缀：`/api`（即完整路径形如 `http://localhost:682/api/...`）

| 前缀 | 说明 |
|------|------|
| `/user` | 注册、登录、登出、资料、仪表盘、活跃度 |
| `/interview` | 面试发送、流式、历史 |
| `/forum` | 论坛（列表/热门/详情游客可读） |
| `/practice` | 刷题 |
| `/conversation` | 会话、SSE、报告导出 |
| `/membership` | 会员下单与状态 |
| `/stripe/webhook` | Stripe 回调 |
| `/admin-api` | 管理端全部接口 |
| `/store` | 用户侧提示词 / 配置 |
| `/ai` | 演示用 AI 接口（免用户鉴权） |
| `/health` | 健康检查 |

HTTP 约定：仅使用 **GET / POST**；写操作走 POST + JSON Body；列表接口分页；删除均为**逻辑删除**。

---

## 数据库说明

- **MySQL**：用户、会话、消息、论坛、练习、配置等业务表（部分启动时自动初始化字段，如用户 `status`）
- **PostgreSQL + PGVector**：知识库向量检索（维度默认 1536，索引 HNSW）
- **无外键设计**；列表删除统一逻辑删除

---

## 部署提示

1. 生产建议使用 `spring.profiles.active=prod`，密钥全部走环境变量
2. 前端构建：`npm run build`，通过 Nginx 反代 `/api` 到后端 `682`
3. 需保证 Redis、MySQL、PostgreSQL 与后端网络互通
4. 若本机访问 GitHub / 依赖仓库需代理，终端也要配置代理（浏览器系统代理对 `git`/`mvn` 不一定生效）

---

## 开发约定（摘要）

- Controller 只做参数接收与调用 Service，不写业务、不直接访问 Mapper
- DTO / QueryDTO / VO / Entity 分离
- 构造器注入（推荐 Lombok `@RequiredArgsConstructor`）
- 统一 `ApiResponse` 返回；业务异常由全局处理器处理

---

## 作者

李嘉骏 — 毕业设计《AI 面试官》
)
