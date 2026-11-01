#!/usr/bin/env bash
set -Eeuo pipefail

if [[ $# -ne 10 ]]; then
  echo 'Usage: deploy-candidate.sh PROJECT REGION SERVICE INSTANCE_CONNECTION_NAME DB_NAME DB_USER DB_SECRET SECRET_VERSION IMAGE BUILD_ID' >&2
  exit 2
fi
PROJECT="$1"; REGION="$2"; SERVICE="$3"; INSTANCE_CONNECTION_NAME="$4"
DB_NAME="$5"; DB_USER="$6"; DB_SECRET="$7"; SECRET_VERSION="$8"
IMAGE="${9}"; BUILD_ID="${10}"

EXPECTED_PREFIX="${PROJECT}:${REGION}:"
if [[ "$INSTANCE_CONNECTION_NAME" != "${EXPECTED_PREFIX}"* ]]; then
  echo 'ERROR: Cloud SQL instance connection name does not match project/region' >&2
  exit 3
fi

RUNTIME_SA="inventory-runtime@${PROJECT}.iam.gserviceaccount.com"
#TAG="candidate-${BUILD_ID}"  # Unique per deployment. Never reuse an active revision tag.

# Use the first 8 characters of the Cloud Build ID
SHORT_BUILD_ID="${BUILD_ID:0:8}"

# Generate a shorter revision tag
CANDIDATE_TAG="c-${SHORT_BUILD_ID}"

echo "Deploying candidate with tag: ${CANDIDATE_TAG}"



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
  --image "$IMAGE" --tag "$CANDIDATE_TAG" "${TRAFFIC_ARG[@]}" \
  --service-account "$RUNTIME_SA" \
  --ingress all --no-allow-unauthenticated \
  --cpu 1 --memory 1Gi --min-instances 0 --max-instances 1 \
  --concurrency 20 --timeout 300 --port 8080 \
  --update-env-vars "SPRING_PROFILES_ACTIVE=cloud,INSTANCE_CONNECTION_NAME=${INSTANCE_CONNECTION_NAME},DB_NAME=${DB_NAME},DB_USER=${DB_USER}" \
  --update-secrets "DB_PASSWORD=${DB_SECRET}:${SECRET_VERSION}" \
  --startup-probe 'httpGet.path=/actuator/health,httpGet.port=8080,periodSeconds=10,timeoutSeconds=5,failureThreshold=24' \
  --quiet

REVISION="$(gcloud run services describe "$SERVICE" --project "$PROJECT" --region "$REGION" --format='value(status.latestCreatedRevisionName)')"
if [[ -z "$REVISION" ]]; then
  echo 'ERROR: Cloud Run did not return latestCreatedRevisionName' >&2
  exit 4
fi
printf '%s\n' "$REVISION" >/workspace/deployed-revision.txt
printf '%s\n' "$CANDIDATE_TAG" >/workspace/deployed-tag.txt
printf 'Candidate revision: %s\nUnique testing tag: %s\n' "$REVISION" "$CANDIDATE_TAG"
