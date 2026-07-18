#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=/dev/null
source "${SCRIPT_DIR}/config.env"

if [[ -z "${DB_PASSWORD}" ]]; then
  echo "DB_PASSWORD must be set in config.env" >&2
  exit 1
fi

if az postgres flexible-server show --resource-group "${RESOURCE_GROUP}" --name "${POSTGRES_SERVER}" >/dev/null 2>&1; then
  echo "Postgres server ${POSTGRES_SERVER} already exists"
else
  az postgres flexible-server create \
    --resource-group "${RESOURCE_GROUP}" \
    --name "${POSTGRES_SERVER}" \
    --location "${LOCATION}" \
    --admin-user "${DB_USER}" \
    --admin-password "${DB_PASSWORD}" \
    --sku-name Standard_B1ms \
    --tier Burstable \
    --storage-size 32 \
    --version 15 \
    --public-access 0.0.0.0-255.255.255.255 \
    --yes
fi

# Database (ignore if exists)
az postgres flexible-server db create \
  --resource-group "${RESOURCE_GROUP}" \
  --server-name "${POSTGRES_SERVER}" \
  --name "${DB_NAME}" \
  2>/dev/null || true

HOST="$(az postgres flexible-server show -g "${RESOURCE_GROUP}" -n "${POSTGRES_SERVER}" --query fullyQualifiedDomainName -o tsv)"
echo "Postgres host: ${HOST}"
echo "JDBC URL: jdbc:postgresql://${HOST}:5432/${DB_NAME}?sslmode=require"
echo "Note: public access is open for Container Apps simplicity; tighten firewall rules for production."
