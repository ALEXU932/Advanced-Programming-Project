@echo off
title AI Electric Billing System
echo ============================================
echo  AI Electric Billing System - Build + Run
echo ============================================

set BASE=C:\Users\alexd\Desktop\Electric Billing System\ElectricBillingSystem
set SRC=%BASE%\src\main\java
set OUT=%BASE%\out
set LIB=%BASE%\lib\commons-collections4-4.4.jar;%BASE%\lib\commons-compress-1.24.0.jar;%BASE%\lib\commons-io-2.15.1.jar;%BASE%\lib\itextpdf-5.5.13.3.jar;%BASE%\lib\javax.mail-1.6.2.jar;%BASE%\lib\log4j-api-2.21.1.jar;%BASE%\lib\mysql-connector-j-8.0.33.jar;%BASE%\lib\poi-5.2.5.jar;%BASE%\lib\poi-ooxml-5.2.5.jar;%BASE%\lib\xmlbeans-5.1.1.jar

if not exist "%OUT%" mkdir "%OUT%"

echo [1/2] Compiling...

javac -cp "%LIB%" -sourcepath "%SRC%" -d "%OUT%" ^
  "%SRC%\db\DatabaseManager.java" ^
  "%SRC%\models\User.java" ^
  "%SRC%\models\Customer.java" ^
  "%SRC%\models\Tariff.java" ^
  "%SRC%\models\Meter.java" ^
  "%SRC%\models\Payment.java" ^
  "%SRC%\models\MeterReading.java" ^
  "%SRC%\models\Bill.java" ^
  "%SRC%\utils\PasswordUtils.java" ^
  "%SRC%\utils\BillCalculator.java" ^
  "%SRC%\utils\PDFGenerator.java" ^
  "%SRC%\utils\ProfilePicUtils.java" ^
  "%SRC%\utils\AuditLogger.java" ^
  "%SRC%\utils\SessionManager.java" ^
  "%SRC%\utils\ExcelExporter.java" ^
  "%SRC%\utils\EmailService.java" ^
  "%SRC%\utils\CsvImporter.java" ^
  "%SRC%\utils\SystemSettings.java" ^
  "%SRC%\ai\ConsumptionPredictor.java" ^
  "%SRC%\ai\AnomalyDetector.java" ^
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
  "%SRC%\gui\CustomerAIPanel.java" ^
  "%SRC%\gui\CustomerNotificationsPanel.java" ^
  "%SRC%\gui\CustomerSupportPanel.java" ^
  "%SRC%\gui\CustomerProfilePanel.java" ^
  "%SRC%\Main.java"

if %ERRORLEVEL% NEQ 0 (
  echo.
  echo [ERROR] Compilation failed. See errors above.
  pause
  exit /b 1
)

echo [2/2] Starting application...
echo.
java -cp "%OUT%;%LIB%" Main

pause
