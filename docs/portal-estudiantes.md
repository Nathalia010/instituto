# Portal estudiantil de Centro Saber

Actualización: el portal ahora tiene rutas separadas, logos adjuntos por el usuario, Enfermería e ICFES y un flujo local de solicitudes/usuarios. Consultar `demo-local.md` para las cuentas de prueba y el funcionamiento actual. Las vistas vacías descritas a continuación corresponden al primer entregable.

Interfaz pública implementada en React + TypeScript dentro del proyecto existente. La ruta `/` muestra el portal para estudiantes y `/administracion` mantiene el centro de plantillas administrativo/docente.

## Información consultada el 12 de octubre de 2026

| Fuente | Datos usados |
| --- | --- |
| https://www.centrosaber.com/ | Validación para adultos, preparación de educación informal, preparación preuniversitaria, vínculo ICFES y mapa institucional. |
| https://www.centrosaber.com/nosotros/ | Propósito de reintegración educativa de jóvenes y adultos y convenios educativos/sociales. |
| https://www.centrosaber.com/contact/ | Dirección, referencia a estación Olaya, teléfono/WhatsApp y horario publicado. |
| https://www.centrosaber.com/quiero-estudiar/ | Enlace de consulta del proceso de inscripción, sin copiar ni enviar formularios. |

Se redactaron resúmenes propios y se enlazó a las fuentes. La marca visual de texto y las ilustraciones SVG/CSS son nuevas; no son una reproducción del logotipo oficial. No se añadieron tarifas, duraciones, docentes, certificaciones ni programas técnicos no publicados en estas fuentes.

La página anuncia un plazo de inscripción ICFES sin año identificable. El nuevo portal no lo presenta como una convocatoria actual. El enlace al servicio externo de estudiantes procede del sitio institucional; no se pudo verificar su funcionamiento en la consulta web. Tampoco se completó una inscripción ni se envió información a WhatsApp.

## Interacciones

Navegación por secciones, menú móvil, detalles de programa desplegables, pestañas de consulta personal con navegación por teclado, teléfono, WhatsApp, mapa y regreso al área administrativa. Los enlaces institucionales abren sus destinos externos; el sitio no transmite automáticamente información personal.

## Consulta personal

Mi resumen, mis datos, mi matrícula, mis notas y mis pagos están preparados como vistas vacías. No se muestran identidades ni notas ficticias como si existiera una sesión. El estado indica que la verificación de identidad todavía está en preparación.

Para habilitar datos reales implementar el plan de `seguridad.md`, enlazar cada identidad a un estudiante y consultar `/api/portal/me` desde la sesión del servidor. No aceptar un documento o UUID enviado desde el frontend como prueba de identidad. Notas/pagos serán solo de lectura; los filtros anuales deben conservar el historial de cada matrícula. El backend actual sigue denegando estas rutas.

Las rutas del frontend separan las pantallas, no constituyen control de roles. La protección se mantiene en el backend. El centro de plantillas sigue siendo una herramienta de preparación pública sin acceso a registros oficiales.
