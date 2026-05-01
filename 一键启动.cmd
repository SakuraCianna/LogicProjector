@echo off
start "LogicProjector SpringBackend" /D "%~dp0SpringBackend" cmd /k mvn spring-boot:run
start "LogicProjector FastBackend" /D "%~dp0FastBackend" cmd /k D:\anaconda3\envs\logicprojector-fast\python.exe -m uvicorn app.main:app --reload --port 8000
start "LogicProjector VueFrontend" /D "%~dp0VueFrontend" cmd /k npm run dev
