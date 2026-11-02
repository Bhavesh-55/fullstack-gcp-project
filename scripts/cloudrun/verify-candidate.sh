
#!/usr/bin/env bash
set -Eeuo pipefail

if [[ $# -ne 3 ]]; then
  echo "Usage: verify-candidate.sh PROJECT REGION SERVICE" >&2
  exit 2
fi

PROJECT="$1"
REGION="$2"
SERVICE="$3"

REVISION_FILE="/workspace/deployed-revision.txt"
TAG_FILE="/workspace/deployed-tag.txt"

if [[ ! -s "$REVISION_FILE" || ! -s "$TAG_FILE" ]]; then
  echo "ERROR: Deployment metadata files are missing." >&2
  exit 3
fi

REVISION="$(cat "$REVISION_FILE")"
TAG="$(cat "$TAG_FILE")"

echo "Verifying candidate revision: $REVISION"
echo "Candidate tag: $TAG"

# Retry until the revision is ready.
for attempt in $(seq 1 20); do

  READY_STATUS="$(gcloud run revisions describe "$REVISION" \
    --project="$PROJECT" \
    --region="$REGION" \
    --format="value(status.conditions[0].status)")"

  echo "Attempt $attempt: revision status = $READY_STATUS"

  if [[ "$READY_STATUS" == "True" ]]; then
    echo "Candidate revision is ready."
    exit 0
  fi

  sleep 10
done

echo "ERROR: Candidate revision did not become ready." >&2
exit 1