# FoodView 开发约定

面向全体成员与 AI agent 的工程约定。产品背景与功能规划见 [README.md](README.md)。英文镜像在 [AGENTS.md](AGENTS.md)，两份必须同一次提交一起改。

## 1. 项目总体结构

食见是前后端分离的 Web 应用：`frontend/` 用 uni-app 一套代码编译到 H5 与微信小程序，`backend/` 是 Spring Boot 单体，MySQL 跑在 Docker 里。仓库是 monorepo。

```
.
├── backend/                        Spring Boot 单体，Maven 构建
│   ├── src/main/java/com/foodview/
│   │   ├── FoodviewBackendApplication.java
│   │   ├── common/
│   │   │   ├── api/                ApiResponse、ErrorCode
│   │   │   ├── exception/          BizException、GlobalExceptionHandler
│   │   │   └── web/                PingController
│   │   └── <领域>/                  每个业务领域一个包，内部自带 controller/service/mapper/entity/dto
│   ├── src/main/resources/
│   │   ├── application.yml         公共配置：端口、/api 前缀、actuator、MyBatis-Plus
│   │   ├── application-dev.yml     本地数据源，凭据可被环境变量覆盖
│   │   └── db/migration/           Flyway 迁移脚本，V1__xxx.sql 起，当前为空
│   ├── mvnw / mvnw.cmd             Maven wrapper
│   └── pom.xml
├── frontend/                       uni-app 工程，H5 与微信小程序
│   ├── src/pages/                  页面，在 src/pages.json 注册
│   ├── src/api/                    按领域拆的请求函数
│   ├── src/request/                H5 与小程序共用的请求封装
│   ├── src/pages.json              路由、tabBar、分包
│   ├── src/manifest.json           微信 appid 等分端配置
│   ├── vite.config.ts              H5 开发时的 /api 代理
│   └── pnpm-workspace.yaml         pnpm 12 的全部设置：依赖构建白名单与 nodeLinker
├── docker-compose.yml              本地 MySQL 8.4
├── .editorconfig                   编辑格式规则（机器强制）
├── .gitattributes                  换行规则（机器强制）
├── CONVENTION.md                   本文件，中文
├── AGENTS.md                       英文镜像
├── CLAUDE.md                       指向 AGENTS.md
└── README.md                       产品背景与功能
```

- 包根 `com.foodview`，类后缀 `Controller` / `Service` / `Mapper` / `Entity` / `DTO` / `VO`。
- 后端按功能分包，不按 `controller/` `service/` `entity/` 这类技术层平铺。一个领域一个包，跨包只依赖 `common` 和对方的 service。
- 前端组件以 `Fv` 开头，页面放 `src/pages/<模块>/<页面>.vue`。

## 2. 开发环境

- JDK 25，Maven 3.10，构建只用 Maven，不引入 Gradle。仓库自带 wrapper，装了 Maven 用 `mvn`，没装就用 `./mvnw`，两者都是 3.10。
- Node 26 + pnpm 12。pnpm 12 只从 `frontend/pnpm-workspace.yaml` 读设置，不再读 `.npmrc`。其中两项都不能删：`allowBuilds` 放行依赖的 postinstall（否则 `pnpm install` 报 `ERR_PNPM_IGNORED_BUILDS`），`nodeLinker: hoisted` 换成扁平 node_modules（isolated 布局下 uni-app 的 rollup 解析不到 pinia 的传递依赖，`pnpm build:h5` 会失败）。
- Docker 与 Docker Compose，MySQL 只跑在容器里。Docker daemon 必须已经启动（`docker info` 能通），否则 compose 命令会直接失败。
- 应用进程不进容器：后端用 `mvn spring-boot:run`，前端用 Vite dev server。理由见第 8 节。

| 命令 | 作用 |
| --- | --- |
| `docker compose up -d` | 启动 MySQL（3306），数据在命名卷 `foodview-mysql-data` |
| `docker compose logs -f mysql` | 看数据库日志 |
| `docker compose down` / `down -v` | 停止，后者连数据一起删 |
| `cd backend && mvn spring-boot:run` | 启动后端，8080 |
| `cd backend && mvn -q -DskipTests compile` | 后端最小验证，提交前跑 |
| `cd frontend && pnpm install` | 安装前端依赖 |
| `cd frontend && pnpm dev:h5` | H5 开发服务器，5173，`/api` 已代理到 8080 |
| `cd frontend && pnpm dev:mp-weixin` | 小程序产物，用微信开发者工具打开 `dist/dev/mp-weixin` |
| `cd frontend && pnpm type-check` | vue-tsc 类型检查 |
| `cd frontend && pnpm build:h5` | 前端最小验证，提交前跟 type-check 一起跑 |
| `cd frontend && pnpm build:mp-weixin` | 小程序生产构建，产物在 `dist/build/mp-weixin` |

提交前用 `git diff --check` 自查空白与换行问题。

## 3. Git 协作规范

### 3.1 分支模型（只有 main 和功能分支）

- `main`：稳定分支，任何时候都能跑起来。只接受 PR 合入，不直接 push。
- `feat/<名称>`：功能分支，从 main 拉出，PR 合回 main 后**立即删除**。纯缺陷修复用 `fix/<名称>`。
- 命名用 kebab-case 英文短语，如 `feat/shop-list`、`fix/mp-login-loop`。
- 一个分支只承载一件事（一个功能或一个修复），做完即合，不长期挂账。

### 3.2 Commit 信息（Conventional Commits）

- 格式：`<type>(<scope>): <subject>`。type 与 subject 用**英文**，subject 用祈使句、不超过 72 字符、结尾不加句号；scope 可省略。
- type 取值：`feat` `fix` `docs` `style` `refactor` `perf` `test` `build` `ci` `chore`。
- body（可选）说明**为什么**这么改，每行不超过 72 字符；footer 可写 `Closes #123`、`BREAKING CHANGE: ...`。
- 一个 commit 只做一件事，不要把无关的格式化改动混进功能 commit（见第 4 节）。

示例：

```
feat(shop): return supply status with the dish list

The list page showed stale "on sale" badges because supply status
was fetched separately. Closes #21.
```

### 3.3 PR

- 标题沿用 Conventional Commit 格式；描述至少写清：**动机（为什么）**、**改动内容**、**验证方式**（跑了哪些命令、手测了什么）。UI 改动附截图或录屏。
- 合并前要求：
  - 至少 **1 名成员 approve**；
  - 后端改动跑过 `cd backend && mvn -q -DskipTests compile`，前端改动跑过 `cd frontend && pnpm type-check && pnpm build:h5`；
  - 涉及表结构的改动，从空库重放一遍迁移（`docker compose down -v && docker compose up -d` 后启动后端）；
  - CI（`.github/workflows/ci.yml`）两个 job 全绿：`backend` 跑 `./mvnw -B verify`，`frontend` 跑 `pnpm install --frozen-lockfile`、`type-check`、`build:h5`、`build:mp-weixin`。
- 合并方式：**merge commit**（保留提交历史），不用 squash / rebase 合并。review 期间的修改直接在分支上追加 commit 或 force-push 均可，合并前不强制整理历史。
- 合并后删除功能分支；PR 描述或 commit footer 用 `Closes #N` 关联 issue。
- PR 保持小而聚焦，大改动拆成多个 PR。

## 4. 编码格式

- 所有文本文件统一 **UTF-8（无 BOM）+ LF**。唯一例外：`*.bat` / `*.cmd` 用 CRLF。
- 缩进用空格，宽度由 [.editorconfig](.editorconfig) 规定：Java / XML 为 **4 空格**，前端（`*.ts` `*.vue` `*.js` `*.json` `*.scss`）与 YAML 为 **2 空格**。
- 文件末尾保留一个空行；去掉行尾空白（Markdown 除外，行尾两个空格是硬换行）。
- [.editorconfig](.editorconfig) 与 [.gitattributes](.gitattributes) 是机器强制的规则：IDE 请开启 EditorConfig 支持；提交前可用 `git diff --check` 检查空白与换行问题。
- 不要在同一个 commit 里做全文件格式化。

## 5. 一般编码须知

### 动手前

- 不臆测：先读文件与调用方，弄清受影响的流程再改；不假设代码行为。
- 只做被要求的事：不顺手重构、不改无关格式、不做无关修复。范围之外的问题记录下来另行处理。

### 实现

- 决策阶梯，停在第一个能解决问题的选项：这个东西需要存在吗 → 复用仓库里已有实现 → 用标准库 → 用平台原生能力 → 用已安装依赖 → 一行能写清就写一行 → 否则写最小可运行实现。
- 优先删除而不是新增。用最少的文件、最小的 diff。
- 写无聊的代码，不写聪明的代码。避免多余抽象、单实现接口、投机性配置、为未来预留的脚手架。
- 修根因，不修症状；改共享行为前先看所有调用方，能修公共路径就修公共路径。
- 注释写「为什么」，不写「做了什么」；能靠命名说清的不写注释。

### 不可妥协

- 边界输入校验、防数据丢失的错误处理、安全控制、可访问性、关键边界 case 的正确性——任何简化都不能牺牲这些。
- 密钥、token、个人配置不入库。
- 非平凡的新逻辑必须留一个最小可运行检查：后端写成 JUnit 5 测试，目录结构与命名镜像被测代码，断言具体输入输出而不是抽象桩数据。前端骨架期没有测试框架，改动以 `pnpm build:h5` 加手测为准，引入测试框架要先在第 8 节登记再动手。

## 6. 后端与数据库须知

- 端口 8080，`server.servlet.context-path: /api`，**controller 里不写 `/api` 前缀**。
- 所有业务接口返回统一响应体：

```json
{ "code": 0, "message": "ok", "data": {} }
```

  `code == 0` 表示成功，非 0 是业务错误码，由 `ErrorCode` 枚举定义；前端只判断 `code`，不解析 `message`。HTTP 状态码仍然表达语义：400 参数错误、401 未认证、403 无权限、404 不存在、500 服务端异常。
- 分页统一 `data = { records, total, page, size }`（列表接口尚未落地，落地时按这个形状）。
- 时间统一 `Asia/Shanghai`，接口返回 `yyyy-MM-dd HH:mm:ss` 字符串。
- actuator 只暴露 `health`，路径 `/api/actuator/health`。多开的端点都是以后忘了关的安全面。
- 未匹配的路径和未暴露的 actuator 端点走 `NoResourceFoundException`，`GlobalExceptionHandler` 里单独映射成 404；不要让它们掉进兜底的 500 分支。
- MyBatis-Plus：Mapper 继承 `BaseMapper`，需要手写 SQL 时把 XML 放 `src/main/resources/mapper/`；实体字段用 `camelCase`，靠下划线映射对上库里的 `snake_case`。分页用 MyBatis-Plus 的插件，不自己写 `limit`。
- 库名 `foodview`，字符集 `utf8mb4` / `utf8mb4_0900_ai_ci`。本地凭据 `foodview` / `foodview` 写在 `docker-compose.yml` 与 `application-dev.yml` 里，仅限本地。生产走环境变量 `DB_URL` / `DB_USERNAME` / `DB_PASSWORD`，禁止把真实凭据提交进仓库。
- **禁止 `ddl-auto`**。建表只走 Flyway：`backend/src/main/resources/db/migration/V1__xxx.sql`，版本号递增，已提交的迁移文件不改内容，要改就加新版本。
- 表名、字段名 `snake_case`；表名用单数（`shop`、`review_image`）。主键 `BIGINT AUTO_INCREMENT`；时间字段 `created_at` / `updated_at`，`DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP`，`updated_at` 加 `ON UPDATE CURRENT_TIMESTAMP`。
- Spring Boot 4.1 的 starter 名字与 3.x 不同：web 是 `spring-boot-starter-webmvc`，迁移是 `spring-boot-starter-flyway`，测试依赖是它们各自的 `-test` 变体。查资料时注意版本。
- Java 25 上 Lombok 会打印 `sun.misc.Unsafe` 弃用警告，属于正常现象，不要去消除它。Lombok 版本由 Boot 的 dependency management 管，不单独指定。

## 7. 前端须知

- 底座是 uni-app 官方 `vite-ts` 模板。`@dcloudio/*`、`vite`、`typescript`、`vue-tsc` 的版本是模板锁定的组合，**不许单独升级其中任何一个**，要升就整体切换到新的模板版本。
- 只保留 H5 与微信小程序两个平台及其 `dev:` / `build:` 脚本。要加别的端（App、支付宝小程序等）先在 issue 里讨论，别顺手装回来。
- 小程序端不许出现 `window` / `document` / DOM API，分端差异用条件编译（`// #ifdef H5`、`// #ifdef MP-WEIXIN`）。布局单位优先 `rpx`，适配 PC 宽屏用 `@media` 断点。
- 请求只走 `src/request/`，页面里不直接调 `uni.request` 或 `axios`。H5 用相对路径 `/api`，由 Vite 代理转发到 8080，代码里不出现后端域名；小程序端没有代理，`request` 会退回 `VITE_API_BASE`，本地默认 `http://localhost:8080/api`，真机必须换成备案过的 HTTPS 域名。
- 状态用 Pinia，已在 `src/main.ts` 注册，store 以后放 `src/stores/`，现在还没有。
- 样式先用普通 CSS。`uni.scss` 里的变量要装了 `sass` 才可用，等真需要嵌套或主题变量时再加这个依赖。暂不引入 UI 组件库与原子化 CSS（见第 8 节）。
- 条件编译注释在 `.ts` 里同样生效，但 `vue-tsc` 看到的是未处理的源码：不要用 `#ifdef` / `#ifndef` 两个分支各写一个同名 `const`，改成 `let` 加条件赋值，否则类型检查报重复声明。
- 微信小程序主包上限 2MB，图片走 CDN 或外链，页面多了用 `subPackages` 分包。

## 8. 明确推迟的决定

下表是主动推迟的事项。要做其中任何一项，先改这张表，再动代码。

| 事项 | 现状 | 备注 |
| --- | --- | --- |
| `shop` / `dish` / `review` 等业务表 | 未建 | 需求定了再写 `V1__init.sql` |
| 登录鉴权 | 未做 | 所有接口当前放行；Sa-Token 与 Spring Security + JWT 尚未选型 |
| Redis | 未接入 | 先不加依赖 |
| 对象存储（图片上传） | 未选型 | 本地先落磁盘也不做，等有上传需求 |
| 接口文档与前端类型生成 | 未接入 | springdoc + `openapi-typescript` 是候选方案 |
| UI 组件库、UnoCSS、`sass` | 未引入 | 样式用普通 CSS |
| 前端测试框架 | 未引入 | 以 `pnpm type-check`、`pnpm build:h5` 与手测为准 |
| 应用容器化部署 | 推迟 | 开发期应用不进容器；等有服务器再补 `backend/Dockerfile` 与 `docker-compose.prod.yml`（nginx 托管 `frontend/dist`） |
