# Database Connection Architecture - Inventory Management System

## Overview
The Inventory Management System uses a **client-server architecture** with separate database connections for the backend and Android frontend.

---

## 1. BACKEND DATABASE CONNECTION

### Database Type
**PostgreSQL** (Relational Database)

### Connection Configuration
Located in: `inventory-backend/src/main/resources/application.properties`

```properties
# Updated for Deployment
spring.datasource.url=${DATABASE_URL:jdbc:postgresql://localhost:5432/inventory_management}
spring.datasource.username=${DATABASE_USERNAME:postgres}
spring.datasource.password=${DATABASE_PASSWORD:postgres}
spring.datasource.driver-class-name=org.postgresql.Driver
```

### Connection Details
- **Host:** localhost (Default) / Dynamic (via Environment Variable)
- **Port:** 5432 (Default)
- **Database Name:** inventory_management
- **Username:** postgres (Default)
- **Password:** postgres (Default)
- **Driver:** PostgreSQL JDBC Driver

### ORM Framework
**Spring Data JPA** (Hibernate)

JPA Configuration:
```properties
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=true
```

- **DDL Auto:** `update` - Automatically creates/updates schema
- **SQL Display:** Disabled in production for performance
- **SQL Formatting:** Enabled for readability

---

## 2. ANDROID APP DATABASE CONNECTION

### Architecture
The Android app uses a **hybrid storage approach**:

#### A. Backend API Communication (Primary Data Source)
**OkHttp3 HTTP Client** for REST API calls to the Spring Boot backend

**Base URL Resolution:**
From `app/build.gradle.kts` (Build Variants):
- **Debug:** `http://172.23.180.194:8080/` (Local Development)
- **Release:** `https://your-deployed-inventory-api.com/` (Production Deployment)

The app tries multiple endpoints in order in `BackendApi.kt` for robust connectivity during development.

---

## 3. DEPLOYMENT CONFIGURATION

### Environment Variables (Backend)
When deploying the backend (e.g., to Render, Railway, or AWS), set the following:

| Variable | Description |
|----------|-------------|
| `DATABASE_URL` | Full JDBC URL (e.g., `jdbc:postgresql://host:port/db`) |
| `DATABASE_USERNAME` | Database user |
| `DATABASE_PASSWORD` | Database password |
| `PORT` | The port the server should listen on (defaults to 8080) |

### Build Variants (Android)
- Use the **debug** variant for local testing.
- Use the **release** variant for generating the APK to be used on other phones over the internet.

---

Generated: April 16, 2026 (Updated for Deployment)
