# CI/CD tutorial — GitHub approval → Google Cloud Build → Cloud Run (Java 17)

This is a hands-on deployment exercise for the generated Inventory Backend repository.
It adds two Cloud Build configurations:

- `cloudbuild-build-only.yaml`: Gradle test → Docker image → Artifact Registry. Does **not** need a cloud database.
- `cloudbuild.yaml`: the same build + Cloud Run candidate revision + readiness verification + 100% traffic promotion.

This environment uses **Docker** from the existing Dockerfile, rather than Jib. Docker and Jib both build OCI-compatible images; Jib requires an extra Gradle plugin and is a later exercise. You do **not** need JFrog, private workers, VPC connectors, or proxy variables for the first lab.

**Important:** All example names are editable. They are not resources that already exist in your Google Cloud project. Running commands can create billable resources. Configure billing budget alerts first. This walkthrough cannot provision resources in your account automatically.

## A. What a successful pipeline means

1. GitHub branch rules prevent merging without **one reviewer approval** and passing GitHub Actions `test` job from `.github/workflows/pr-checks.yml`.
2. A merge creates a new commit on `main`; Cloud Build receives the push event.
3. Cloud Build executes Gradle unit tests.
4. Cloud Build builds and pushes an image tagged with its unique `BUILD_ID`.
5. The pipeline creates a **uniquely tagged candidate** Cloud Run revision. For an existing service the candidate starts with **0%** traffic. The first ever deployment has no prior revision and initializes the service.
6. The readiness step verifies Cloud Run reports that precise revision as `Ready=True`.
7. Only then does the pipeline route 100% of ordinary service traffic to the candidate.

**Readiness is not a complete end-to-end API test.** Add a true authenticated HTTP health/API smoke test before production, especially when adding Apigee. If a failed revision is not ready, the promotion stage does not run, so the previous service traffic is not switched. The Cloud Run deployment itself can still leave behind a failed revision. Once promoted, rollback is a *separate* action.

## B. Before Cloud Run: decide the PostgreSQL hosting

Your PC's PostgreSQL `localhost:5432` is reachable to Spring Boot running on **your PC**, not from Cloud Run. You must choose and configure a reachable PostgreSQL instance first. Options:

- A managed PostgreSQL hosting provider reachable securely from Cloud Run; check encryption/TLS and whether it permits inbound cloud addresses.
- Cloud SQL PostgreSQL, with Cloud Run connectivity configured appropriately (not included in this basic script); **Cloud SQL is billed**.

The provided `application.yml` expects `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`. The `DB_PASSWORD` value is resolved from Google Secret Manager. A provider requiring JDBC `sslmode=require`, certificate configuration, or a Cloud SQL Unix socket needs an explicit configuration change before deployment. Do not expose PostgreSQL publicly without network controls.

## C. Google Cloud bootstrap (Google Cloud Shell — Bash)

Choose your own **real** project ID before executing:

```bash
export PROJECT_ID='REPLACE_WITH_YOUR_GCP_PROJECT_ID'
export REGION='asia-south1'                   # Example only; confirm your preferred region
export AR_REPO='inventory-images'
export SERVICE='inventory-backend'

gcloud config set project "$PROJECT_ID"
gcloud services enable cloudbuild.googleapis.com run.googleapis.com \
  artifactregistry.googleapis.com secretmanager.googleapis.com iam.googleapis.com

gcloud artifacts repositories create "$AR_REPO" \
  --repository-format=docker --location="$REGION" \
  --description='Inventory Java 17 backend images'
```

Create dedicated service accounts:

```bash
gcloud iam service-accounts create inventory-build \
  --display-name='Inventory Cloud Build deploy identity'
gcloud iam service-accounts create inventory-runtime \
  --display-name='Inventory Cloud Run runtime identity'

export BUILD_SA="inventory-build@${PROJECT_ID}.iam.gserviceaccount.com"
export RUNTIME_SA="inventory-runtime@${PROJECT_ID}.iam.gserviceaccount.com"
```

Grant least-privilege roles necessary for this lab (the Cloud Build execution identity also needs permission to produce logs):

```bash
# Repository-scoped image push permission.
gcloud artifacts repositories add-iam-policy-binding "$AR_REPO" \
  --location="$REGION" --member="serviceAccount:${BUILD_SA}" \
  --role='roles/artifactregistry.writer'

# Cloud Run deployment permission (project-scoped for ease of first deployment).
gcloud projects add-iam-policy-binding "$PROJECT_ID" \
  --member="serviceAccount:${BUILD_SA}" --role='roles/run.admin'

# Build service account may deploy Cloud Run as inventory-runtime.
gcloud iam service-accounts add-iam-policy-binding "$RUNTIME_SA" \
  --member="serviceAccount:${BUILD_SA}" \
  --role='roles/iam.serviceAccountUser'

# Logs written by the build identity.
gcloud projects add-iam-policy-binding "$PROJECT_ID" \
  --member="serviceAccount:${BUILD_SA}" --role='roles/logging.logWriter'
```

Cloud Build's own service agent / GitHub integration permissions and the person creating the trigger have their own permissions. Google Cloud Console may prompt you to grant the necessary identity access to select the service account for a trigger. Do not grant broad Owner/Editor to the build service account.

Create the database password secret through **Google Cloud Console → Security → Secret Manager → Create secret**:

- Name: `inventory-db-password` (matches the example `_DB_SECRET`)
- Secret value: your remotely hosted database password
- Version: `1` (matches `_DB_SECRET_VERSION`)

Then grant only the runtime identity permission to read it:

```bash
gcloud secrets add-iam-policy-binding inventory-db-password \
  --member="serviceAccount:${RUNTIME_SA}" \
  --role='roles/secretmanager.secretAccessor'
```

If you use another secret version, change `_DB_SECRET_VERSION` in the trigger substitutions. Avoid typing credentials into the repository, YAML, or build logs.

## D. Learn Cloud Build before deploying (no cloud DB required)

1. Push the backend source to **your separate backend GitHub repository**. Do not push passwords.
2. In Google Cloud Console, open **Cloud Build → Triggers**.
3. Connect your GitHub repository and install/authorize the official Cloud Build GitHub integration as prompted.
4. Create a trigger named `inventory-backend-build-only` for a **push to branch** `^main$`.
5. Set configuration to **Cloud Build configuration file**, path `cloudbuild-build-only.yaml`.
6. Select the `inventory-build` service account and a supported region; use Cloud Logging for logs.
7. Test a new commit to `main` (after required approval). Observe three build stages: tests, Docker build, Docker push.
8. In **Artifact Registry → Repositories → inventory-images**, confirm the new image tag is present.

For a one-off build without connecting GitHub, from the source directory in a shell with `gcloud` authenticated:

```bash
gcloud builds submit . --project="$PROJECT_ID" --region="$REGION" \
  --config=cloudbuild-build-only.yaml \
  --service-account="projects/${PROJECT_ID}/serviceAccounts/${BUILD_SA}"
```

This validates image publishing independently of PostgreSQL/Cloud Run.

## E. Enable full blue/green deployment after DB is ready

1. Verify remote PostgreSQL connectivity from GCP, permissions, TLS settings, and supported schema migrations.
2. Edit `_DB_HOST`, `_DB_USER`, `_DB_NAME`, `_DB_PORT` in `cloudbuild.yaml`, **or configure these as trigger substitutions**. Never use `localhost` here.
3. In Cloud Build → Triggers, configure a `push to branch` trigger for `^main$` with file **`cloudbuild.yaml`** and the `inventory-build` service account. Disable the earlier build-only trigger to avoid duplicate builds.
4. Merge a tested feature branch to main after approval.
5. In Cloud Build → History, inspect all six steps.
6. In Cloud Run → Services → `inventory-backend` → Revisions, inspect revision tags and traffic split. The first deployment is initialized directly; later successful deployments stage a candidate at 0%, then move to 100% after readiness.

The **Cloud Run service stays IAM-protected** (`--no-allow-unauthenticated`). You may grant your own Google identity invoke permission for authenticated manual tests:

```bash
gcloud run services add-iam-policy-binding "$SERVICE" \
  --project="$PROJECT_ID" --region="$REGION" \
  --member='user:YOUR_GOOGLE_ACCOUNT_EMAIL' --role='roles/run.invoker'

SERVICE_URL="$(gcloud run services describe "$SERVICE" \
  --project="$PROJECT_ID" --region="$REGION" \
  --format='value(status.url)')"
TOKEN="$(gcloud auth print-identity-token)"
curl -fsS -H "Authorization: Bearer $TOKEN" \
  "$SERVICE_URL/actuator/health"
```

For browser-based Angular integration in a later phase, we will design an appropriate authenticated gateway access model; do **not** expose the service or embed service-account tokens in Angular just to simplify testing.

### Manual rollback

Find the previous healthy revision under Cloud Run → Service → Revisions. To switch all traffic to it:

```bash
gcloud run services update-traffic inventory-backend \
  --project="$PROJECT_ID" --region="$REGION" \
  --to-revisions='REPLACE_WITH_OLD_REVISION=100'
```

This traffic rollback does **not** roll back Liquibase schema changes. Database migrations must therefore be backward-compatible across blue and green revisions.

## F. GitHub: make one approval mandatory

1. Go to the backend GitHub repository → **Settings → Branches** or Rulesets (according to your plan/UI).
2. Protect `main`. Enable **Require a pull request before merging**, **one approving review**, **required status checks** (GitHub Actions job `test`), and **block force pushes**.
3. Add the intended reviewer as an authorized repository collaborator. GitHub enforcement relies on the GitHub **account and repository access**, not on an email field in Cloud Build.
4. To insist that a **particular** reviewer approves, add `.github/CODEOWNERS` with `* @actual-username` and enable **Require review from Code Owners** where supported.
5. GitHub Free supports protected branches for **public** repositories; private repository enforcement generally requires eligible paid GitHub plans. Never publish passwords or keys even in a private repository.

The existing `.github/workflows/pr-checks.yml` runs Gradle tests when a PR targets `main` (and can be selected as the required check). Cloud Build's `main` push trigger runs only **after** the merge produces a new commit. Cloud Build itself does **not** enforce the GitHub reviewer requirement.

## G. How this differs from your work screenshots

| Screenshot step | Lab equivalent | Why |
| --- | --- | --- |
| Service account token | Cloud Build's own authenticated identity | No custom token file / JFrog authentication |
| Gradle Jib push | Docker build + push to Artifact Registry | The existing backend already has a Dockerfile |
| Deploy green revision | Deploy new, uniquely tagged revision; 0% if service exists | Does not disturb current traffic |
| Readiness check | Check `Ready=True` for exact created revision | Prevents promotion on failed revision |
| IAM binding | One-time IAM roles during bootstrap | No need to modify IAM on every push |
| Blue → Green 100% | `update-traffic --to-revisions REV=100` | New version becomes active only after readiness |

Not included yet: private Cloud Build pool, VPC connector, internal-only ingress, JFrog registry credentials, Apigee API proxy, HTTP business-endpoint smoke test, 42Crunch, deployment manual gate, automatic post-promotion rollback. Those are additional industry topics, not requirements for understanding your first successful CD pipeline.

## H. Budget safety

- Set project-level budget alerts before enabling deployments. They notify but do not disable services automatically.
- Cloud Run `--min-instances 0` reduces idle compute, but there are still potential network, build, storage, database, and resource charges.
- **Cloud SQL, Cloud Build private pools, private networking, and Apigee** can incur significant charges. Do not create them merely to match the example screenshots.
- Remove unused Artifact Registry images and stop/delete resources at the end of the lab.

## References

- Cloud Build deployment: https://docs.cloud.google.com/build/docs/deploying-builds/deploy-cloud-run
- Cloud Run traffic: https://docs.cloud.google.com/run/docs/rollouts-rollbacks-traffic-migration
- Cloud Run secrets: https://docs.cloud.google.com/run/docs/configuring/services/secrets
- GitHub protected branches: https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-protected-branches/about-protected-branches
