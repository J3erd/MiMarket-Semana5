@echo off
chcp 65001 >nul
title Mi Market - Servidor
echo === Mi Market: iniciando servidor Node.js ===
rem Conecta TODOS los dispositivos (celular/emulador) con el puerto 3000 de esta PC (adb reverse)
set ADB=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe
if exist "%ADB%" (
  for /f "skip=1 tokens=1,2" %%a in ('"%ADB%" devices') do (
    if "%%b"=="device" (
      echo adb reverse en %%a
      "%ADB%" -s %%a reverse tcp:3000 tcp:3000
    )
  )
) else ( echo No se encontro adb: %ADB% )
cd /d "%~dp0"
if not exist node_modules ( call npm install )
call npm start
pause
