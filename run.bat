@echo off
echo ============================================
echo  AI-Enhanced Electric Billing System
echo ============================================

set BASE=%~dp0
set OUT=%BASE%out
set LIB=%BASE%lib

:: Build classpath from all jars in lib folder
set CP=%OUT%
for %%f in ("%LIB%\*.jar") do call :addcp "%%f"
goto :run

:addcp
set CP=%CP%;%~1
goto :eof

:run
if not exist "%OUT%\Main.class" (
    echo Application not compiled. Running compile.bat first...
    call "%BASE%compile.bat"
    if %ERRORLEVEL% NEQ 0 exit /b 1
)

echo Starting application...
java -cp "%CP%" Main
pause
