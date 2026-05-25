# Despliegue en Azure Container Apps

La app esta desplegada en **Azure Container Apps** y disponible en:

> **https://prueba-tecnica-ceiba.graydune-89367257.centralus.azurecontainerapps.io**
>
> Swagger UI: `/swagger-ui/index.html`
>
> Endpoints `/api/**` requieren el header `X-API-KEY` (la clave se configuro
> como secreto en el Container App, no se publica en este repo).

---

## Como funciona el flujo

```
git push origin main
       │
       ▼
GitHub Actions (.github/workflows/azure-deploy.yml)
       │
       ├── Build de imagen Docker (multi-stage)
       └── Push a GHCR (ghcr.io/luisdaca/prueba-tecnica-ceiba:latest)
       │
       ▼
[manual]  .\deploy.ps1   (desde mi maquina local)
       │
       └── az containerapp update --image ...
             │
             ▼
Azure Container Apps actualiza la revision y enruta el trafico
```

### Por que el ultimo paso es manual

La cuenta `@campusucc.edu.co` (Azure for Students en el tenant de mi
universidad) **no tiene permisos para registrar aplicaciones en Microsoft
Entra ID**, lo que impide crear Service Principals para autenticar GitHub
Actions contra Azure por OIDC.

Si en un futuro el admin del tenant me habilita ese permiso, el workflow se
puede extender en pocas lineas con `azure/login@v2` + `azure/container-apps-deploy-action@v2`
y el deploy queda 100% automatico.

Como compensacion, deje un script `deploy.ps1` que ejecuta el update con un
solo comando local — el flujo termina siendo:

1. `git push` → GitHub Actions construye y publica la imagen automaticamente.
2. `.\deploy.ps1` → Azure usa la nueva imagen.

---

## Recursos creados en Azure

| Recurso | Nombre | Notas |
|---|---|---|
| Resource Group | `rg-prueba-tecnica` | Region: Central US (unica permitida por Azure for Students en mi caso) |
| Container Apps Environment | `env-prueba-tecnica` | El "namespace" logico donde vive la app |
| Container App | `prueba-tecnica-ceiba` | 0–2 replicas, 0.5 vCPU, 1 GiB de memoria |
| Secret (en el Container App) | `api-key` | Inyectado al contenedor como variable de entorno `API_KEY` |

### Por que Container Apps en lugar de App Service

- Free tier real (180.000 vCPU-segundos/mes gratis).
- Escalado a cero automatico: si nadie usa la app por minutos, no consume.
- Mas moderno (corre sobre Kubernetes managed por Microsoft).

---

## Comandos que use para crear los recursos

```bash
# 1. Login
az login

# 2. Variables
RG="rg-prueba-tecnica"
LOCATION="centralus"
ENV="env-prueba-tecnica"
APP="prueba-tecnica-ceiba"
API_KEY_VALUE="<clave-segura-generada>"

# 3. Asegurar extensiones / providers
az extension add --name containerapp --upgrade
az provider register --namespace Microsoft.App
az provider register --namespace Microsoft.OperationalInsights

# 4. Resource Group y Environment
az group create --name $RG --location $LOCATION
az containerapp env create \
    --name $ENV \
    --resource-group $RG \
    --location $LOCATION

# 5. Container App (con imagen placeholder inicial)
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
    --secrets "api-key=$API_KEY_VALUE" \
    --env-vars "API_KEY=secretref:api-key"

# 6. Primer despliegue manual (apuntar a la imagen real en GHCR)
az containerapp update \
    --name $APP \
    --resource-group $RG \
    --image ghcr.io/luisdaca/prueba-tecnica-ceiba:latest
```

---

## Re-desplegar tras un cambio en el codigo

```powershell
# 1. Hacer el cambio, commit y push
git add .
git commit -m "<descripcion del cambio>"
git push

# 2. Esperar a que el workflow termine (verlo en github.com/.../actions)
#    Cuando aparezca "Imagen publicada en ghcr.io/...:latest" la imagen ya
#    esta en el registry.

# 3. Actualizar el Container App con la nueva imagen
.\deploy.ps1
```

El script `deploy.ps1` espera a que la nueva revision este `Running` y
imprime la URL publica al final.

---

## Operaciones utiles

```bash
# URL publica
az containerapp show --name prueba-tecnica-ceiba --resource-group rg-prueba-tecnica \
    --query "properties.configuration.ingress.fqdn" -o tsv

# Stream de logs
az containerapp logs show --name prueba-tecnica-ceiba --resource-group rg-prueba-tecnica --follow

# Listar revisiones (cada deploy crea una)
az containerapp revision list --name prueba-tecnica-ceiba --resource-group rg-prueba-tecnica -o table

# Rotar el secreto api-key
az containerapp secret set --name prueba-tecnica-ceiba --resource-group rg-prueba-tecnica \
    --secrets "api-key=<nueva-clave>"
az containerapp update --name prueba-tecnica-ceiba --resource-group rg-prueba-tecnica
```

---

## Limpiar todo (cuando ya no haga falta)

```bash
az group delete --name rg-prueba-tecnica --yes --no-wait
```

Elimina todos los recursos creados de una sola vez.
