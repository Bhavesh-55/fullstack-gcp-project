# Incremental learning checkpoints

## Phase 1: Backend (code delivered)

1. Import the project into IntelliJ; inspect build.gradle, application.yml, and InventoryApplication.
2. Create the local PostgreSQL database using pgAdmin. Supply credentials via DB_PASSWORD.
3. Run `gradlew.bat clean test` and `gradlew.bat bootRun`.
4. Inspect Liquibase `databasechangelog` / `databasechangeloglock` and five business tables.
5. Use the bundled Postman collection to create Category, Supplier, Product, then Order.
6. Verify GET/POST/PUT/DELETE and Order PATCH; test validation and 404/409 behavior.
7. Run `gradlew.bat generateOpenApiSpec` with the API live; inspect `specs/openapi.json`.
8. Commit the application into your **backend repository** and open a review PR.

## Phase 2: Frontend (separate repository)

Confirm the installed Angular CLI/runtime versions; create forms, lists, sorting, search, pagination and REST integration.

## Phase 3: GCP deployment and CI/CD

Decide a remotely accessible PostgreSQL deployment first. Then provision Artifact Registry, Cloud Run, CI service account, Cloud Build GitHub trigger, appropriate secrets. Configure repo branch protection requiring an authorized review before merge. Trigger deployment on `main`, not on PR approval. Add a smoke test and rollback mechanism.

## Phase 4: Apigee

Create Apigee evaluation organization only when ready. Configure API proxy routing to Cloud Run service, environment, revisions, policies, API products, usage analytics. Compare with AWS API Gateway.

## Phase 5: 42Crunch

Inspect and validate `specs/openapi.json` in static contract audit; perform controlled conformance scan against *your own* deployed API. Compare failures with Spring and Apigee responses.

## Phase 6: Authentication / authorization (if desired)

Design identity provider, JWT validation at gateway/backend, role-based policies, browser-safe token handling and route permissions.
