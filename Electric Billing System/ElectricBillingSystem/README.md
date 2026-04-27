# AI-Enhanced Electric Billing System

A complete Java Swing desktop application with MySQL backend and AI-powered analytics.

## Prerequisites

- JDK 8 or above
- MySQL 8.0
- MySQL Connector/J JAR (mysql-connector-j-8.0.33.jar)

## Setup

### 1. Database Setup
```sql
-- Run the schema file in MySQL:
mysql -u root -p < database/schema.sql
```

### 2. Configure Database Connection
Edit `src/main/java/db/DatabaseManager.java`:
```java
private static final String USER = "root";
private static final String PASSWORD = "your_mysql_password";
```

### 3. Add MySQL JDBC Driver
Download `mysql-connector-j-8.0.33.jar` and place it in the `lib/` folder.
Download: https://dev.mysql.com/downloads/connector/j/

### 4. Add Background Image
Place your background image as `resources/background.jpg`

### 5. Build & Run
```
build.bat
```

Or manually:
```bash
javac -cp "lib/mysql-connector-j-8.0.33.jar" -d out -sourcepath src/main/java src/main/java/Main.java
java -cp "out;lib/mysql-connector-j-8.0.33.jar" Main
```

## Default Login
- Admin: `admin` / `admin123`

## Features

| Feature | Description |
|---------|-------------|
| Authentication | Secure SHA-256 hashed login for admin & customers |
| Customer Management | Full CRUD for customers with meter numbers |
| Meter Readings | Input readings with automatic anomaly detection |
| Billing | Tiered billing calculation, bill generation & export |
| Tariff Management | Create and manage electricity tariff rates |
| AI Prediction | Linear regression for next-month consumption forecast |
| Anomaly Detection | Z-score based unusual consumption detection |
| Reports | Monthly billing, consumption, payment status reports |
| CSV Export | Export any report to CSV |
| Bill Export | Export bills as formatted text files |
| Customer Portal | Self-service portal for customers |

## Architecture

```
Presentation Layer  →  Java Swing GUI (BackgroundPanel, UITheme)
Business Logic      →  BillCalculator, ConsumptionPredictor, AnomalyDetector
Data Layer          →  MySQL via JDBC (DatabaseManager)
```

## AI Module

- **ConsumptionPredictor**: Linear regression on historical kWh data
- **AnomalyDetector**: Z-score method (threshold: 2.0 std deviations)
- **Recommendations**: Trend-based energy saving suggestions
