#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=/dev/null
source "${SCRIPT_DIR}/config.env"

if [[ -z "${JWT_SECRET_KEY}" || -z "${OPENAI_API_KEY}" || -z "${MINIO_SECRET_KEY}" ]]; then
  echo "JWT_SECRET_KEY, OPENAI_API_KEY, and MINIO_SECRET_KEY must be set" >&2
  exit 1
fi

if az keyvault show --name "${KEY_VAULT_NAME}" --resource-group "${RESOURCE_GROUP}" >/dev/null 2>&1; then
  echo "Key Vault ${KEY_VAULT_NAME} already exists"
else
  az keyvault create \
    --resource-group "${RESOURCE_GROUP}" \
    --name "${KEY_VAULT_NAME}" \
    --location "${LOCATION}" \
    --enable-rbac-authorization false \
    --output table
fi

upsert() {
  local name="$1"
  local value="$2"
  if [[ -z "${value}" ]]; then
    echo "Skipping ${name} (empty)"
    return 0
  fi
  az keyvault secret set --vault-name "${KEY_VAULT_NAME}" --name "${name}" --value "${value}" --output none
  echo "Stored secret ${name}"
}

upsert "DB-PASSWORD" "${DB_PASSWORD}"
upsert "JWT-SECRET-KEY" "${JWT_SECRET_KEY}"
upsert "OPENAI-API-KEY" "${OPENAI_API_KEY}"
upsert "STRIPE-API-KEY" "${STRIPE_API_KEY:-}"
upsert "STRIPE-WEBHOOK-SECRET" "${STRIPE_WEBHOOK_SECRET:-}"
upsert "MINIO-ACCESS-KEY" "${MINIO_ACCESS_KEY}"
upsert "MINIO-SECRET-KEY" "${MINIO_SECRET_KEY}"
upsert "CASHFREE-APP-ID" "${CASHFREE_APP_ID:-}"
upsert "CASHFREE-SECRET-KEY" "${CASHFREE_SECRET_KEY:-}"

echo "Key Vault secrets ready: ${KEY_VAULT_NAME}"
