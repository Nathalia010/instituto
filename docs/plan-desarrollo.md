# Plan de desarrollo por prioridad

## 1. Fundamentos — entregado

Arquitectura, modelo de datos, migración inicial, proyecto React/TypeScript, esqueleto Spring Boot, configuración PostgreSQL local, plan de seguridad, contrato de importación y centro de plantillas vacías. La etapa no incluye pantallas de gestión ni credenciales operativas.

## 2. Identidad y permisos — siguiente

Crear usuarios con hash seguro, activación, sesiones y CSRF, MFA de administradores, recuperación, desactivación y límites de acceso. Implementar filtros de pertenencia y probar la matriz completa. Criterio: un profesor y un estudiante no acceden a datos fuera de su alcance ni alterando las peticiones.

## 3. Gestión académica

Estudiantes y docentes; cursos, periodos, ofertas/grupos, asignaciones y matrículas. Escalas y evaluaciones configurables; notas con control de versión, auditoría y motivos de corrección. Filtros de año, curso y estado. Criterio: conservar historial de varios años y rechazar notas fuera de escala o de otra oferta.

## 4. Gestión financiera

Conceptos, cobros, abonos, revisión y confirmación, reversiones transaccionales y referencias únicas. Definir mora y sobrepagos. Exportación de informes neutralizando fórmulas. Criterio: los saldos concilian con movimientos independientes, incluso con varios pagos, reversión y concurrencia.

## 5. Importación

Archivos reales anonimizados, correspondencia de columnas, selección de hoja, vista previa, informe de errores y confirmación idempotente en transacción. Criterio: ninguna carga modifica datos sin confirmación; duplicados y fallos no producen registros parciales.

## 6. Portal e informes

Acceso estudiantil por identidad verificada, consulta de datos propios, cursos y solicitudes. Panel con filtros anuales y académico/financiero combinables. Criterio: consultar finalizados con deuda sin mezclar estados, usando periodos anteriores.

## 7. Preparación para internet

HTTPS, cuentas mínimas de base de datos, registros sin secretos, monitoreo, copias cifradas en almacenamiento separado y ensayo de restauración. Acuerdo de objetivos de recuperación y retención. Revisión de privacidad de la institución y pruebas de permisos completas. Criterio: restaurar una copia en otra base de datos y comprobar conteos, saldos y acceso antes de publicación.
