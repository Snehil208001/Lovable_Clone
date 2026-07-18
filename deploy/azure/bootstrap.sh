#!/usr/bin/env bash
# One-time Azure bootstrap (infra). Deploy apps with 06–08 after this.
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

if [[ ! -f "${SCRIPT_DIR}/config.env" ]]; then
  echo "Copy config.env.example → config.env and fill in values first." >&2
  exit 1
fi

"${SCRIPT_DIR}/00-login.sh"
"${SCRIPT_DIR}/01-resource-group.sh"
"${SCRIPT_DIR}/02-acr.sh"
"${SCRIPT_DIR}/03-postgres.sh"
"${SCRIPT_DIR}/04-keyvault.sh"
"${SCRIPT_DIR}/05-container-env.sh"

echo ""
echo "Bootstrap complete. Next:"
echo "  bash deploy/azure/06-deploy-minio.sh"
echo "  bash deploy/azure/07-deploy-backend.sh   # FRONTEND_URL can be temporary"
echo "  bash deploy/azure/08-deploy-frontend.sh"
echo "  # set FRONTEND_URL in config.env, then re-run 07-deploy-backend.sh"
echo "  # Stripe webhook → https://<backend>/webhooks/payment"
