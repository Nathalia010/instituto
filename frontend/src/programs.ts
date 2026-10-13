import { useEffect, useState } from 'react';
import { api, programNames } from './api';
export type PublicProgram={id:string;name:string;description:string;hours:string};
export function usePrograms(){
 const [programs,setPrograms]=useState<PublicProgram[]>([]),[error,setError]=useState('');
 useEffect(()=>{let active=true;const load=()=>api<PublicProgram[]>('/programs').then(rows=>{if(active){for(const row of rows)programNames[row.id]=row.name;setPrograms(rows);setError('');}}).catch(()=>{if(active)setError('No fue posible cargar los cursos. Intenta nuevamente.');});load();window.addEventListener('focus',load);return()=>{active=false;window.removeEventListener('focus',load);};},[]);return {programs,error};
}
