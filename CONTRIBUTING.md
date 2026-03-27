# 贡献指南

感谢你考虑为 CoderNote 做贡献。

## 开发环境

- JDK 11+
- Maven 3.8+
- Node.js 18+（建议使用 Node.js 18 或 20 LTS）
- MySQL 8.0+

项目结构：

- `backend/`：Spring Boot 服务
- `frontend/`：Vue 3 + Vite 客户端

## 本地启动

1. 执行 [docs/schema.sql](/docs/schema.sql) 初始化数据库。
2. 以后端 [backend/.env.example](/backend/.env.example) 作为配置参考。
3. 以前端 [frontend/.env.example](/frontend/.env.example) 作为配置参考。
4. 启动后端：

```bash
cd backend
mvn -DskipTests spring-boot:run
```

5. 启动前端：

```bash
cd frontend
npm install
npm run dev
```

## 提交前检查

- 不要提交 `node_modules`、`dist`、`target`、`uploads`、`.env`、数据库导出文件或其他本地产物。
- 如果你修改了配置行为，请在同一个 PR 中更新 README 或示例配置文件。
- 在本地验证受影响的主要流程。
- 当行为、接口或数据结构发生变化时，请补充或更新文档。

## Pull Request 期望

- 使用清晰的 PR 标题准确描述变更内容。
- 尽量让一个 PR 聚焦一个主题。
- 请在描述中包含：
  - 改了什么
  - 如何验证
  - 风险或影响是什么
  - 是否涉及配置或数据迁移

## Issue 期望

- Bug 报告应包含复现步骤、预期行为、实际行为和环境信息。
- 功能建议应说明使用场景、预期价值和合理范围。
- 不要在公开 issue 中报告安全问题。请参阅 [SECURITY.md](/SECURITY.md)。

## 代码风格

- 后端 Java 使用 4 空格缩进。
- 前端、YAML 和 Markdown 使用 2 空格缩进。
- 仓库默认采用 UTF-8 与 LF，具体见 [/.editorconfig](/.editorconfig) 和 [/.gitattributes](/.gitattributes)。
