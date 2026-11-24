# Repository Guidelines

## Project Structure & Module Organization

- `admin/`: Vue 3, TypeScript, Element Plus, and Tailwind frontend. `src/views/` contains pages, `layouts/` navigation, `api/` request functions, `stores/` Pinia state, `mock/` fixtures, and `styles/` shared CSS. Built assets go to `admin/dist/`.
- `server/`: Java 21/Spring Boot Maven modules. `gateway/` routes requests; `common/` provides utilities; `modules/system/system-api/` holds contracts and `system-service/` implementations. Sources/configuration live in `src/main/java/` and `src/main/resources/`; tests in `system-service/src/test/java/`.
- `server/sql/` contains database scripts. `web/` is planned, not implemented. Keep application code out of the repository root.

## Build, Test, and Development Commands

Run commands from the indicated module. Docker is preferred.

From `admin/` (Node.js 22+ for local npm commands):

- `npm ci`: install locked dependencies.
- `npm run dev`: start Vite on port 5173.
- `npm run typecheck`: check Vue/TypeScript types.
- `npm run build`: type-check and build production assets.
- `npm run preview`: preview the build on port 4173.
- `docker compose up -d --build admin-web`: serve the frontend on port 3000.

From `server/` (JDK 21 and Maven):

- `mvn clean install -DskipTests`: build/install modules without tests.
- `mvn test "-Dtest=PasswordRegisterStrategyTest" "-Dsurefire.failIfNoSpecifiedTests=false"`: select registration unit tests; confirm JUnit 5 discovery and nonzero execution in the output.
- `docker compose up -d --build`: start the backend stack.

## Coding Style & Naming Conventions

Follow `.editorconfig`: UTF-8, LF, two-space indentation, and final newlines. Avoid unrelated reformatting of existing four-space Java/XML files. Use Vue `<script setup lang="ts">`, strict TypeScript, single quotes, and no semicolons in frontend code. Name components `PascalCase.vue`, pages `views/<feature>/index.vue`, and Java classes `PascalCase` with suffixes such as `Controller` or `Service`. Use `@/` imports. No ESLint/Prettier configuration exists.

## Testing Guidelines

Backend tests use JUnit 5, Mockito, and Spring Boot Test; name classes `*Test` under matching package paths. No coverage threshold or frontend test runner is configured. Check frontend builds and manually exercise login, permissions, and menu routing. `EmailIntegrationTest` currently sends real email; run it only with explicit authorization and controlled recipients.

## Commit & Pull Request Guidelines

History uses short Chinese and English descriptions without enforced prefixes. Write focused, descriptive commits. PRs should explain affected modules, link relevant issues, list validation results, and include screenshots for UI changes. Document configuration or SQL changes.

## Security & Configuration

Keep credentials in ignored local configuration or environment variables such as `RESEND_API_KEY`. Never commit secrets or put them in browser-exposed `VITE_*` variables. Configure `VITE_USE_MOCK` and `VITE_PROXY_TARGET` explicitly for the intended backend.
