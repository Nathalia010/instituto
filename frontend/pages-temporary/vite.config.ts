import {defineConfig} from 'vite';
import react from '@vitejs/plugin-react';
import path from 'node:path';
const base=process.env.PAGES_TEMPORARY_BASE||'/instituto/publicacion-temporal/';
export default defineConfig({
 root:path.resolve(import.meta.dirname,'..'),base,plugins:[{
  name:'temporary-pages-adapter',enforce:'pre',
  transform(source,id){
   if(!id.includes('/src/')&&!id.includes('\\src\\'))return;
   const name=id.replaceAll('\\','/');
   if(name.endsWith('/api.ts'))source=source.replace(/export async function api<T>[\s\S]*?\r?\n}\r?\n/,"export {pagesApi as api} from './pages-temporary/runtime';\n");
   if(name.endsWith('/main.tsx'))source="import TemporaryDemo from './pages-temporary/TemporaryApp';\nimport {setupTemporaryPages} from './pages-temporary/runtime';\nsetupTemporaryPages("+JSON.stringify(base)+");\n"+source.replace('<App />','<TemporaryDemo/>');
   source=source.replaceAll('window.location.pathname',"(new URLSearchParams(window.location.search).get('route')||'/')");
   // Root image/file references must resolve under the repository subdirectory.
   source=source.replace(/(["'`])\/(marca|comunicados|manuales|plantillas|vendor)\//g,(_,quote,folder)=>quote+base+folder+'/');
   return source;
  }
 },react()],build:{outDir:path.resolve(import.meta.dirname,'../../publicacion-temporal'),emptyOutDir:true}});
