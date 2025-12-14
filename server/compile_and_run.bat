@echo off
echo ========================================
echo Word Game Server - Compile and Run
echo ========================================
echo.

REM Navigate to Server directory
cd /d "%~dp0"

echo [1/3] Cleaning old build...
if exist bin rmdir /s /q bin
mkdir bin

echo [2/3] Compiling Java files...
javac -d bin src\*.java

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ERROR: Compilation failed!
    pause
    exit /b 1
)

echo [3/3] Starting server...
echo.
java -cp bin com.example.wrd.GameServer

pause
