# CoderNote

CoderNote 是一个面向编程学习者的全栈学习工作区。它在同一个项目中整合了错题追踪、学习资料、笔记、复习计划和 AI 助手。

## 功能亮点

- 用户账户系统，支持注册、登录、验证码、资料更新、修改密码和头像上传
- 错题管理，支持标签、掌握度追踪、附件、搜索和 PDF 导出
- 学习资料管理，支持收藏、标签、附件和笔记关联
- Markdown 笔记系统，支持收藏、版本历史、关联资料、关联错题和 PDF 导出
- 复习计划，包含每日复习中心、复习模式、每日统计和进度追踪
- 全局搜索与仪表盘统计
- AI 助手，支持题目分析、总结和聊天
- 内置本地模拟 OAuth 流程，可用于 `qq`、`wechat`、`github` 登录或账号绑定演示

## 技术栈

后端：

- Java 11
- Spring Boot 2.7.18
- MyBatis-Plus 3.5.5
- MySQL 8.0
- Knife4j 3.0.3
- PDFBox 2.0.31

前端：

- Vue 3
- Vue Router 4
- Pinia
- Element Plus
- Axios
- Vite 6

## 仓库结构

```text
.
|-- backend/                    # Spring Boot 服务
|   |-- src/main/java/          # 后端源代码
|   `-- src/main/resources/     # 应用配置和 Mapper
|-- frontend/                   # Vue 3 前端项目
|-- docs/
|   |-- schema.sql              # 数据库初始化脚本
|   |-- REQUIREMENT_TRACE.md    # 功能到实现的追踪文档
|   `-- OPEN_SOURCE_RELEASE_CHECKLIST.md
|-- .github/                    # issue 模板、PR 模板、CI、Dependabot
`-- README.md
```

## 快速开始

### 1. 环境要求

- JDK 11+
- Maven 3.8+
- Node.js 18+（建议使用 Node.js 18 或 20 LTS）
- MySQL 8.0+

### 2. 初始化数据库

执行 [docs/schema.sql](docs/schema.sql)。脚本中已包含：

```sql
CREATE DATABASE IF NOT EXISTS coder_note;
```

### 3. 配置后端

后端支持通过环境变量覆盖本地配置。可参考 [backend/.env.example](backend/.env.example) 的变量清单。

重要变量：

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `OAUTH_FRONTEND_BASE_URL`
- `APP_UPLOAD_BASE_DIR`
- `APP_AVATAR_BASE_DIR`
- 如果需要外部 AI 提供方，可配置 `QWEN_*`、`KIMI_*`、`OPENAI_*`

默认后端配置文件为 [backend/src/main/resources/application.yml](backend/src/main/resources/application.yml)。

### 4. 配置前端

可参考 [frontend/.env.example](frontend/.env.example) 的变量清单。

重要变量：

- `VITE_API_BASE_URL`
- `VITE_DEV_SERVER_PORT`
- `VITE_DEV_PROXY_TARGET`

### 5. 启动后端

```bash
cd backend
mvn -DskipTests spring-boot:run
```

默认后端地址：`http://127.0.0.1:8080`

### 6. 启动前端

```bash
cd frontend
npm install
npm run dev
```

默认前端地址：`http://127.0.0.1:5173`

默认开发代理：

- `/api -> http://127.0.0.1:8080`

## API 与认证

- API 前缀：`/api/v1`
- 响应格式：`code / message / data`
- 认证模式：`Session + Cookie`
- Knife4j 文档：`http://127.0.0.1:8080/doc.html`

## AI 说明

- 内置默认模型：`SAFE_GPT_SIM`
- 可选提供方：`QWEN`、`KIMI`、`OPENAI`
- 主要接口：
  - `GET /api/v1/ai/models`
  - `POST /api/v1/ai/question-analysis`
  - `POST /api/v1/ai/summary`
  - `POST /api/v1/ai/chat`
- 内置免费额度：每位用户每天 10 次 AI 调用

## 安全与开源说明

- 新密码使用 `BCrypt` 存储。
- 历史 `MD5` 密码哈希仍可兼容，用户成功认证后会自动升级。
- 本仓库中的 OAuth 实现为本地模拟流程，仅用于开发和演示，不是生产级第三方 OAuth 集成。
- 本地构建产物和运行数据会被刻意忽略，包括 `frontend/node_modules`、`frontend/dist`、`backend/target`、`backend/uploads` 以及本地 `.env` 文件。
- 首次公开推送前，请检查 [docs/OPEN_SOURCE_RELEASE_CHECKLIST.md](docs/OPEN_SOURCE_RELEASE_CHECKLIST.md)。

## 构建

前端：

```bash
cd frontend
npm run build
```

后端：

```bash
cd backend
mvn clean package -DskipTests
```

## 协作

- 贡献指南：[CONTRIBUTING.md](CONTRIBUTING.md)
- 安全策略：[SECURITY.md](SECURITY.md)
- 行为准则：[CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md)
- 许可证：[LICENSE](LICENSE)
- 需求追踪：[docs/REQUIREMENT_TRACE.md](docs/REQUIREMENT_TRACE.md)

## 发布就绪情况

当前仓库已包含：

- GitHub issue 模板
- GitHub pull request 模板
- GitHub Actions CI 工作流
- Dependabot 配置
- 后端与前端环境变量示例文件
- 更严格的本地与生成文件忽略规则

## 许可证

本项目基于 MIT License 发布。详情见 [LICENSE](LICENSE)。
