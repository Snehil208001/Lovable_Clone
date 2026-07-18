#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=/dev/null
source "${SCRIPT_DIR}/config.env"

az group create --name "${RESOURCE_GROUP}" --location "${LOCATION}" --output table
echo "Resource group ${RESOURCE_GROUP} ready in ${LOCATION}"
