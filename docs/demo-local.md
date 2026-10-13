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

El dashboard muestra los accesos rápidos debajo de la bienvenida: Descargar Excel completo, Atender solicitudes, Usuarios estudiantiles, Gestión de información y Descargar plantillas. La descarga directa reúne los diez apartados en un único archivo XLSX de todos los años; las descargas por año siguen disponibles en Gestión de información.

El módulo permite consultar y buscar estudiantes, docentes, cursos, periodos, matrículas, notas, cobros y pagos, abrir una ficha y descargar cada catálogo en XLSX o CSV. Se incluyen los estudiantes que ya tienen cuenta. El libro completo contiene diez hojas: los ocho catálogos más las notas y movimientos existentes del portal, conservados aparte porque no tienen los códigos de matrícula/cobro del nuevo catálogo.

El selector ofrece todos los años registrados o uno concreto. Docentes y cursos no tienen año y se exportan completos. Matrículas toman el año del periodo; notas y cobros el de su matrícula; pagos el de su cobro. Estudiantes también se seleccionan por matrículas del año consultado. Las notas históricas del portal usan su año académico y sus movimientos la fecha del movimiento.

La carga admite CSV UTF-8 con separador coma, punto y coma o tabulación, y Excel XLSX con selección de hoja. Límite: 5 MB, 2.000 filas y 100 columnas. Se relacionan las columnas antes de previsualizar. El servidor valida campos, documentos, fechas, escala de notas, importes, referencias y duplicados. No admite fórmulas ni macros. Los identificadores se exportan como texto para conservar ceros iniciales; el CSV neutraliza fórmulas al abrirse en una hoja de cálculo.

No se guardan registros hasta confirmar la vista previa. La confirmación vuelve a validar, guarda solo las filas válidas en una transacción y registra auditoría. Las filas existentes nunca se sobrescriben. La vista previa vence en 15 minutos y pertenece a la sesión que la creó. Puede descargarse el listado de errores. Orden de carga: estudiantes/docentes, cursos, periodos, matrículas, notas/cobros, pagos. Los pagos importados quedan EN_REVISION.

Los registros importados persisten en la base H2 de esta demostración para consulta y exportación. No crean credenciales automáticamente ni sustituyen todavía las notas/movimientos del portal estudiantil. La creación de cuentas sigue en Usuarios estudiantiles desde Solicitudes. La conexión de estos catálogos al portal y su migración a PostgreSQL son trabajo posterior.

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
