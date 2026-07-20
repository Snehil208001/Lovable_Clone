#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
# shellcheck source=/dev/null
source "${SCRIPT_DIR}/config.env"

CLIENT="${FRONTEND_URL:-https://example.com}"
if [[ -z "${MINIO_URL}" ]]; then
  echo "MINIO_URL must be set (run 06-deploy-minio.sh first)" >&2
  exit 1
fi

HOST="$(az postgres flexible-server show -g "${RESOURCE_GROUP}" -n "${POSTGRES_SERVER}" --query fullyQualifiedDomainName -o tsv)"
JDBC_URL="jdbc:postgresql://${HOST}:5432/${DB_NAME}?sslmode=require"
LOGIN_SERVER="$(az acr show -n "${ACR_NAME}" -g "${RESOURCE_GROUP}" --query loginServer -o tsv)"
ACR_USER="$(az acr credential show -n "${ACR_NAME}" --query username -o tsv)"
ACR_PASS="$(az acr credential show -n "${ACR_NAME}" --query passwords[0].value -o tsv)"
IMAGE="${LOGIN_SERVER}/backend:latest"

echo "Building backend image ${IMAGE}..."
az acr build \
  --registry "${ACR_NAME}" \
  --resource-group "${RESOURCE_GROUP}" \
  --image "backend:latest" \
  --file "${REPO_ROOT}/Dockerfile.backend" \
  "${REPO_ROOT}"

kv() { az keyvault secret show --vault-name "${KEY_VAULT_NAME}" --name "$1" --query value -o tsv 2>/dev/null || true; }
DB_PASSWORD_VAL="$(kv DB-PASSWORD)"; DB_PASSWORD_VAL="${DB_PASSWORD_VAL:-$DB_PASSWORD}"
JWT_VAL="$(kv JWT-SECRET-KEY)"; JWT_VAL="${JWT_VAL:-$JWT_SECRET_KEY}"
OPENAI_VAL="$(kv OPENAI-API-KEY)"; OPENAI_VAL="${OPENAI_VAL:-$OPENAI_API_KEY}"
STRIPE_VAL="$(kv STRIPE-API-KEY)"; STRIPE_VAL="${STRIPE_VAL:-${STRIPE_API_KEY:-}}"
STRIPE_WH_VAL="$(kv STRIPE-WEBHOOK-SECRET)"; STRIPE_WH_VAL="${STRIPE_WH_VAL:-${STRIPE_WEBHOOK_SECRET:-}}"
MINIO_AK="$(kv MINIO-ACCESS-KEY)"; MINIO_AK="${MINIO_AK:-$MINIO_ACCESS_KEY}"
MINIO_SK="$(kv MINIO-SECRET-KEY)"; MINIO_SK="${MINIO_SK:-$MINIO_SECRET_KEY}"
CASHFREE_APP_VAL="$(kv CASHFREE-APP-ID)"; CASHFREE_APP_VAL="${CASHFREE_APP_VAL:-${CASHFREE_APP_ID:-}}"
CASHFREE_SK_VAL="$(kv CASHFREE-SECRET-KEY)"; CASHFREE_SK_VAL="${CASHFREE_SK_VAL:-${CASHFREE_SECRET_KEY:-}}"

ENV_VARS=(
  "SPRING_PROFILES_ACTIVE=prod"
  "PORT=8080"
  "SPRING_DATASOURCE_URL=${JDBC_URL}"
  "DB_USER=${DB_USER}"
  "DB_PASSWORD=${DB_PASSWORD_VAL}"
  "JWT_SECRET_KEY=${JWT_VAL}"
  "OPENAI_API_KEY=${OPENAI_VAL}"
  "STRIPE_API_KEY=${STRIPE_VAL}"
  "STRIPE_WEBHOOK_SECRET=${STRIPE_WH_VAL}"
  "STRIPE_DEFAULT_PRICE_ID=${STRIPE_DEFAULT_PRICE_ID:-}"
  "CLIENT_URL=${CLIENT}"
  "CORS_ALLOWED_ORIGINS=${CLIENT}"
  "MINIO_URL=${MINIO_URL}"
  "MINIO_REGION=us-east-1"
  "MINIO_ACCESS_KEY=${MINIO_AK}"
  "MINIO_SECRET_KEY=${MINIO_SK}"
  "MINIO_PROJECT_BUCKET=${MINIO_PROJECT_BUCKET}"
  "CASHFREE_APP_ID=${CASHFREE_APP_VAL}"
  "CASHFREE_SECRET_KEY=${CASHFREE_SK_VAL}"
  "CASHFREE_ENV=${CASHFREE_ENV:-PRODUCTION}"
  "CASHFREE_NOTIFY_URL=${CASHFREE_NOTIFY_URL:-}"
  "CASHFREE_DEFAULT_AMOUNT_INR=${CASHFREE_DEFAULT_AMOUNT_INR:-600}"
)

if az containerapp show -g "${RESOURCE_GROUP}" -n "${BACKEND_APP}" >/dev/null 2>&1; then
  az containerapp update \
    --name "${BACKEND_APP}" \
    --resource-group "${RESOURCE_GROUP}" \
    --image "${IMAGE}" \
    --set-env-vars "${ENV_VARS[@]}" \
    --output table
else
  az containerapp create \
    --name "${BACKEND_APP}" \
    --resource-group "${RESOURCE_GROUP}" \
    --environment "${CONTAINER_ENV}" \
    --image "${IMAGE}" \
    --registry-server "${LOGIN_SERVER}" \
    --registry-username "${ACR_USER}" \
    --registry-password "${ACR_PASS}" \
    --target-port 8080 \
    --ingress external \
    --cpu 1.0 --memory 2.0Gi \
    --min-replicas 0 --max-replicas 5 \
    --env-vars "${ENV_VARS[@]}" \
    --output table
fi

FQDN="$(az containerapp show -g "${RESOURCE_GROUP}" -n "${BACKEND_APP}" --query properties.configuration.ingress.fqdn -o tsv)"
BACKEND_URL="https://${FQDN}"
echo "Backend URL: ${BACKEND_URL}"
echo "Health: ${BACKEND_URL}/actuator/health"
echo "Stripe webhook: ${BACKEND_URL}/webhooks/payment"

TMP="$(mktemp)"
grep -v '^export BACKEND_URL=' "${SCRIPT_DIR}/config.env" > "${TMP}"
echo "export BACKEND_URL=\"${BACKEND_URL}\"" >> "${TMP}"
mv "${TMP}" "${SCRIPT_DIR}/config.env"
