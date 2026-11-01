#!/usr/bin/env bash
set -Eeuo pipefail
PROJECT="${1:?PROJECT required}"
REGION="${2:?REGION required}"
SERVICE="${3:?SERVICE required}"
REVISION="$(cat /workspace/deployed-revision.txt)"

# The status of the particular new revision, not the service's previous active revision.
# This checks Cloud Run platform readiness, not a complete API integration smoke test.
for attempt in $(seq 1 18); do
  STATUS="$(gcloud run revisions describe "$REVISION" \
    --project "$PROJECT" --region "$REGION" \
    --format='json(status.conditions)' |
    python3 -c 'import json,sys; d=json.load(sys.stdin); conditions=d.get("status",{}).get("conditions",[]); print(next((str(c.get("status","")).lower() for c in conditions if c.get("type")=="Ready"),"unknown"))')"
  echo "Revision $REVISION readiness attempt $attempt: $STATUS"
  if [[ "$STATUS" == true ]]; then
    echo 'Revision is Ready; platform readiness gate passed.'
    exit 0
  fi
  if [[ "$STATUS" == false ]]; then
    echo 'Revision is not ready yet; checking again.'
  fi
  sleep 10
done

echo "ERROR: New revision $REVISION did not become Ready. Existing traffic was not promoted." >&2
exit 1
