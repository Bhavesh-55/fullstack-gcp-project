# Backend architecture and file walkthrough

## Request path

`Angular/Postman → @RestController → @Valid DTO → @Service @Transactional → Repository → PostgreSQL`

The service then maps the persistence entity to a response record and sends the JSON through the controller. On GCP, Apigee will sit in front of Cloud Run and forward the same route.

## File/package responsibilities

| File or directory | Purpose |
|---|---|
| `InventoryApplication.java` | `@SpringBootApplication` entry point / component scan |
| `controller/*Controller.java` | `@RestController`, mappings, DTO validation, OpenAPI operation descriptions, HTTP status codes |
| `dto/*Request.java` | Java 17 records for JSON inputs with Bean Validation annotations |
| `dto/*Response.java` | Output-only records; don't leak JPA entities to consumers |
| `service/*Service.java` | Business validations, `@Transactional` boundaries, cross-repository orchestration |
| `repository/*Repository.java` | `JpaRepository` interfaces, derived query methods, read-side fetch graphs |
| `entity/*` | `@Entity`, `@Table`, relations and columns, audit lifecycle callbacks |
| `mapper/InventoryMapper.java` | Entity → DTO mapping, avoiding lazy association exposure at the API boundary |
| `exception/GlobalExceptionHandler.java` | HTTP 400/404/409/500 consistent error response |
| `config/CorsConfig.java` | Browser origin allowance for Angular development; change on deployment |
| `config/OpenApiConfig.java` | Swagger/OpenAPI metadata |
| `db/changelog` | Versioned PostgreSQL table creation and constraints |
| `build.gradle` | Java toolchain, Gradle dependencies, test and `generateOpenApiSpec` task |
| `specs/` | Generated OpenAPI contract in Git (for Apigee / 42Crunch validation) |
| `Dockerfile` | Build and runtime stages using Java 17 for future Cloud Run |
| `.github/workflows/pr-checks.yml` | PR-only test workflow, distinct from later GCP main-merge deployment trigger |

## Database relationships

`Category 1 ── * Product * ── 1 Supplier`

`PurchaseOrder 1 ── * OrderItem * ── 1 Product`

Order.totalAmount is derived from OrderItem.lineTotal; order items snapshot unit price when placed. The order's foreign keys reference products, so deleting an ordered product is prevented. Active is required in incoming DTOs; no custom authentication or authorization has been built yet.

## Validation and HTTP behavior

- `@NotBlank`, `@NotNull`, `@Positive`, `@DecimalMin`, `@Digits`, `@Email` reject invalid payloads
- `@Valid` in controllers triggers validation before calling a service
- `@Transactional` rolls back failed writes
- `@ManyToOne(fetch=LAZY)` holds foreign-key associations
- `@OneToMany(cascade=ALL, orphanRemoval=true)` manages order item lifecycle
- `@RestControllerAdvice` centralizes error mapping
- `@Operation`, `@Tag`, `@Schema` contribute to OpenAPI generation

## Limitations / future enhancements

This intentionally remains a learning backend: no paging sort whitelist, no stock decrement, no request idempotency key for orders, no audit user, no production authentication, no database integration tests, no 42Crunch wiring or Apigee policies yet. Those choices are explicit rather than hidden assumptions. Concurrency protections and stricter money/business checks should be revisited before any real commerce usage.
