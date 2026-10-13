# Verificación del primer entregable

Pruebas ejecutadas el 12 de octubre de 2026:

- `npm.cmd run build`: TypeScript y compilación Vite correctos.
- `mvn test`: 4 pruebas de Spring Security aprobadas. Health público; lectura administrativa y portal cerrados incluso con roles simulados; escrituras protegidas contra CSRF.
- `node tools/check-database.mjs`: migración aplicada a PostgreSQL embebido PGlite; rechazó documento duplicado, periodo inválido, nota fuera de escala, evaluación de otro grupo y referencia de pago repetida. Comprobó saldo con dos cobros y varios abonos, exclusión de pagos en revisión, reversión, bloqueo de cambios en movimientos/auditoría y rollback ante un duplicado.
- Libro XLSX: ocho hojas inspeccionadas y renderizadas; 200 filas de captura vacías por hoja, encabezados sin truncar, formatos de texto para documentos/códigos y listas permitidas. Ocho CSV UTF-8 con encabezados equivalentes.
- Navegador: búsqueda de Pagos, filtro Financiero, guía y descargas reales de Excel/CSV. Revisiones a 1280 × 900 y 390 × 844; sin desbordamiento horizontal en el centro de plantillas móvil.

## Límites

Ampliación del portal estudiantil: compilación TypeScript/Vite correcta; detalles de validación desplegables, vistas de notas/pagos y navegación con flechas comprobadas en navegador. Menú móvil, contacto y regreso al área administrativa funcionando. A 390 × 844, el ancho del contenido coincide con el del documento sin desbordamiento horizontal. La consulta personal sigue deshabilitada por falta de autenticación.

La prueba de PostgreSQL embebido valida el SQL y sus restricciones; no sustituye arrancar Flyway y Spring Boot contra PostgreSQL 17 externo. Docker/PostgreSQL externo no están disponibles en esta sesión. Tampoco se ha probado el libro en Microsoft Excel de escritorio, solo su estructura y renderizado. No se ha desplegado ni habilitado autenticación o módulos de gestión.

## Repetir la prueba SQL

Desde la raíz:

```powershell
npm.cmd install --prefix .tools/sql --no-save @electric-sql/pglite
node tools/check-database.mjs
```

Los datos de la prueba son ficticios, se guardan en memoria y se descartan al terminar.

## Reproducir plantillas

`tools/build-templates.mjs` usa `@oai/artifact-tool` desde el runtime de artefactos de Codex, conectado a `tools/node_modules`. El sitio utiliza los archivos ya generados y no requiere ese runtime para funcionar. Los campos se definen en `tools/template-schema.json`.
