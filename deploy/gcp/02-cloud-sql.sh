#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=/dev/null
source "${SCRIPT_DIR}/config.env"

gcloud config set project "${PROJECT_ID}"

if [[ -z "${DB_PASSWORD}" ]]; then
  echo "DB_PASSWORD must be set in config.env" >&2
  exit 1
fi

if gcloud sql instances describe "${CLOUD_SQL_INSTANCE}" >/dev/null 2>&1; then
  echo "Cloud SQL instance ${CLOUD_SQL_INSTANCE} already exists"
else
  gcloud sql instances create "${CLOUD_SQL_INSTANCE}" \
    --database-version=POSTGRES_15 \
    --tier=db-f1-micro \
    --region="${REGION}" \
    --storage-size=20GB \
    --storage-auto-increase \
    --availability-type=ZONAL \
    --database-flags=cloudsql.iam_authentication=off
  echo "Created Cloud SQL instance ${CLOUD_SQL_INSTANCE}"
fi

if gcloud sql databases describe "${DB_NAME}" --instance="${CLOUD_SQL_INSTANCE}" >/dev/null 2>&1; then
  echo "Database ${DB_NAME} already exists"
else
  gcloud sql databases create "${DB_NAME}" --instance="${CLOUD_SQL_INSTANCE}"
fi

# Create or update DB user password
if gcloud sql users list --instance="${CLOUD_SQL_INSTANCE}" --format='value(name)' | grep -qx "${DB_USER}"; then
  gcloud sql users set-password "${DB_USER}" \
    --instance="${CLOUD_SQL_INSTANCE}" \
    --password="${DB_PASSWORD}"
else
  gcloud sql users create "${DB_USER}" \
    --instance="${CLOUD_SQL_INSTANCE}" \
    --password="${DB_PASSWORD}"
fi

CONNECTION_NAME="$(gcloud sql instances describe "${CLOUD_SQL_INSTANCE}" --format='value(connectionName)')"
echo "Cloud SQL connection name: ${CONNECTION_NAME}"
echo "Set CLOUD_SQL_INSTANCE_CONNECTION=${CONNECTION_NAME} when deploying the backend"
