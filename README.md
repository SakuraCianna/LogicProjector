# LogicProjector

LogicProjector 是一个面向算法教学场景的智能演示生成平台。教师提交 Java 算法代码后，系统会识别算法类型，生成可视化讲解流程，并可导出带字幕、语音和动画的视频课件。

项目目标不是单纯展示算法结果，而是把一段算法代码转成可讲解、可回放、可导出的教学内容，适合课程备课、课堂演示和短视频课件制作。

当前调用链路：

```text
VueFrontend -> SpringBackend -> FastBackend
```

前端只调用 SpringBackend；SpringBackend 负责任务编排、鉴权、计费和队列；FastBackend 只作为媒体导出 worker，由 SpringBackend 调用。

## 当前状态

- 已接入 MySQL，项目不再使用内存数据库。
- 已提供 `create.sql` 手动初始化脚本。
- 已提供 Docker Compose 全量部署配置。
- Spring 集成测试中涉及真实数据库清理的用例默认禁用，避免误删本地开发库数据。
- 本地推荐启动方式：`mvn spring-boot:run` 启动 SpringBackend，配合 FastBackend 和 VueFrontend 启动。

## 功能概览

- 用户注册、登录和 JWT 鉴权。
- Java 算法代码提交和任务状态轮询。
- AI 算法识别、讲解摘要和可视化步骤生成。
- 教师工作台侧边栏历史记录，可恢复最近生成和导出任务。
- RabbitMQ 异步处理生成任务和导出任务。
- 用户额度校验、导出额度冻结、结算和失败退款。
- FastBackend 生成帧图片、字幕、TTS 音频，并通过 ffmpeg 合成视频。
- Docker Compose 全量部署 VueFrontend、SpringBackend、FastBackend、MySQL、RabbitMQ。

## 业务流程

### 生成讲解流程

1. 用户在 VueFrontend 登录或注册。
2. 前端把 JWT 保存在 localStorage 的 `pas_token`。
3. 用户提交 Java 算法代码。
4. SpringBackend 创建生成任务，校验用户额度和代码语言。
5. SpringBackend 在事务提交后发布 RabbitMQ 生成任务消息。
6. 生成任务监听器消费消息，调用 AI 识别算法并生成讲解摘要。
7. SpringBackend 根据算法类型提取可视化状态，并生成前端可播放的步骤数据。
8. 任务完成后扣除生成额度，前端轮询并展示讲解结果。

### 导出视频流程

1. 用户在已完成的生成任务上发起导出。
2. SpringBackend 校验任务归属和任务状态。
3. SpringBackend 冻结预估导出额度，并创建导出任务。
4. SpringBackend 通过 RabbitMQ 异步处理导出任务。
5. 导出 worker 调用 FastBackend 的 `/exports` 接口。
6. FastBackend 渲染帧图片、字幕文件、TTS 音频，并用 ffmpeg 合成视频。
7. SpringBackend 根据 worker 返回的 `tokenUsage + renderSeconds + concurrencyUnits` 结算实际费用。
8. 导出成功后前端展示下载链接；导出失败则释放冻结额度并记录错误。

## 架构说明

```text
Browser
  |
  | /api, /health
  v
VueFrontend / Nginx
  |
  | proxy to SpringBackend
  v
SpringBackend
  |-- MySQL: 用户、任务、账单、日志持久化
  |-- RabbitMQ: 生成任务和导出任务异步队列
  |-- DeepSeek API: 算法识别和讲解生成
  |
  | HTTP /exports
  v
FastBackend
  |-- Pillow: 帧渲染
  |-- edge-tts: 语音生成
  |-- ffmpeg: 视频合成
```

关键设计约束：

- VueFrontend 不直接调用 FastBackend，避免绕过 SpringBackend 的鉴权、计费和任务状态控制。
- 任务处理通过 RabbitMQ 异步执行，接口请求只负责创建任务和查询状态。
- SpringBackend 只从受控下载目录读取导出文件，不信任 worker 返回的任意绝对路径。
- 额度变更使用冻结、结算、退款模型，失败路径会补偿冻结额度。

## 项目结构

```text
LogicProjector/
  SpringBackend/   Spring Boot 业务后端
  VueFrontend/     Vue3 教师工作台
  FastBackend/     FastAPI 媒体导出 worker
  create.sql       MySQL 建库建表脚本
  docker-compose.yml
  .env.example     环境变量模板
  一键启动.cmd      本地三端启动脚本
```

## 技术栈

- 前端：Vue 3、Vite、Vitest、Nginx
- 业务后端：Spring Boot 3、Spring Security、JWT、Spring Data JPA、MySQL、RabbitMQ
- 媒体 worker：FastAPI、Uvicorn、Pillow、edge-tts、ffmpeg、pytest
- 部署：Docker、Docker Compose

## 端口约定

| 服务 | 本地端口 | 说明 |
| --- | --- | --- |
| VueFrontend | `5173` | Vite dev server 默认端口 |
| SpringBackend | `8080` | REST API 和健康检查 |
| FastBackend | `8000` | 媒体导出 worker |
| MySQL | `3306` | 业务数据库 |
| RabbitMQ | `5672` | AMQP 队列 |
| RabbitMQ Management | `15672` | RabbitMQ 管理页面 |

## 本地依赖

- Java 21
- Maven
- Node.js / npm
- MySQL 8，默认数据库 `logic_projector`
- RabbitMQ，默认地址 `localhost:5672`
- ffmpeg，需要在 `PATH` 中
- Conda 环境 `logicprojector-fast`

FastBackend 必须使用专用 conda 环境，不要把依赖安装到 Anaconda `base`。

```powershell
conda create -n logicprojector-fast python=3.12 -y
conda activate logicprojector-fast
cd FastBackend
python -m pip install -r requirements.txt
```

## 本地配置

根目录 `.env` 保存本地敏感配置，不提交到 git。首次运行可复制模板：

```powershell
copy .env.example .env
```

当前 SpringBackend 会从 `SpringBackend/src/main/resources/application.yml` 读取配置，并通过下面配置导入根目录 `.env`：

```yml
spring:
  config:
    import: optional:file:../.env[.properties]
```

本地默认数据库配置：

```text
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/logic_projector?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=123456
```

需要在 `.env` 中配置真实的 DeepSeek key：

```text
PAS_AI_DEEPSEEK_API_KEY=你的 DeepSeek key
PAS_AUTH_JWT_SECRET=至少 32 字节的 JWT 密钥
```

常用环境变量：

| 变量 | 用途 | 本地示例 |
| --- | --- | --- |
| `SPRING_DATASOURCE_URL` | MySQL JDBC 地址 | `jdbc:mysql://localhost:3306/logic_projector?...` |
| `SPRING_DATASOURCE_USERNAME` | MySQL 用户名 | `root` |
| `SPRING_DATASOURCE_PASSWORD` | MySQL 密码 | `123456` |
| `SPRING_RABBITMQ_HOST` | RabbitMQ 主机 | `localhost` |
| `SPRING_RABBITMQ_PORT` | RabbitMQ 端口 | `5672` |
| `PAS_AI_DEEPSEEK_API_KEY` | DeepSeek API Key | 不要提交真实值 |
| `PAS_AUTH_JWT_SECRET` | JWT 签名密钥 | 至少 32 字节 |
| `PAS_EXPORT_WORKER_BASE_URL` | FastBackend 地址 | `http://localhost:8000` |
| `PAS_EXPORT_DOWNLOAD_ROOT` | Spring 下载文件根目录 | `../FastBackend/outputs` |

## 数据库初始化

项目提供 `create.sql`，可用于手动初始化 MySQL：

```powershell
mysql -uroot -p123456 < create.sql
```

如果只创建数据库而不执行 `create.sql`，SpringBackend 会因为缺少表而无法正常处理业务请求：

```sql
CREATE DATABASE logic_projector DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

SpringBackend 不负责自动建表或自动更新表结构。数据库结构采用手动维护方式：首次部署执行 `create.sql`，后续实体字段变化时同步更新 SQL 脚本并手动执行变更。`create.sql` 不会被 Spring 自动执行。

主要数据表：

| 表名 | 作用 |
| --- | --- |
| `users` | 用户账号、密码哈希、可用额度、冻结额度和状态 |
| `generation_tasks` | 代码生成讲解任务、算法识别结果、可视化 payload 和状态 |
| `export_tasks` | 视频导出任务、导出文件路径、冻结/实际扣费和状态 |
| `billing_records` | 额度扣减、冻结、结算、退款记录 |
| `system_logs` | 生成/导出过程日志和异常详情 |

## 本地启动

启动前确认：

- MySQL 已启动，并已执行 `create.sql` 完成表结构初始化。
- RabbitMQ 已启动。
- `.env` 已配置。
- FastBackend conda 环境已安装依赖。

### 一键启动

在项目根目录双击：

```text
一键启动.cmd
```

脚本会分别打开三个窗口：

- SpringBackend：`http://localhost:8080`
- FastBackend：`http://localhost:8000`
- VueFrontend：通常是 `http://localhost:5173`

### 手动启动

SpringBackend：

```powershell
cd SpringBackend
mvn spring-boot:run
```

FastBackend：

```powershell
cd FastBackend
D:\anaconda3\envs\logicprojector-fast\python.exe -m uvicorn app.main:app --reload --port 8000
```

VueFrontend：

```powershell
cd VueFrontend
npm install
npm run dev
```

Vite dev server 会把 `/api` 和 `/health` 代理到 `http://localhost:8080`。

## 常用接口

认证接口：

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/api/auth/register` | 注册用户 |
| `POST` | `/api/auth/login` | 登录并返回 JWT |
| `GET` | `/api/auth/me` | 获取当前登录用户 |

生成任务接口：

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/api/generation-tasks` | 创建代码讲解生成任务 |
| `GET` | `/api/generation-tasks/{id}` | 查询生成任务详情 |
| `GET` | `/api/generation-tasks/recent` | 查询最近生成任务 |

导出任务接口：

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/api/generation-tasks/{id}/exports` | 基于生成任务创建导出任务 |
| `GET` | `/api/export-tasks/{id}` | 查询导出任务详情 |
| `GET` | `/api/export-tasks/recent` | 查询最近导出任务 |
| `GET` | `/api/export-tasks/{id}/download` | 下载导出视频 |

健康检查：

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/health` | SpringBackend 健康检查 |
| `GET` | `/health/queues` | RabbitMQ 队列状态 |

## Docker 部署

云服务器可直接用 Docker Compose 部署全部服务。

1. 准备环境变量：

```powershell
copy .env.example .env
```

2. 编辑 `.env`，至少替换：

```text
PAS_AI_DEEPSEEK_API_KEY=你的 DeepSeek key
PAS_AUTH_JWT_SECRET=至少 32 字节的 JWT 密钥
```

3. 启动：

```powershell
docker compose up -d --build
```

4. 访问：

- 前端：`http://服务器地址/`
- SpringBackend：`http://服务器地址:8080/`
- FastBackend：`http://服务器地址:8000/`
- RabbitMQ 管理页：`http://服务器地址:15672/`

Docker Compose 首次使用空 MySQL volume 时会初始化 `logic_projector` 数据库。`root / 123456` 是当前初始部署值，公开生产环境建议改成强密码。

Docker 内部服务名：

| 服务 | Compose 服务名 | 内部地址 |
| --- | --- | --- |
| MySQL | `mysql` | `mysql:3306` |
| RabbitMQ | `rabbitmq` | `rabbitmq:5672` |
| FastBackend | `fast-backend` | `http://fast-backend:8000` |
| SpringBackend | `spring-backend` | `http://spring-backend:8080` |
| VueFrontend | `vue-frontend` | Nginx 对外暴露 `80` |

FastBackend 和 SpringBackend 共享 Docker volume `logicprojector_outputs`：

- FastBackend 写入 `/app/outputs`
- SpringBackend 以只读方式挂载 `/app/outputs`
- SpringBackend 下载接口只从该目录解析导出文件

## 常用检查

没有专用 MySQL 测试库时，不运行 Spring 集成测试，只做编译打包：

```powershell
cd SpringBackend
mvn -DskipTests package
```

VueFrontend：

```powershell
cd VueFrontend
npm run test -- --run
npm run build
```

FastBackend：

```powershell
cd FastBackend
D:\anaconda3\envs\logicprojector-fast\python.exe -m pytest tests -v
```

Docker Compose 静态配置检查：

```powershell
docker compose config --quiet
```

## 故障排查

### SpringBackend 启动失败，提示无法连接数据库

- 确认 MySQL 已启动。
- 确认数据库 `logic_projector` 已存在。
- 确认 `.env` 中 `SPRING_DATASOURCE_URL`、用户名、密码正确。
- 如果使用 Docker，确认 `mysql` 服务健康：`docker compose ps`。

### 生成或导出任务一直停留在 PENDING

- 确认 RabbitMQ 已启动。
- 查看 `GET /health/queues` 队列状态。
- 确认 SpringBackend 的 Rabbit listener 正常启动。
- 只轮询任务状态不会触发任务执行，必须有 RabbitMQ 消费者处理消息。

### 视频导出失败

- 本地运行时确认 `ffmpeg` 在 `PATH` 中。
- Docker 部署时 FastBackend 镜像会安装 `ffmpeg`。
- 检查 FastBackend 日志和导出任务的 `errorMessage`。
- 确认 SpringBackend 的 `PAS_EXPORT_DOWNLOAD_ROOT` 和 FastBackend 输出目录指向同一个位置或共享 volume。

### 登录后接口仍然 403

- 确认前端 localStorage 中存在 `pas_token`。
- 确认请求头包含 `Authorization: Bearer <token>`。
- 确认 `PAS_AUTH_JWT_SECRET` 没有在服务重启或多容器之间不一致。

### DeepSeek 调用失败

- 确认 `.env` 里配置了 `PAS_AI_DEEPSEEK_API_KEY`。
- 确认 key 有效且账户额度可用。
- 检查 SpringBackend 日志中的 AI provider 错误信息。

## 运行注意事项

- SpringBackend 业务接口默认需要 JWT，开放接口为 `/api/auth/**` 和 `/health/**`。
- VueFrontend 把 JWT 存在 localStorage 的 `pas_token`。
- RabbitMQ 是生成/导出任务执行的必要依赖，轮询状态不会触发任务处理。
- FastBackend 导出视频需要 ffmpeg。
- FastBackend 生成文件位于 `FastBackend/outputs/`，该目录已被 git 忽略。
- `.env` 包含敏感配置，已被 git 忽略，不要提交。
- `create.sql` 是人工初始化脚本；表结构由 SQL 脚本和人工变更维护，SpringBackend 不自动建表。
- 本地数据库中有真实数据时，不要随意运行会清空表的集成测试。
