# Escribe en el .env las IP de la prueba en varias computadoras (IP_ESTA_MAQUINA, IP_EUREKA, IP_POSTGRES, IP_GATEWAY e IP_WEB).
# Detecta sola la IP de esta máquina (la red activa que tiene puerta de enlace, wifi o hotspot) y deja el resto del .env como estaba.
#   En la máquina A (la que tiene Eureka, la base y el gateway):   .\infra\ajustar-ip.ps1
#   En las demás máquinas, con la IP de la A:                       .\infra\ajustar-ip.ps1 -IpA 192.168.0.229
param([string]$IpA = "")
$ErrorActionPreference = "Stop"

$raiz = Split-Path -Parent $PSScriptRoot
$archivo = Join-Path $raiz ".env"

$red = Get-NetIPConfiguration | Where-Object { $_.IPv4DefaultGateway -ne $null -and $_.NetAdapter.Status -eq "Up" } | Select-Object -First 1
if (-not $red) { throw "No hay una red activa. Conectate al wifi o al hotspot y vuelve a intentar." }
$mia = $red.IPv4Address.IPAddress
if ($IpA -eq "") { $IpA = $mia }

$nuevas = [ordered]@{ IP_ESTA_MAQUINA = $mia; IP_EUREKA = $IpA; IP_POSTGRES = $IpA; IP_GATEWAY = $IpA; IP_WEB = $IpA }
$lineas = @()
if (Test-Path $archivo) {
    $lineas = @(Get-Content $archivo -Encoding UTF8 | Where-Object { $_ -notmatch '^(IP_ESTA_MAQUINA|IP_EUREKA|IP_POSTGRES|IP_GATEWAY|IP_WEB)=' })
}
foreach ($clave in $nuevas.Keys) { $lineas += "$clave=$($nuevas[$clave])" }
# ASCII y sin BOM: con BOM docker compose no reconoce la primera variable.
Set-Content -Path $archivo -Value $lineas -Encoding ascii

Write-Host "Red usada: $($red.InterfaceAlias)"
Write-Host "IP de esta maquina:  $mia"
Write-Host "IP de la maquina A:  $IpA"
