#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
# shellcheck source=/dev/null
source "${SCRIPT_DIR}/config.env"

if [[ -z "${BACKEND_URL}" ]]; then
  FQDN="$(az containerapp show -g "${RESOURCE_GROUP}" -n "${BACKEND_APP}" --query properties.configuration.ingress.fqdn -o tsv 2>/dev/null || true)"
  if [[ -n "${FQDN}" ]]; then
    BACKEND_URL="https://${FQDN}"
  fi
fi
if [[ -z "${BACKEND_URL}" ]]; then
  echo "BACKEND_URL is required (deploy backend first)" >&2
  exit 1
fi

LOGIN_SERVER="$(az acr show -n "${ACR_NAME}" -g "${RESOURCE_GROUP}" --query loginServer -o tsv)"
ACR_USER="$(az acr credential show -n "${ACR_NAME}" --query username -o tsv)"
ACR_PASS="$(az acr credential show -n "${ACR_NAME}" --query passwords[0].value -o tsv)"
IMAGE="${LOGIN_SERVER}/frontend:latest"

echo "Building frontend with NEXT_PUBLIC_API_BASE_URL=${BACKEND_URL}..."
az acr build \
  --registry "${ACR_NAME}" \
  --resource-group "${RESOURCE_GROUP}" \
  --image "frontend:latest" \
  --file "${REPO_ROOT}/frontend/Dockerfile" \
  --build-arg "NEXT_PUBLIC_API_BASE_URL=${BACKEND_URL}" \
  --no-logs \
  "${REPO_ROOT}/frontend"

if az containerapp show -g "${RESOURCE_GROUP}" -n "${FRONTEND_APP}" >/dev/null 2>&1; then
  az containerapp update \
    --name "${FRONTEND_APP}" \
    --resource-group "${RESOURCE_GROUP}" \
    --image "${IMAGE}" \
    --output table
else
  az containerapp create \
    --name "${FRONTEND_APP}" \
    --resource-group "${RESOURCE_GROUP}" \
    --environment "${CONTAINER_ENV}" \
    --image "${IMAGE}" \
    --registry-server "${LOGIN_SERVER}" \
    --registry-username "${ACR_USER}" \
    --registry-password "${ACR_PASS}" \
    --target-port 8080 \
    --ingress external \
    --cpu 0.5 --memory 1.0Gi \
    --min-replicas 0 --max-replicas 5 \
    --env-vars "PORT=8080" "NODE_ENV=production" \
    --output table
fi

FQDN="$(az containerapp show -g "${RESOURCE_GROUP}" -n "${FRONTEND_APP}" --query properties.configuration.ingress.fqdn -o tsv)"
FRONTEND_URL="https://${FQDN}"
echo "Frontend URL: ${FRONTEND_URL}"
echo "Re-run 07-deploy-backend.sh with FRONTEND_URL set so CORS/Stripe redirects match."

TMP="$(mktemp)"
grep -v '^export FRONTEND_URL=' "${SCRIPT_DIR}/config.env" > "${TMP}"
echo "export FRONTEND_URL=\"${FRONTEND_URL}\"" >> "${TMP}"
mv "${TMP}" "${SCRIPT_DIR}/config.env"
