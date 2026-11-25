@echo off
setlocal
cd /d "%~dp0"
if defined JAVA_HOME if not exist "%JAVA_HOME%\bin\java.exe" set "JAVA_HOME="
set "SERVER_IP=127.0.0.1"
set /p SERVER_IP=IP deste computador para clientes na rede (Enter usa so este PC):
if not defined SERVER_IP set "SERVER_IP=127.0.0.1"
call "%~dp0mvnw.cmd" compile exec:java "-Dexec.mainClass=com.warbound.server.ServerMain" "-Dexec.args=%SERVER_IP%"
if errorlevel 1 pause
