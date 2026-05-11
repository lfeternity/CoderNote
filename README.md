# CoderNote

CoderNote 是一个面向编程学习者的全栈学习工作台，用来沉淀错题、学习资料、笔记、复习计划和 AI 辅助分析。项目采用前后端分离架构，后端提供统一 API、会话认证、文件上传和 AI 能力，前端提供学习内容管理、复习执行、统计分析和富文本编辑体验。

## 功能概览

- 用户与账号：注册、登录、图形验证码、资料维护、修改密码、头像上传与重置。
- 错题管理：错题录入、标签归类、掌握状态、附件上传、筛选搜索、PDF 导出。
- 学习资料：资料分类、来源记录、标签管理、收藏、与错题和笔记建立关联。
- 笔记系统：富文本编辑、标签、版本历史、收藏、关联错题和资料、PDF 导出。
- 复习系统：复习中心、复习模式、计划执行记录、每日统计、弱项标签分析。
- 搜索统计：全局搜索错题、资料、笔记和标签，提供学习数据统计看板。
- AI 助手：错题分析、内容总结、对话问答，支持多种模型提供方配置。
- 安全机制：Session + Cookie 登录态、Redis 会话存储、CSRF 防护、请求追踪。

## 技术栈

后端：

- Java 11
- Spring Boot 2.7.18
- MyBatis-Plus 3.5.5
- MySQL 8+
- Redis 6+
- Spring Session Redis
- Knife4j 3.0.3
- Apache PDFBox 2.0.31

前端：

- Vue 3
- Vue Router 4
- Pinia
- Element Plus
- Axios
- TipTap 3
- Vite 6

## 项目结构

```text
.
|-- backend/                         # Spring Boot 后端服务
|   |-- src/main/java/com/codernote/platform
|   |   |-- config/                  # 应用配置
|   |   |-- controller/              # API 控制器
|   |   |-- dto/                     # 请求和响应对象
|   |   |-- entity/                  # 数据实体
|   |   |-- mapper/                  # MyBatis-Plus Mapper
|   |   |-- security/                # 登录态、CSRF、请求追踪
|   |   |-- service/                 # 业务接口
|   |   `-- service/impl/            # 业务实现
|   `-- src/main/resources/
|       `-- application.yml          # 默认配置
|-- frontend/                        # Vue 前端项目
|   |-- src/api/                     # 接口封装
|   |-- src/components/              # 通用组件
|   |-- src/pages/                   # 页面
|   |-- src/router/                  # 路由
|   |-- src/store/                   # Pinia 状态
|   |-- src/styles/                  # 样式
|   `-- src/utils/                   # 工具函数
|-- docs/
|   `-- schema.sql                   # 数据库初始化脚本
|-- .github/                         # GitHub 模板与 CI 配置
|-- README.md
`-- LICENSE
```

## 环境要求

- JDK 11+
- Maven 3.8+
- Node.js 18+，推荐 Node.js 20 LTS
- npm 9+
- MySQL 8+
- Redis 6+

## 快速启动

### 1. 初始化数据库

在项目根目录执行：

```bash
mysql -u <用户名> -p < docs/schema.sql
```

脚本会自动创建并使用数据库 `coder_note`。

### 2. 配置后端

后端默认配置位于：

```text
backend/src/main/resources/application.yml
```

可参考：

```text
backend/.env.example
```

`backend/.env.example` 只是环境变量模板，Spring Boot 不会自动加载。需要在系统环境变量、IDE 运行配置、启动脚本或进程管理器中显式注入。

常用配置项：

- 数据库：`DB_URL`、`DB_USERNAME`、`DB_PASSWORD`
- Redis：`REDIS_HOST`、`REDIS_PORT`、`REDIS_PASSWORD`、`REDIS_DB`
- 会话：`SESSION_TIMEOUT`、`SESSION_COOKIE_SECURE`、`SESSION_COOKIE_SAME_SITE`
- 安全：`APP_SECURITY_CORS_ALLOWED_ORIGINS`、`APP_SECURITY_CSRF_ENABLED`
- 上传：`APP_UPLOAD_BASE_DIR`、`APP_AVATAR_BASE_DIR`
- AI：`APP_AI_ALLOW_RUNTIME_MODEL_CONFIG`、`APP_AI_RUNTIME_ALLOWED_HOSTS`、`QWEN_*`、`KIMI_*`、`OPENAI_*`、`DEEPSEEK_*`、`GEMINI_*`、`CLAUDE_*`
- 缓存与调度：`APP_CACHE_*`、`APP_SCHEDULE_REVIEW_DAILY_*`

### 3. 启动后端

```bash
cd backend
mvn -DskipTests spring-boot:run
```

默认服务地址：

```text
http://127.0.0.1:8080
```

### 4. 配置前端

前端环境变量示例：

```text
frontend/.env.example
```

常用配置项：

- `VITE_API_BASE_URL`：API 基础路径，默认 `/api/v1`
- `VITE_DEV_SERVER_PORT`：开发服务器端口，默认 `5173`
- `VITE_DEV_PROXY_TARGET`：开发代理目标，默认 `http://127.0.0.1:8080`

### 5. 启动前端

```bash
cd frontend
npm install
npm run dev
```

默认访问地址：

```text
http://127.0.0.1:5173
```

## 认证与安全

- API 前缀：`/api/v1`
- 统一响应格式：`{ code, message, data }`
- 登录态：`Session + Cookie`
- 会话存储：Redis
- 认证拦截：默认拦截 `/api/v1/**`，登录、注册、验证码和公开接口放行
- CSRF：写操作需要携带 `X-CSRF-Token`
- 请求追踪：支持并回传 `X-Request-Id`
- API 文档：启用 `KNIFE4J_ENABLE=true` 后访问 `http://127.0.0.1:8080/doc.html`

## AI 能力

默认模型为 `SAFE_GPT_SIM`。如需接入真实模型，可通过环境变量配置以下提供方：

- QWEN
- KIMI
- OPENAI
- DEEPSEEK
- GEMINI
- CLAUDE

主要接口：

- `GET /api/v1/ai/models`
- `POST /api/v1/ai/question-analysis`
- `POST /api/v1/ai/summary`
- `POST /api/v1/ai/chat`

系统默认按用户维度限制 AI 免费调用次数，每位用户每天 10 次，基于 Redis 计数。

## 文件上传

- 上传接口：`POST /api/v1/file/upload`
- 默认大小限制：20MB
- 支持类型：图片、PDF、Word、Markdown、文本、PPT、Excel
- 服务端会校验扩展名和文件魔数，降低伪造文件类型风险

## 构建与检查

后端编译：

```bash
cd backend
mvn -B -ntp -DskipTests clean compile
```

前端构建：

```bash
cd frontend
npm ci
npm run build
```

## 开发约定

- 不提交 `.env`、本地配置、构建产物、运行日志、上传文件和密钥。
- 数据库结构以 `docs/schema.sql` 为初始化参考。
- 前后端接口以 `/api/v1` 为统一前缀。
- 新功能应尽量保持页面、接口、DTO、实体和数据库脚本同步。
- 提交前建议至少执行一次后端编译和前端构建。

## 许可证

本项目使用 MIT License，详见 [LICENSE](LICENSE)。
