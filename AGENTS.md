# AGENTS.md

## 工作约定
- 默认用中文回复用户。

## 边界
- 这是 3 个独立应用：`SpringBackend/`、`VueFrontend/`、`FastBackend/`；根目录没有统一 task runner。
- 真实调用链是 `VueFrontend -> SpringBackend -> FastBackend`；Vue 只能调用 Spring，不能绕过鉴权/计费直接调 Fast worker。
- 跨端行为优先从这些入口追：Spring HTTP `generation/GenerationTaskController.java`、`exporttask/ExportTaskController.java`、`recharge/RechargeController.java`；Spring 队列 `GenerationTaskListener.java`、`ExportTaskListener.java`；Spring worker client `HttpMediaExportWorkerClient.java`；Vue shell/API `src/App.vue`、`src/api/pasApi.ts`；Fast worker `app/main.py`、`app/services/export_pipeline.py`。

## 命令
- Spring dev：在 `SpringBackend` 运行 `mvn spring-boot:run`。
- Spring 无测试编译：在 `SpringBackend` 运行 `mvn -DskipTests package`。
- Spring 单测：在 `SpringBackend` 运行 `mvn -q -Dtest=GenerationTaskControllerTest test`；完整 `mvn test` 可能碰到无 H2/需 MySQL 的 JPA 测试。
- Spring 集成/JPA 测试 `GenerationTaskServiceFlowTest`、`ExportTaskServiceTest`、`AuthFlowIntegrationTest`、`GenerationTaskPersistenceTest`、`ExportTaskPersistenceTest` 被 `@Disabled`，只在专用 MySQL 测试库上考虑启用；不要对本地开发库跑会清表或写表的测试。
- Vue dev/test/build：在 `VueFrontend` 分别运行 `npm run dev`、`npm run test -- --run`、`npm run build`；`build` 已包含 `vue-tsc -b`，没有单独 typecheck/lint 脚本。
- Vue 单 spec：在 `VueFrontend` 运行 `npm run test -- --run src/App.spec.ts`。
- Fast dev：在 `FastBackend` 运行 `D:\anaconda3\envs\logicprojector-fast\python.exe -m uvicorn app.main:app --reload --port 8000`。
- Fast tests：在 `FastBackend` 运行 `D:\anaconda3\envs\logicprojector-fast\python.exe -m pytest tests/test_media_timeline.py -v`；不要假设全局 `python`/`pytest` 或 Anaconda `base` 环境可用。

## 运行真相
- Spring 配置源是 `SpringBackend/src/main/resources/application.yml`，并按相对路径导入根目录 `../.env`；通常要从 `SpringBackend` 作为工作目录启动。
- `.env.example` 不是完整本地 Spring 配置：`SPRING_DATASOURCE_URL`、`SPRING_RABBITMQ_USERNAME`、`SPRING_RABBITMQ_PASSWORD` 在 `application.yml` 没有默认值，本地缺失会启动失败；Docker Compose 会显式注入这些值。
- 不要提交真实 `.env`、DeepSeek key 或生产 JWT secret；`.env*` 默认忽略，只有 `.env.example` 例外。
- Spring Security 只放行 `/api/auth/register`、`/api/auth/login`、`/health/**` 和 OPTIONS；`/api/auth/me`、生成、导出、充值接口都需要 Bearer JWT。Vue token 存在 localStorage 的 `pas_token`。
- RabbitMQ 是生成/导出执行的必要依赖；Spring 提交事务后发 AMQP，`@RabbitListener` 才处理任务，轮询状态不会触发执行。
- Spring 只用 MySQL；表结构靠 `create.sql`/人工 SQL 维护，Spring 不自动建表或迁移。
- `create.sql` 第一行会 `DROP DATABASE logic_projector`，不要对有价值的本地库直接执行。
- Vue 默认同源 `/api`；Vite 代理 `/api`、`/health` 到 `http://localhost:8080`，Docker Nginx 代理到 `spring-backend:8080`。
- Fast export 输出根目录来自 `PAS_OUTPUT_ROOT`，默认 `outputs`；Spring 下载只从 `pas.export.download-root` 拼接 worker 返回的相对 `videoPath`，没有任意路径 fallback。
- 导出扣费按 worker 返回的 `tokenUsage + renderSeconds + concurrencyUnits` 结算；创建导出时先冻结 `pas.export.freeze-estimate`。
- Fast 视频合成需要 `ffmpeg`；Dockerfile 会安装，本地必须在 `PATH` 中。当前 Fast 测试主要 mock/检查时间线与帧渲染，不证明 live TTS/ffmpeg 端到端可用。

## 变更检查
- 跨端导出改动且没有专用 MySQL 测试库时，至少跑：Spring `mvn -DskipTests package`，Vue `npm run test -- --run` 和 `npm run build`，Fast 上述 pytest。
- 认证、历史侧边栏、任务归属改动优先检查 Spring `GenerationTaskControllerTest`、`ExportTaskControllerTest`、`AuthControllerTest` 和 Vue `src/App.spec.ts`。
- 队列健康检查接口是 `GET http://localhost:8080/health/queues`。

## 容易踩坑
- `FastBackend/outputs/`、`VueFrontend/dist/`、`VueFrontend/node_modules/`、`SpringBackend/target/` 被忽略；不要提交生成产物。
- 后端不要留下 Eclipse 项目配置（`.classpath`、`.project`、`.settings/`、`.factorypath`）。
- 修改 `UserAccount` 构造器或字段时，同步检查直接 `new UserAccount(...)` 的 Spring 测试。
- 最近生成列表只返回 `sourcePreview`；Vue 重新打开历史项依赖详情接口 `GenerationTaskResponse.sourceCode`。
