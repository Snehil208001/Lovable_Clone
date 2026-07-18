#!/usr/bin/env bash
# One-time GCP bootstrap + first deploy. Requires gcloud authenticated with Owner/Editor.
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

if [[ ! -f "${SCRIPT_DIR}/config.env" ]]; then
  echo "Copy config.env.example → config.env and fill in values first." >&2
  exit 1
fi

"${SCRIPT_DIR}/00-enable-apis.sh"
"${SCRIPT_DIR}/01-artifact-registry.sh"
"${SCRIPT_DIR}/02-cloud-sql.sh"
"${SCRIPT_DIR}/03-gcs-bucket.sh"
"${SCRIPT_DIR}/04-secrets.sh"

echo ""
echo "Bootstrap complete. Next:"
echo "  1. Create GCS HMAC keys and put MINIO_* in config.env, then re-run 04-secrets.sh"
echo "  2. Set FRONTEND_URL to a temporary value (e.g. https://example.com) and run 05-deploy-backend.sh"
echo "  3. Put BACKEND_URL from step 2 into config.env and run 06-deploy-frontend.sh"
echo "  4. Re-run 05-deploy-backend.sh with CLIENT_URL / FRONTEND_URL = real frontend URL"
echo "  5. Point Stripe webhook to https://<backend>/webhooks/payment"
