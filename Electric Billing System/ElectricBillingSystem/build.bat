@echo off
echo ============================================
echo  AI-Enhanced Electric Billing System Build
echo ============================================

set BASE=C:\Users\Tewelde\Desktop\ELECTR~2\ELECTR~1
set SRC=%BASE%\src\main\java
set OUT=%BASE%\out
set LIB=%BASE%\lib\mysql-connector-j-8.0.33.jar;%BASE%\lib\itextpdf-5.5.13.3.jar

if not exist "%OUT%" mkdir "%OUT%"

echo Compiling...
javac -cp "%LIB%" -sourcepath "%SRC%" -d "%OUT%" "%SRC%\db\DatabaseManager.java" "%SRC%\models\User.java" "%SRC%\models\Customer.java" "%SRC%\models\Tariff.java" "%SRC%\models\MeterReading.java" "%SRC%\models\Bill.java" "%SRC%\utils\PasswordUtils.java" "%SRC%\utils\BillCalculator.java" "%SRC%\utils\PDFGenerator.java" "%SRC%\ai\ConsumptionPredictor.java" "%SRC%\ai\AnomalyDetector.java" "%SRC%\gui\BackgroundPanel.java" "%SRC%\gui\UITheme.java" "%SRC%\gui\LoginFrame.java" "%SRC%\gui\RegisterDialog.java" "%SRC%\gui\AdminDashboard.java" "%SRC%\gui\AdminHomePanel.java" "%SRC%\gui\CustomerManagementPanel.java" "%SRC%\gui\MeterReadingPanel.java" "%SRC%\gui\BillingPanel.java" "%SRC%\gui\TariffPanel.java" "%SRC%\gui\AIAnalyticsPanel.java" "%SRC%\gui\ReportsPanel.java" "%SRC%\gui\CustomerDashboard.java" "%SRC%\gui\CustomerHomePanel.java" "%SRC%\gui\CustomerBillsPanel.java" "%SRC%\gui\CustomerReadingsPanel.java" "%SRC%\gui\CustomerAIPanel.java" "%SRC%\gui\CustomerProfilePanel.java" "%SRC%\Main.java"

if %ERRORLEVEL% NEQ 0 (
    echo Compilation FAILED!
    pause
    exit /b 1
)

echo Compilation successful!
echo.
echo Running application...
java -cp "%OUT%;%LIB%" Main
pause
