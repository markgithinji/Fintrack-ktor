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
You'll need PostgreSQL for data and Redis for rate limiting and token management. The easiest way is using Docker Compose:

```bash
docker-compose up -d
```

Alternatively, you can run them individually:

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
- **Kotlinx Serialization**: Type-safe, multiplatform JSON serialization with custom handlers for `BigDecimal` and `UUID`.
- **DoubleReceive**: Optimized request body handling for logging and validation.

**Database & Persistence:**
- **PostgreSQL**: Primary relational database with a **Normalized Financial Schema** using join tables for complex relationships.
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
- **Audit Logging**: Structured logging of sensitive actions (registration, login, password changes) with IP and User-Agent tracking for security forensics.

### 📊 Financial Intelligence
- **Merchant Intelligence & Normalization**: Automated description cleaning that transforms messy bank strings (e.g., `"STARBUCKS STORE #1234 NY"`) into clean merchant identities.
- **Dynamic Server-Side Sync Parsing**: Advanced keyword matching engine for transaction categorization that can be updated on-the-fly via the server without needing a mobile client release.
- **Predictive Burn-Rate Analytics**: Intelligent forecasting that predicts exactly *which month* a user is likely to exceed their annual budget based on current spending velocity.
- **Behavioral Correlations**: Insight engine that identifies links between financial events, such as detecting lifestyle creep by correlating income growth with specific luxury spending.
- **Recurring Bill Detection**: Pattern-based detection of subscription and recurring payments based on frequency and merchant history.
- **Advanced Budgeting Engine**: High-integrity tracking of spending against multi-account and multi-category limits using a relational join architecture.

### ⚙️ Maintainability & Architecture
- **Clean Architecture**: Strict separation of concerns (Domain -> Application -> Data -> Presentation).
- **Functional Domain Patterns**: Implementation of the `Result` sealed class pattern for robust, exception-free business logic.
- **Feature-Modular Design**: Code organized by business domains (Auth, Transactions, Budgets, Summary) to reduce coupling.
- **Type Safety**: End-to-end type safety using Kotlin's powerful type system and Exposed DSL, ensuring zero-rounding error precision for financial data.

---

## 🏗️ Architecture Layers

1.  **Domain**: Pure Kotlin logic, Entities, and Repository Interfaces. No dependencies on frameworks. Uses `Result<T>` for functional error handling.
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
