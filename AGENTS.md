# Repository Guidelines

## Project Structure & Module Organization
- `backend/`: Spring Boot 3.5 service (Java 21) with MongoDB, Spring Security, and DTO/controller/service layers under `backend/src/main/java/com/team103`. Tests live in `backend/src/test/java`. Runtime configs belong in `backend/src/main/resources`.
- `web/greenacademy_web/`: Next.js 15 + TypeScript app (Tailwind 4, ESLint) with pages and components in `src`, static assets in `public`, and local secrets in `.env.local`.
- `mobile/`: Android/Gradle module; app code under `mobile/app/src`, shared Gradle scripts at the module root. Build artifacts land in `mobile/build` or `mobile/app/build`.

## Build, Test, and Development Commands
- Backend: `cd backend && ./mvnw spring-boot:run` (local API with devtools), `./mvnw clean test` (unit/integration tests), `./mvnw package` (build runnable jar in `target/`).
- Web: `cd web/greenacademy_web && npm install` (once), `npm run dev` (Next dev server with Turbopack), `npm run build` (optimized production bundle), `npm run start` (serve the build), `npm run lint` (ESLint/TypeScript checks).
- Mobile: `cd mobile && ./gradlew assembleDebug` (debug APK), `./gradlew test` (unit tests), `./gradlew lint` (Android lint). Use Android Studio for device/emulator runs.

## Coding Style & Naming Conventions
- Java: follow Spring conventions; classes `PascalCase`, methods/fields `camelCase`, packages `lowercase`. Prefer Lombok annotations already in use; keep controllers thin and services transactional. Format with the IDE’s Google/Default style (4-space indent).
- TypeScript/React: function components with `PascalCase` filenames (e.g., `StudentList.tsx`), hooks prefixed with `use*`, Tailwind utility classes co-located. Keep props typed with interfaces/types.
- Android/Kotlin/Java: match existing package structure; resources `snake_case` (`activity_main.xml`), IDs `camelCase`.

## Testing Guidelines
- Backend uses `spring-boot-starter-test` and `spring-security-test`; prefer `@SpringBootTest` or slice tests where possible. Name tests `*Test.java` mirroring package paths. Aim for meaningful coverage on controllers/services and security rules.
- Web currently lint-focused; add unit tests as `*.test.tsx/ts` near sources when introducing logic (Vitest/RTL recommended). Run `npm run lint` before pushing.
- Mobile: keep unit tests in `app/src/test`; instrumentation in `app/src/androidTest`. Stub network/data layers for deterministic runs.

## Commit & Pull Request Guidelines
- Commits: use imperative, concise subjects (e.g., `Add student attendance endpoint`), group related changes only. Include scope if helpful (`backend:`, `web:`).
- PRs: provide a short summary, linked issue/task, and quick test notes (commands run). Add API contract changes, screenshots/GIFs for UI updates, and migration/config steps when relevant.

## Security & Configuration Tips
- Never commit secrets; use `.env.local` (web) and local Spring properties for credentials. Keep Mongo/JWT keys out of source.
- Check in example config only (e.g., `.env.local.example` if needed). Rotate credentials immediately if exposed.
