# Guía de despliegue a Azure Container Apps

Este documento describe paso a paso cómo desplegar la API en Azure Container
Apps usando GitHub Actions + GitHub Container Registry (GHCR), con
autenticación moderna por OIDC (Federated Credentials).

> 📌 El workflow ya está creado en `.github/workflows/azure-deploy.yml`. Solo
> falta crear los recursos en Azure y configurar los secrets/variables en
> GitHub.

---

## 1. Pre-requisitos

- Cuenta de Azure activa (Free Trial, Azure for Students o pagada).
- Repositorio publicado en GitHub.
- Azure CLI instalada localmente (`winget install Microsoft.AzureCLI` en
  Windows). Alternativamente todo se puede hacer desde el portal web, pero
  con CLI es más rápido y reproducible.

---

## 2. Variables que usaré en los comandos

Ajusta los nombres si quieres. El **nombre del Container App debe ser único en
tu suscripción**.

```bash
RG="rg-prueba-tecnica"
LOCATION="eastus"
ENV="env-prueba-tecnica"
APP="prueba-tecnica-java"
SP_NAME="sp-github-deploy-prueba-tecnica"
GITHUB_REPO="<tu-usuario>/<tu-repo>"
```

---

## 3. Login en Azure

```bash
az login
```

Si tienes varias suscripciones:

```bash
az account list -o table
az account set --subscription "<id-o-nombre-suscripcion>"
```

---

## 4. Crear los recursos de Azure

### 4.1 Resource Group

```bash
az group create --name $RG --location $LOCATION
```

### 4.2 Container Apps Environment

```bash
az containerapp env create \
    --name $ENV \
    --resource-group $RG \
    --location $LOCATION
```

> Tarda ~2 minutos. Es un namespace lógico donde viven las Container Apps.

### 4.3 La Container App (con imagen placeholder por ahora)

```bash
az containerapp create \
    --name $APP \
    --resource-group $RG \
    --environment $ENV \
    --image mcr.microsoft.com/azuredocs/containerapps-helloworld:latest \
    --target-port 8080 \
    --ingress external \
    --min-replicas 0 \
    --max-replicas 2 \
    --cpu 0.5 \
    --memory 1Gi \
    --env-vars "API_KEY=secretref:api-key" \
    --secrets "api-key=<elige-una-api-key-segura>"
```

Reemplaza `<elige-una-api-key-segura>` por una clave fuerte (ej. salida de
`openssl rand -base64 32`).

> 💡 `min-replicas=0` hace que la app escale a cero cuando no hay tráfico —
> es lo que mantiene el costo en $0 dentro del free tier.

---

## 5. Configurar autenticación GitHub Actions → Azure (OIDC)

Esto crea un **Service Principal** que solo puede ser invocado desde tu
workflow específico, sin contraseñas. Es la práctica moderna recomendada por
Microsoft.

### 5.1 Crear el Service Principal

```bash
SUBSCRIPTION_ID=$(az account show --query id -o tsv)

az ad sp create-for-rbac \
    --name $SP_NAME \
    --role contributor \
    --scopes /subscriptions/$SUBSCRIPTION_ID/resourceGroups/$RG \
    --json-auth
```

Copia los valores `clientId`, `tenantId` y `subscriptionId` — los necesitarás
en GitHub.

### 5.2 Configurar Federated Credentials

Reemplaza `<APP_ID>` por el `clientId` de arriba y `<GITHUB_REPO>` por
`usuario/repo`:

```bash
APP_ID="<APP_ID>"

az ad app federated-credential create \
    --id $APP_ID \
    --parameters '{
      "name": "github-actions-main",
      "issuer": "https://token.actions.githubusercontent.com",
      "subject": "repo:'$GITHUB_REPO':ref:refs/heads/main",
      "audiences": ["api://AzureADTokenExchange"]
    }'
```

Esto autoriza al workflow corriendo en la rama `main` del repo a obtener
tokens de Azure sin contraseñas.

---

## 6. Configurar los secrets y variables en GitHub

En GitHub, ve a tu repositorio → **Settings → Secrets and variables →
Actions**.

### Secrets (pestaña "Secrets")

| Nombre | Valor |
|---|---|
| `AZURE_CLIENT_ID` | `clientId` del Service Principal |
| `AZURE_TENANT_ID` | `tenantId` |
| `AZURE_SUBSCRIPTION_ID` | `subscriptionId` |

### Variables (pestaña "Variables")

| Nombre | Valor |
|---|---|
| `AZURE_RESOURCE_GROUP` | El valor de `$RG` (ej. `rg-prueba-tecnica`) |
| `AZURE_CONTAINER_APP` | El valor de `$APP` (ej. `prueba-tecnica-java`) |

---

## 7. Hacer público el paquete de GHCR (opcional pero recomendado)

Por defecto las imágenes pusheadas a GHCR son privadas. Para que Container
Apps pueda hacer pull sin credenciales, hazlo público:

1. Tras el primer push exitoso desde GitHub Actions, ve a tu perfil de GitHub
   → **Packages**.
2. Click en `prueba-tecnica-java`.
3. **Package settings** (botón derecho) → **Change visibility** → **Public**.

> Si prefieres mantener la imagen privada, configura un Pull Secret en
> Container Apps con `az containerapp registry set --identity system ...` —
> escapa al alcance de esta guía pero está documentado en Microsoft Learn.

---

## 8. Disparar el primer despliegue

```bash
git push origin main
```

GitHub Actions ejecutará el workflow `azure-deploy.yml`:
- Construirá la imagen Docker.
- La subirá a GHCR.
- Actualizará la Container App.

Ve a la pestaña **Actions** del repo para seguir el progreso (~3-5 min).

---

## 9. Obtener la URL pública

```bash
az containerapp show \
    --name $APP \
    --resource-group $RG \
    --query "properties.configuration.ingress.fqdn" \
    -o tsv
```

Te dará algo como `prueba-tecnica-java.kindrock-12345.eastus.azurecontainerapps.io`.

Pruébalo:

```bash
URL="https://<fqdn-obtenido-arriba>"
curl $URL/v3/api-docs
curl -H "X-API-KEY: <tu-api-key>" $URL/api/v1/bicicletas
```

Y accede a Swagger UI:

```
https://<fqdn>/swagger-ui/index.html
```

---

## 10. Monitoreo y logs

```bash
# Stream de logs en vivo
az containerapp logs show --name $APP --resource-group $RG --follow

# Estado actual
az containerapp show --name $APP --resource-group $RG -o table
```

---

## 11. Limpiar todo (cuando ya no necesites el despliegue)

```bash
az group delete --name $RG --yes --no-wait
```

Esto elimina todos los recursos creados.
