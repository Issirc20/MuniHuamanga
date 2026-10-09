# PLAN DE CONTINGENCIA Y ROLLBACK DE DESPLIEGUE
## Sistema de Gestión Digital de Licencias de Funcionamiento — MuniHuamanga
**Versión:** `v0.9.0-rc1`  
**Objetivo:** Procedimiento operativo estandarizado para revertir un despliegue defectuoso en producción a una versión estable anterior sin pérdida de integridad de datos ni afectación a los trámites ciudadanos.

---

## 1. CRITERIOS DE ACTIVACIÓN DE ROLLBACK

El Comité de Cambios o el Ingeniero de Guardia declarará el estado de **Rollback Inmediato** si tras un pase a producción se presenta cualquiera de los siguientes incidentes durante los primeros 30 minutos:

1. **Fallo Crítico de Disponibilidad:** El endpoint `/actuator/health` responde `DOWN` o no responde tras 3 reinicios consecutivos del contenedor.
2. **Degradación Severa de Rendimiento:** La latencia en el endpoint de verificación QR (`/api/public/licencias/{cod}`) supera los 3,000 ms en más del 5% de peticiones.
3. **Pérdida de Transaccionalidad / Errores 500:** Tasa de errores HTTP 500 superior al 1% en la bandeja de evaluación de expedientes o en el registro de pagos SAT.
4. **Vulnerabilidad de Seguridad No Detectada:** Fuga de datos personales no ofuscados en consultas ciudadanas.

---

## 2. PROCEDIMIENTO PASO A PASO DE ROLLBACK

```
[INCIDENTE DETECTADO]
         │
         ▼
 1. Aislar Tráfico Externo (Página de Mantenimiento)
         │
         ▼
 2. Detener Contenedor Backend Actual
         │
         ▼
 3. Evaluar Estado de Base de Datos
       ├─ ¿Hubo migración destructiva de DDL?
       │   ├─ SÍ ──> 4. Restaurar Backup de BD (pg_restore)
       │   └─ NO ──> Continuar a paso 5
         ▼
 5. Revertir Imagen de Contenedor a Tag Anterior
         │
         ▼
 6. Iniciar Servicios con Tag Anterior y Smoke Test
         │
         ▼
 7. Restablecer Tráfico y Comunicar Incidente
```

---

## 3. COMANDOS OPERATIVOS DE REVERSIÓN

### Paso 1: Poner en modo mantenimiento el proxy inverso (Nginx)
Si cuenta con proxy frontal, redirija el tráfico temporalmente:
```bash
# Activar página institucional de mantenimiento
ln -sf /etc/nginx/sites-available/mantenimiento.conf /etc/nginx/sites-enabled/munihuamanga.conf
systemctl reload nginx
```

### Paso 2: Detener el contenedor de aplicación
```bash
docker compose stop app
```

### Paso 3: Revertir la versión de la imagen Docker
En el archivo `docker-compose.yml`, cambie el tag de la imagen de la versión actual a la versión previa estable (por ejemplo `0.8.0` o el commit SHA previo):

```yaml
  app:
    image: munihuamanga/servicio-expedientes:0.8.0  # Versión anterior estable
```

O si se utiliza Git en el host:
```bash
git checkout <COMMIT_HASH_ANTERIOR_ESTABLE>
docker compose build app
```

### Paso 4: Restauración de Base de Datos (Solo si hubo corrupción o cambios de DDL)
Si los scripts SQL o Hibernate alteraron tablas de forma incompatible con la versión anterior:

```bash
# 1. Detener conexiones a la base de datos
docker compose stop app

# 2. Restaurar el volcado previo generado antes del despliegue:
docker exec -i muni_licencias_postgres pg_restore \
    -U muni_user \
    -d muni_licencias_db \
    --clean \
    --if-exists \
    -v < /var/backups/muni/backup_pre_deploy.dump
```

### Paso 5: Levantar la versión anterior y validar salud
```bash
docker compose up -d app
docker compose logs -f --tail=50 app
```

Verificar que responda satisfactoriamente:
```bash
curl -I http://localhost:8081/actuator/health
```

### Paso 6: Reactivar el tráfico ciudadano
```bash
ln -sf /etc/nginx/sites-available/munihuamanga-prod.conf /etc/nginx/sites-enabled/munihuamanga.conf
systemctl reload nginx
```

---

## 4. MATRIZ DE CONTACTOS DE EMERGENCIA Y ESCALAMIENTO

| Rol | Responsable | Canal / Teléfono |
|---|---|---|
| **Líder de Arquitectura** | Equipo TI MuniHuamanga | ti-arquitectura@munihuamanga.gob.pe |
| **Oficial de Seguridad (CISO)** | Ciberseguridad MPH | ciso@munihuamanga.gob.pe |
| **Administrador de BD (DBA)** | DBA PostgreSQL | dba@munihuamanga.gob.pe |
| **Gerente de Desarrollo Económico** | Mesa de Ayuda Licencias | licencias@munihuamanga.gob.pe |
