// Adapter used only by the separate GitHub Pages build.
let base='/';
let snapshot:Promise<Record<string,unknown>>|undefined;
export function setupTemporaryPages(root:string){
 base=root;
 // Pages has no server-side routing. Preserve every route in the query string.
 const rewrite=(node:Element)=>{
  const links=node.matches('a[href]')?[node]:Array.from(node.querySelectorAll('a[href]'));
  for(const link of links){const href=link.getAttribute('href')||'';if(!href.startsWith('/')||href.startsWith('//')||href.startsWith(base))continue;
   if(href.startsWith('/api/'))continue;
   if(/^\/(marca|comunicados|manuales|plantillas|vendor)\//.test(href)){link.setAttribute('href',base+href.slice(1));continue;}
   const url=new URL(href,location.origin);const query=new URLSearchParams(url.search);query.set('route',url.pathname);link.setAttribute('href',base+'?'+query.toString()+url.hash);
  }
 };
 new MutationObserver(records=>{for(const record of records){if(record.type==='attributes'&&record.target instanceof Element)rewrite(record.target);for(const node of record.addedNodes)if(node instanceof Element)rewrite(node);}}).observe(document.documentElement,{subtree:true,childList:true,attributes:true,attributeFilter:['href']});
 rewrite(document.documentElement);
}
export async function pagesApi<T>(path:string,body?:unknown):Promise<T>{
 if(body!==undefined||path.startsWith('/admin')||path.startsWith('/student')||path.startsWith('/login'))throw new Error('Esta vista temporal no tiene acceso al servidor. Comunícate con la institución.');
 snapshot??=fetch(base+'contenido-publico.json').then(async response=>{if(!response.ok)throw new Error('No se pudo cargar la vista pública.');return response.json();});
 const data=await snapshot;
 if(!(path in data))throw new Error('Este contenido no está disponible en la vista temporal.');
 return structuredClone(data[path]) as T;
}
