# Portal estudiantil de Centro Saber

Actualización: el portal ahora tiene rutas separadas, logos adjuntos por el usuario, Enfermería e ICFES y un flujo local de solicitudes/usuarios. Consultar `demo-local.md` para las cuentas de prueba y el funcionamiento actual. Las vistas vacías descritas a continuación corresponden al primer entregable.

## Actualización del 13 de octubre de 2026

`/nosotros` incorpora el concepto Promover, el objeto social de reintegración educativa de jóvenes y adultos y la vinculación mediante convenios educativos/sociales en educación informal. Se adaptó el contenido de https://www.centrosaber.com/nosotros/ y la captura suministrada; no se atribuyen convenios específicos ni acreditaciones no publicadas. La pestaña se llama Sobre nosotros.

`/icfes` incorpora cinco pasos de orientación/inscripción, envío de comprobante por contacto con la institución, consulta de formulario, capacitación, material de estudio y accesos oficiales de citación/resultados. La solicitud local continúa siendo una solicitud de información, no una inscripción al examen. El formulario externo de Centro Saber se enlaza a la página institucional porque no se obtuvo su contenido desde la herramienta web.

La captura suministrada publica COP 110.000 hasta el 1 de noviembre, COP 150.000 hasta el 7 de diciembre y recepción de solicitudes del 1 de octubre al 1 de noviembre. Se muestran dentro de un apartado desplegable, con la falta de año y la referencia contradictoria a abril 2026 identificadas. Son valores del anuncio, no tarifas oficiales verificadas.

El cronograma oficial de Saber 11 consultado en https://www.icfes.gov.co/evaluaciones-icfes/cronograma-de-evaluaciones/cronograma-examen-saber-11/ indica, para calendario A 2026, citaciones el 10 de julio, aplicación el 26 de julio y resultados el 25 de septiembre. Se presenta como periodo finalizado. Las fechas 2027 de la captura se conservan como anuncio institucional pendiente de confirmación, sin atribuirles verificación oficial.

Accesos oficiales comprobados: citación https://www.icfesinteractivo.gov.co/citacion-web/pages/citacion/reportes/consultarCitacionIndividual.jsf; resultados https://resultadossaber11.icfes.gov.co/login; guía Saber 11 de 2026 https://www.icfes.gov.co/wp-content/uploads/2026/03/17-marzo-guia-de-orientacion-saber-11-2026-2.pdf. Se verificaron compilación, navegación, despliegue de valores y selección de ICFES en el formulario local sin enviar una solicitud.

Interfaz pública implementada en React + TypeScript dentro del proyecto existente. La ruta `/` muestra el portal para estudiantes y `/administracion` mantiene el centro de plantillas administrativo/docente.

## Información consultada el 12 de octubre de 2026

| Fuente | Datos usados |
| --- | --- |
| https://www.centrosaber.com/ | Validar bachiller, preparación de educación informal, preparación preuniversitaria, vínculo ICFES y mapa institucional. |
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


## Registro y convivencia

El portal tiene la pestaña Registro (`/registro`), enlaces diferenciados a inscripción/documentos y contacto con Neli y Estiven. La institución revisa los formularios externos; el portal no lee ni confirma sus respuestas. Administración configura enlaces y contactos y gestiona Fechas y eventos desde Gestión de información. Solo se publican los eventos con estado PUBLICADO, filtrables por año académico. Los eventos ICFES publicados se muestran en su página; las fechas históricas de las capturas conservan su advertencia de verificación.

El manual en `/convivencia` es una propuesta para revisión institucional, con PDF descargable e impresión. Incluye principios, derechos, deberes, registro y clases, privacidad, convivencia digital, ruta de atención, medidas educativas y pasos para su adopción. No se solicita aceptación de un reglamento no aprobado.
