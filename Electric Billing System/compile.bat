@echo off
REM Compile from the script folder so it builds the current workspace sources.
pushd "%~dp0"
set BASE=%CD%
set SRC=%BASE%\src\main\java
set OUT=%BASE%\out
set LIB=%BASE%\lib\commons-collections4-4.4.jar;%BASE%\lib\commons-compress-1.24.0.jar;%BASE%\lib\commons-io-2.15.1.jar;%BASE%\lib\itextpdf-5.5.13.3.jar;%BASE%\lib\javax.mail-1.6.2.jar;%BASE%\lib\log4j-api-2.21.1.jar;%BASE%\lib\mysql-connector-j-8.0.33.jar;%BASE%\lib\mysql-connector-j-9.6.0.jar;%BASE%\lib\poi-5.2.5.jar;%BASE%\lib\poi-ooxml-5.2.5.jar;%BASE%\lib\xmlbeans-5.1.1.jar
if not exist "%OUT%" mkdir "%OUT%"
javac -cp "%LIB%" -sourcepath "%SRC%" -d "%OUT%" "src\main\java\**\*.java"
if %ERRORLEVEL% EQU 0 (echo COMPILATION SUCCESS) else (echo COMPILATION FAILED)
popd
