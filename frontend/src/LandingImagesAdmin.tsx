import {useEffect,useState} from 'react';
import {api} from './api';
import EventImageEditor from './EventImageEditor';
export default function LandingImagesAdmin(){
 const [courses,setCourses]=useState<Record<string,string>[]>([]),[selected,setSelected]=useState(''),[error,setError]=useState('');
 useEffect(()=>{api<Record<string,string>[]>('/admin/data/records/cursos').then(setCourses).catch(e=>setError(e.message));},[]);
 const code=selected||courses[0]?.codigo_curso;
 return <section><h2>Imágenes de la página principal</h2><p>Las imágenes predeterminadas se conservan cuando no has subido una propia. Selecciona una imagen, revisa la vista previa y pulsa Guardar imagen. Quitar imagen recupera la predeterminada.</p>{error&&<p role="alert" className="alert alert-danger">{error}</p>}<EventImageEditor code="hero" endpointOverride="/admin/registration/page-images/home/hero/image" title="Imagen principal de bienvenida" description="Reemplaza el símbolo de la portada con una fotografía JPG o PNG. Recomendado: una imagen horizontal de la comunidad educativa."/><hr className="my-4"/><label className="form-label">Curso de la tarjeta<select className="form-select" value={code||''} onChange={e=>setSelected(e.target.value)}>{courses.map(c=><option key={c.codigo_curso} value={c.codigo_curso}>{c.nombre}</option>)}</select></label>{code&&<EventImageEditor key={code} code={code} endpointOverride={`/admin/registration/page-images/course/${encodeURIComponent(code)}/image`} title="Imagen de la tarjeta del curso" description="El curso conserva su ilustración predeterminada hasta que guardes una fotografía. La imagen solo será pública si el curso está activo y publicado."/>}</section>;
}
