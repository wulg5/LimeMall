@echo off
chcp 65001 >nul
rem =============================================
rem  青柠商城 - 启动后端（端口 8080）
rem  前置：MySQL 已启动（小皮面板）
rem =============================================
cd /d %~dp0

set JAVA_HOME=D:\program\Java\jdk-21
set JAR=backend\target\limemall-backend.jar

if not exist "%JAR%" (
  echo [错误] 未找到 %JAR%，请先执行：cd backend ^&^& mvn -s maven-settings.xml -DskipTests package
  pause
  exit /b 1
)

echo [青柠商城] 启动后端服务（http://127.0.0.1:8080）...
start "LimeMall-Backend" "%JAVA_HOME%\bin\java" -jar "%JAR%"
echo 后端已在新窗口启动。接口文档：http://127.0.0.1:8080/swagger-ui.html
