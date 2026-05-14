@echo off
echo ============================================
echo  AI-Enhanced Electric Billing System Build
echo ============================================

:: Set paths relative to this script's location
set BASE=%~dp0
set SRC=%BASE%src\main\java
set OUT=%BASE%out
set LIB=%BASE%lib

:: Build classpath from all jars in lib folder
set CP=
for %%f in ("%LIB%\*.jar") do call :addcp "%%f"
goto :compile

:addcp
if "%CP%"=="" (set CP=%~1) else (set CP=%CP%;%~1)
goto :eof

:compile
if not exist "%OUT%" mkdir "%OUT%"

echo Compiling all source files...
javac -encoding UTF-8 -cp "%CP%" -sourcepath "%SRC%" -d "%OUT%" ^
  "%SRC%\database\DatabaseManager.java" ^
  "%SRC%\database\User.java" ^
  "%SRC%\database\Customer.java" ^
  "%SRC%\database\Tariff.java" ^
  "%SRC%\database\Meter.java" ^
  "%SRC%\database\Payment.java" ^
  "%SRC%\database\MeterReading.java" ^
  "%SRC%\database\Bill.java" ^
  "%SRC%\Logic\PasswordUtils.java" ^
  "%SRC%\Logic\BillCalculator.java" ^
  "%SRC%\Logic\AuditLogger.java" ^
  "%SRC%\Logic\SessionManager.java" ^
  "%SRC%\Logic\SystemSettings.java" ^
  "%SRC%\Logic\ProfilePicUtils.java" ^
  "%SRC%\Logic\AnomalyDetector.java" ^
  "%SRC%\Logic\ConsumptionPredictor.java" ^
  "%SRC%\report\PDFGenerator.java" ^
  "%SRC%\report\ExcelExporter.java" ^
  "%SRC%\report\EmailService.java" ^
  "%SRC%\report\CsvImporter.java" ^
  "%SRC%\gui\BackgroundPanel.java" ^
  "%SRC%\gui\UITheme.java" ^
  "%SRC%\gui\AvatarPanel.java" ^
  "%SRC%\gui\FormDialog.java" ^
  "%SRC%\gui\LoginFrame.java" ^
  "%SRC%\gui\RegisterDialog.java" ^
  "%SRC%\gui\AdminDashboard.java" ^
  "%SRC%\gui\AdminHomePanel.java" ^
  "%SRC%\gui\AdminProfilePanel.java" ^
  "%SRC%\gui\AuditLogPanel.java" ^
  "%SRC%\gui\SettingsPanel.java" ^
  "%SRC%\gui\DisputesPanel.java" ^
  "%SRC%\gui\CustomerManagementPanel.java" ^
  "%SRC%\gui\MeterManagementPanel.java" ^
  "%SRC%\gui\MeterReadingPanel.java" ^
  "%SRC%\gui\BillingPanel.java" ^
  "%SRC%\gui\PaymentPanel.java" ^
  "%SRC%\gui\TariffPanel.java" ^
  "%SRC%\gui\AIAnalyticsPanel.java" ^
  "%SRC%\gui\AnomalyPanel.java" ^
  "%SRC%\gui\ReportsPanel.java" ^
  "%SRC%\gui\CustomerDashboard.java" ^
  "%SRC%\gui\CustomerHomePanel.java" ^
  "%SRC%\gui\CustomerBillsPanel.java" ^
  "%SRC%\gui\CustomerPaymentPanel.java" ^
  "%SRC%\gui\CustomerReadingsPanel.java" ^
  "%SRC%\gui\CustomerUsageChartPanel.java" ^
  "%SRC%\gui\CustomerAIPanel.java" ^
  "%SRC%\gui\CustomerNotificationsPanel.java" ^
  "%SRC%\gui\CustomerSupportPanel.java" ^
  "%SRC%\gui\CustomerProfilePanel.java" ^
  "%SRC%\Main.java"

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo *** COMPILATION FAILED ***
    pause
    exit /b 1
)

echo.
echo *** COMPILATION SUCCESSFUL ***
pause
