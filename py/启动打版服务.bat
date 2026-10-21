@echo off
chcp 65001 >nul
cd /d %~dp0
title DST 打版服务（端口 8000）
echo ==============================================
echo    绣纹智创 - DST 工业级打版服务
echo ==============================================
echo.
echo [1/2] 检查 8000 端口是否有残留进程...
set FOUND=0
for /f "tokens=5" %%a in ('netstat -ano ^| findstr "LISTENING" ^| findstr ":8000 "') do (
  set FOUND=1
  echo        发现残留进程 PID=%%a，正在停止...
  taskkill /PID %%a /F >nul 2>&1
)
if "%FOUND%"=="0" echo        端口 8000 空闲
echo.
echo [2/2] 启动服务（代码修改后自动热更新）...
echo.
echo        启动成功后浏览器打开 http://localhost:8000/docs 可看到接口文档
echo        使用期间请保持本窗口开启；停止服务：关窗口或按 Ctrl+C
echo ==============================================
echo.
python -m uvicorn api:app --host 0.0.0.0 --port 8000 --reload
pause
