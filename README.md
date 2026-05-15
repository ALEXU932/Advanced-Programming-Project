# AI-Enhanced Electric Billing System

A full-featured Java Swing desktop application for managing electricity billing, meter readings, payments, and AI-powered consumption analytics. Built with a MySQL backend and a modern dark-themed UI.

---

## Prerequisites

- **JDK 11 or above** (tested with JDK 26)
- **MySQL 8.0**
- All required JARs are already included in the `lib/` folder — no separate downloads needed

---

## Setup

### 1. Create the Database

Run the schema file against your MySQL instance:

```bash
mysql -u root -p < database/schema.sql
```

This creates the `electric_billing_db` database with all tables, default tariffs, and a sample admin account.

### 2. Configure the Database Connection

Edit `src/main/java/database/DatabaseManager.java` and set your MySQL password:

```java
private static final String PASSWORD = "your_mysql_password";
```

The default connection URL targets `localhost:3306`. Change the `URL` constant if your MySQL runs on a different host or port.

### 3. Compile

Run the compile script from the project root:

```bat
compile.bat
```

This compiles all source files into the `out/` directory using all JARs in `lib/`.

### 4. Run

```bat
run.bat
```

`run.bat` automatically runs `compile.bat` first if the application has not been compiled yet.

---

## Default Login Credentials

| Role     | Username    | Password   |
|----------|-------------|------------|
| Admin    | `admin`     | `admin123` |
| Customer | `customer1` | `admin123` |
| Customer | `customer2` | `admin123` |

---

## Features

### Admin Panel

| Feature                | Description                                                                 |
|------------------------|-----------------------------------------------------------------------------|
| Dashboard              | Overview of customers, bills, payments, and recent activity                 |
| Customer Management    | Full CRUD for customer accounts and profiles                                |
| Meter Management       | Register and manage meters (Single Phase, Three Phase, Smart, Prepaid)      |
| Meter Readings         | Record readings with automatic anomaly detection on submission               |
| Billing                | Generate bills using flat-rate or tiered pricing; export as PDF or Excel     |
| Tariff Management      | Create and activate electricity tariff rate structures                       |
| Payments               | Record and track payments with multiple payment methods                      |
| Anomaly Detection      | View flagged unusual consumption events with severity levels                 |
| AI Analytics           | Consumption forecasts and trend analysis across all customers                |
| Reports                | Monthly billing, consumption, payment status, and anomaly reports            |
| Audit Log              | Full log of all user actions with timestamps                                 |
| Disputes               | Manage and resolve customer bill disputes                                    |
| Settings               | Configure company info, billing rules, tax, SMTP email, and security policy  |
| Profile                | Admin profile management with avatar/profile picture support                 |

### Customer Portal

| Feature                | Description                                                                 |
|------------------------|-----------------------------------------------------------------------------|
| Home                   | Summary of current balance, latest reading, and recent bills                |
| My Bills               | View all bills with status (Pending, Paid, Overdue); download as PDF        |
| Payments               | Submit payments and view payment history                                     |
| Meter Readings         | View personal reading history                                                |
| Usage Chart            | Visual chart of monthly consumption over time                                |
| AI Predictions         | Personalized next-month consumption forecast with energy-saving tips         |
| Notifications          | System and billing notifications                                             |
| Disputes               | Raise and track disputes on bills                                            |
| Support                | Contact support and view help information                                    |
| Profile                | Update personal details and profile picture                                  |

---

## AI Module

### ConsumptionPredictor
Uses **linear regression** on historical monthly kWh readings to forecast next month's consumption. Falls back to an industry baseline of 150 kWh when fewer than 2 readings are available. Confidence is calculated using the R² coefficient and capped at 75% until at least 6 months of data are present.

### AnomalyDetector
Uses the **Z-score method** with a threshold of 2.0 standard deviations. Requires a minimum of 3 historical readings. Anomalies are classified as LOW (|z| > 2.0), MEDIUM (|z| > 2.5), or HIGH (|z| > 3.5) severity.

### BillCalculator
Supports both **flat-rate** and **tiered billing**:
- First 100 kWh — base rate
- 101–300 kWh — 1.2× base rate
- Above 300 kWh — 1.5× base rate

Tax percentage, fixed charges, and minimum bill amount are all configurable via the Settings panel.

---

## Architecture

```
src/main/java/
├── Main.java                   Application entry point
├── database/                   Data model & DB access
│   ├── DatabaseManager.java    JDBC connection, auto-migration on startup
│   ├── User / Customer / Meter / MeterReading / Bill / Payment / Tariff
├── Logic/                      Business logic
│   ├── BillCalculator.java     Flat-rate and tiered billing
│   ├── ConsumptionPredictor.java  Linear regression forecasting
│   ├── AnomalyDetector.java    Z-score anomaly detection
│   ├── AuditLogger.java        Action audit trail
│   ├── SessionManager.java     Login attempts, lockout, session state
│   ├── SystemSettings.java     Runtime settings loaded from DB
│   ├── PasswordUtils.java      SHA-256 password hashing & verification
│   └── ProfilePicUtils.java    Profile image encoding/decoding
├── report/                     Export & communication
│   ├── PDFGenerator.java       Bill PDF export (iTextPDF)
│   ├── ExcelExporter.java      Report Excel export (Apache POI)
│   ├── EmailService.java       SMTP email delivery (JavaMail)
│   └── CsvImporter.java        Bulk data import from CSV
└── gui/                        Swing UI layer
    ├── UITheme.java             Centralized colors, fonts, and component factories
    ├── BackgroundPanel.java     Gradient/image background panel
    ├── LoginFrame.java          Login screen
    ├── AdminDashboard.java      Admin main window
    ├── CustomerDashboard.java   Customer main window
    └── ...panels               Individual feature panels
```

---

## Database Schema

The schema is in `database/schema.sql`. Key tables:

| Table              | Purpose                                          |
|--------------------|--------------------------------------------------|
| `users`            | Authentication (username, hashed password, role) |
| `admins`           | Admin profile linked to users                    |
| `customers`        | Customer profile linked to users                 |
| `meters`           | Meter registry with type and status              |
| `meter_readings`   | Consumption readings per meter                   |
| `tariffs`          | Rate structures for billing                      |
| `bills`            | Generated bills with status tracking             |
| `payments`         | Payment records linked to bills                  |
| `usage_log`        | Anomaly detection log per reading                |
| `disputes`         | Customer bill disputes and resolutions           |
| `audit_log`        | Full user action audit trail                     |
| `system_settings`  | Key-value store for all configurable settings    |
| `ai_features`      | AI predictions and fraud scores per customer     |

Schema migrations run automatically on every startup via `DatabaseManager.runMigrations()` — they are idempotent and safe to run repeatedly.

---

## Dependencies (included in `lib/`)

| Library                        | Version  | Purpose                        |
|--------------------------------|----------|--------------------------------|
| mysql-connector-j              | 8.0.33 / 9.6.0 | MySQL JDBC driver        |
| itextpdf                       | 5.5.13.3 | PDF bill generation            |
| poi / poi-ooxml                | 5.2.5    | Excel report export            |
| xmlbeans                       | 5.1.1    | Required by POI OOXML          |
| commons-collections4           | 4.4      | Required by POI                |
| commons-compress               | 1.24.0   | Required by POI                |
| commons-io                     | 2.15.1   | File utilities                 |
| javax.mail                     | 1.6.2    | SMTP email delivery            |
| log4j-api                      | 2.21.1   | Logging                        |
