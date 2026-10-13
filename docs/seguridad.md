# Plan de autenticación y autorización

Esta etapa deja todas las rutas cerradas salvo `/api/health`. No hay cuentas predeterminadas ni contraseñas de ejemplo utilizables.

## Identidad

Solo administrador y profesor son usuarios internos. ESTUDIANTE representa una identidad de consulta, ligada a un único estudiante y sin acceso administrativo.

Implementar contraseñas con Argon2id mediante un encoder mantenido, activación con token de un solo uso, vencimiento y recuperación que invalida las sesiones previas. Guardar el hash del token de recuperación y OTP, no el token. Administrador: MFA obligatorio, incluyendo recuperación supervisada. Profesor: contraseña y opción de segundo factor.

Estudiante: documento normalizado + código enviado exclusivamente al correo previamente verificado. Respuesta genérica para documentos desconocidos, límites por IP y cuenta, máximo de intentos y códigos con caducidad breve y un solo uso. Nunca usar fecha de expedición como único segundo factor. No permitir cambiar el correo desde la pantalla de acceso.

Sesión HttpOnly, Secure en producción, SameSite, rotación del identificador al autenticar, vencimiento tras 20 minutos de inactividad y cierre de sesión con invalidación. CSRF en escrituras y salida. No almacenar tokens ni datos oficiales en localStorage. Límites de intentos persistentes, compartidos entre réplicas.

## Matriz de autorización prevista

| Operación | Administrador | Profesor | Estudiante |
| --- | --- | --- | --- |
| Gestionar estudiantes, cursos, periodos y matrículas | Sí | No | No |
| Consultar listados | Sí | Solo ofertas asignadas | No |
| Registrar/editar notas | Sí con auditoría | Solo ofertas asignadas | No |
| Consultar notas | Sí | Solo estudiantes de sus ofertas | Solo propias |
| Cobros, pagos y reportes financieros | Sí | No por defecto | Solo propios, lectura |
| Importar/exportar archivos administrativos | Sí | No | No |
| Gestionar identidades y permisos | Sí | No | No |
| Consultar auditoría | Sí | No | No |
| Enviar solicitudes | Según función | Según función | Como su identidad |

Aplicar controles de rol con `@PreAuthorize` y controles de pertenencia en servicios/repositorios. Un profesor no puede enviar otro `profesor_id` para simular autorización. El portal usa `/api/portal/me`; obtiene `estudiante_id` de la sesión, no de un parámetro del navegador. Si una URL necesita un identificador, consultar con ambos identificadores: recurso y propietario autorizado.

Las facultades financieras adicionales de un profesor requieren permisos explícitos futuros, no cambiar globalmente el rol PROFESOR. Las rutas no descritas se deniegan por defecto.

## Pruebas necesarias para habilitar módulos

1. Administrador, profesor A, profesor B y dos estudiantes ficticios: probar cada permiso por HTTP.
2. Modificar ID en URL/cuerpo: ningún acceso entre estudiantes ni entre ofertas de distintos docentes.
3. Probar sesión vencida, logout, cuenta desactivada, CSRF y escalamiento de rol.
4. OTP vencido, repetido, incorrecto; respuestas indistinguibles para cuentas existentes y desconocidas.
5. Importación con fórmulas, ZIP excesivo, formato falso, duplicados, concurrencia y error a mitad de transacción.
6. Cambios sensibles con auditoría atómica y recuperación verificable del respaldo.

## Operación futura

HTTPS, TLS a PostgreSQL con certificado verificado, secretos en entorno/gestor de secretos, cuenta de ejecución mínima y base de datos sin puerto público. CORS restringido si se separan dominios. No abrir rutas por ocultar o mostrar botones.

Antes de usar datos reales: implementar las fases de identidad y permisos, definir tratamiento y conservación de datos con la institución y verificar respaldo/restauración. Este proyecto todavía no realiza respaldos ni MFA.
