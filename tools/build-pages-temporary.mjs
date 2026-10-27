import fs from 'node:fs/promises';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {createHash} from 'node:crypto';
import {execFileSync} from 'node:child_process';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const base=process.env.PAGES_TEMPORARY_BASE||'/instituto/publicacion-temporal/';
if(!/^\/(?:[A-Za-z0-9_-]+\/)+$/.test(base))throw new Error('PAGES_TEMPORARY_BASE debe ser una ruta como /instituto/publicacion-temporal/.');
const output=path.join(root,'publicacion-temporal'),cache=path.join(root,'.tools/pages-temporary');
try{await fs.access(output);await fs.access(path.join(output,'.centro-saber-temporal'));}catch(error){if(await fs.stat(output).catch(()=>null))throw new Error('La carpeta publicacion-temporal ya existe sin identificador. No se sobrescribirá.');}
const door=path.join(root,'index.html');
try{if(!(await fs.readFile(door,'utf8')).includes('CENTRO-SABER-PUERTA-TEMPORAL'))throw new Error('Ya existe un index.html ajeno a la vista temporal.');}catch(error){if(error.code!=='ENOENT')throw error;}
await fs.mkdir(path.join(cache,'preview-media'),{recursive:true});
const snapshot={};
async function get(endpoint){const response=await fetch('http://127.0.0.1:8080/api/demo'+endpoint);if(!response.ok)throw new Error(`No se pudo consultar el contenido público ${endpoint}: ${response.status}`);return response.json();}
// Strict public allowlist: never authenticate or export student/admin records.
for(const endpoint of ['/programs','/home/appearance','/announcements','/gallery','/sites','/registration/settings','/agenda','/leveling-dates'])snapshot[endpoint]=await get(endpoint);
const eventCodes=new Set([...snapshot['/agenda'].filter(e=>e.programacion_url?.startsWith('/eventos/')).map(e=>decodeURIComponent(e.programacion_url.split('/')[2])),...snapshot['/announcements'].map(e=>e.event)].filter(Boolean));
for(const code of eventCodes){const endpoint='/event-pages/'+encodeURIComponent(code);const response=await fetch('http://127.0.0.1:8080/api/demo'+endpoint);if([403,404].includes(response.status))continue;if(!response.ok)throw new Error('No se pudo exportar '+endpoint);snapshot[endpoint]=await response.json();}
const images=new Map();
async function convert(value){
 if(Array.isArray(value))return Promise.all(value.map(convert));
 if(value&&typeof value==='object')return Object.fromEntries(await Promise.all(Object.entries(value).map(async([key,item])=>[key,await convert(item)])));
 if(typeof value!=='string')return value;
 if(value.startsWith('[')||value.startsWith('{')){try{return JSON.stringify(await convert(JSON.parse(value)));}catch(error){if(!(error instanceof SyntaxError))throw error;}}
 if(/^\/(marca|comunicados|manuales|plantillas|vendor)\//.test(value))return base+value.slice(1);
 if(value.startsWith('/api/')){
  if(!/^\/api\/demo\/(?:gallery\/images\/|sites\/images\/|agenda\/images\/|announcements\/images\/|event-pages\/[^/]+\/photos\/|page-images\/)/.test(value))throw new Error('Recurso fuera de la lista pública: '+value);
  if(!images.has(value))images.set(value,(async()=>{const response=await fetch('http://127.0.0.1:8080'+value);if(!response.ok)throw new Error('No se pudo exportar una imagen pública.');const type=response.headers.get('content-type')||'';if(!['image/png','image/jpeg'].includes(type.split(';')[0]))throw new Error('Tipo de imagen inesperado.');const file=createHash('sha256').update(value).digest('hex').slice(0,24)+(type.startsWith('image/png')?'.png':'.jpg');await fs.writeFile(path.join(cache,'preview-media',file),Buffer.from(await response.arrayBuffer()));return base+'preview-media/'+file;})());
  return images.get(value);
 }
 return value;
}
const content=await convert(snapshot);
// Field definitions from source code only; no authenticated database export.
const catalogSource=await fs.readFile(path.join(root,'backend/src/main/java/co/educontrol/demo/DataCatalog.java'),'utf8');
content['/demo/catalog']=[...catalogSource.matchAll(/def\("([^"]+)","([^"]+)",(true|false),"([^"]*)","([^"]*)"\)/g)].map(([,id,label,annual,fields,required])=>({id,label,annual:annual==='true',fields:fields.split(','),required:required.split(',')}));
execFileSync(process.execPath,[path.join(root,'frontend/node_modules/vite/bin/vite.js'),'build','--config',path.join(root,'frontend/pages-temporary/vite.config.ts')],{cwd:path.join(root,'frontend'),stdio:'inherit',env:{...process.env,PAGES_TEMPORARY_BASE:base}});
await fs.writeFile(path.join(output,'contenido-publico.json'),JSON.stringify(content,null,2));
await fs.mkdir(path.join(output,'preview-media'),{recursive:true});
for(const image of images.values()){const file=path.posix.basename(await image);await fs.copyFile(path.join(cache,'preview-media',file),path.join(output,'preview-media',file));}
await fs.writeFile(path.join(output,'.centro-saber-temporal'),'Generated public preview. No private records.\n');
await fs.writeFile(path.join(root,'.nojekyll'),'');
await fs.writeFile(door,'<!doctype html>\n<!-- CENTRO-SABER-PUERTA-TEMPORAL: elimina este archivo para retirar la entrada. -->\n<html lang="es"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Centro Saber</title><meta http-equiv="refresh" content="0;url=./publicacion-temporal/"></head><body><a href="./publicacion-temporal/">Entrar a Centro Saber</a></body></html>\n');
console.log('Vista temporal lista en publicacion-temporal/. Puerta: index.html. La aplicación local no cambió.');
