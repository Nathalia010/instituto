# Centro Saber

Base inicial de un sistema educativo para validación de bachillerato, cursos y formación técnica, con 50 a 200 estudiantes.

## Qué puedes usar ahora

La demostración funcional actual está descrita en [demo-local.md](docs/demo-local.md): programas e ICFES en pestañas separadas, formularios persistentes, ingreso administrativo, creación de usuarios y consulta estudiantil con cuentas ficticias. Para probarla ejecuta `./tools/start-demo.ps1` desde la raíz en una terminal y `npm.cmd run dev` desde `frontend` en otra. El script usa JDK/Maven configurados o los incluidos con IntelliJ.

- Centro de plantillas en React + TypeScript: buscar, filtrar y descargar un Excel vacío con ocho hojas o CSV por módulo.
- Portal estudiantil público de Centro Saber: programas, propósito institucional, contacto, ubicación y vistas de consulta personal en preparación.
- Guía para llenar estudiantes, docentes, cursos, periodos, matrículas, notas, cobros y pagos.
- Backend Spring Boot con comprobación de disponibilidad y rutas cerradas por defecto.
- Migración PostgreSQL con relaciones, historial por periodo, notas por matrícula y movimientos financieros separados.

El perfil local `demo` permite probar sesiones, solicitudes y consulta personal con datos ficticios. El perfil PostgreSQL, la autenticación para operación real, la gestión académica completa, la importación y la publicación siguen el plan de desarrollo. No ingresar información real en la demostración.

## Documentos

- [Arquitectura y decisiones](docs/arquitectura.md)
- [Modelo entidad-relación](docs/modelo-datos.md)
- [Autenticación y autorización](docs/seguridad.md)
- [Importaciones](docs/importaciones.md)
- [Tareas por prioridad](docs/plan-desarrollo.md)
- [Pruebas realizadas y límites](docs/verificacion.md)
- [Portal estudiantil y fuentes institucionales](docs/portal-estudiantes.md)

## Ejecutar el centro de plantillas

Requiere Node 22.12 o superior compatible (comprobado con Node 24).

```powershell
cd frontend
npm.cmd install
npm.cmd run dev
```

Abrir `http://127.0.0.1:5173/` para estudiantes y `http://127.0.0.1:5173/administracion` para el área administrativa/docente. Funciona sin backend: la información institucional es pública, las plantillas están vacías y la consulta personal aún no está habilitada. Las rutas visuales no sustituyen la autorización del backend. Para verificar tipos y crear el sitio compilado:

```powershell
npm.cmd run build
npm.cmd run preview
```

## Base de datos local

Requiere Docker Desktop con Compose. Desde la raíz copiar `.env.example` a `.env` y definir una contraseña local propia en `POSTGRES_PASSWORD`. El archivo está excluido del control de versiones. Usar esa misma contraseña en `DATABASE_PASSWORD` al ejecutar Java. Spring Boot **no lee automáticamente** el `.env` de Compose.

```powershell
Copy-Item .env.example .env
docker compose up -d database
```

El puerto PostgreSQL se expone solo en loopback y el volumen conserva los datos. Para detener sin borrar datos: `docker compose stop`. Flyway aplicará V1 al arrancar el backend. No aplicar manualmente el script y después arrancar Flyway sobre esa base sin una estrategia de baseline.

## Backend local

Requiere JDK 21 o compatible y Maven 3.6.3 o superior. Java 8 no sirve para este proyecto. En IntelliJ: abrir `backend/pom.xml` como proyecto Maven y seleccionar un JDK compatible.

En PowerShell establecer variables para esta sesión. Pedir la contraseña sin dejarla escrita en el comando:

```powershell
$env:DATABASE_URL = 'jdbc:postgresql://localhost:5432/educontrol'
$env:DATABASE_USERNAME = 'educontrol_owner'
$dbCredential = Get-Credential -UserName $env:DATABASE_USERNAME -Message 'Contraseña de PostgreSQL local'
$env:DATABASE_PASSWORD = $dbCredential.GetNetworkCredential().Password
cd backend
mvn test
mvn spring-boot:run
```

Comprobar `http://127.0.0.1:8080/api/health`. Las demás rutas están denegadas. El backend escucha en loopback. Para internet se requiere configurar proxy HTTPS y completar seguridad, respaldo y módulos.

La cuenta propietaria de Compose se usa solo para desarrollo y migraciones iniciales. Antes de operar separar credenciales de migración y ejecución, y restringir permisos. En producción `COOKIE_SECURE=true` y URL PostgreSQL con TLS verificado.

## Estructura

```text
backend/                  Spring Boot, pruebas y migración Flyway
frontend/src/             Pantalla inicial de plantillas
frontend/public/plantillas Libro vacío, CSV y guía
docs/                     Arquitectura, datos, seguridad y etapas
tools/                    Generador reproducible de plantillas
compose.yaml              PostgreSQL local
```

Las reglas de aprobación y mora se definirán con la institución antes de automatizar resultados. El control académico y los saldos siempre se conservarán por separado.
