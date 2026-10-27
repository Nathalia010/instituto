// Adapter used only by the separate GitHub Pages build.
import {simulate} from './demo';
let base='/';
let snapshot:Promise<Record<string,unknown>>|undefined;
export function setupTemporaryPages(root:string){
 base=root;
 const originalFetch=window.fetch.bind(window);
 window.fetch=(input,init)=>{const url=new URL(typeof input==='string'?input:input instanceof URL?input.href:input.url,location.href);if(url.origin===location.origin&&url.pathname.startsWith('/api/'))return Promise.resolve(new Response(JSON.stringify({message:'La carga de archivos no está disponible en la demo.'}),{status:400,headers:{'Content-Type':'application/json'}}));return originalFetch(input,init);};
 // Pages has no server-side routing. Preserve every route in the query string.
 const rewrite=(node:Element)=>{
  const links=node.matches('a[href]')?[node]:Array.from(node.querySelectorAll('a[href]'));
  for(const link of links){const href=link.getAttribute('href')||'';if(!href.startsWith('/')||href.startsWith('//')||href.startsWith(base))continue;
   if(href.startsWith('/api/')){link.removeAttribute('href');link.setAttribute('aria-disabled','true');link.setAttribute('title','Exportaciones no habilitadas en esta demo.');continue;}
   if(/^\/(marca|comunicados|manuales|plantillas|vendor)\//.test(href)){link.setAttribute('href',base+href.slice(1));continue;}
   const url=new URL(href,location.origin);const query=new URLSearchParams(url.search);query.set('route',url.pathname);link.setAttribute('href',base+'?'+query.toString()+url.hash);
  }
 };
 new MutationObserver(records=>{for(const record of records){if(record.type==='attributes'&&record.target instanceof Element)rewrite(record.target);for(const node of record.addedNodes)if(node instanceof Element)rewrite(node);}}).observe(document.documentElement,{subtree:true,childList:true,attributes:true,attributeFilter:['href']});
 rewrite(document.documentElement);
}
export async function pagesApi<T>(path:string,body?:unknown):Promise<T>{
 snapshot??=fetch(base+'contenido-publico.json').then(async response=>{if(!response.ok)throw new Error('No se pudo cargar la vista pública.');return response.json();});
 const data=await snapshot;
 return simulate(path,body as Record<string,unknown>|undefined,data) as T;
}
