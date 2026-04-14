# CoderNote

CoderNote 是一个面向编程学习者的全栈学习工作区，整合了错题管理、学习资料、笔记、复习计划、统计分析与 AI 助手。

## 核心能力

- 用户系统：注册、登录、验证码、资料更新、改密、头像上传与重置
- 错题管理：标签、掌握状态、附件、筛选搜索、单个/批量 PDF 导出
- 学习资料：分类与来源管理、标签、收藏、与错题/笔记联动
- 笔记系统：富文本编辑、标签、版本历史、收藏、关联错题/资料、PDF 导出
- 复习系统：复习中心、复习模式、执行记录、每日统计、弱项标签
- 搜索与统计：全局搜索（错题/资料/笔记）与仪表盘统计
- AI 助手：错题分析、内容总结、对话问答，支持多提供方
- OAuth 账号体系：支持 GitHub / QQ / 微信登录与账号绑定

## 技术栈

后端：

- Java 11
- Spring Boot 2.7.18
- MyBatis-Plus 3.5.5
- MySQL 8+
- Redis 6+
- Spring Session (Redis)
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
|-- backend/                            # Spring Boot 服务
|   |-- src/main/java/com/codernote/platform
|   |   |-- controller/                 # API 控制器
|   |   |-- service/                    # 业务层
|   |   |-- config/                     # 应用配置
|   |   `-- security/                   # 认证、CSRF、请求追踪
|   `-- src/main/resources/
|       |-- mapper/                     # MyBatis XML
|       `-- application.yml             # 默认配置
|-- frontend/                           # Vue 3 前端项目
|   |-- src/pages/                      # 页面
|   |-- src/components/                 # 组件
|   `-- src/utils/request.js            # Axios 封装（含 CSRF 处理）
|-- docs/
|   |-- schema.sql                      # 数据库初始化脚本
|   `-- OPEN_SOURCE_RELEASE_CHECKLIST.md
|-- .github/
|   |-- workflows/ci.yml                # CI
|   |-- ISSUE_TEMPLATE/
|   `-- pull_request_template.md
`-- README.md
```

## 快速开始

### 1. 环境准备

- JDK 11+
- Maven 3.8+
- Node.js 18+（推荐 20 LTS）
- npm 9+
- MySQL 8+
- Redis 6+

### 2. 初始化数据库

执行脚本：

```bash
mysql -u <user> -p < docs/schema.sql
```

脚本会自动创建并使用数据库：`coder_note`。

### 3. 配置后端

后端配置文件：`backend/src/main/resources/application.yml`  
可参考示例：`backend/.env.example`（该文件不会被 Spring Boot 自动加载，需要在环境变量或 IDE 中显式注入）。

常用变量（按模块）：

- 数据源与缓存：`DB_URL`、`DB_USERNAME`、`DB_PASSWORD`、`REDIS_*`
- 会话与安全：`SESSION_TIMEOUT`、`SESSION_COOKIE_*`、`APP_SECURITY_CORS_ALLOWED_ORIGINS`、`APP_SECURITY_CSRF_*`
- 文件上传：`APP_UPLOAD_BASE_DIR`、`APP_AVATAR_BASE_DIR`
- AI：`APP_AI_ALLOW_RUNTIME_MODEL_CONFIG`、`APP_AI_RUNTIME_ALLOWED_HOSTS`、`QWEN_*`、`KIMI_*`、`OPENAI_*`、`DEEPSEEK_*`、`GEMINI_*`、`CLAUDE_*`
- OAuth：`OAUTH_FRONTEND_BASE_URL`、`OAUTH_BACKEND_BASE_URL`、`OAUTH_GITHUB_*`、`OAUTH_QQ_*`、`OAUTH_WECHAT_*`
- 缓存与调度：`APP_CACHE_*`、`APP_SCHEDULE_REVIEW_DAILY_*`

### 4. 配置前端

前端环境变量示例：`frontend/.env.example`

- `VITE_API_BASE_URL`（默认 `/api/v1`）
- `VITE_DEV_SERVER_PORT`（默认 `5173`）
- `VITE_DEV_PROXY_TARGET`（默认 `http://127.0.0.1:8080`）

### 5. 启动后端

```bash
cd backend
mvn -DskipTests spring-boot:run
```

默认地址：`http://127.0.0.1:8080`

### 6. 启动前端

```bash
cd frontend
npm install
npm run dev
```

默认地址：`http://127.0.0.1:5173`

## API 与认证机制

- API 前缀：`/api/v1`
- 统一响应：`{ code, message, data }`
- 认证方式：`Session + Cookie`（Session 存 Redis）
- 认证拦截：默认拦截 `/api/v1/**`，登录/注册/公开接口放行
- CSRF：写操作需带 `X-CSRF-Token`；后端会回写 `XSRF-TOKEN` Cookie 与响应头
- 请求追踪：支持并回传 `X-Request-Id`
- API 文档：`http://127.0.0.1:8080/doc.html`（`KNIFE4J_ENABLE=true` 时启用）

## AI 与 OAuth 说明

AI：

- 默认模型：`SAFE_GPT_SIM`
- 支持提供方：`QWEN`、`KIMI`、`OPENAI`、`DEEPSEEK`、`GEMINI`、`CLAUDE`
- 主要接口：
  - `GET /api/v1/ai/models`
  - `POST /api/v1/ai/question-analysis`
  - `POST /api/v1/ai/summary`
  - `POST /api/v1/ai/chat`
- 免费额度：每位用户每天 10 次（基于 Redis 计数）

OAuth：

- 支持平台：GitHub / QQ / 微信
- 入口：`/api/v1/user/oauth/login/authorize/{platform}`
- 绑定：`/api/v1/user/oauth/bind/authorize/{platform}`
- 回调：`/api/v1/user/oauth/callback/{platform}`
- 需在环境变量中配置对应 `enabled/client-id/client-secret`

## 文件上传约束

- 接口：`POST /api/v1/file/upload`
- 默认最大大小：20MB
- 支持扩展名：图片、pdf、doc/docx、md/txt/rtf、ppt/pptx、xls/xlsx
- 服务端会校验扩展名与文件魔数，防止伪造类型上传

## 构建与检查

后端构建：

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

## GitHub 提交规范

### Commit Message（Conventional Commits）

推荐格式：`type(scope): subject`

- `feat`: 新功能
- `fix`: 修复
- `docs`: 文档
- `refactor`: 重构
- `test`: 测试
- `chore`: 构建/工具/发布杂项

示例：

- `feat(review): add daily refresh lock`
- `fix(auth): validate csrf token for mutating requests`
- `docs(readme): align with current oauth and ai providers`

### 提交前检查

- 按 [docs/OPEN_SOURCE_RELEASE_CHECKLIST.md](docs/OPEN_SOURCE_RELEASE_CHECKLIST.md) 逐项核对
- 确认未提交密钥、隐私数据、本地 `.env`、构建产物与运行目录
- 至少执行一次后端编译与前端构建

### 首次发布参考命令

```bash
git add .
git commit -m "chore: prepare open source release"
git push -u origin main
```

## 协作与规范

- 贡献指南：[CONTRIBUTING.md](CONTRIBUTING.md)
- 安全策略：[SECURITY.md](SECURITY.md)
- 行为准则：[CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md)
- 许可证：[LICENSE](LICENSE)

## License

MIT License，详见 [LICENSE](LICENSE)。
