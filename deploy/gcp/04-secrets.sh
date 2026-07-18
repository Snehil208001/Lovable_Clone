#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=/dev/null
source "${SCRIPT_DIR}/config.env"

gcloud config set project "${PROJECT_ID}"

upsert_secret() {
  local name="$1"
  local value="$2"
  if [[ -z "${value}" ]]; then
    echo "Skipping ${name} (empty value)"
    return 0
  fi
  if gcloud secrets describe "${name}" >/dev/null 2>&1; then
    printf '%s' "${value}" | gcloud secrets versions add "${name}" --data-file=-
    echo "Updated secret ${name}"
  else
    printf '%s' "${value}" | gcloud secrets create "${name}" --data-file=- --replication-policy=automatic
    echo "Created secret ${name}"
  fi
}

upsert_secret "DB_PASSWORD" "${DB_PASSWORD}"
upsert_secret "JWT_SECRET_KEY" "${JWT_SECRET_KEY}"
upsert_secret "OPENAI_API_KEY" "${OPENAI_API_KEY}"
upsert_secret "STRIPE_API_KEY" "${STRIPE_API_KEY}"
upsert_secret "STRIPE_WEBHOOK_SECRET" "${STRIPE_WEBHOOK_SECRET}"
upsert_secret "MINIO_ACCESS_KEY" "${MINIO_ACCESS_KEY}"
upsert_secret "MINIO_SECRET_KEY" "${MINIO_SECRET_KEY}"

PROJECT_NUMBER="$(gcloud projects describe "${PROJECT_ID}" --format='value(projectNumber)')"
RUNTIME_SA="${PROJECT_NUMBER}-compute@developer.gserviceaccount.com"

for secret in DB_PASSWORD JWT_SECRET_KEY OPENAI_API_KEY STRIPE_API_KEY STRIPE_WEBHOOK_SECRET MINIO_ACCESS_KEY MINIO_SECRET_KEY; do
  if gcloud secrets describe "${secret}" >/dev/null 2>&1; then
    gcloud secrets add-iam-policy-binding "${secret}" \
      --member="serviceAccount:${RUNTIME_SA}" \
      --role="roles/secretmanager.secretAccessor" \
      --quiet >/dev/null
  fi
done

echo "Granted secretAccessor to ${RUNTIME_SA}"
