#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=/dev/null
source "${SCRIPT_DIR}/config.env"

gcloud config set project "${PROJECT_ID}"

if gcloud artifacts repositories describe "${AR_REPO}" --location="${REGION}" >/dev/null 2>&1; then
  echo "Artifact Registry repo ${AR_REPO} already exists"
else
  gcloud artifacts repositories create "${AR_REPO}" \
    --repository-format=docker \
    --location="${REGION}" \
    --description="AuraCode container images"
  echo "Created Artifact Registry repo ${AR_REPO}"
fi

gcloud auth configure-docker "${REGION}-docker.pkg.dev" --quiet
