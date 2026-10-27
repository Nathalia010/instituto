# Publicación temporal en GitHub Pages

Esta publicación es una copia de la parte pública. El proyecto React + Java y su base de datos continúan funcionando como antes en local.

## Preparar o actualizar la copia

Con el servidor local activo, ejecuta desde la raíz:

```powershell
node tools/build-pages-temporary.mjs
```

El comando consulta exclusivamente los endpoints públicos, copia las imágenes publicadas y genera `publicacion-temporal/`. No exporta estudiantes, usuarios, credenciales, notas, pagos, solicitudes ni auditorías. Los contactos institucionales y las fechas publicadas sí se incluyen. Revisa la copia antes de subirla.

La dirección predeterminada es `https://nathalia010.github.io/instituto/`. Para otro repositorio, cambia `PAGES_TEMPORARY_BASE` antes de generar la copia.

## Publicar con la configuración de la captura

1. Sube los cambios al repositorio con GitHub Desktop o Git. Incluye `index.html`, `.nojekyll` y toda la carpeta `publicacion-temporal/`. No subas `.data`, `.env`, `.tools` ni archivos privados.
2. En Settings → Pages conserva **Deploy from a branch → main → /(root)** y guarda si cambiaste la selección.
3. Espera a que termine la publicación de Pages y abre la dirección del sitio.

La portada raíz (`index.html`) es la puerta temporal: conduce a la copia estática. Las páginas internas usan `?route=...`, así se pueden abrir, compartir y recargar bajo la carpeta del repositorio sin errores 404.

## Qué funciona en esta copia

Programas, comunicados, imágenes, sedes, contactos, calendario publicado, ICFES, videos, guías, enlaces de formularios externos, manual de convivencia y programación publicada de eventos. El contenido corresponde al momento de ejecutar el comando; los cambios posteriores en administración requieren volver a generar y subir la copia.

Las cuentas, el panel administrativo, las solicitudes internas y el agendamiento no se conectan a Java en GitHub Pages. Esos accesos muestran un aviso y contacto con la institución; no simulan guardados ni solicitan credenciales.

## Retirar la puerta y volver al proyecto habitual

Elimina del repositorio **`index.html`**, **`.nojekyll`** (si fue creado solo para esta vista) y **`publicacion-temporal/`**, y sube esa eliminación. Despublica el sitio en Settings → Pages si quieres retirar por completo su acceso público.

Para volver a trabajar localmente basta con iniciar los servidores habituales; no necesitas deshacer código. El adaptador se activa únicamente con la configuración `frontend/pages-temporary/vite.config.ts`; el comando normal `npm --prefix frontend run build` conserva el comportamiento original.
