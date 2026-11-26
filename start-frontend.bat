@echo off
chcp 65001 >nul
rem =============================================
rem  青柠商城 - 启动前端（端口 5173）
rem  前置：已完成 npm install
rem =============================================
cd /d %~dp0frontend

if not exist node_modules (
  echo [提示] 首次运行，正在安装依赖...
  call npm install
)

echo [青柠商城] 启动前端开发服务器（http://localhost:5173）...
start "LimeMall-Frontend" cmd /c "npm run dev"
echo 前端已在新窗口启动。
echo   后台管理：http://localhost:5173/admin/login
echo   商城前台：http://localhost:5173/shop
echo   演示账号见 README（部署公网前请务必修改）
