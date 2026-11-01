#!/usr/bin/env bash
set -Eeuo pipefail

if [[ $# -ne 11 ]]; then
  echo 'Usage: deploy-candidate.sh PROJECT REGION SERVICE DB_HOST DB_PORT DB_NAME DB_USER DB_SECRET SECRET_VERSION IMAGE BUILD_ID' >&2
  exit 2
fi
PROJECT="$1"; REGION="$2"; SERVICE="$3"; DB_HOST="$4"; DB_PORT="$5"
DB_NAME="$6"; DB_USER="$7"; DB_SECRET="$8"; SECRET_VERSION="$9"
IMAGE="${10}"; BUILD_ID="${11}"

if [[ -z "$DB_HOST" || "$DB_HOST" == REPLACE_* || "$DB_HOST" == localhost || "$DB_HOST" == 127.0.0.1 ]]; then
  echo 'ERROR: Configure _DB_HOST with a PostgreSQL host reachable from Cloud Run before enabling deployment.' >&2
  exit 3
fi

RUNTIME_SA="inventory-runtime@${PROJECT}.iam.gserviceaccount.com"
TAG="candidate-${BUILD_ID}"  # Unique per deployment. Never reuse an active revision tag.

# Cloud Run startup depends on DB availability; Liquibase runs when the app starts.
# First deployment has no blue revision; it must receive traffic after it becomes ready.
TRAFFIC_ARG=()
if gcloud run services describe "$SERVICE" --project "$PROJECT" --region "$REGION" --format='value(metadata.name)' >/dev/null 2>&1; then
  echo 'Existing service found: stage new revision with 0% traffic.'
  TRAFFIC_ARG=(--no-traffic)
else
  echo 'No previous Cloud Run service: first deployment initializes the service.'
fi

# Keep access IAM-protected even though the Cloud Run HTTPS endpoint is internet-routable.
# For authenticated testing, grant the tester roles/run.invoker on the service separately.
gcloud run deploy "$SERVICE" \
  --project "$PROJECT" --region "$REGION" --platform managed \
  --image "$IMAGE" --tag "$TAG" "${TRAFFIC_ARG[@]}" \
  --service-account "$RUNTIME_SA" \
  --ingress all --no-allow-unauthenticated \
  --cpu 1 --memory 1Gi --min-instances 0 --max-instances 2 \
  --concurrency 20 --timeout 300 --port 8080 \
  --update-env-vars "SPRING_PROFILES_ACTIVE=cloud,DB_HOST=${DB_HOST},DB_PORT=${DB_PORT},DB_NAME=${DB_NAME},DB_USER=${DB_USER}" \
  --update-secrets "DB_PASSWORD=${DB_SECRET}:${SECRET_VERSION}" \
  --startup-probe 'httpGet.path=/actuator/health/readiness,httpGet.port=8080,periodSeconds=10,timeoutSeconds=5,failureThreshold=24' \
  --quiet

REVISION="$(gcloud run services describe "$SERVICE" --project "$PROJECT" --region "$REGION" --format='value(status.latestCreatedRevisionName)')"
if [[ -z "$REVISION" ]]; then
  echo 'ERROR: Cloud Run did not return latestCreatedRevisionName' >&2
  exit 4
fi
printf '%s\n' "$REVISION" >/workspace/deployed-revision.txt
printf '%s\n' "$TAG" >/workspace/deployed-tag.txt
printf 'Candidate revision: %s\nUnique testing tag: %s\n' "$REVISION" "$TAG"
