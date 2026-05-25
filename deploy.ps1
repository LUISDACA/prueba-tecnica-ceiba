# =============================================================================
# Script para actualizar la imagen del Azure Container App desde GHCR.
# Uso:
#   .\deploy.ps1                # usa la imagen :latest
#   .\deploy.ps1 -Tag sha-abc   # usa una imagen tageada con SHA especifico
#
# Requisitos:
#   - Azure CLI instalada y autenticada ('az login').
#   - El workflow de GitHub Actions ya empujo la imagen a GHCR.
# =============================================================================

param(
    [string]$Tag = "latest",
    [string]$ResourceGroup = "rg-prueba-tecnica",
    [string]$AppName = "prueba-tecnica-ceiba",
    [string]$Image = "ghcr.io/luisdaca/prueba-tecnica-ceiba"
)

$ErrorActionPreference = "Stop"

$fullImage = "${Image}:${Tag}"
Write-Host "Actualizando $AppName con imagen $fullImage..." -ForegroundColor Cyan

az containerapp update `
    --name $AppName `
    --resource-group $ResourceGroup `
    --image $fullImage `
    --only-show-errors `
    --output none

Write-Host "Esperando a que la nueva revision este Running..." -ForegroundColor Cyan
$timeout = 180
$elapsed = 0
do {
    Start-Sleep -Seconds 5
    $elapsed += 5
    $state = az containerapp revision list `
        --name $AppName `
        --resource-group $ResourceGroup `
        --query "[?properties.template.containers[0].image=='$fullImage'].properties.runningState | [0]" `
        -o tsv 2>$null
    Write-Host "  Estado: ${state} (${elapsed}s)"
} while ($state -ne "Running" -and $state -ne "Failed" -and $elapsed -lt $timeout)

if ($state -eq "Running") {
    $fqdn = az containerapp show `
        --name $AppName `
        --resource-group $ResourceGroup `
        --query "properties.configuration.ingress.fqdn" `
        -o tsv
    Write-Host "`nDeploy exitoso." -ForegroundColor Green
    Write-Host "URL: https://$fqdn" -ForegroundColor Green
    Write-Host "Swagger: https://$fqdn/swagger-ui/index.html" -ForegroundColor Green
} else {
    Write-Host "`nDeploy fallo o tardo demasiado. Estado final: $state" -ForegroundColor Red
    Write-Host "Revisa los logs con:" -ForegroundColor Yellow
    Write-Host "  az containerapp logs show --name $AppName --resource-group $ResourceGroup --follow"
    exit 1
}
