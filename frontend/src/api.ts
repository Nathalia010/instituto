export async function api<T>(path:string,body?:unknown):Promise<T>{
 const headers:Record<string,string>={};
 if(body!==undefined){const r=await fetch('/api/demo/csrf',{credentials:'same-origin'});if(!r.ok)throw new Error('No se pudo conectar con el servidor local.');const c=await r.json();headers[c.header]=c.token;headers['Content-Type']='application/json';}
 const r=await fetch(`/api/demo${path}`,{method:body===undefined?'GET':'POST',headers,credentials:'same-origin',body:body===undefined?undefined:JSON.stringify(body)});
 if(!r.ok){const errors:Record<number,string>={400:'Revisa los campos del formulario.',401:'Credenciales incorrectas o sesión vencida.',403:'Acceso no autorizado. Vuelve a ingresar.',409:'Ya existe un registro o una solicitud activa con estos datos.',429:'Demasiados intentos. Espera un minuto.'};throw new Error(errors[r.status]||'No se pudo completar la operación. Comprueba que el servidor local está activo.');}return r.json();
}
export type Student={id:string;name:string;email:string;phone:string;program:string;document:string;issued:string;academic_year:number;academic_status:string;admin_notes?:string;notes:{subject:string;grade:number;year:number}[];movements:{concept:string;type:string;amount:number;date:string}[];balance:number};
export const activeProgramNames:Record<string,string>={vinculacion:'Vinculación menor (CJK)',validacion:'Preparación validación Bachillerato 299 (ADULTOS)',administrativo:'Auxiliar Administrativo (Curso preparación)',infancia:'Primera Infancia (Curso preparación)',enfermeria:'Enfermería (Preuniversitario)',icfes:'PRE ICFES · Preparación',refuerzo:'Refuerzo escolar'};
export const programNames:Record<string,string>={...activeProgramNames,preparacion:'Preparación en educación informal',preuniversitario:'Preparación preuniversitaria'};
export const statusNames:Record<string,string>={EN_CURSO:'En curso',FINALIZADO:'Finalizado',NO_FINALIZADO:'No finalizado',RETIRADO:'Retirado',PENDIENTE_EVALUACION:'Pendiente de evaluación',PENDIENTE_MATRICULA:'Pendiente de matrícula'};

