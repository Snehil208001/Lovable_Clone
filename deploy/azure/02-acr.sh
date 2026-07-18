#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=/dev/null
source "${SCRIPT_DIR}/config.env"

if az acr show --name "${ACR_NAME}" --resource-group "${RESOURCE_GROUP}" >/dev/null 2>&1; then
  echo "ACR ${ACR_NAME} already exists"
else
  az acr create \
    --resource-group "${RESOURCE_GROUP}" \
    --name "${ACR_NAME}" \
    --sku Basic \
    --admin-enabled true \
    --output table
fi

az acr login --name "${ACR_NAME}"
echo "ACR login server: $(az acr show -n "${ACR_NAME}" -g "${RESOURCE_GROUP}" --query loginServer -o tsv)"
