# Azure deploy (AuraCode)

Primary production target: **Azure Container Apps** + **PostgreSQL Flexible Server** + **MinIO** (S3 API) + **Key Vault** + **ACR**.

See the root [README.md](../../README.md) **Production on Azure** section for the full walkthrough (CLI and Portal).

| Script | Purpose |
|---|---|
| `config.env.example` | Copy → `config.env` (gitignored) |
| `00-login.sh` | Select subscription |
| `01-resource-group.sh` | Resource group |
| `02-acr.sh` | Azure Container Registry |
| `03-postgres.sh` | Postgres Flexible Server 15 |
| `04-keyvault.sh` | Secrets |
| `05-container-env.sh` | Container Apps environment |
| `06-deploy-minio.sh` | MinIO (S3 for project files) |
| `07-deploy-backend.sh` | Build + deploy API |
| `08-deploy-frontend.sh` | Build + deploy Next.js |
| `bootstrap.sh` | Runs 00–05 |

Requires [Azure CLI](https://learn.microsoft.com/cli/azure/install-azure-cli) (`az login`).
