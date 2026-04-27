@echo off
REM Compile all Java sources in this repository into the local out folder.
pushd "%~dp0"
set BASE=%CD%
REM Project sources live in the ElectricBillingSystem subfolder of the workspace root.
set SRC=%BASE%\ElectricBillingSystem\src\main\java
set OUT=%BASE%\ElectricBillingSystem\out
set LIB=%BASE%\ElectricBillingSystem\lib\commons-collections4-4.4.jar;%BASE%\ElectricBillingSystem\lib\commons-compress-1.24.0.jar;%BASE%\ElectricBillingSystem\lib\commons-io-2.15.1.jar;%BASE%\ElectricBillingSystem\lib\itextpdf-5.5.13.3.jar;%BASE%\ElectricBillingSystem\lib\javax.mail-1.6.2.jar;%BASE%\ElectricBillingSystem\lib\log4j-api-2.21.1.jar;%BASE%\ElectricBillingSystem\lib\mysql-connector-j-8.0.33.jar;%BASE%\ElectricBillingSystem\lib\mysql-connector-j-9.6.0.jar;%BASE%\ElectricBillingSystem\lib\poi-5.2.5.jar;%BASE%\ElectricBillingSystem\lib\poi-ooxml-5.2.5.jar;%BASE%\ElectricBillingSystem\lib\xmlbeans-5.1.1.jar
if not exist "%OUT%" mkdir "%OUT%"
setlocal enabledelayedexpansion
rem Compile each file individually to avoid command-line length/expansion issues
set found=0
for /r "%SRC%" %%f in (*.java) do (
  set found=1
  echo Compiling: %%f
  javac -cp "%LIB%" -sourcepath "%SRC%" -d "%OUT%" "%%f"
  if ERRORLEVEL 1 (
    echo FAILED compiling %%f
    endlocal
    popd
    exit /b 1
  )
)
if "%found%"=="0" (
  echo No Java files found under %SRC%
  endlocal
  popd
  exit /b 1
)
echo COMPILATION SUCCESS
endlocal
popd
