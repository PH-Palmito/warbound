@echo off
setlocal
cd /d "%~dp0"
if defined JAVA_HOME if not exist "%JAVA_HOME%\bin\java.exe" set "JAVA_HOME="
call "%~dp0mvnw.cmd" compile exec:java "-Dexec.mainClass=com.warbound.client.WarboundClientApp"
if errorlevel 1 pause
