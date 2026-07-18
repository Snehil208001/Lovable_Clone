#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=/dev/null
source "${SCRIPT_DIR}/config.env"

# Log Analytics workspace for Container Apps
LAW="auracode-logs"
if ! az monitor log-analytics workspace show -g "${RESOURCE_GROUP}" -n "${LAW}" >/dev/null 2>&1; then
  az monitor log-analytics workspace create \
    --resource-group "${RESOURCE_GROUP}" \
    --workspace-name "${LAW}" \
    --location "${LOCATION}" \
    --output table
fi
LAW_ID="$(az monitor log-analytics workspace show -g "${RESOURCE_GROUP}" -n "${LAW}" --query customerId -o tsv)"
LAW_KEY="$(az monitor log-analytics workspace get-shared-keys -g "${RESOURCE_GROUP}" -n "${LAW}" --query primarySharedKey -o tsv)"

if az containerapp env show -g "${RESOURCE_GROUP}" -n "${CONTAINER_ENV}" >/dev/null 2>&1; then
  echo "Container Apps environment ${CONTAINER_ENV} already exists"
else
  az containerapp env create \
    --resource-group "${RESOURCE_GROUP}" \
    --name "${CONTAINER_ENV}" \
    --location "${LOCATION}" \
    --logs-workspace-id "${LAW_ID}" \
    --logs-workspace-key "${LAW_KEY}" \
    --output table
fi

echo "Container Apps environment ready: ${CONTAINER_ENV}"
