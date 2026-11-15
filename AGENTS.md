# FoodView agent guidelines

Engineering conventions for everyone working on this repo: team members and AI agents alike. Product background and feature plans live in [README.md](README.md). This file mirrors [CONVENTION.md](CONVENTION.md), the Chinese original. Change both in the same commit.

## 1. Project layout

FoodView is a split frontend and backend web application. `frontend/` is one uni-app codebase compiled to H5 and to a WeChat mini program. `backend/` is a Spring Boot monolith. MySQL runs in Docker. The repo is a monorepo.

```
.
├── backend/                        Spring Boot monolith, built with Maven
│   ├── src/main/java/com/foodview/
│   │   ├── FoodviewBackendApplication.java
│   │   ├── common/
│   │   │   ├── api/                ApiResponse, ErrorCode
│   │   │   ├── exception/          BizException, GlobalExceptionHandler
│   │   │   └── web/                PingController
│   │   └── <domain>/               One package per business domain, holding its own controller/service/mapper/entity/dto
│   ├── src/main/resources/
│   │   ├── application.yml         Port, /api prefix, actuator, MyBatis-Plus
│   │   ├── application-dev.yml     Local datasource, overridable through environment variables
│   │   └── db/migration/           Flyway migrations, starting at V1__xxx.sql, empty for now
│   ├── mvnw, mvnw.cmd              Maven wrapper
│   └── pom.xml
├── frontend/                       uni-app project for H5 and WeChat mini program
│   ├── src/pages/                  Pages, registered in pages.json
│   ├── src/api/                    Request functions grouped by domain
│   ├── src/request/                Request wrapper shared by H5 and the mini program
│   ├── src/stores/                 Pinia
│   ├── pages.json                  Routes, tabBar, subpackages
│   ├── manifest.json               Per-platform config including the WeChat appid
│   └── pnpm-workspace.yaml         Dependency build allowlist required by pnpm 12, do not delete
├── docker-compose.yml              Local MySQL 8.4
├── .editorconfig                   Editor rules enforced by tooling
├── .gitattributes                  Line-ending rules enforced by tooling
├── CONVENTION.md                   The Chinese original
├── AGENTS.md                       This file, the English mirror
├── CLAUDE.md                       Points at AGENTS.md
└── README.md                       Product background and features
```

- The package root is `com.foodview`, with the class suffixes `Controller`, `Service`, `Mapper`, `Entity`, `DTO` and `VO`.
- Group the backend by feature, not by technical layer. Do not flatten everything into `controller/`, `service/` and `entity/`. Each domain package depends on `common` and on other domains' services only.
- Prefix frontend components with `Fv`. Put pages at `src/pages/<module>/<page>.vue`.

## 2. Development environment

- JDK 25 and Maven 3.10. Maven is the only build tool here; do not introduce Gradle. The repo ships a wrapper, so use `mvn` when Maven is installed and `./mvnw` when it is not; both are 3.10.
- Node 26 with pnpm 12. pnpm 12 blocks dependency postinstall scripts, so the allowlist lives under `allowBuilds:` in `frontend/pnpm-workspace.yaml`. That key replaces the older `onlyBuiltDependencies`; removing it makes `pnpm install` fail with `ERR_PNPM_IGNORED_BUILDS`.
- Docker and Docker Compose. MySQL runs in a container and nowhere else. The Docker daemon must already be running (`docker info` succeeds), otherwise every compose command fails outright.
- Application processes stay out of containers: run the backend with `mvn spring-boot:run` and the frontend through the Vite dev server. The reasoning is in section 8.

| Command | What it does |
| --- | --- |
| `docker compose up -d` | Start MySQL on 3306, data in the named volume `foodview-mysql-data` |
| `docker compose logs -f mysql` | Follow the database log |
| `docker compose down`, `down -v` | Stop MySQL; the second also deletes the data |
| `cd backend && mvn spring-boot:run` | Start the backend on 8080 |
| `cd backend && mvn -q -DskipTests compile` | Backend smoke check, run before committing |
| `cd frontend && pnpm install` | Install frontend dependencies |
| `cd frontend && pnpm dev:h5` | H5 dev server on 5173, `/api` already proxied to 8080 |
| `cd frontend && pnpm dev:mp-weixin` | Mini program build, open `dist/dev/mp-weixin` in WeChat DevTools |
| `cd frontend && pnpm build:h5` | Frontend smoke check, run before committing |

Run `git diff --check` before committing to catch whitespace and line-ending problems.

## 3. Git workflow

### 3.1 Branches (main and feature branches only)

- `main` is the stable branch and always runs. It takes changes through pull requests only, never direct pushes.
- `feat/<name>` is a feature branch cut from `main` and deleted as soon as its pull request lands. Use `fix/<name>` for a pure bug fix.
- Name branches with a short English kebab-case phrase, for example `feat/shop-list` or `fix/mp-login-loop`.
- One branch carries one thing, a single feature or a single fix. Merge it when it is done.

### 3.2 Commit messages (Conventional Commits)

- Format: `<type>(<scope>): <subject>`. Write type and subject in English. Use the imperative mood, keep the subject under 72 characters, and leave off the trailing period. The scope is optional.
- Allowed types: `feat` `fix` `docs` `style` `refactor` `perf` `test` `build` `ci` `chore`.
- The optional body explains why the change is needed, wrapped at 72 characters. The footer carries `Closes #123` or `BREAKING CHANGE: ...`.
- One commit does one thing. Keep unrelated formatting changes out of a feature commit (see section 4).

```
feat(shop): return supply status with the dish list

The list page showed stale "on sale" badges because supply status
was fetched separately. Closes #21.
```

### 3.3 Pull requests

- Use the Conventional Commit format for the title. The description states the motivation, what changed, and how you verified it (commands run, what you exercised by hand). Add a screenshot or recording for UI changes.
- Before merging:
  - At least one team member approves.
  - Backend changes pass `cd backend && mvn -q -DskipTests compile`; frontend changes pass `cd frontend && pnpm build:h5`.
  - Changes that touch table structure replay the migrations from an empty database (`docker compose down -v && docker compose up -d`, then start the backend).
- Merge with a merge commit to keep the history. Squash and rebase merges are out.
- Follow-up commits on the branch during review are fine, and force-pushing is fine. History does not need tidying before merge.
- Delete the branch after merging. Link the issue with `Closes #N` in the pull request description or a commit footer.
- Keep pull requests small and focused. Split large work into several.

## 4. File formatting

- All text files use UTF-8 without BOM and LF line endings. The one exception is `*.bat` and `*.cmd`, which use CRLF.
- Indent with spaces, with widths set in [.editorconfig](.editorconfig): Java and XML use 4 spaces; frontend files (`*.ts` `*.vue` `*.js` `*.json` `*.scss`) and YAML use 2 spaces.
- End every file with a newline and strip trailing whitespace, except in Markdown, where two trailing spaces are a hard line break.
- [.editorconfig](.editorconfig) and [.gitattributes](.gitattributes) are enforced by tooling. Turn on EditorConfig support in your IDE, and run `git diff --check` before committing.
- Keep whole-file reformatting out of commits that change behaviour.

## 5. General coding practice

### Before changing code

- Read the files and their callers first so you know which flows the change affects. Do not guess at runtime behaviour.
- Do what was asked. Leave unrelated refactors, formatting, and fixes alone, and record out-of-scope problems for a separate change.

### Implementation

- Walk the decision ladder and stop at the first option that solves the problem: does this need to exist at all, can I reuse an existing implementation in the repo, does the standard library do it, does the platform provide it, is there an installed dependency for it, can it be one readable line, and otherwise write the smallest working implementation.
- Delete before you add. Fewest files, smallest diff.
- Write boring code. Skip abstractions that earn nothing, interfaces with one implementation, speculative configuration, and scaffolding for future needs.
- Fix the root cause. When you change shared behaviour, read every caller and fix the common path where one exists.
- Comment on the why, since the code already says what. Skip the comment when a good name says it.

### Non-negotiable

- Input validation at trust boundaries, error handling that prevents data loss, security controls, accessibility, and correctness on relevant edge cases survive every simplification.
- Secrets, tokens, and personal configuration never enter the repo.
- Non-trivial new logic leaves one minimal runnable check. On the backend that is a JUnit 5 test that mirrors the directory layout and naming of the code under test and asserts concrete inputs and outputs rather than abstract fixture data. The frontend has no test framework yet, so frontend changes rest on `pnpm build:h5` plus manual checks; register a framework in section 8 before adding one.

## 6. Backend and database specifics

- The port is 8080 and `server.servlet.context-path` is `/api`, so controllers never repeat the `/api` prefix.
- Every business endpoint returns the same envelope:

```json
{ "code": 0, "message": "ok", "data": {} }
```

  `code == 0` means success. A non-zero `code` is a business error defined in the `ErrorCode` enum; the frontend branches on `code` and never parses `message`. HTTP status codes keep their meaning: 400 for bad input, 401 unauthenticated, 403 forbidden, 404 missing, 500 server error.
- Pagination uses `data = { records, total, page, size }`. No list endpoint exists yet; follow this shape when one does.
- Times use `Asia/Shanghai` and come back as `yyyy-MM-dd HH:mm:ss` strings.
- actuator exposes `health` only, at `/api/actuator/health`. Any extra endpoint is attack surface someone will forget to close.
- Unmatched paths and unexposed actuator endpoints arrive as `NoResourceFoundException`. `GlobalExceptionHandler` maps them to 404 on their own; never let them reach the catch-all branch and come back as 500.
- MyBatis-Plus: mappers extend `BaseMapper`. Put hand-written SQL in XML files under `src/main/resources/mapper/`. Entity fields are `camelCase` and map to `snake_case` columns by the underscore setting. Page with the MyBatis-Plus plugin instead of writing `limit` by hand.
- The database is `foodview` with `utf8mb4` / `utf8mb4_0900_ai_ci`. Local credentials `foodview` / `foodview` live in `docker-compose.yml` and `application-dev.yml` and are local only. Production reads `DB_URL`, `DB_USERNAME` and `DB_PASSWORD` from the environment. Never commit real credentials.
- No `ddl-auto`. Table structure changes only through Flyway at `backend/src/main/resources/db/migration/V1__xxx.sql`, with increasing version numbers. A committed migration is never edited; add a new version instead.
- Table and column names are `snake_case` and tables are singular (`shop`, `review_image`). Primary keys are `BIGINT AUTO_INCREMENT`. Timestamps are `created_at` and `updated_at`, both `DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP`, with `ON UPDATE CURRENT_TIMESTAMP` on `updated_at`.
- Spring Boot 4.1 renamed starters compared with 3.x: web is `spring-boot-starter-webmvc`, migrations are `spring-boot-starter-flyway`, and test dependencies are the matching `-test` variants. Check the version when following online examples.
- Lombok prints a `sun.misc.Unsafe` deprecation warning on Java 25. That is expected; do not try to silence it. Boot's dependency management owns the Lombok version, so never pin it by hand.

## 7. Frontend specifics

- The base is the official uni-app `vite-ts` template. The versions of `@dcloudio/*`, `vite`, `typescript` and `vue-tsc` form a locked set, so never upgrade one of them alone. Switch the whole template version instead.
- Keep only the H5 and WeChat mini program platforms and their `dev:` / `build:` scripts. Discuss any other platform (App, Alipay mini program, and the rest) in an issue before adding it back.
- No `window`, `document` or DOM API in mini program code. Handle per-platform differences with conditional compilation (`// #ifdef H5`, `// #ifdef MP-WEIXIN`). Use `rpx` for layout units and `@media` breakpoints for wide PC screens.
- Requests go through `src/request/` only. Pages never call `uni.request` or `axios` directly. H5 proxies `/api` to port 8080 through Vite, and the backend hostname never appears in frontend code.
- State lives in Pinia (`src/stores/`). Page-local state stays in the page.
- Style with SCSS and the `uni.scss` variables. No UI component library and no atomic CSS for now (see section 8).
- The WeChat mini program main package is capped at 2MB, so images come from a CDN or remote URL and extra pages go into `subPackages`.

## 8. Deferred decisions

Every item below is postponed on purpose. Update this table before writing the code.

| Item | Status | Notes |
| --- | --- | --- |
| Business tables (`shop`, `dish`, `review`, ...) | Not created | Write `V1__init.sql` once the model settles |
| Authentication | Not built | Every endpoint is open; Sa-Token versus Spring Security with JWT is undecided |
| Redis | Not wired | No dependency yet |
| Object storage for uploads | Undecided | Not even local disk storage until uploading is a real requirement |
| API docs and frontend type generation | Not wired | springdoc plus `openapi-typescript` is the candidate |
| UI component library, UnoCSS | Not added | SCSS only |
| Frontend test framework | Not added | `pnpm build:h5` plus manual checks |
| Containerised deployment | Postponed | Applications stay out of containers while developing; add `backend/Dockerfile` and `docker-compose.prod.yml` (nginx serving `frontend/dist`) once there is a server |
