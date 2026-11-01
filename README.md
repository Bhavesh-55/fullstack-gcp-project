# Inventory Backend — Spring Boot 3.5 / Java 17 / PostgreSQL / Liquibase

A complete educational backend for a four-module Inventory Management project. Intended to be deployed to Google Cloud Run, published via Apigee, and scanned using 42Crunch in **later phases**. The current project is **local-first** and intentionally has **no authentication**.

## What is included

- Java 17 / Spring Boot 3.5.16 / Gradle 8.14.6 (Groovy DSL)
- REST CRUD for **Categories, Suppliers, Products, Orders**
- Request/response DTOs using Java records, Bean Validation, JPA entities and repositories
- Foreign keys for products → categories/suppliers and order items → orders/products
- Transactional services and consistent JSON error handling
- PostgreSQL via environment variables; Liquibase version-controlled migrations
- Swagger UI, generated OpenAPI contract (`specs/openapi.json`), health endpoint
- JUnit + Mockito unit tests, importable Postman collection
- Multi-stage Dockerfile for later Cloud Run deployment
- GitHub pull request test workflow (**not** the GCP deployment trigger)

## Prerequisites

- JDK 17 (`java -version`)
- PostgreSQL database server running on localhost:5432, accessible via pgAdmin
- Internet access on first Gradle run (to download Gradle and Maven dependencies)
- IntelliJ IDEA optional

## Setup (Windows PowerShell)

Open the project root and create the database in pgAdmin's query editor (connected to `postgres`):

```sql
CREATE DATABASE inventory_db;
```

Set environment variables in the **same terminal session** that launches Spring Boot:

```powershell
$env:DB_HOST = "localhost"
$env:DB_PORT = "5432"
$env:DB_NAME = "inventory_db"
$env:DB_USER = "postgres"
$env:DB_PASSWORD = "<your existing local PostgreSQL password>"
$env:SPRING_PROFILES_ACTIVE = "local"
.\gradlew.bat clean test
.\gradlew.bat bootRun
```

If launching from IntelliJ's Run Configuration, add DB_PASSWORD and other DB variables there rather than expecting the terminal environment to carry over. **Never commit a real password.**

The supplied `gradlew.bat` / `gradlew` scripts are **portable SHA-256 verified Gradle bootstraps**, not the official Gradle Wrapper JAR. They automatically download Gradle 8.14.6 on first run. See `gradle/wrapper/README.md` for replacing them with an official wrapper. On Linux/macOS run `./gradlew clean test`, `./gradlew bootRun`.

## URLs

| Purpose | Local URL |
|---|---|
| API Swagger UI | http://localhost:8080/swagger-ui/index.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |
| Health | http://localhost:8080/actuator/health |
| Categories | http://localhost:8080/api/v1/categories |
| Suppliers | http://localhost:8080/api/v1/suppliers |
| Products | http://localhost:8080/api/v1/products |
| Orders | http://localhost:8080/api/v1/orders |

The project starts with **empty business tables**; use Swagger/Postman to create records in this order: **Category → Supplier → Product → Order**.

## API endpoints

| Entity | Endpoints |
|---|---|
| Category | GET /api/v1/categories, GET /{id}, POST, PUT /{id}, DELETE /{id} |
| Supplier | GET /api/v1/suppliers, GET /{id}, POST, PUT /{id}, DELETE /{id} |
| Product | GET /api/v1/products?search=keyboard&page=0&size=20, GET /{id}, POST, PUT /{id}, DELETE /{id} |
| Order | GET /api/v1/orders, GET /{id}, POST, PUT /{id}, DELETE /{id}, PATCH /{id}/status |

All list endpoints return Spring Data `Page<...>` JSON, not a raw array. IDs are PostgreSQL BIGINT identities. Missing IDs return 404, invalid payloads 400, unique-key conflicts 409. Deleting in-use categories/suppliers/products is rejected.

## Example create request sequence

### POST /api/v1/categories

```json
{"name":"Electronics","description":"Devices","active":true}
```

### POST /api/v1/suppliers

```json
{"name":"ABC Supplies","email":"sales@abc.example","phone":"9876543210","active":true}
```

### POST /api/v1/products

```json
{"sku":"ELEC-101","name":"Keyboard","description":"USB keyboard","price":799.00,"stockQuantity":20,"categoryId":1,"supplierId":1,"active":true}
```

### POST /api/v1/orders

```json
{"customerName":"Test Customer","customerEmail":"test@example.com","items":[{"productId":1,"quantity":2}]}
```

### PATCH /api/v1/orders/1/status

```json
{"status":"CONFIRMED"}
```

Order item unit price and totals are set by **the server from Product.price**; clients don't provide them. Order status starts at `PENDING`; status changes to `CONFIRMED` or `CANCELLED` and cannot transition further. Only pending orders can have items edited. In this educational version, orders **do not decrement stock** or reserve inventory. Order totals are recalculated when pending orders are edited; historical price snapshots stay fixed after confirmation.

## Database and Liquibase

Liquibase executes `src/main/resources/db/changelog/db.changelog-master.yaml` at startup, creating five business tables:

- `categories`
- `suppliers`
- `products` (FK to categories and suppliers)
- `purchase_orders`
- `order_items` (FK to purchase_orders and products)

Liquibase also creates `databasechangelog` and `databasechangeloglock` metadata tables. Hibernate is set to `ddl-auto=validate`, so it validates mappings instead of creating tables. To inspect migrations in pgAdmin:

```sql
SELECT id, author, filename, dateexecuted FROM databasechangelog ORDER BY orderexecuted;
SELECT table_name FROM information_schema.tables WHERE table_schema='public' ORDER BY table_name;
```

Never edit an already-applied Liquibase changeset. Add a new numbered changeset and include it in the master changelog.

## OpenAPI specs folder

With the app running, open a second terminal:

```powershell
.\gradlew.bat generateOpenApiSpec
```

This exports the **live** `/v3/api-docs` JSON to `specs/openapi.json` in the Git repository. A custom URL can be supplied with `-PapiDocsUrl=http://localhost:8081/v3/api-docs`. The spec is generated from controller and DTO annotations, not handwritten. Commit the generated spec when an API contract changes; it will be used for Apigee/42Crunch labs.

## Automated tests

```powershell
.\gradlew.bat clean test
.\gradlew.bat bootJar
```

Fast JUnit/Mockito service tests need no live DB. **Full database integration tests are not yet included**; the application is still expected to run against your local PostgreSQL for manual integration verification. GitHub Actions runs tests on PRs into `main`; no GCP deployment is triggered yet.

## Key source files and how requests flow

`Controller` receives HTTP request, @Valid checks DTO, `Service` applies @Transactional business logic, `Repository` runs JPA SQL against PostgreSQL, `Mapper` converts Entity to response DTO. `GlobalExceptionHandler` standardizes errors. `OpenApiConfig` configures Swagger description. `CorsConfig` allows your Angular development server at localhost:4200; configure origins before production. See `docs/ARCHITECTURE.md`.

## Later GCP phases (not configured yet)

- Move PostgreSQL from localhost to a database reachable securely by Cloud Run (Cloud SQL pricing to be reviewed)
- Provision Artifact Registry, Cloud Build, Cloud Run and service accounts
- Configure GitHub branch protection / one authorized reviewer / PR merge to main
- Add Cloud Build trigger on main, run build/test/push/deploy automatically
- Publish backend in Apigee as an API proxy and API product
- Use generated OpenAPI specification for 42Crunch static and runtime checks
- Add proper user authentication/authorization (not included in current code)

Cloud Run **cannot access your laptop's localhost**. Do not merge into main expecting deployment until GCP resources, Cloud Build trigger, and remotely accessible DB are configured. Your reviewer is a GitHub *user identity*, not just an email string.

## Version notes

Java 17; Spring Boot 3.5.16; springdoc 2.8.17; Gradle 8.14.6. Spring Boot 3.5 is past open-source maintenance; pinned here to respect the requested training stack, not a current production-support endorsement.

## Cloud Build / Cloud Run CI/CD lab

For the GitHub PR approval → `main` merge → Cloud Build → Cloud Run blue/green exercise, see [`docs/CLOUD-BUILD-CLOUD-RUN.md`](docs/CLOUD-BUILD-CLOUD-RUN.md).

- `cloudbuild-build-only.yaml`: test, build, and push image without a cloud database.
- `cloudbuild.yaml`: six-stage CI/CD with candidate revision verification and traffic promotion. Requires remote PostgreSQL and Google Cloud IAM/Secret Manager setup.
- `scripts/cloudrun/`: the Cloud Run deployment, readiness, and promotion scripts.
