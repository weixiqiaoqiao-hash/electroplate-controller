@echo off
setlocal

echo [1/3] Build frontend...
cd /d "%~dp0frontend"
call npm run build
if errorlevel 1 goto fail

echo [2/3] Copy static resources to backend...
if not exist "..\backend\src\main\resources\static" mkdir "..\backend\src\main\resources\static"
xcopy /e /i /y "dist" "..\backend\src\main\resources\static"
if errorlevel 1 goto fail

echo [3/3] Package backend...
cd /d "%~dp0backend"
call mvn -q -DskipTests package
if errorlevel 1 goto fail

echo.
echo BUILD OK: backend\target\electroplate-backend.jar  ^(frontend + backend in one jar, port 8080^)
goto end

:fail
echo.
echo BUILD FAILED, check errors above.
exit /b 1

:end
endlocal
