# Contributing

Thanks for considering a contribution to CoderNote.

## Development Environment

- JDK 11+
- Maven 3.8+
- Node.js 18+ (Node.js 18 or 20 LTS recommended)
- MySQL 8.0+

Project layout:

- `backend/`: Spring Boot service
- `frontend/`: Vue 3 + Vite client

## Local Setup

1. Run [docs/schema.sql](/docs/schema.sql) to initialize the database.
2. Use [backend/.env.example](/backend/.env.example) as the backend configuration reference.
3. Use [frontend/.env.example](/frontend/.env.example) as the frontend configuration reference.
4. Start the backend:

```bash
cd backend
mvn -DskipTests spring-boot:run
```

5. Start the frontend:

```bash
cd frontend
npm install
npm run dev
```

## Before You Submit

- Do not commit `node_modules`, `dist`, `target`, `uploads`, `.env`, database dumps, or other local artifacts.
- If you change configuration behavior, update the README or example config files in the same PR.
- Verify the main affected flows locally.
- Add or update documentation when behavior, endpoints, or data structures change.

## Pull Request Expectations

- Use a clear PR title that describes the actual change.
- Keep one PR focused on one topic when possible.
- Include:
  - what changed
  - how it was verified
  - what risk or impact exists
  - whether config or data migration is involved

## Issue Expectations

- Bug reports should include reproduction steps, expected behavior, actual behavior, and environment details.
- Feature requests should include the use case, expected value, and reasonable scope.
- Do not report security issues in public issues. See [SECURITY.md](/SECURITY.md).

## Style

- Backend Java uses 4-space indentation.
- Frontend, YAML, and Markdown use 2-space indentation.
- The repository defaults to UTF-8 and LF with [/.editorconfig](/.editorconfig) and [/.gitattributes](/.gitattributes).
