#!/usr/bin/env bash
set -Eeuo pipefail
PROJECT="${1:?PROJECT required}"
REGION="${2:?REGION required}"
SERVICE="${3:?SERVICE required}"
REVISION="$(cat /workspace/deployed-revision.txt)"

echo "Promoting revision $REVISION to 100% of service traffic..."
gcloud run services update-traffic "$SERVICE" \
  --project "$PROJECT" --region "$REGION" \
  --to-revisions "${REVISION}=100" \
  --quiet

echo 'Cloud Run traffic allocation:'
gcloud run services describe "$SERVICE" \
  --project "$PROJECT" --region "$REGION" \
  --format='yaml(status.traffic)'
