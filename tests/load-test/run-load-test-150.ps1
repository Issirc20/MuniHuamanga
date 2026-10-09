# ==============================================================================
# Script de Pruebas de Carga - Sistema de Licencias MuniHuamanga
# US-15: Validacion de Capacidad Objetivo >= 150 Usuarios Concurrentes (RNF-01, RNF-02, RNF-20)
# ==============================================================================

param (
    [int]$TotalRequests = 150,
    [string]$BaseUrl = "http://localhost:8081"
)

Add-Type -AssemblyName System.Net.Http

Write-Host "==================================================================" -ForegroundColor Cyan
Write-Host "   MuniHuamanga - Suite de Pruebas de Carga y Concurrencia QA" -ForegroundColor Cyan
Write-Host "   Capacidad Objetivo: >= $TotalRequests usuarios concurrentes" -ForegroundColor Cyan
Write-Host "   Criterio RNF-01 / RNF-02: Latencia < 3.00s | Tasa de error < 1%" -ForegroundColor Cyan
Write-Host "==================================================================" -ForegroundColor Cyan

function Ejecutar-PruebaCarga {
    param (
        [string]$NombreEscenario,
        [string]$Url,
        [int]$Usuarios
    )

    Write-Host "`n>>> Ejecutando Escenario: $NombreEscenario ($Usuarios peticiones simultaneas)..." -ForegroundColor Yellow

    $handler = [System.Net.Http.HttpClientHandler]::new()
    $client = [System.Net.Http.HttpClient]::new($handler)
    $client.Timeout = [System.TimeSpan]::FromSeconds(15)

    $stopwatch = [System.Diagnostics.Stopwatch]::StartNew()
    $taskSwList = [System.Collections.Generic.List[hashtable]]::new()
    $tasks = [System.Collections.Generic.List[System.Threading.Tasks.Task[System.Net.Http.HttpResponseMessage]]]::new()

    for ($i = 1; $i -le $Usuarios; $i++) {
        $sw = [System.Diagnostics.Stopwatch]::StartNew()
        $t = $client.GetAsync($Url)
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

    $totalExecMs = $stopwatch.ElapsedMilliseconds
    $exitosaCount = ($results | Where-Object { $_.Success -eq $true }).Count
    $fallosCount = ($results | Where-Object { $_.Success -ne $true }).Count
    $latencias = $results | ForEach-Object { $_.ElapsedMs }
    $avgMs = [Math]::Round(($latencias | Measure-Object -Average).Average, 2)
    $minMs = ($latencias | Measure-Object -Minimum).Minimum
    $maxMs = ($latencias | Measure-Object -Maximum).Maximum
    $tasaError = [Math]::Round(($fallosCount / $Usuarios) * 100, 2)
    $throughput = [Math]::Round(($Usuarios / ($totalExecMs / 1000)), 2)

    $sortedLatencias = $latencias | Sort-Object
    $p50 = $sortedLatencias[[Math]::Floor($sortedLatencias.Count * 0.50)]
    $p95 = $sortedLatencias[[Math]::Floor($sortedLatencias.Count * 0.95)]
    $p99 = $sortedLatencias[[Math]::Floor($sortedLatencias.Count * 0.99)]

    Write-Host "------------------------------------------------------------------" -ForegroundColor Cyan
    Write-Host "   Metricas: $NombreEscenario" -ForegroundColor Cyan
    Write-Host "------------------------------------------------------------------" -ForegroundColor Cyan
    Write-Host "Peticiones Totales Ejecutadas   : $Usuarios"
    Write-Host "Peticiones Exitosas (HTTP 200)  : $exitosaCount" -ForegroundColor Green
    $falloColor = if ($fallosCount -eq 0) { "Green" } else { "Red" }
    Write-Host "Peticiones Fallidas             : $fallosCount" -ForegroundColor $falloColor
    $tasaColor = if ($tasaError -lt 1.0) { "Green" } else { "Red" }
    Write-Host "Tasa de Error                   : $tasaError %" -ForegroundColor $tasaColor
    Write-Host "Tiempo Total de Ejecucion       : $totalExecMs ms"
    Write-Host "Latencia Minima                 : $minMs ms"
    $avgColor = if ($avgMs -lt 3000) { "Green" } else { "Red" }
    Write-Host "Latencia Media                  : $avgMs ms" -ForegroundColor $avgColor
    Write-Host "Latencia Mediana (P50)          : $p50 ms"
    Write-Host "Latencia Percentil 95 (P95)     : $p95 ms"
    Write-Host "Latencia Percentil 99 (P99)     : $p99 ms"
    Write-Host "Latencia Maxima                 : $maxMs ms"
    Write-Host "Rendimiento (Throughput)        : $throughput req/seg" -ForegroundColor Yellow

    return @{
        Escenario = $NombreEscenario
        Total = $Usuarios
        Exitos = $exitosaCount
        Fallos = $fallosCount
        TasaError = $tasaError
        TotalMs = $totalExecMs
        LatenciaMedia = $avgMs
        P50 = $p50
        P95 = $p95
        P99 = $p99
        Throughput = $throughput
        Cumple = ($exitosaCount -eq $Usuarios -and $avgMs -lt 3000)
    }
}

# 1. Verificacion de conectividad
$endpointSalud = "$BaseUrl/actuator/health"
try {
    $salud = Invoke-RestMethod -Uri $endpointSalud -TimeoutSec 5
    Write-Host "Servidor en linea ($endpointSalud) -> Estado: $($salud.status)" -ForegroundColor Green
} catch {
    Write-Host "ERROR: No se pudo conectar al servidor en $BaseUrl. Asegurese de que este levantado." -ForegroundColor Red
    exit 1
}

# 2. Ejecutar Escenarios
$r1 = Ejecutar-PruebaCarga -NombreEscenario "1. Verificacion Publica QR (RNF-20)" -Url "$BaseUrl/api/public/licencias/LIC-2026-00000002" -Usuarios $TotalRequests
$r2 = Ejecutar-PruebaCarga -NombreEscenario "2. Consulta Tramite Ciudadano (RNF-01/H07)" -Url "$BaseUrl/api/expedientes/tramite/EXP-2026-00001" -Usuarios $TotalRequests

Write-Host "`n==================================================================" -ForegroundColor Cyan
Write-Host "                   RESUMEN CONSOLIDADO QA" -ForegroundColor Cyan
Write-Host "==================================================================" -ForegroundColor Cyan
Write-Host ("{0,-38} | {1,6} | {2,8} | {3,8} | {4,10}" -f "Escenario", "Reqs", "Avg(ms)", "P95(ms)", "Throughput")
Write-Host ("-" * 80)
Write-Host ("{0,-38} | {1,6} | {2,8} | {3,8} | {4,10} req/s" -f $r1.Escenario, $r1.Total, $r1.LatenciaMedia, $r1.P95, $r1.Throughput)
Write-Host ("{0,-38} | {1,6} | {2,8} | {3,8} | {4,10} req/s" -f $r2.Escenario, $r2.Total, $r2.LatenciaMedia, $r2.P95, $r2.Throughput)
Write-Host ("-" * 80)

if ($r1.Cumple -and $r2.Cumple) {
    Write-Host "`n[EVALUACION FINAL]: TODOS LOS ESCENARIOS CUMPLEN RNF-01 (>= 150 usuarios) Y RNF-02 (< 3.00s)." -ForegroundColor Green
} else {
    Write-Host "`n[EVALUACION FINAL]: SE DETECTARON INCUMPLIMIENTOS EN LOS UMBRALES." -ForegroundColor Red
}
