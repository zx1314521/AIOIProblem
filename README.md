# AIOIProblem

AIOIProblem is an internal teaching assistant platform for algorithm problem selection. It predicts information olympiad problem difficulty, generates progressive hints, tracks solved problems, manages problem sets, and recommends practice based on weak areas.

## Stack

- Backend: Java 21, Spring Boot, Spring Security, Spring Data JPA, Flyway, PostgreSQL
- Frontend: Vue 3, Vite, TypeScript
- AI providers: DeepSeek API or local Codex CLI

## Local Development

```powershell
docker compose up -d postgres
```

Backend:

```powershell
cd backend
./mvnw test
./mvnw spring-boot:run
```

Frontend:

```powershell
cd frontend
npm install
npm run dev
```

The frontend defaults to `http://localhost:5173` and proxies API calls to `http://localhost:8080`.

## AI Configuration

Backend defaults are in `backend/src/main/resources/application.yml`.

Common environment variables:

- `AIOI_JWT_SECRET`: JWT signing secret.
- `AIOI_AI_PROVIDER`: `deepseek`, `codex`, or `mock`.
- `DEEPSEEK_API_KEY`: DeepSeek API key.
- `DEEPSEEK_BASE_URL`: DeepSeek-compatible API URL.
- `CODEX_CLI_COMMAND`: local Codex CLI command.

## Difficulty Labels

- 入门
- 简单
- CSPJ中等
- CSPS提高
- NOIP困难
- 地狱NOI

