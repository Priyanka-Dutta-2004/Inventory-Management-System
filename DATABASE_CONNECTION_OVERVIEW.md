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
spring.datasource.url=jdbc:postgresql://localhost:5432/inventory_management
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.datasource.driver-class-name=org.postgresql.Driver
```

### Connection Details
- **Host:** localhost
- **Port:** 5432
- **Database Name:** inventory_management
- **Username:** postgres
- **Password:** postgres
- **Driver:** PostgreSQL JDBC Driver

### ORM Framework
**Spring Data JPA** (Hibernate)

JPA Configuration:
```properties
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

- **DDL Auto:** `update` - Automatically creates/updates schema
- **SQL Display:** Enabled for debugging
- **SQL Formatting:** Enabled for readability

### Dependencies
From `inventory-backend/build.gradle.kts`:
```kotlin
implementation("org.springframework.boot:spring-boot-starter-data-jpa")
runtimeOnly("org.postgresql:postgresql")
```

### JPA Entities & Repositories
The backend uses JPA repository pattern for database access:

#### Entity: InventoryAssetEntity
Location: `inventory-backend/src/main/kotlin/.../inventory/InventoryAssetEntity.kt`

```kotlin
@Entity
@Table(name = "inventory_assets")
data class InventoryAssetEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(name = "asset_id", unique = true)
    val assetId: String,
    val name: String,
    val category: String,
    val location: String,
    val status: InventoryStatus,
    // ... more fields
)

enum class InventoryStatus {
    AVAILABLE, IN_USE, MAINTENANCE, RETIRED
}
```

#### Repository: InventoryAssetRepository
Location: `inventory-backend/src/main/kotlin/.../inventory/InventoryAssetRepository.kt`

```kotlin
interface InventoryAssetRepository : JpaRepository<InventoryAssetEntity, Long> {
    fun existsByAssetIdIgnoreCase(assetId: String): Boolean
    fun findByAssetIdIgnoreCase(assetId: String): InventoryAssetEntity?
}
```

#### Other Entities & Repositories
- **UserEntity** & **UserRepository**
- **RoleEntity** & **RoleRepository**

### Data Initialization
**DataSeeder.kt** initializes default data:
- 3 Roles (ADMIN, MANAGER, EMPLOYEE)
- 3 Default Users with different roles
- Sample inventory assets

Location: `inventory-backend/src/main/kotlin/.../config/DataSeeder.kt`

---

## 2. ANDROID APP DATABASE CONNECTION

### Architecture
The Android app uses a **hybrid storage approach**:

#### A. Backend API Communication (Primary Data Source)
**OkHttp3 HTTP Client** for REST API calls to the Spring Boot backend

Location: `app/src/main/java/.../network/BackendApi.kt`

**Connection Configuration:**
```kotlin
val client: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(2, TimeUnit.SECONDS)
    .readTimeout(5, TimeUnit.SECONDS)
    .writeTimeout(5, TimeUnit.SECONDS)
    .build()
```

**Base URL Resolution:**
From `app/build.gradle.kts`:
```kotlin
buildConfigField("String", "BACKEND_BASE_URL", "\"http://172.23.180.194:8080/\"")
```

The app tries multiple endpoints in order:
1. `http://172.23.180.194:8080/` (Configured build config)
2. `http://10.0.2.2:8080/` (Android emulator host alias)
3. `http://10.0.3.2:8080/` (Alternative emulator host)
4. `http://127.0.0.1:8080/` (Localhost)
5. `http://localhost:8080/` (Localhost alias)

**Health Check:**
Validates backend availability via `actuator/health` endpoint before making requests

**Dependencies:**
```kotlin
implementation("com.squareup.okhttp3:okhttp:4.12.0")
```

#### B. Local Storage (SharedPreferences)
For local caching and offline data handling

**EmployeeRequestRepository** (SharedPreferences-based):
Location: `app/src/main/java/.../employee/EmployeeRequestRepository.kt`

Stores:
- **Employee request drafts** (in-progress forms)
- **Submitted requests** (local cache)

**Data Storage Format:** JSON (serialized via org.json)

```kotlin
object EmployeeRequestRepository {
    private const val preferencesName = "employee_requests"
    private const val keyRequests = "submitted_requests"
    private const val keyDraft = "request_draft"
}
```

### Connection Method
**HTTP REST API** over TCP/IP

### Authentication
Uses Firebase for authentication (from dependencies):
```kotlin
implementation(platform("com.google.firebase:firebase-bom:34.10.0"))
implementation("com.google.firebase:firebase-analytics")
```

---

## 3. DATA FLOW DIAGRAM

```
┌─────────────────────────────────────────────────────────────┐
│                   ANDROID APPLICATION                        │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌──────────────────────────────────────────────────────┐   │
│  │         User Interface (Activities)                   │   │
│  │  - AdminLoginActivity                                 │   │
│  │  - EmployeeLoginActivity                              │   │
│  │  - ManagerLoginActivity                               │   │
│  └──────────────────┬───────────────────────────────────┘   │
│                     │                                         │
│  ┌──────────────────▼───────────────────────────────────┐   │
│  │  Repository Layer                                     │   │
│  │  - EmployeeRequestRepository (SharedPreferences)     │   │
│  │  - EmployeeAssetRepository                           │   │
│  └──────────────────┬───────────────────────────────────┘   │
│                     │                                         │
│  ┌──────────────────▼───────────────────────────────────┐   │
│  │  Network Layer (BackendApi)                          │   │
│  │  - OkHttp3 Client                                     │   │
│  │  - JSON Request/Response Handling                    │   │
│  │  - Health Check & URL Resolution                    │   │
│  └──────────────────┬───────────────────────────────────┘   │
│                     │                                         │
└─────────────────────┼─────────────────────────────────────────┘
                      │
                      │ HTTP REST API (TCP/IP)
                      │
┌─────────────────────▼─────────────────────────────────────────┐
│              SPRING BOOT BACKEND (Port 8080)                  │
├───────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │  REST Controllers                                        │ │
│  │  - InventoryAdminController                             │ │
│  │  - Other API endpoints                                  │ │
│  └──────────────┬──────────────────────────────────────────┘ │
│                 │                                              │
│  ┌──────────────▼──────────────────────────────────────────┐ │
│  │  Service Layer                                           │ │
│  │  - InventoryAdminService                                │ │
│  │  - Business Logic                                        │ │
│  └──────────────┬──────────────────────────────────────────┘ │
│                 │                                              │
│  ┌──────────────▼──────────────────────────────────────────┐ │
│  │  Repository Layer (Spring Data JPA)                     │ │
│  │  - InventoryAssetRepository                             │ │
│  │  - UserRepository                                       │ │
│  │  - RoleRepository                                       │ │
│  └──────────────┬──────────────────────────────────────────┘ │
│                 │                                              │
│  ┌──────────────▼──────────────────────────────────────────┐ │
│  │  Hibernate ORM & JPA                                     │ │
│  │  - Entity Mapping                                        │ │
│  │  - Auto DDL (spring.jpa.hibernate.ddl-auto=update)      │ │
│  └──────────────┬──────────────────────────────────────────┘ │
│                 │                                              │
└─────────────────┼──────────────────────────────────────────────┘
                  │
                  │ JDBC Connection Pool
                  │
┌─────────────────▼──────────────────────────────────────────────┐
│         PostgreSQL Database                                     │
│         Host: localhost:5432                                    │
│         Database: inventory_management                          │
├────────────────────────────────────────────────────────────────┤
│  Tables:                                                        │
│  - inventory_assets                                             │
│  - users                                                        │
│  - roles                                                        │
│  - (other tables)                                               │
└────────────────────────────────────────────────────────────────┘
```

---

## 4. CONNECTION MODULES & DEPENDENCIES

### Backend (Spring Boot)

| Module | Purpose | Version |
|--------|---------|---------|
| spring-boot-starter-web | REST API endpoints | 3.4.3 |
| spring-boot-starter-data-jpa | ORM & Database abstraction | 3.4.3 |
| spring-boot-starter-validation | Input validation | 3.4.3 |
| postgresql | PostgreSQL JDBC Driver | Latest |
| jackson-module-kotlin | JSON serialization | Latest |
| spring-boot-starter-actuator | Health checks | 3.4.3 |

### Android App

| Module | Purpose | Version |
|--------|---------|---------|
| OkHttp3 | HTTP Client | 4.12.0 |
| Firebase | Authentication & Analytics | 34.10.0 |
| SharedPreferences | Local Data Storage | Built-in Android |

---

## 5. CONNECTION FLOW SUMMARY

### Backend → Database
1. **Connection Pool** created by Spring Boot
2. **Hibernate ORM** translates JPA entities to SQL
3. **JDBC Driver** (PostgreSQL) executes queries
4. **TCP/IP Connection** to PostgreSQL server on localhost:5432

### Android App → Backend
1. **OkHttp3 Client** creates HTTP request
2. **URL Resolution** tries multiple backend URLs
3. **Health Check** validates backend availability
4. **HTTP Request** sent over TCP/IP (Port 8080)
5. **JSON Response** parsed by app
6. **Local Caching** via SharedPreferences (optional)

---

## 6. Key Features

✅ **Automatic Schema Management** - Hibernate auto-creates/updates tables  
✅ **Type Safety** - Kotlin with JPA entities  
✅ **REST API** - Spring Boot controllers expose data  
✅ **Flexible Backend Resolution** - App tries multiple URLs  
✅ **Local Caching** - Android app caches requests locally  
✅ **Firebase Integration** - Authentication & Analytics  
✅ **Detailed SQL Logging** - Debugging enabled in dev  

---

## 7. Database Credentials

| Property | Value |
|----------|-------|
| Host | localhost |
| Port | 5432 |
| Database | inventory_management |
| Username | postgres |
| Password | postgres |
| Type | PostgreSQL |

⚠️ **Note:** These are development credentials. Use environment variables or secrets management in production.

---

## 8. Backend Server Configuration

| Setting | Value |
|---------|-------|
| Server Address | 0.0.0.0 (Listen on all interfaces) |
| Server Port | 8080 |
| Spring Boot Version | 3.4.3 |
| Java Version | 21 |

---

Generated: April 16, 2026
