# ==============================================================================
# SCRIPT DE VERIFICACION DE MIGRACIONES VERSIONADAS Y RECUPERACION POSTGRESQL 16
# ERS-MH-V2-001 / R4.1 Aceptacion Tecnica
# ==============================================================================
$ErrorActionPreference = "Stop"

Write-Host "=== 1. VERIFICANDO CONECTIVIDAD A POSTGRESQL 16 ===" -ForegroundColor Cyan
$pgVersion = docker exec muni_postgres16 psql -U muni_user -d muni_licencias_db -t -A -c "SELECT version();"
Write-Host "PostgreSQL Version: $pgVersion" -ForegroundColor Green

Write-Host "`n=== 2. CREANDO RESPALDO PRE-MIGRACION (pg_dump) ===" -ForegroundColor Cyan
$backupDir = "d:\ArqSoftware\MuniHuamanga\database\backups"
if (-not (Test-Path $backupDir)) {
    New-Item -ItemType Directory -Force -Path $backupDir | Out-Null
}
$backupPre = "$backupDir\backup_pre_migration.sql"

# Usar cmd para redireccionar stdout de docker directamente sin problemas de encoding
cmd.exe /c "docker exec muni_postgres16 pg_dump -U muni_user -d muni_licencias_db --clean --if-exists > ""$backupPre"""

$sizePre = (Get-Item $backupPre).Length
Write-Host "Respaldo Pre-Migracion generado con exito: $backupPre ($sizePre bytes)" -ForegroundColor Green

Write-Host "`n=== 3. APLICANDO MIGRACIONES VERSIONADAS INCREMENTALES ===" -ForegroundColor Cyan
cmd.exe /c "docker exec -i muni_postgres16 psql -U muni_user -d muni_licencias_db < ""d:\ArqSoftware\MuniHuamanga\database\migrations\V2.0.0__baseline_schema_metadata.sql"""
Write-Host "Migracion V2.0.0 aplicada." -ForegroundColor Green

cmd.exe /c "docker exec -i muni_postgres16 psql -U muni_user -d muni_licencias_db < ""d:\ArqSoftware\MuniHuamanga\database\migrations\V2.1.0__incremental_auditoria_correlativos_seguridad.sql"""
Write-Host "Migracion V2.1.0 aplicada." -ForegroundColor Green

Write-Host "`n=== 4. VERIFICANDO TABLA DE CONTROL DE VERSIONES (schema_version) ===" -ForegroundColor Cyan
docker exec muni_postgres16 psql -U muni_user -d muni_licencias_db -c "SELECT installed_rank, version, description, installed_by, installed_on, success FROM schema_version ORDER BY installed_rank;"

Write-Host "`n=== 5. GENERANDO RESPALDO POST-MIGRACION ===" -ForegroundColor Cyan
$backupPost = "$backupDir\backup_post_migration.sql"
cmd.exe /c "docker exec muni_postgres16 pg_dump -U muni_user -d muni_licencias_db --clean --if-exists > ""$backupPost"""
$sizePost = (Get-Item $backupPost).Length
Write-Host "Respaldo Post-Migracion generado: $backupPost ($sizePost bytes)" -ForegroundColor Green

Write-Host "`n=== 6. PRUEBA DE RECUPERACION EN BASE DE DATOS AISLADA ===" -ForegroundColor Cyan
Write-Host "Creando base de datos muni_licencias_recovery_test..."
docker exec muni_postgres16 psql -U muni_user -d postgres -c "DROP DATABASE IF EXISTS muni_licencias_recovery_test;"
docker exec muni_postgres16 psql -U muni_user -d postgres -c "CREATE DATABASE muni_licencias_recovery_test OWNER muni_user;"

Write-Host "Restaurando respaldo post-migracion en base de recuperacion..."
cmd.exe /c "docker exec -i muni_postgres16 psql -U muni_user -d muni_licencias_recovery_test < ""$backupPost"" > NUL 2>&1"

Write-Host "Verificando relaciones e integridad en la base restaurada..."
$tableCount = docker exec muni_postgres16 psql -U muni_user -d muni_licencias_recovery_test -t -A -c "SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public';"
$versionCount = docker exec muni_postgres16 psql -U muni_user -d muni_licencias_recovery_test -t -A -c "SELECT count(*) FROM schema_version WHERE success = true;"

Write-Host "Tablas restauradas: $tableCount" -ForegroundColor Green
Write-Host "Migraciones registradas en recuperacion: $versionCount" -ForegroundColor Green

if ([int]$tableCount -ge 32 -and [int]$versionCount -ge 2) {
    Write-Host "`n>>> VALIDACION DE MIGRACION Y RECUPERACION: PASS <<<" -ForegroundColor Green
} else {
    Write-Error "Fallo en la prueba de recuperacion."
}
