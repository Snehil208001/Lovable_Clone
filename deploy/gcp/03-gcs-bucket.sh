#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=/dev/null
source "${SCRIPT_DIR}/config.env"

gcloud config set project "${PROJECT_ID}"

if gcloud storage buckets describe "gs://${GCS_BUCKET}" >/dev/null 2>&1; then
  echo "Bucket gs://${GCS_BUCKET} already exists"
else
  gcloud storage buckets create "gs://${GCS_BUCKET}" \
    --location="${REGION}" \
    --uniform-bucket-level-access
  echo "Created bucket gs://${GCS_BUCKET}"
fi

echo ""
echo "Create HMAC keys for S3 interoperability (MinIO client):"
echo "  Cloud Console → Cloud Storage → Settings → Interoperability → Create a key"
echo "  Or:"
echo "  gcloud storage hmac create \$(gcloud config get-value account) --project=${PROJECT_ID}"
echo ""
echo "Put access_id → MINIO_ACCESS_KEY and secret → MINIO_SECRET_KEY in config.env"
echo "MINIO_PROJECT_BUCKET should be: ${GCS_BUCKET}"
