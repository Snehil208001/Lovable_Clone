#!/usr/bin/env bash
# MinIO on Container Apps — S3 API for the existing Java MinIO client (Azure Blob is not S3-native).
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=/dev/null
source "${SCRIPT_DIR}/config.env"

if [[ -z "${MINIO_SECRET_KEY}" ]]; then
  echo "MINIO_SECRET_KEY must be set" >&2
  exit 1
fi

az containerapp create \
  --name "${MINIO_APP}" \
  --resource-group "${RESOURCE_GROUP}" \
  --environment "${CONTAINER_ENV}" \
  --image "minio/minio:latest" \
  --target-port 9000 \
  --ingress external \
  --cpu 0.5 --memory 1.0Gi \
  --min-replicas 1 --max-replicas 1 \
  --command '["minio", "server", "/data", "--console-address", ":9001"]' \
  --env-vars \
    "MINIO_ROOT_USER=${MINIO_ACCESS_KEY}" \
    "MINIO_ROOT_PASSWORD=${MINIO_SECRET_KEY}" \
  --output table \
  2>/dev/null || az containerapp update \
  --name "${MINIO_APP}" \
  --resource-group "${RESOURCE_GROUP}" \
  --image "minio/minio:latest" \
  --set-env-vars \
    "MINIO_ROOT_USER=${MINIO_ACCESS_KEY}" \
    "MINIO_ROOT_PASSWORD=${MINIO_SECRET_KEY}" \
  --output table

FQDN="$(az containerapp show -g "${RESOURCE_GROUP}" -n "${MINIO_APP}" --query properties.configuration.ingress.fqdn -o tsv)"
MINIO_URL="https://${FQDN}"
echo "MinIO S3 API URL: ${MINIO_URL}"
echo "Add to config.env: MINIO_URL=${MINIO_URL}"
echo "Create bucket '${MINIO_PROJECT_BUCKET}' via MinIO console or mc client after first start."
# Persist into generated file for later scripts
grep -v '^export MINIO_URL=' "${SCRIPT_DIR}/config.env" > "${SCRIPT_DIR}/config.env.tmp" || cp "${SCRIPT_DIR}/config.env" "${SCRIPT_DIR}/config.env.tmp"
echo "export MINIO_URL=\"${MINIO_URL}\"" >> "${SCRIPT_DIR}/config.env.tmp"
mv "${SCRIPT_DIR}/config.env.tmp" "${SCRIPT_DIR}/config.env"
