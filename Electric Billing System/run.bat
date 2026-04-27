@echo off
REM Run from the repository root. This script computes its base folder so it works across machines.
pushd "%~dp0"
set BASE=%CD%
REM Use the ElectricBillingSystem subfolder where sources and compiled classes live.
set OUT=%BASE%\ElectricBillingSystem\out
set LIB=%BASE%\ElectricBillingSystem\lib\commons-collections4-4.4.jar;%BASE%\ElectricBillingSystem\lib\commons-compress-1.24.0.jar;%BASE%\ElectricBillingSystem\lib\commons-io-2.15.1.jar;%BASE%\ElectricBillingSystem\lib\itextpdf-5.5.13.3.jar;%BASE%\ElectricBillingSystem\lib\javax.mail-1.6.2.jar;%BASE%\ElectricBillingSystem\lib\log4j-api-2.21.1.jar;%BASE%\ElectricBillingSystem\lib\mysql-connector-j-8.0.33.jar;%BASE%\ElectricBillingSystem\lib\mysql-connector-j-9.6.0.jar;%BASE%\ElectricBillingSystem\lib\poi-5.2.5.jar;%BASE%\ElectricBillingSystem\lib\poi-ooxml-5.2.5.jar;%BASE%\ElectricBillingSystem\lib\xmlbeans-5.1.1.jar
if not exist "%OUT%" mkdir "%OUT%"
java -cp "%OUT%;%LIB%" Main
popd
