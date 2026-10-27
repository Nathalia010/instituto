# Publicación temporal en GitHub Pages

Esta publicación es una copia de la parte pública. El proyecto React + Java y su base de datos continúan funcionando como antes en local.

## Preparar o actualizar la copia

Con el servidor local activo, ejecuta desde la raíz:

```powershell
node tools/build-pages-temporary.mjs
```

El comando consulta exclusivamente los endpoints públicos, copia las imágenes publicadas y genera `publicacion-temporal/`. No exporta estudiantes, usuarios, credenciales reales, notas, pagos, solicitudes ni auditorías de la base de datos. Los contactos institucionales y las fechas publicadas sí se incluyen. Revisa la copia antes de subirla.

La dirección predeterminada es `https://nathalia010.github.io/instituto/`. Para otro repositorio, cambia `PAGES_TEMPORARY_BASE` antes de generar la copia.

## Publicar con la configuración de la captura

1. Sube los cambios al repositorio con GitHub Desktop o Git. Incluye `index.html`, `.nojekyll` y toda la carpeta `publicacion-temporal/`. No subas `.data`, `.env`, `.tools` ni archivos privados.
2. En Settings → Pages conserva **Deploy from a branch → main → /(root)** y guarda si cambiaste la selección.
3. Espera a que termine la publicación de Pages y abre la dirección del sitio.

La portada raíz (`index.html`) es la puerta temporal: conduce a la copia estática. Las páginas internas usan `?route=...`, así se pueden abrir, compartir y recargar bajo la carpeta del repositorio sin errores 404.

## Qué funciona en esta copia

Programas, comunicados, imágenes, sedes, contactos, calendario publicado, ICFES, videos, guías, enlaces de formularios externos, manual de convivencia y programación publicada de eventos. El contenido corresponde al momento de ejecutar el comando; los cambios posteriores en administración requieren volver a generar y subir la copia.

La demo ahora permite entrar al panel administrativo y al espacio estudiantil con cuentas ficticias. Los datos y cambios de prueba se guardan en localStorage de ese navegador; la sesión simulada se mantiene en sessionStorage. No existe autenticación real ni conexión con Java en esta copia. No ingreses información personal real.

- Administrador: **adminN**, clave **1234987A**.
- Estudiante: documento **1000000001**, expedición **2020-01-15**, año/clave **2026**.

Puedes explorar los módulos, crear registros individuales, editar estudiantes y notas, activar o desactivar cuentas ficticias y probar solicitudes de nivelación. Algunas opciones de edición de la página no están implementadas y muestran un aviso. La carga de archivos y las exportaciones administrativas están deshabilitadas. Las plantillas estáticas siguen disponibles.

Usa **Accesos de prueba y funciones disponibles → Restablecer demostración** para volver a los ejemplos iniciales. Los cambios no se comparten entre personas ni computadores. Para actualizar GitHub Pages, vuelve a subir la carpeta generada completa, incluido su index.html y los nuevos assets.

Verificación de la lógica de demostración:

```powershell
node tools/test-pages-demo.mjs
```

## Retirar la puerta y volver al proyecto habitual

Elimina del repositorio **`index.html`**, **`.nojekyll`** (si fue creado solo para esta vista) y **`publicacion-temporal/`**, y sube esa eliminación. Despublica el sitio en Settings → Pages si quieres retirar por completo su acceso público.

Para volver a trabajar localmente basta con iniciar los servidores habituales; no necesitas deshacer código. El adaptador se activa únicamente con la configuración `frontend/pages-temporary/vite.config.ts`; el comando normal `npm --prefix frontend run build` conserva el comportamiento original.
