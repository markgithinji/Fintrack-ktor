# 🚀 Fintrack Ktor API — Personal Finance Backend Service

**Fintrack Ktor API** is a robust, high-performance backend service built with **Ktor** and **Kotlin**, designed to power the [Fintrack KMP mobile application](https://github.com/markgithinji/fintrack-kmp). It follows **clean architecture** principles to deliver a scalable, maintainable, and secure financial data API.

> 🔒 **Production Ready**: Features enterprise-grade security, monitoring, and reliability measures.

---

## 🚀 Quick Setup

### Prerequisites
- **Java 17** or higher
- **Docker Desktop** (installed and running)
- **Git**
- **Postman**

### 1. Clone & Setup
```bash
git clone https://github.com/markgithinji/Fintrack-ktor
cd fintrack-ktor
```

### 2. Database Setup with Docker
```bash
# Pull PostgreSQL image
docker pull postgres:15

# Create and run container
docker run -d --name fintrack-db -e POSTGRES_DB=fintrack_db -e POSTGRES_USER=fintrack -e POSTGRES_PASSWORD=secret -p 5432:5432 postgres:15
```

### 3. Verify Database
```bash
docker ps
# Should show fintrack-db running
```

### 4 Build the application
```bash
./gradlew build

# Run the application
./gradlew run
```

### 5 Testing with Sample Data
##### Register a user:
```bash
POST http://localhost:8080/auth/register
```
Body:
```bash
{
  "email": "test@example.com",
  "password": "testpass123"
}
£ Save the JWT token from the response
```

##### Get your account IDs:
```bash
GET http://localhost:8080/accounts
Headers: Authorization: Bearer <your-jwt-token>
# Save the account IDs from the response
```

##### Prepare sample data:
- Download sample data: [sample-transactions.json](https://gist.githubusercontent.com/markgithinji/a6f2b56c782b404e8e71ee9238b3e1e8/raw/sample-transactions.json)
- Use this AI prompt to update the account IDs and dates:

**AI Prompt:** *"First, replace all accountId values in this JSON: replace accountId 1 with [your-bank-account-id] and accountId 2 with [your-wallet-account-id]. Then update all dates to be within the last 7 days from today, ensuring each day has multiple transactions (both income and expenses) with realistic timestamps throughout each day. Keep the same structure and categories, but spread the transactions evenly across the past week with 2-4 transactions per day."*

##### Add sample transactions:
```bash
POST http://localhost:8080/transactions/bulk
Headers: Authorization: Bearer <your-jwt-token>
Body: [paste-your-updated-json-here]
```
> 📝 **Important**: Make sure to replace the account IDs in the sample data with your real account IDs from step 3, and update the dates to be recent (within the last 7 days). This ensures the KMP mobile app charts display meaningful data since they work with recent transaction history.
> 🔑 Note: You need to include the JWT token in the Authorization header for all protected endpoints.

---

## 🏗️ Tech Stack

**Server & Framework:**
- **Ktor**: Asynchronous web framework for Kotlin
- **Netty Engine**: High-performance web server
- **Kotlin Serialization**: Type-safe JSON serialization/deserialization
- **Content Negotiation**: Automatic content type handling

**Database & Persistence:**
- **PostgreSQL**: Primary relational database
- **Exposed**: Type-safe SQL DSL and DAO
- **HikariCP**: High-performance connection pooling
- **Kotlinx DateTime**: Modern date/time handling
- **Pagination**: Efficient server-side pagination for large datasets

**Security & Authentication:**
- **JWT Authentication**: Stateless token-based auth
- **BCrypt**: Secure password hashing
- **Rate Limiting**: API abuse protection
- **Request Validation**: Input sanitization and validation

**Monitoring & Operations:**
- **Micrometer Metrics**: Application metrics collection
- **Prometheus**: Metrics endpoint for monitoring
- **Health Checks**: System and dependency health monitoring
- **Structured Logging**: JSON logging with Logstash encoder

**Dependency & Configuration:**
- **Koin**: Dependency injection framework
- **YAML Configuration**: Externalized configuration management
- **Gradle Version Catalog**: Centralized dependency management

**Code Quality & Style:**
- **Spotless**: Automated code formatting and license headers
- **Detekt**: Static code analysis with customizable rulesets
- **Ktlint**: Kotlin linter integrated with Spotless

---

## 💡 Core Features

### 🔐 Security & Auth
- **JWT-based Authentication** with configurable expiration
- **Secure Password Hashing** using BCrypt
- **Rate Limiting** per endpoint to prevent abuse
- **Request Validation** with comprehensive error handling

### 📊 Financial Operations
- **User Management** with secure registration/login
- **Account CRUD** for multiple account types (checking, savings, credit)
- **Transaction Processing** with category tagging
- **Budget Management** with category-wise limits
- **Financial Analytics** for spending insights and trends
- **Server-side Pagination** for efficient data retrieval

### ⚡ Performance & Reliability
- **Async Non-blocking** I/O operations
- **Connection Pooling** for database efficiency
- **Structured Logging** for easy debugging and monitoring
- **Health Checks** for system reliability
- **Optimized Queries** with pagination support

### 🔍 Monitoring & Metrics
- **Prometheus Metrics** endpoint for monitoring
- **Request Logging** with correlation IDs
- **Error Tracking** with detailed status pages
- **Performance Metrics** for API endpoints

### 🎯 Code Quality
- **Automated Code Formatting** with Spotless
- **Static Analysis** with Detekt for bug detection
- **Consistent Code Style** across the codebase
- **License Header Management** automated by Spotless

---

## 🏗️ Architecture

**Clean Architecture Layers:**
- **Presentation**: Ktor routes, authentication, validation, DTOs
- **Application**: Use cases, business logic, services  
- **Domain**: Domain objects, interfaces
- **Data**: Database entities, repositories, mappers

---

## 🛠️ Code Quality

### Spotless (Code Formatting)
```bash
# Apply code formatting
./gradlew spotlessApply

# Check code formatting
./gradlew spotlessCheck
```

## 📊 API Endpoints

> 📋 **Coming Soon**: Complete API documentation with endpoints for users, accounts, transactions, budgets, and analytics will be published shortly.

---
