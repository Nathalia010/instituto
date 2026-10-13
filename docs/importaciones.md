# Contrato del importador y de las plantillas

Descargas existentes: libro XLSX con ocho hojas y ocho CSV individuales en `frontend/public/plantillas`. Contienen encabezados y filas vacías, sin estudiantes de ejemplo. La guía explica los formatos. El importador se desarrollará en la etapa 5.

## Flujo previsto

1. Administrador elige módulo, archivo y hoja. Aceptar `.xlsx` y `.csv`, máximo inicial 5 MB y 2.000 filas por operación, ajustable tras medir. Verificar tipo real y límites de descompresión del XLSX; rechazar XLS, XLSM, macros y fórmulas en campos importables.
2. Leer encabezados y proponer correspondencias. El administrador puede relacionar `Nombre completo` o `Nombres y apellidos` a un campo compatible. No separar nombres automáticamente sin confirmación.
3. Validar tipos, referencias, obligatoriedad, duplicados y conflictos. Ignorar únicamente filas completamente vacías. Mostrar las filas válidas, errores y consecuencias propuestas. Ninguna nota o pago se guarda al cargar el archivo.
4. Guardar una previsualización temporal ligada al usuario, hash del archivo, mapeo y caducidad. No exponerla a otros administradores. Errores descargables con fila, campo y mensaje, neutralizando fórmulas CSV que empiezan con `=`, `+`, `-`, `@` y caracteres de control.
5. Administrador confirma la selección de filas válidas. Revalidar referencias y duplicados en base de datos, adquirir los bloqueos necesarios y usar una transacción. Una restricción incumplida revierte toda la selección. La previsualización no garantiza que la base siga igual.
6. Registrar una única confirmación idempotente por importación y su resultado en auditoría. Dos solicitudes concurrentes no duplican pagos. Ante fallo conservar informe y registrar el fallo fuera de la transacción revertida.

No hacer UPSERT automático de notas ni pagos. Estudiantes existentes: resolver explícitamente si se vinculan o se omiten; cambios de contacto separados y auditados. Notas existentes se consideran conflicto y requieren una operación específica con motivo. Cada pago tiene referencia estable y única; reutilizar la referencia del movimiento de origen para detectar cargas repetidas.

## Dependencias

Preparar estudiantes/docentes, cursos y periodos; crear ofertas y asignaciones; preparar matrículas; configurar evaluaciones y conceptos de pago; preparar notas y cobros; finalmente pagos. Las plantillas no crean usuarios, contraseñas, asignaciones ni escalas. Esos elementos se configurarán en el panel autorizado.

## Validación

Documento como texto, manteniendo ceros iniciales. Correo con sintaxis válida y límite de longitud. Fechas ISO en CSV y fechas de Excel en XLSX. Notas decimales dentro de la escala de la evaluación. Moneda COP con máximo dos decimales y valor positivo; sin símbolos ni separadores de miles en CSV. Año y grupo deben resolver una oferta concreta, no un curso ambiguo. Mapeos y rangos validables en el servidor aunque Excel permita rellenar valores incorrectos.
