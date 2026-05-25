# Inventory Management System

A comprehensive system for tracking and managing organizational assets, featuring a Spring Boot backend and an Android mobile client.

## 🏗️ Architecture

The project follows a client-server architecture:
- **Backend**: Spring Boot 3.4.3 (Kotlin) with Spring Data JPA.
- **Database**: PostgreSQL (Relational).
- **Frontend**: Android App (Kotlin) using OkHttp for API communication.

---

## 🛠️ Requirements

- **Java**: JDK 21
- **Android SDK**: API Level 34+
- **Database**: PostgreSQL 15+
- **Build Tool**: Gradle (included via wrapper)

---

## 🚀 Getting Started

### 1. Backend Setup
1.  Navigate to the `inventory-backend` directory.
2.  Configure your database connection in `src/main/resources/application.properties`.
3.  Run the application using Gradle:
    ```bash
    ./gradlew bootRun
    ```
4.  The backend will automatically seed default data on first run via `DataSeeder.kt`.

### 2. Android App Setup
1.  Open the project in Android Studio.
2.  The app is configured to look for the backend at `http://10.0.2.2:8080` (Android Emulator default) or a custom IP defined in `app/build.gradle.kts`.
3.  Sync Gradle and run the `app` module on an emulator or physical device.

---

## 🔐 Default Credentials

The following users are seeded by default for testing purposes:

| Role | Email | Password |
| :--- | :--- | :--- |
| **Admin** | `admin@ims.com` | `admin123` |
| **Manager** | `manager@lnt.in` | `1234@` |
| **Employee** | `user@ims.com` | `user123` |
| **IT Support** | `itsupport@ims.com` | `itsupport123` |

---

## 📝 TODO & Requirements

### Technical Requirements
- [x] RESTful API for asset management.
- [x] Role-based access control (Admin, Manager, Employee, IT Staff).
- [x] Database schema for assets, users, and maintenance tickets.
- [x] Email notification service (basic structure).

### Pending Tasks (TODOs)
- [ ] **Email Configuration**: Replace placeholder SMTP settings in `EmailService.kt`.
- [ ] **Data Backup**: Implement data extraction/backup rules in `data_extraction_rules.xml`.
- [ ] **Security**: Enhance password security (currently stored as plain strings in `DataSeeder`).
- [ ] **Deployment**: Configure production URLs in `app/build.gradle.kts` for the release variant.
- [ ] **Reports**: Expand reporting capabilities in `AdminReportService` and `ManagerReportService`.

---

## 📂 Project Structure

- `/app`: Android application source code.
- `/inventory-backend`: Spring Boot backend source code.
- `DATABASE_CONNECTION_OVERVIEW.md`: Detailed architecture of database connections.
- `schema.sql`: Reference PostgreSQL schema.
