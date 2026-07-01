# 🚀 Fintrack Ktor API — Personal Finance Backend Service

**Fintrack Ktor API** is a robust, high-performance backend service built with **Ktor** and **Kotlin**, designed to power the [Fintrack KMP mobile application](https://github.com/markgithinji/fintrack-kmp). It follows **clean architecture** principles and implements modern security standards to deliver a scalable, maintainable, and secure financial data API.

> 🔒 **Production Ready**: Features enterprise-grade security, distributed monitoring, and high-availability architecture.

---

## 🚀 Quick Setup

### Prerequisites
- **Java 17** or higher
- **Docker Desktop** (installed and running)
- **Git**

### 1. Clone & Setup
```bash
git clone https://github.com/markgithinji/fintrack-backend
cd fintrack-backend
```

### 2. Infrastructure Setup with Docker
You'll need PostgreSQL for data and Redis for rate limiting and token management.

```bash
# PostgreSQL
docker run -d --name fintrack-db -e POSTGRES_DB=fintrack_db -e POSTGRES_USER=fintrack -e POSTGRES_PASSWORD=secret -p 5432:5432 postgres:15

# Redis
docker run -d --name fintrack-redis -p 6379:6379 redis:alpine
```

### 3. Build & Run
```bash
./gradlew build
./gradlew run
```

### 4. Testing with Sample Data
To see the API in action, you can follow this quick flow:

##### Register a user:
```bash
POST http://localhost:8080/auth/register
{
  "email": "test@example.com",
  "password": "testpass123"
}
```
*Note: Save the JWT token from the response.*

##### Get your account IDs:
```bash
GET http://localhost:8080/accounts
Headers: Authorization: Bearer <your-jwt-token>
```

##### Add sample transactions (Bulk):
1. **Fetch IDs**: Get your `accountId` from the `/accounts` endpoint.
2. **Prepare Data**: Use the [sample-transactions.json](sample-data/sample-transactions.json) file included in this repo. You can use this AI prompt with your `accountId` to generate a personalized dataset:
    > "Using the structure in `sample-data/sample-transactions.json`, replace all `accountId` values with [your-id]. Update all dates to be within the last 7 days from today, ensuring 3-4 transactions per day with realistic timestamps and categories."
3. **Upload**:
```bash
POST http://localhost:8080/transactions/bulk
Headers: Authorization: Bearer <your-jwt-token>
Body: [your-generated-json]
```

##### Cleanup:
To start fresh:
```bash
DELETE http://localhost:8080/transactions/clear
Headers: Authorization: Bearer <your-jwt-token>
```

---

## 🏗️ Tech Stack

**Server & Framework:**
- **Ktor**: Asynchronous non-blocking web framework for Kotlin.
- **Netty Engine**: High-performance event-driven network application framework.
- **Kotlinx Serialization**: Type-safe, multiplatform JSON serialization.
- **DoubleReceive**: Optimized request body handling for logging and validation.

**Database & Persistence:**
- **PostgreSQL**: Primary relational database for financial records.
- **Exposed**: Type-safe SQL DSL and Lightweight DAO for Kotlin.
- **HikariCP**: Production-ready high-performance JDBC connection pooling.
- **Flyway**: Automated database schema migrations and versioning.
- **Redis (Jedis)**: High-speed in-memory data store for distributed rate limiting and token management.

**Security & Authentication:**
- **JWT (JSON Web Tokens)**: Stateless authentication with secure payload signing.
- **Argon2id**: Industry-standard, memory-hard password hashing (Winner of PHC).
- **Redis Token Blacklist**: Immediate session revocation/logout capabilities.
- **Distributed Rate Limiting**: Redis-backed protection against brute-force and DDoS.
- **Request Validation**: Strict input sanitization and schema enforcement.

**Observability & Operations:**
- **Micrometer & Prometheus**: Real-time metrics collection and monitoring.
- **Structured Logging**: JSON-encoded logs with Logstash for ELK/Grafana integration.
- **Correlation IDs**: Trace requests across service boundaries.
- **Health Checks**: Automated system and dependency health monitoring.

**Development & Quality:**
- **Koin**: Pragmatic lightweight dependency injection.
- **Detekt**: Static code analysis for code smells and complexity.
- **Spotless & Ktlint**: Automated code formatting and style enforcement.
- **Gradle Version Catalog**: Centralized, type-safe dependency management.

---

## 💡 Core Features

### 🔐 Advanced Security
- **Modern Hashing**: Migrated from BCrypt to **Argon2id** for superior resistance against GPU/ASIC cracking.
- **Stateful Logout in Stateless Auth**: Uses Redis to blacklist JWTs upon logout, ensuring tokens cannot be reused.
- **Granular Rate Limiting**: Per-endpoint and per-user limits to ensure API stability.
- **Audit Logging**: Comprehensive request/response logging for security auditing.

### 📊 Financial Intelligence
- **Advanced Analytics**: Category-wise distribution, period comparisons (Week/Month/Year), and spending trends.
- **Bulk Operations**: High-performance bulk transaction creation for data imports/syncs.
- **Budgeting Engine**: Real-time tracking of spending against category-specific limits.
- **Flexible Reporting**: Server-side aggregation for responsive mobile charts.

### ⚙️ Maintainability & Architecture
- **Clean Architecture**: Strict separation of concerns (Domain -> Application -> Data -> Presentation).
- **Feature-Modular Design**: Code organized by business domains (Auth, Transactions, Budgets, Summary) to reduce coupling.
- **Migration Strategy**: Graceful migration path for legacy password hashes (BCrypt to Argon2) during user login.
- **Type Safety**: End-to-end type safety using Kotlin's powerful type system and Exposed DSL.

---

## 🏗️ Architecture Layers

1.  **Domain**: Pure Kotlin logic, Entities, and Repository Interfaces. No dependencies on frameworks.
2.  **Application**: Implementation of business use cases (Services).
3.  **Data**: Database implementations, Redis services, Mappers, and External API integrations.
4.  **Presentation**: Ktor Routes, DTOs, Authentication interceptors, and Exception mapping.

---

## 🛠️ Code Quality & Standards

We maintain high standards through automated tooling:

```bash
# Check for code smells and style violations
./gradlew detekt

# Automatically fix formatting issues
./gradlew spotlessApply
```

---

## 📊 API Documentation

> 📋 **API Spec**: The API follows RESTful principles. Interactive Swagger/OpenAPI documentation is available at `/openapi` (Coming Soon).

---
