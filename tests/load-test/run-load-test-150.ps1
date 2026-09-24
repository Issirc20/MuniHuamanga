# ==============================================================================
# Script de Pruebas de Carga — Sistema de Licencias MuniHuamanga
# US-15: Validación de Capacidad Objetivo >= 150 Usuarios Concurrentes (RNF-01, RNF-02)
# ==============================================================================

param (
    [int]$TotalRequests = 150,
    [string]$BaseUrl = "http://localhost:8081"
)

Write-Host "==================================================================" -ForegroundColor Cyan
Write-Host "   MuniHuamanga — Prueba de Carga y Concurrencia (RNF-01 / RNF-02)" -ForegroundColor Cyan
Write-Host "   Usuarios concurrentes objetivo: $TotalRequests" -ForegroundColor Cyan
Write-Host "   Objetivo: Latencia menor a 3.00s | Tasa de error menor a 1%" -ForegroundColor Cyan
Write-Host "==================================================================" -ForegroundColor Cyan

$endpoint = "$BaseUrl/api/public/licencias/LIC-2026-00000002"

Write-Host "Verificando conectividad inicial con: $endpoint" -ForegroundColor Yellow
try {
    $probe = Invoke-RestMethod -Uri $endpoint -TimeoutSec 5
    Write-Host "Conexión exitosa. Licencia: $($probe.numeroLicencia) | Estado: $($probe.estado)`n" -ForegroundColor Green
} catch {
    Write-Host "ERROR: No se pudo conectar al servidor en $BaseUrl. Asegúrese de que servicio-expedientes esté corriendo." -ForegroundColor Red
    exit 1
}

Write-Host "Iniciando ráfaga de $TotalRequests peticiones simultáneas concurrentes..." -ForegroundColor Yellow

$handler = [System.Net.Http.HttpClientHandler]::new()
$client = [System.Net.Http.HttpClient]::new($handler)
$client.Timeout = [System.TimeSpan]::FromSeconds(10)

# Disparar 150 peticiones simultáneas verdaderamente concurrentes
$stopwatch = [System.Diagnostics.Stopwatch]::StartNew()
$taskSwList = [System.Collections.Generic.List[hashtable]]::new()
$tasks = [System.Collections.Generic.List[System.Threading.Tasks.Task[System.Net.Http.HttpResponseMessage]]]::new()

for ($i = 1; $i -le $TotalRequests; $i++) {
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    $t = $client.GetAsync($endpoint)
    $tasks.Add($t)
    $taskSwList.Add(@{ Task = $t; Sw = $sw; Id = $i })
}

[System.Threading.Tasks.Task]::WaitAll($tasks.ToArray())
$stopwatch.Stop()

$results = [System.Collections.Generic.List[hashtable]]::new()
foreach ($item in $taskSwList) {
    $t = $item.Task
    $elapsed = $item.Sw.ElapsedMilliseconds
    $isOk = $false
    $statusCode = 0
    if ($t.Status -eq [System.Threading.Tasks.TaskStatus]::RanToCompletion) {
        $resp = $t.Result
        $statusCode = [int]$resp.StatusCode
        $isOk = ($statusCode -eq 200)
    }
    $results.Add(@{
        Id = $item.Id
        Success = $isOk
        StatusCode = $statusCode
        ElapsedMs = $elapsed
    })
}

$client.Dispose()
$handler.Dispose()


# Métricas
$totalExecMs = $stopwatch.ElapsedMilliseconds
$exitosaCount = ($results | Where-Object { $_.Success -eq $true }).Count
$fallosCount = ($results | Where-Object { $_.Success -ne $true }).Count
$latencias = $results | ForEach-Object { $_.ElapsedMs }
$avgMs = [Math]::Round(($latencias | Measure-Object -Average).Average, 2)
$minMs = ($latencias | Measure-Object -Minimum).Minimum
$maxMs = ($latencias | Measure-Object -Maximum).Maximum
$tasaError = [Math]::Round(($fallosCount / $TotalRequests) * 100, 2)
$throughput = [Math]::Round(($TotalRequests / ($totalExecMs / 1000)), 2)

Write-Host "`n==================================================================" -ForegroundColor Cyan
Write-Host "                RESULTADOS DE LA PRUEBA DE CARGA" -ForegroundColor Cyan
Write-Host "==================================================================" -ForegroundColor Cyan
Write-Host "Peticiones Totales Ejecutadas   : $TotalRequests"
Write-Host "Peticiones Exitosas (HTTP 200)   : $exitosaCount" -ForegroundColor Green
Write-Host "Peticiones Fallidas             : $fallosCount" -ForegroundColor $(if ($fallosCount -eq 0) { "Green" } else { "Red" })
Write-Host "Tasa de Error                   : $tasaError %" -ForegroundColor $(if ($tasaError -lt 1.0) { "Green" } else { "Red" })
Write-Host "Tiempo Total de Ejecución       : $totalExecMs ms"
Write-Host "Latencia Mínima                 : $minMs ms"
Write-Host "Latencia Media                  : $avgMs ms" -ForegroundColor $(if ($avgMs -lt 3000) { "Green" } else { "Red" })
Write-Host "Latencia Máxima                 : $maxMs ms"
Write-Host "Rendimiento (Throughput)        : $throughput req/seg" -ForegroundColor Yellow
Write-Host "==================================================================" -ForegroundColor Cyan

if ($exitosaCount -eq $TotalRequests -and $avgMs -lt 3000) {
    Write-Host "`n[RESULTADO]: CUMPLE CON EXITO LOS REQUERIMIENTOS RNF-01 (>= 150 usuarios) Y RNF-02 (menor a 3.0s de latencia)." -ForegroundColor Green
} else {
    Write-Host "`n[RESULTADO]: NO SE CUMPLIERON TODOS LOS UMBRALES DE RENDIMIENTO." -ForegroundColor Red
}
