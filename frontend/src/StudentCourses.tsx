import { useEffect, useState } from 'react';
import { api, statusNames } from './api';
export type StudentCourse={code:string;name:string;year:number;subjects:string[];academicStatus:string;levelingAllowed:boolean;startDate:string;endDate:string};
export default function StudentCourses(){
 const [courses,setCourses]=useState<StudentCourse[]>([]),[error,setError]=useState(''),[loading,setLoading]=useState(true);
 useEffect(()=>{let alive=true;api<StudentCourse[]>('/student/courses').then(rows=>{if(alive)setCourses(rows);}).catch(e=>{if(alive)setError(e.message);}).finally(()=>{if(alive)setLoading(false);});return()=>{alive=false;};},[]);
 return <><p>Puedes estudiar varios cursos. Aquí se muestran tus programas y matrículas por año.</p>{error&&<p role="alert" className="alert alert-danger">{error}</p>}{loading?<p>Cargando matrículas…</p>:<div className="row g-3 mb-4">{courses.map(c=><article key={`${c.code}-${c.year}`} className="col-lg-6"><div className="card p-4 h-100"><h3>{c.name}</h3><p>{c.year} · {statusNames[c.academicStatus]||c.academicStatus}</p>{c.subjects.length>0&&<p>Materias: {c.subjects.join(', ')}</p>}{c.startDate&&<p>Inicio: {c.startDate}{c.endDate&&` · Fin: ${c.endDate}`}</p>}<small>{c.levelingAllowed?'Puedes solicitar nivelaciones de las materias configuradas en este curso.':'Este curso no está habilitado para nuevas nivelaciones.'}</small></div></article>)}</div>}{!loading&&!courses.length&&<p>No hay cursos asociados a tu matrícula. Consulta con administración.</p>}</>;
}
