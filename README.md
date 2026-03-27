# CoderNote

CoderNote is a full-stack learning workspace for programming students. It combines error tracking, study materials, notes, review planning, and an AI assistant in one project.

## Highlights

- User account system with register, login, captcha, profile update, password change, and avatar upload
- Error question management with tagging, mastery tracking, attachments, search, and PDF export
- Study material management with favorites, tags, attachments, and note linking
- Markdown note system with favorites, version history, related materials, related questions, and PDF export
- Review planning with daily review center, review mode, daily stats, and progress tracking
- Global search and dashboard statistics
- AI assistant for question analysis, summarization, and chat
- Local mock OAuth flow for `qq`, `wechat`, and `github` login or account binding demos

## Tech Stack

Backend:

- Java 11
- Spring Boot 2.7.18
- MyBatis-Plus 3.5.5
- MySQL 8.0
- Knife4j 3.0.3
- PDFBox 2.0.31

Frontend:

- Vue 3
- Vue Router 4
- Pinia
- Element Plus
- Axios
- Vite 6

## Repository Layout

```text
.
|-- backend/                    # Spring Boot service
|   |-- src/main/java/          # backend source code
|   `-- src/main/resources/     # application config and mappers
|-- frontend/                   # Vue 3 frontend project
|-- docs/
|   |-- schema.sql              # database bootstrap script
|   |-- REQUIREMENT_TRACE.md    # feature-to-implementation trace
|   `-- OPEN_SOURCE_RELEASE_CHECKLIST.md
|-- .github/                    # issue templates, PR template, CI, Dependabot
`-- README.md
```

## Quick Start

### 1. Requirements

- JDK 11+
- Maven 3.8+
- Node.js 18+ (Node.js 18 or 20 LTS recommended)
- MySQL 8.0+

### 2. Initialize the database

Run [docs/schema.sql](docs/schema.sql). The script already includes:

```sql
CREATE DATABASE IF NOT EXISTS coder_note;
```

### 3. Configure the backend

The backend uses environment variables for local overrides. Use [backend/.env.example](backend/.env.example) as the reference list.

Important variables:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `OAUTH_FRONTEND_BASE_URL`
- `APP_UPLOAD_BASE_DIR`
- `APP_AVATAR_BASE_DIR`
- `QWEN_*`, `KIMI_*`, `OPENAI_*` if you want external AI providers

The default backend config file is [backend/src/main/resources/application.yml](backend/src/main/resources/application.yml).

### 4. Configure the frontend

Use [frontend/.env.example](frontend/.env.example) as the reference list.

Important variables:

- `VITE_API_BASE_URL`
- `VITE_DEV_SERVER_PORT`
- `VITE_DEV_PROXY_TARGET`

### 5. Start the backend

```bash
cd backend
mvn -DskipTests spring-boot:run
```

Default backend address: `http://127.0.0.1:8080`

### 6. Start the frontend

```bash
cd frontend
npm install
npm run dev
```

Default frontend address: `http://127.0.0.1:5173`

Default development proxy:

- `/api -> http://127.0.0.1:8080`

## API and Auth

- API prefix: `/api/v1`
- Response format: `code / message / data`
- Auth mode: `Session + Cookie`
- Knife4j docs: `http://127.0.0.1:8080/doc.html`

## AI Notes

- Built-in default model: `SAFE_GPT_SIM`
- Optional providers: `QWEN`, `KIMI`, `OPENAI`
- Main endpoints:
  - `GET /api/v1/ai/models`
  - `POST /api/v1/ai/question-analysis`
  - `POST /api/v1/ai/summary`
  - `POST /api/v1/ai/chat`
- Built-in free quota: 10 AI calls per user per day

## Security and Open Source Notes

- New passwords are stored with `BCrypt`.
- Legacy `MD5` password hashes are still accepted and are upgraded automatically after a successful authentication flow.
- The OAuth implementation in this repository is a local mock flow for development and demo use. It is not a production third-party OAuth integration.
- Local build artifacts and runtime data are intentionally ignored, including `frontend/node_modules`, `frontend/dist`, `backend/target`, `backend/uploads`, and local `.env` files.
- Before the first public push, review [docs/OPEN_SOURCE_RELEASE_CHECKLIST.md](docs/OPEN_SOURCE_RELEASE_CHECKLIST.md).

## Build

Frontend:

```bash
cd frontend
npm run build
```

Backend:

```bash
cd backend
mvn clean package -DskipTests
```

## Collaboration

- Contribution guide: [CONTRIBUTING.md](CONTRIBUTING.md)
- Security policy: [SECURITY.md](SECURITY.md)
- Code of conduct: [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md)
- License: [LICENSE](LICENSE)
- Requirement trace: [docs/REQUIREMENT_TRACE.md](docs/REQUIREMENT_TRACE.md)

## Release Readiness

This repository now includes:

- GitHub issue templates
- GitHub pull request template
- GitHub Actions CI workflow
- Dependabot configuration
- Sample environment variable files for backend and frontend
- Stronger ignore rules for local and generated files

## License

This project is released under the MIT License. See [LICENSE](LICENSE) for details.
