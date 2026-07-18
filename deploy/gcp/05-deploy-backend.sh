#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=/dev/null
source "${SCRIPT_DIR}/config.env"

gcloud config set project "${PROJECT_ID}"

IMAGE="${REGION}-docker.pkg.dev/${PROJECT_ID}/${AR_REPO}/backend:latest"
CONNECTION_NAME="$(gcloud sql instances describe "${CLOUD_SQL_INSTANCE}" --format='value(connectionName)')"
JDBC_URL="jdbc:postgresql:///${DB_NAME}?cloudSqlInstance=${CONNECTION_NAME}&socketFactory=com.google.cloud.sql.postgres.SocketFactory"

CLIENT="${CLIENT_URL:-${FRONTEND_URL}}"
if [[ -z "${CLIENT}" ]]; then
  echo "Set FRONTEND_URL or CLIENT_URL in config.env (frontend Cloud Run URL)" >&2
  exit 1
fi

echo "Building ${IMAGE}..."
gcloud builds submit \
  --config=- \
  --substitutions=_IMAGE="${IMAGE}" \
  <<'EOF'
steps:
  - name: gcr.io/cloud-builders/docker
    args: ["build", "-f", "Dockerfile.backend", "-t", "${_IMAGE}", "."]
images:
  - ${_IMAGE}
EOF

# Allow Cloud Run runtime SA to connect to Cloud SQL
PROJECT_NUMBER="$(gcloud projects describe "${PROJECT_ID}" --format='value(projectNumber)')"
RUNTIME_SA="${PROJECT_NUMBER}-compute@developer.gserviceaccount.com"
gcloud projects add-iam-policy-binding "${PROJECT_ID}" \
  --member="serviceAccount:${RUNTIME_SA}" \
  --role="roles/cloudsql.client" \
  --quiet >/dev/null || true

echo "Deploying Cloud Run service ${BACKEND_SERVICE}..."
gcloud run deploy "${BACKEND_SERVICE}" \
  --image="${IMAGE}" \
  --region="${REGION}" \
  --platform=managed \
  --allow-unauthenticated \
  --port=8080 \
  --cpu=1 \
  --memory=1Gi \
  --min-instances=0 \
  --max-instances=10 \
  --timeout=3600 \
  --cpu-boost \
  --no-cpu-throttling \
  --add-cloudsql-instances="${CONNECTION_NAME}" \
  --set-env-vars="SPRING_PROFILES_ACTIVE=prod,SPRING_DATASOURCE_URL=${JDBC_URL},DB_USER=${DB_USER},CLIENT_URL=${CLIENT},CORS_ALLOWED_ORIGINS=${CLIENT},MINIO_URL=https://storage.googleapis.com,MINIO_REGION=${MINIO_REGION:-us-east1},MINIO_PROJECT_BUCKET=${GCS_BUCKET},STRIPE_DEFAULT_PRICE_ID=${STRIPE_DEFAULT_PRICE_ID:-}" \
  --set-secrets="DB_PASSWORD=DB_PASSWORD:latest,JWT_SECRET_KEY=JWT_SECRET_KEY:latest,OPENAI_API_KEY=OPENAI_API_KEY:latest,STRIPE_API_KEY=STRIPE_API_KEY:latest,STRIPE_WEBHOOK_SECRET=STRIPE_WEBHOOK_SECRET:latest,MINIO_ACCESS_KEY=MINIO_ACCESS_KEY:latest,MINIO_SECRET_KEY=MINIO_SECRET_KEY:latest"

BACKEND_URL="$(gcloud run services describe "${BACKEND_SERVICE}" --region="${REGION}" --format='value(status.url)')"
echo "Backend URL: ${BACKEND_URL}"
echo "Stripe webhook endpoint: ${BACKEND_URL}/webhooks/payment"
echo "Health: ${BACKEND_URL}/actuator/health"
echo "Update FRONTEND_URL / NEXT_PUBLIC_API_BASE_URL, then run 06-deploy-frontend.sh"
echo "BACKEND_URL=${BACKEND_URL}" >> "${SCRIPT_DIR}/config.env.generated" || true
