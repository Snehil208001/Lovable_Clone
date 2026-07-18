#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=/dev/null
source "${SCRIPT_DIR}/config.env"

gcloud config set project "${PROJECT_ID}"

if [[ -z "${BACKEND_URL}" ]]; then
  BACKEND_URL="$(gcloud run services describe "${BACKEND_SERVICE}" --region="${REGION}" --format='value(status.url)' 2>/dev/null || true)"
fi
if [[ -z "${BACKEND_URL}" ]]; then
  echo "BACKEND_URL is required (deploy backend first or set in config.env)" >&2
  exit 1
fi

IMAGE="${REGION}-docker.pkg.dev/${PROJECT_ID}/${AR_REPO}/frontend:latest"

echo "Building ${IMAGE} with NEXT_PUBLIC_API_BASE_URL=${BACKEND_URL}..."
gcloud builds submit \
  --config=- \
  --substitutions=_IMAGE="${IMAGE}",_API_URL="${BACKEND_URL}" \
  <<'EOF'
steps:
  - name: gcr.io/cloud-builders/docker
    args:
      - "build"
      - "-f"
      - "frontend/Dockerfile"
      - "--build-arg"
      - "NEXT_PUBLIC_API_BASE_URL=${_API_URL}"
      - "-t"
      - "${_IMAGE}"
      - "frontend"
images:
  - ${_IMAGE}
EOF

echo "Deploying Cloud Run service ${FRONTEND_SERVICE}..."
gcloud run deploy "${FRONTEND_SERVICE}" \
  --image="${IMAGE}" \
  --region="${REGION}" \
  --platform=managed \
  --allow-unauthenticated \
  --port=8080 \
  --cpu=1 \
  --memory=512Mi \
  --min-instances=0 \
  --max-instances=10 \
  --timeout=300

FRONTEND_URL="$(gcloud run services describe "${FRONTEND_SERVICE}" --region="${REGION}" --format='value(status.url)')"
echo "Frontend URL: ${FRONTEND_URL}"
echo "Re-run 05-deploy-backend.sh with CLIENT_URL=${FRONTEND_URL} so CORS/Stripe redirects match."
