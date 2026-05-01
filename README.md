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

确认 RabbitMQ 已经启动后，在项目根目录双击：

```text
一键启动.cmd
```

它会分别打开三个窗口：

- SpringBackend：`http://localhost:8080`
- FastBackend：`http://localhost:8000`
- VueFrontend：Vite 默认地址，通常是 `http://localhost:5173`

## 手动启动

### SpringBackend

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

### Spring 测试

```powershell
cd SpringBackend
mvn test
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
- Spring 目前依赖 RabbitMQ 处理生成/导出异步任务，只轮询状态不会触发任务执行。
- Spring 默认保护业务接口，登录后前端会把 JWT 存到 localStorage 的 `pas_token`。
- FastBackend 输出视频需要 ffmpeg；如果 ffmpeg 不可用，相关媒体测试会跳过或导出失败。
- `FastBackend/outputs/` 可能产生本地媒体文件，不要把生成的视频提交到仓库。
