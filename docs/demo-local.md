# Demostración local de Centro Saber

Esta ampliación implementa un flujo funcional con Spring Boot, sesiones de servidor y una base H2 persistente. El perfil PostgreSQL original permanece separado. No se ha publicado el sistema.

## Rutas

| Ruta | Función |
| --- | --- |
| `/` | Programas, incluido Enfermería solicitado por la institución. |
| `/informacion?programa=enfermeria` | Formulario público para solicitar información. |
| `/icfes` | Preparación y enlaces de información de inscripción. |
| `/estudiantes` | Ingreso estudiantil y, tras autenticar, datos/matrícula/notas/pagos. |
| `/nosotros`, `/contacto` | Información institucional. |
| `/administracion` | Ingreso administrativo, solicitudes, creación de estudiantes y plantillas. |

## Arrancar

En una terminal con JDK 21 o compatible y Maven, desde `backend`:

```powershell
mvn spring-boot:run '-Dspring-boot.run.profiles=demo'
```

En otra terminal, desde `frontend`:

```powershell
npm.cmd run dev
```

No requiere configurar PostgreSQL para la demostración. H2 se guarda en `backend/.data/centrosaber-demo.mv.db`, excluido del control de versiones. Las solicitudes y usuarios sobreviven al reinicio del backend; las sesiones se cierran al reiniciar. La aplicación solo escucha en `127.0.0.1`.

En este computador también puedes iniciar el backend desde la raíz con `./tools/start-demo.ps1`, que localiza el JDK y Maven incluidos con IntelliJ.

## Cuentas de prueba

Administrador: `adminN`, clave `1234987A`.

Estudiante: cédula `1000000001`, expedición `2020-01-15`, año/clave `2026`. Nombre, notas y movimientos son ficticios. La cuenta muestra dos notas, un cobro de COP 500.000 y un abono de COP 200.000.

El administrador recibe nombre, correo, celular, programa, mensaje, consentimiento y fecha. Puede abrir correo/teléfono/WhatsApp sin que se envíen mensajes automáticamente. Para crear un usuario desde la solicitud completa documento, expedición y año. El servidor registra el estudiante, vincula la solicitud y escribe auditoría dentro de una transacción. La misma solicitud no puede convertirse dos veces ni crear un documento duplicado. El nuevo estudiante queda pendiente de matrícula y sin notas o movimientos inventados.

## Seguridad y límites

Este perfil se habilita explícitamente con `demo`. El perfil por defecto sigue cerrando las rutas no implementadas. Contraseñas/claves verificadas mediante BCrypt, sesión HttpOnly, protección CSRF, límites de intentos y permisos en el servidor. `/student/me` obtiene el ID desde la sesión, no desde un documento suministrado en una consulta.

Cédula, fecha de expedición y año tienen poca entropía y pueden ser conocidos por terceros. Este mecanismo se implementa únicamente para probar el flujo solicitado. Antes de publicar o usar datos reales sustituirlo por credenciales individuales y una segunda verificación, añadir MFA administrativo, configuración de secretos, privacidad, recuperación y PostgreSQL con permisos mínimos. Las credenciales de prueba están deliberadamente incluidas solo en el perfil demo.

Nequi y DaviPlata muestran `3143544501`, con copia del número y consulta por WhatsApp. No se ha validado que el teléfono tenga billeteras registradas ni se ejecutan transferencias. PayPal se muestra sin enlace. Crear un usuario no confirma una matrícula, un pago o elegibilidad para un programa.

## Diseño y fuentes

Administración abre con un dashboard y un menú lateral único: Dashboard, Solicitudes, Usuarios estudiantiles, Gestión de información y Plantillas. Los indicadores, las solicitudes recientes y el interés por programa se calculan con los registros del servidor. Las tarjetas y accesos rápidos permiten abrir sus módulos; en móvil el menú se despliega desde el botón Menú. Plantillas está integrado en el mismo panel, sin una segunda navegación.

## Gestión de información

El dashboard muestra los accesos rápidos debajo de la bienvenida: Descargar Excel completo, Atender solicitudes, Usuarios estudiantiles, Gestión de información y Descargar plantillas. La descarga directa reúne los once apartados en un único archivo XLSX de todos los años; las descargas por año siguen disponibles en Gestión de información.

El módulo permite consultar y buscar estudiantes, docentes, cursos, periodos, matrículas, notas, cobros, pagos y fechas/eventos, abrir una ficha y descargar cada catálogo en XLSX o CSV. Se incluyen los estudiantes que ya tienen cuenta. El libro completo contiene once hojas: los nueve catálogos más las notas y movimientos existentes del portal, conservados aparte porque no tienen los códigos de matrícula/cobro del nuevo catálogo.

El selector ofrece todos los años registrados o uno concreto. Docentes y cursos no tienen año y se exportan completos. Matrículas toman el año del periodo; notas y cobros el de su matrícula; pagos el de su cobro. Estudiantes también se seleccionan por matrículas del año consultado. Las notas históricas del portal usan su año académico y sus movimientos la fecha del movimiento.

La carga admite CSV UTF-8 con separador coma, punto y coma o tabulación, y Excel XLSX con selección de hoja. Límite: 5 MB, 2.000 filas y 100 columnas. Se relacionan las columnas antes de previsualizar. El servidor valida campos, documentos, fechas, escala de notas, importes, referencias y duplicados. No admite fórmulas ni macros. Los identificadores se exportan como texto para conservar ceros iniciales; el CSV neutraliza fórmulas al abrirse en una hoja de cálculo.

No se guardan registros hasta confirmar la vista previa. La confirmación vuelve a validar, guarda solo las filas válidas en una transacción y registra auditoría. Las filas existentes nunca se sobrescriben. La vista previa vence en 15 minutos y pertenece a la sesión que la creó. Puede descargarse el listado de errores. Orden de carga: estudiantes/docentes, cursos, periodos, matrículas, notas/cobros, pagos. Los pagos importados quedan EN_REVISION.

Los registros importados persisten en la base H2 de esta demostración para consulta y exportación. No crean credenciales automáticamente ni sustituyen todavía las notas/movimientos del portal estudiantil. La creación de cuentas sigue en Usuarios estudiantiles desde Solicitudes. La conexión de estos catálogos al portal y su migración a PostgreSQL son trabajo posterior.

## Galería de imágenes

Sobre nosotros muestra el perro graduado adjunto, guardado sin edición en `frontend/public/marca/perro-graduado.png`. El programa se llama **Validar bachiller** en las etiquetas del portal, formularios y administración; se conserva el identificador `validacion` para mantener las relaciones de los registros existentes.

El menú administrativo y el dashboard incluyen Galería de imágenes. Permite seleccionar hasta diez archivos por carga, revisar la vista previa y sus descripciones, y publicar explícitamente. Admite JPG/PNG de hasta 5 MB y 16 megapíxeles, con un máximo de treinta imágenes publicadas. Las imágenes se validan y se vuelven a codificar en el servidor; solo administradores pueden publicar, quitar y restaurar, con protección CSRF y auditoría. Los archivos y las descripciones persisten en H2, no únicamente en el navegador.

Quitar una imagen la oculta de la galería y de su endpoint público; se conserva para restaurarla desde administración. El perro fijo de la cabecera sigue visible aunque se retire su diapositiva. En esta base de demostración se publicaron el perro, el logotipo y el símbolo institucional. Pasa las diapositivas cada cinco segundos, dispone de controles manuales y pausa al interactuar o al ocultarse la pestaña. Respeta la preferencia de reducir movimiento y actualiza su contenido al volver a enfocar la ventana. Si no hay imágenes publicadas, no muestra la sección de galería.

Verificación: 20 pruebas del backend aprobadas y compilación del frontend correcta. Se comprobaron en navegador la vista previa, publicación, retirada, restauración, reproducción automática y pausa con las imágenes institucionales.

## Bootstrap y carrusel

Bootstrap 5.3.8 es una dependencia local del frontend, importada antes de los estilos institucionales en `main.tsx`. Portal, administración y centro de plantillas emplean sus clases de botones, formularios, navegación, tarjetas, contenedores, tablas, alertas y utilidades. `bootstrap-theme.css` conserva los colores de Centro Saber y los tonos verdes/azules administrativos. La guía independiente de plantillas carga la copia local de Bootstrap en `public/vendor`; no depende de una CDN.

Los estados y las interacciones siguen bajo control de React para evitar dos controladores sobre los mismos elementos. El carrusel presenta la foto central más grande, dos vistas previas laterales clicables, esquinas redondeadas, fondo oscuro e indicadores inferiores. Con una imagen se muestra solo la principal; con dos, las vistas laterales corresponden a la misma vecina en el recorrido circular. Las imágenes muy anchas se ajustan completas para mantener legibles los logotipos. El diseño móvil conserva el foco central y deja asomar las vistas laterales.

Se comprobaron la compilación, publicación de imagen, tabla de estudiantes y apertura/cierre de edición sin modificar registros, navegación del carrusel, y menús públicos/administrativos en marcos de 390 px (375 px útiles por la barra de desplazamiento), sin desbordamientos horizontales. El marco temporal usado para la revisión se eliminó al terminar.

Solicitudes incorpora filtros combinables de nombre, documento y rango de fechas (zona America/Bogota), aplicados con el botón Aplicar filtros. Puede editarse nombre, documento opcional, contacto, programa y mensaje. Las notas de seguimiento se guardan explícitamente y se muestran a un lado en escritorio, debajo en móvil; tienen un límite de 2.000 caracteres. Se permite quitar una solicitud con confirmación y Deshacer; la eliminación es lógica y conserva el usuario asociado y la auditoría.

Usuarios estudiantiles permite editar los datos personales, contacto, programa y estado académico. La columna Notas aparece inmediatamente después de Estado y admite hasta 500 caracteres, validados tanto en la interfaz como en el servidor. Son observaciones administrativas internas; no cambian las calificaciones ni se entregan al endpoint del portal estudiantil. La edición de una solicitud no modifica automáticamente su usuario. Si se modifica documento/expedición del estudiante, se usan esos nuevos datos para su siguiente ingreso; año/clave no se cambia desde este formulario.

Las columnas nuevas se añaden de forma idempotente a la base H2 existente sin borrar registros. Los permisos de edición, notas, eliminación y restauración son exclusivos de administración y requieren CSRF. Se registran las acciones en auditoría.

Logotipos: archivos adjuntos del usuario, conservados sin edición, en `frontend/public/marca`. Estudiantes: azul, amarillo, fondos claros y tipografía Trebuchet MS/Segoe UI; administración: verde y azul. Las secciones son rutas independientes.

Programas originales, propósito y contacto proceden de `centrosaber.com`, documentados en `portal-estudiantes.md`. Enfermería se añade por petición del usuario; no se atribuyen a la fuente una tarifa, certificación o disponibilidad no verificadas. ICFES tiene su propia sección, formulario de solicitud y enlaces institucionales; las fechas sin año de la página no se presentan como vigentes.

Los enlaces oficiales de ICFES se verificaron en `https://www.icfes.gov.co/`: cronogramas en `https://www.icfes.gov.co/evaluaciones-icfes/cronograma-de-evaluaciones/` y resultados Saber 11 en `https://resultadossaber11.icfes.gov.co/login`. No se realizaron trámites externos.

## Verificación

`npm.cmd run build` y `mvn test`: nueve pruebas aprobadas. Incluyen solicitudes con consentimiento, CSRF, rechazo de credenciales incorrectas, bloqueo de lectura administrativa anónima/estudiantil, creación única transaccional, acceso del estudiante nuevo sin notas ajenas y cierre de sesión.

Ampliación de edición y notas: 12 pruebas aprobadas, incluyendo persistencia de notas, límite de 500/501 caracteres, exclusión de las notas administrativas del portal, eliminación recuperable y conservación del usuario vinculado. En navegador se verificaron los filtros combinados, la exclusión por fecha, edición y guardado de una nota ficticia, eliminación y restauración de la solicitud con sus notas intactas.

Gestión de información: 17 pruebas aprobadas en total. Las nuevas verifican vista previa sin escritura, aislamiento por sesión, protección CSRF, prohibición de exportaciones/importaciones estudiantiles, duplicados, revalidación al confirmar, año por relaciones, libro completo, formatos numéricos y fechas Excel, ceros iniciales y rechazo de fórmulas. En navegador se probó el acceso desde el menú, la descarga anual y una carga ficticia con una fila válida y otra errónea; solo se confirmó la válida.

Prueba en navegador con información ficticia: solicitud de Enfermería, lectura administrativa tras reiniciar el backend, conversión a usuario, ingreso con la cuenta creada y notas vacías sin información de otro estudiante. Se verificaron las notas del usuario inicial y los tres métodos de pago en matrícula. La solicitud y cuenta ficticias quedan disponibles en la base local para revisión.

## Registro, convivencia y fechas (13 de octubre de 2026)

El portal incluye `/registro` con dos formularios externos diferentes: inscripción (`1FAIpQLSe4Eb7DtGo7VTpfMRlwrfEf4GrMhsctWW8uvLjDmWxECUL8yw`) y documentos (`1FAIpQLSdaVqATplzwGEqTBARjY_bN0LE5zgrFNZ_eOUN2toy9FzRCZA`). El primero se verificó como Registro de inscripción 2026; el segundo solicita acceso a Google. No se enviaron respuestas ni se integran automáticamente con las cuentas del portal. Neli (3045822782) y Estiven (3143544501) tienen enlaces telefónicos y de WhatsApp. Administración puede editar los enlaces, nombres y números en Gestión de información → Formularios de registro y contactos.

El catálogo Fechas y eventos permite crear y editar ingresos, clases, inscripciones, ICFES, pagos y otros eventos. Guarda código, año académico/convocatoria, tipo, título, programa, fechas, hora colombiana opcional, lugar, enlace HTTPS, detalle y estado. Solo PUBLICADO se entrega al endpoint público; BORRADOR y CANCELADO quedan privados. El año de convocatoria puede diferir del año de la fecha. El código no cambia al editar. Admite importación con vista previa, plantilla CSV y exportación individual/completa por año. El libro completo ahora contiene once hojas. Los ingresos se muestran en Registro y los eventos del programa ICFES en su pestaña. No se han inventado fechas reales de ingreso.

`/convivencia` muestra una propuesta original de nueve apartados, con descarga PDF e impresión. Es un borrador para revisión y adopción institucional, no un reglamento aprobado. El contenido canónico está en `frontend/public/manuales/convivencia.json`; `tools/build-convivencia.py` genera el PDF de cuatro páginas en `output/pdf` y lo copia a la carpeta pública del frontend. El PDF se renderizó y revisó visualmente en las cuatro páginas.

Verificación final: 23 pruebas del backend aprobadas y `npm.cmd run build` correcto. En navegador se verificaron el guardado de formularios/contactos, las dos URL distintas, los contactos, el manual y un evento ficticio privado → publicado → cancelado. `PRUEBA-AGENDA-2026` queda cancelado en la base local como ejemplo de auditoría y no aparece a los estudiantes.

## Gestión de página

El menú y acceso rápido antes llamado Galería de imágenes ahora se llama Gestión de página. Agrupa las pestañas Imágenes de Sobre nosotros, Fechas y eventos, y Registro y contactos. La agenda y configuración de registro se trasladaron desde Gestión de información; este último mantiene los ocho catálogos académicos. La exportación completa conserva los nueve catálogos, incluida la agenda, y las dos hojas históricas. No se cambiaron los registros, imágenes ni permisos existentes. Las fechas conservan edición, publicación/cancelación, importación con vista previa y exportación por año.

## Calendario académico e ICFES editable

La pestaña pública `/calendario` reúne actividades publicadas por mes de inicio, con filtros por año académico, mes y tipo: ingreso, clase, inscripción, ICFES, pago y otro. Conserva las fechas pasadas para consulta. Gestión de página → Fechas y eventos añade un filtro por tipo que también se aplica a la tabla editable.

Las seis fechas antes escritas directamente en la pantalla ICFES se migran una sola vez a la agenda con códigos estables; el inicio del servidor no sobrescribe cambios administrativos. Los registros ICFES incluyen calendario A/B y etapa (inscripción, citación, aplicación, resultados, pago u otra). La página ICFES y el calendario público leen la misma agenda. Se mantienen las notas de referencia histórica para 2026 y de anuncio por confirmar para 2027. Los plazos de pago/inscripción sin año dejaron de mostrarse como fechas fijas; administración puede añadirlos con la convocatoria correspondiente.

Los eventos Otro pueden tener una imagen propia JPG/PNG: guardar primero el evento, abrir Editar fecha, seleccionar una imagen, revisar su vista previa y pulsar Guardar imagen. Puede reemplazarse o retirarse explícitamente. Las imágenes se validan y recodifican en servidor (5 MB, 16 megapíxeles); persisten en H2 separadas de la galería. Solo el administrador puede gestionarlas con CSRF y auditoría. La consulta pública exige que el evento siga siendo Otro y esté publicado; borradores y cancelados no exponen la imagen. Excel conserva los datos de los eventos, sin incrustar estos archivos binarios.

Se añadieron los enlaces solicitados de material de práctica (okonvirtual.com) y citación (citacion.icfes.edu.co). Verificación: 24 pruebas backend y compilación frontend correctas. En navegador se comprobaron las categorías, año/mes, fechas ICFES, filtro administrativo, selección/vista previa/guardado de imagen y visualización pública. El evento ficticio PRUEBA-AGENDA-2026 volvió a CANCELADO; el evento real existente de salida pedagógica y las fotos de Sobre nosotros se conservaron.

## Eventos, sedes, cursos y solicitudes

Fechas y eventos muestra tablas separadas por tipo. ICFES se subdivide en calendario A (azul), B (morado) y sin calendario; Otros eventos tiene su tabla. Eliminar evento solicita confirmación y realiza eliminación lógica: lo retira del portal, listados y exportaciones, conservando datos e imagen para restaurarlos desde Eventos eliminados. Restaurar conserva el estado anterior, por lo que un evento previamente publicado vuelve a estar visible. Las claves eliminadas siguen reservadas para evitar duplicados; el inicio del servidor no recrea registros eliminados. La tabla de información completa abre Ver ficha a la derecha en escritorio y debajo en pantallas pequeñas.

El calendario académico público excluye el tipo Pagos. La sección ICFES contiene los tres videos y el canal enviados por el usuario, con reproductor YouTube de carga bajo demanda y enlaces alternativos. Sus títulos se verificaron en los reproductores: seminarios de Centro Saber del 6 de noviembre, 16 y 23 de octubre de 2020. Se identifican como recursos de apoyo históricos.

La oferta pública y el selector Quiero estudiar contienen siete opciones sin año: Vinculación menor (CJK), Preparación validación Bachillerato 299 (ADULTOS), Auxiliar Administrativo (Curso preparación), Primera Infancia (Curso preparación), Enfermería (Preuniversitario), PRE ICFES · Preparación y Refuerzo escolar. Se mantienen los identificadores de programas anteriores para no alterar sus registros históricos. Las solicitudes nuevas guardan nombres, apellidos, ciudad y barrio por separado, junto al nombre completo para los listados existentes; administración muestra ciudad y barrio. Las solicitudes previas sin estos campos siguen disponibles.

Contacto muestra tres sedes. Gestión de página → Sedes y contacto permite editar nombre, dirección, teléfono colombiano de diez dígitos, horario, enlace HTTPS de ubicación y foto JPG/PNG con vista previa. La sede 1 conserva la dirección/contacto existentes. Las sedes 2 y 3 y todas las fotos quedan pendientes de completar. No se inventaron nuevas direcciones. Los cambios se guardan explícitamente en H2 y requieren rol administrador, CSRF y auditoría; las imágenes se validan y recodifican.

Verificación final: 27 pruebas backend y compilación frontend correctas. Se comprobaron en navegador las tablas, eliminación recuperable del evento ficticio, ficha lateral, edición/guardado de sede sin cambiar sus datos, las tres sedes públicas, los nuevos campos y siete programas, los tres reproductores YouTube y la ausencia de Pagos en las categorías públicas. PRUEBA-AGENDA-2026 queda eliminado y recuperable; la salida pedagógica real y la galería existente se conservaron.

## Nivelaciones y guías

Debajo del inicio de sesión en `/estudiantes` aparecen la guía general y los siete enlaces enviados: Biología, Tecnología e informática, Ética y religión, Matemáticas, Inglés y Química para segundo semestre, y Cátedra de Paz para ciclos. Se abren en Drive sin copiar archivos al servidor; sus permisos externos no pudieron verificarse con la herramienta web. La sección contiene instrucciones de actividad/revisión y el contacto de WhatsApp de Estiven, leído de la configuración de registro.

Mi espacio estudiantil → Nivelaciones permite solicitar cita con materia, periodo, año académico, motivo, fecha y hora preferidas de Colombia. El servidor toma la identidad de la sesión, exige fecha no pasada y evita duplicar una solicitud pendiente o confirmada de la misma materia, periodo y año. La fecha propuesta no se reserva automáticamente.

Administración dispone de Nivelaciones en el menú y los accesos rápidos, con búsqueda por nombre/documento/materia, filtro de estado y revisión. Puede confirmar cita con docente, fecha, hora y lugar/modalidad, responder, finalizar con resultado o cancelar. Las respuestas y resultados solo se entregan al estudiante dueño de la solicitud. Se guardan en H2 con auditoría y protección CSRF; el resultado no cambia automáticamente las calificaciones. Gestión de página → Fechas y eventos admite el tipo NIVELACION para jornadas públicas generales, separado de las citas personales privadas.

Verificación: 29 pruebas backend y compilación frontend correctas. Se verificaron las guías debajo del ingreso y el flujo estudiante → solicitud pendiente → administración confirma → estudiante consulta respuesta. La cita ficticia de Biología del usuario de prueba quedó CANCELADA, con texto explícito de demostración. No se enviaron documentos ni mensajes a Google Drive o WhatsApp.

## Eliminación de imágenes de galería

Cada tarjeta de Gestión de página → Imágenes de Sobre nosotros tiene Eliminar imagen, con confirmación. La eliminación es lógica: desaparece de la lista principal y del carrusel público, y sus bytes dejan de entregarse por el endpoint público. Imágenes eliminadas permite recuperarla con su estado anterior (publicada o retirada). Quitar de la galería sigue siendo la opción para ocultarla conservándola en la lista principal. La recuperación de una imagen antes publicada respeta el máximo de treinta publicadas. Todas las acciones requieren administrador y CSRF y se auditan; no se alteraron las imágenes existentes durante la verificación de interfaz.

Verificación: 30 pruebas backend aprobadas y compilación frontend correcta. Las pruebas cubren eliminación/recuperación, ocultamiento público, conservación de bytes y estado, y rechazo de operaciones estudiantiles o sin CSRF. En navegador se comprobó la confirmación y su cancelación sin eliminar imágenes del usuario.

## Disponibilidad, eliminación de nivelaciones y registros individuales

Administración → Nivelaciones incluye Fechas disponibles: fecha/hora de Colombia, materia (o todas), docente, lugar/modalidad, capacidad y apertura/cierre de nuevas solicitudes. Las fechas con reservas conservan horario, materia, docente y lugar; la capacidad no se puede reducir por debajo de las reservas. Los estudiantes ven solo fechas futuras, abiertas y con cupos, seleccionadas por materia, y agendan desde Mi espacio → Nivelaciones. La operación bloquea y vuelve a comprobar la fecha, la materia y el cupo en el servidor antes de guardar. Las solicitudes pendientes reservan cupo y siguen requiriendo confirmación administrativa. El enlace externo de Google Forms permanece como opción aparte; sus respuestas no se importan automáticamente.

Cada nivelación administrativa tiene Eliminar nivelación, confirmación y restauración desde Nivelaciones eliminadas. La eliminación lógica la oculta también del estudiante y libera el cupo. Restaurar una solicitud activa verifica duplicados y capacidad. Los registros cancelados/finalizados conservan su historial. Los formularios de fechas y solicitudes requieren administrador o estudiante según corresponda, CSRF y auditoría.

Gestión de información permite Crear uno por uno para estudiantes, docentes, cursos, periodos, matrículas, notas, cobros y pagos, además de la carga XLSX/CSV con mapeo, vista previa y confirmación. Ambos caminos usan las mismas reglas de validación, duplicados y relaciones. Crear un registro de estudiante en el catálogo no crea sus credenciales. Las notas individuales usan escala 0–5; la carga Excel conserva su selector de escala.

Cursos tiene edición de nombre, tipo de programa, duración opcional, descripción y Publicado SI/NO. Los códigos se conservan para mantener referencias. La página inicial y Quiero estudiar leen el catálogo del servidor: crear, editar o publicar un curso actualiza la oferta. Los siete programas previos se incorporaron una vez al catálogo sin inventar duración ni sobrescribir registros existentes; los cursos anteriores sin configuración de publicación no se publican automáticamente. La creación/importación nueva permite publicar explícitamente con SI y la importación usa SI cuando esa columna no se incluye.

Verificación: 33 pruebas backend y compilación frontend aprobadas. Se comprobaron en navegador creación individual de curso, edición reflejada en Quiero estudiar y visibilidad en la página inicial; el curso ficticio curso-demo-manual quedó publicado NO. También se creó una fecha ficticia, se agendó desde el estudiante de prueba, se eliminó la solicitud y se comprobó cupo 0/1. La fecha ficticia del 15 de noviembre quedó cerrada; no se publicaron citas reales ni se enviaron comunicaciones externas.

## Comunicados y programación de eventos
La página principal incluye un carrusel accesible con pausa, navegación manual y movimiento reducido. En Gestión de página → Comunicados, el administrador crea borradores, publica, carga imágenes JPG/PNG con vista previa y elimina o restaura publicaciones. Textos e imágenes quedan en la base local. Feria empresarial y Álbum 2026 conservan los enlaces indicados.

En Fechas y eventos → Editar fecha → Programación y página propia del evento, se editan presentación, lugar, puntos de encuentro, costos, recomendaciones, observaciones, agenda del día y enlace del álbum. El evento y la página deben estar PUBLICADO. Se abre en otra pestaña desde el calendario o un comunicado vinculado. La referencia de la salida del 12 de octubre de 2026 se conserva como evento histórico separado y no cambia eventos existentes. El botón de plantilla carga contenido para revisar antes de guardar.

Los comunicados de nivelaciones consultan las mismas disponibilidades que el espacio estudiantil: fechas futuras, activas y con cupo. La vista pública solo devuelve fecha, hora, materia y lugar; las solicitudes personales permanecen protegidas. El agendamiento se realiza al ingresar al portal.

### Edición sencilla de salidas
En Gestión de página → Fechas y eventos → Editar fecha → Editar lugar, fotos y datos de la salida, se muestran cinco secciones: lugar y fotos, puntos de encuentro, costos y plazos, fecha y recomendaciones. Cada punto tiene nombre, dirección, enlace de Maps y hora; cada valor tiene concepto, importe en COP y fecha límite. Se pueden agregar o quitar filas. La fecha de salida se sincroniza con el inicio del evento en el calendario al guardar. La información existente sigue disponible sin migraciones destructivas.
Las seis fotos del lugar se cargan con vista previa y guardado explícito; se pueden cambiar o retirar. Las fotos de la página solo son públicas si el evento y su programación están publicados. El portal presenta ubicación, fichas de encuentros, valores y fechas, recomendaciones y un carrusel de fotos.
