# LogicProjector

LogicProjector 是一个把算法代码转成教学演示的本地全栈项目。当前链路是：

`VueFrontend -> SpringBackend -> FastBackend`

前端只调用 Spring 后端；视频导出由 Spring 通过 HTTP 调用 FastBackend worker。

## 项目结构

- `SpringBackend/`：业务后端，负责认证、用户额度、生成任务、导出任务、计费、日志、队列和下载。
- `VueFrontend/`：教师工作台，负责登录、提交代码、查看讲解动画、历史记录和视频导出入口。
- `FastBackend/`：媒体导出 worker，负责渲染帧、字幕、TTS 音频和 ffmpeg 视频合成。

## 本地依赖

- Java 21
- Maven
- Node.js / npm
- MySQL 8：默认库名 `logic_projector`，默认账号 `root`，默认密码 `123456`
- RabbitMQ：`localhost:5672`
- ffmpeg：需要在 `PATH` 中，视频导出依赖它
- Conda 环境：`logicprojector-fast`

FastBackend 使用专用 conda 环境，不要把依赖安装到 Anaconda `base`。

```powershell
conda create -n logicprojector-fast python=3.12 -y
conda activate logicprojector-fast
cd FastBackend
python -m pip install -r requirements.txt
```

## 一键启动

确认 MySQL 和 RabbitMQ 已经启动后，在项目根目录双击：

```text
一键启动.cmd
```

它会分别打开三个窗口：

- SpringBackend：`http://localhost:8080`
- FastBackend：`http://localhost:8000`
- VueFrontend：Vite 默认地址，通常是 `http://localhost:5173`

## 手动启动

### SpringBackend

Spring 当前只保留 MySQL 配置，不再使用 H2。默认连接：

```text
jdbc:mysql://localhost:3306/logic_projector
username: root
password: 123456
```

如需覆盖配置，可设置环境变量：`SPRING_DATASOURCE_URL`、`SPRING_DATASOURCE_USERNAME`、`SPRING_DATASOURCE_PASSWORD`。

```powershell
cd SpringBackend
mvn spring-boot:run
```

### FastBackend

```powershell
cd FastBackend
D:\anaconda3\envs\logicprojector-fast\python.exe -m uvicorn app.main:app --reload --port 8000
```

### VueFrontend

```powershell
cd VueFrontend
npm install
npm run dev
```

## 常用检查

### Spring 编译

当前不默认运行 MySQL 集成测试。没有准备测试数据库时，只做编译打包：

```powershell
cd SpringBackend
mvn -DskipTests package
```

### Vue 测试和构建

```powershell
cd VueFrontend
npm run test -- --run
npm run build
```

### FastBackend 测试

```powershell
cd FastBackend
D:\anaconda3\envs\logicprojector-fast\python.exe -m pytest tests -v
```

## 运行提示

- 本地 Spring 配置以 `SpringBackend/src/main/resources/application.yml` 为准。
- Spring 使用 MySQL，不再包含 H2 console。
- Spring 目前依赖 RabbitMQ 处理生成/导出异步任务，只轮询状态不会触发任务执行。
- Spring 默认保护业务接口，登录后前端会把 JWT 存到 localStorage 的 `pas_token`。
- FastBackend 输出视频需要 ffmpeg；如果 ffmpeg 不可用，相关媒体测试会跳过或导出失败。
- `FastBackend/outputs/` 可能产生本地媒体文件，不要把生成的视频提交到仓库。

## Docker 部署

云服务器可以直接使用 Docker Compose 部署五个服务：VueFrontend、SpringBackend、FastBackend、MySQL、RabbitMQ。

1. 复制环境变量模板：

```powershell
copy .env.example .env
```

2. 编辑 `.env`，至少替换：

```text
PAS_AI_DEEPSEEK_API_KEY=你的 DeepSeek key
PAS_AUTH_JWT_SECRET=至少 32 字节的随机密钥
```

3. 启动：

```powershell
docker compose up -d --build
```

4. 访问：

- 前端：`http://服务器地址/`
- Spring API：`http://服务器地址:8080/`
- FastBackend：`http://服务器地址:8000/`
- RabbitMQ 管理页：`http://服务器地址:15672/`

Docker Compose 首次使用空 MySQL volume 时会初始化 `logic_projector` 数据库。`root / 123456` 是当前初始部署值，公开生产环境应改成更强密码。
