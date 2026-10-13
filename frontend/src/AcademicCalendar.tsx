import { useEffect, useState } from 'react';
import { api, programNames } from './api';
import type { CalendarEvent } from './RegistrationPages';
import './academic-calendar.css';

export const eventTypes: Record<string,string> = {INGRESO:'Inicio de clases',CLASE:'Clases',INSCRIPCION:'Inscripciones',ICFES:'ICFES',NIVELACION:'Nivelaciones',PAGO:'Pagos',OTRO:'Otros eventos'};
export const stageNames: Record<string,string> = {INSCRIPCION:'Inscripción',CITACION:'Publicación de citaciones',APLICACION:'Aplicación del examen',RESULTADOS:'Resultados',PAGO:'Pago',OTRO:'Otra etapa'};
export const formatEventDate = (s:string) => new Intl.DateTimeFormat('es-CO',{dateStyle:'long'}).format(new Date(s+'T12:00:00'));
function useAgenda(){
    const [events,setEvents]=useState<CalendarEvent[]>([]),[error,setError]=useState('');
    useEffect(()=>{let active=true;const load=()=>api<CalendarEvent[]>('/agenda').then(rows=>{if(active){setEvents(rows);setError('');}}).catch(()=>{if(active)setError('No fue posible cargar el calendario. Intenta nuevamente.');});load();window.addEventListener('focus',load);return()=>{active=false;window.removeEventListener('focus',load);};},[]);
    return {events,error};
}
function EventCard({event}:{event:CalendarEvent}){
    return <article className={`card calendar-event type-${event.tipo_evento} icfes-calendar-${event.calendario||''}`}>
        {event.tipo_evento==='OTRO'&&event.imagen_url&&<img src={event.imagen_url} alt={event.titulo} className="calendar-event-image" loading="lazy"/>}
        <div className="card-body"><span className="calendar-type">{eventTypes[event.tipo_evento]}{event.calendario?` · Calendario ${event.calendario}`:''}</span><h3>{event.titulo}</h3>
        <p className="calendar-date">{formatEventDate(event.fecha_inicio)}{event.fecha_fin&&event.fecha_fin!==event.fecha_inicio?` al ${formatEventDate(event.fecha_fin)}`:''}{event.hora&&<><br/>{event.hora} · Hora de Colombia</>}</p>
        <p className="small">Año académico {event.anio} · {event.programa&&event.programa!=='todos'?programNames[event.programa]:'Todos los programas'}</p>
        {event.lugar&&<p>{event.lugar}</p>}{event.detalle&&<p className="calendar-detail">{event.detalle}</p>}{event.programacion_url&&<a className="btn btn-primary me-2" href={event.programacion_url} target="_blank" rel="noreferrer">Ver programación ↗</a>}{event.enlace&&<a className="btn btn-outline-primary" href={event.enlace} target="_blank" rel="noreferrer">Información del evento ↗</a>}</div>
    </article>;
}
export default function AcademicCalendar(){
    const {events,error}=useAgenda();const [year,setYear]=useState(''),[type,setType]=useState(''),[month,setMonth]=useState('');
    const years=[...new Set(events.map(e=>e.anio))].sort().reverse();
    const shown=events.filter(e=>e.tipo_evento!=='PAGO'&&(!year||e.anio===year)&&(!type||e.tipo_evento===type)&&(!month||e.fecha_inicio.slice(5,7)===month));
    const months=[...new Set(shown.map(e=>e.fecha_inicio.slice(0,7)))].sort();
    return <div className="institution-page academic-calendar"><p className="cs-eyebrow">ORGANIZA TU PRÓXIMO PASO</p><h1>Calendario <em>académico.</em></h1><p>Encuentra las fechas de ingreso, clases, ICFES y actividades de Centro Saber. Selecciona un año y el tipo de actividad que necesitas.</p>
        <div className="calendar-filters row g-3"><label className="form-label col-md-6">Año académico<select className="form-select" value={year} onChange={e=>setYear(e.target.value)}><option value="">Todos los años</option>{years.map(y=><option key={y}>{y}</option>)}</select></label><label className="form-label col-md-6">Mes de inicio<select className="form-select" value={month} onChange={e=>setMonth(e.target.value)}><option value="">Todos los meses</option>{Array.from({length:12},(_,i)=><option key={i} value={String(i+1).padStart(2,'0')}>{new Intl.DateTimeFormat('es-CO',{month:'long'}).format(new Date(2026,i,1))}</option>)}</select></label></div>
        <nav className="calendar-categories" aria-label="Tipos de actividad">{[['','Todas las actividades'],...Object.entries(eventTypes).filter(([id])=>id!=='PAGO')].map(([id,label])=><button key={id} className={`btn ${type===id?'btn-primary':'btn-outline-primary'}`} aria-pressed={type===id} onClick={()=>setType(id)}>{label}</button>)}</nav>
        {error&&<p role="alert" className="alert alert-danger">{error}</p>}<p className="small">{shown.length} {shown.length === 1 ? 'actividad' : 'actividades'} · Ordenadas por fecha de inicio. Las fechas pasadas se conservan para consulta.</p>
        {!shown.length&&!error&&<p className="alert alert-info">No hay actividades publicadas para esta selección.</p>}
        {months.map(m=><section key={m} className="calendar-month"><h2>{new Intl.DateTimeFormat('es-CO',{month:'long',year:'numeric'}).format(new Date(m+'-01T12:00:00'))}</h2><div className="row g-3">{shown.filter(e=>e.fecha_inicio.startsWith(m)).map(e=><div className="col-md-6" key={e.codigo_evento}><EventCard event={e}/></div>)}</div></section>)}
    </div>;
}
export function IcfesCalendar(){
    const {events,error}=useAgenda();const [year,setYear]=useState('');const relevant=events.filter(e=>e.tipo_evento==='ICFES');
    const years=[...new Set(relevant.map(e=>e.anio))].sort().reverse();const shown=relevant.filter(e=>!year||e.anio===year);const groups=[...new Set(shown.map(e=>`${e.anio}|${e.calendario||''}`))].sort();
    return <section className="icfes-dates"><div className="cs-section-head"><div><p className="cs-eyebrow">DESPUÉS DE LA INSCRIPCIÓN</p><h2>Calendarios y fechas ICFES</h2></div><label className="form-label">Año de convocatoria<select className="form-select" value={year} onChange={e=>setYear(e.target.value)}><option value="">Todos los años</option>{years.map(y=><option key={y}>{y}</option>)}</select></label></div>
        {error&&<p className="alert alert-warning">{error}</p>}{!shown.length&&!error&&<p className="alert alert-info">No hay fechas ICFES publicadas para esta convocatoria.</p>}<div className="icfes-calendar-grid d-grid">{groups.map(group=>{const [y,c]=group.split('|');return <article className={`card icfes-calendar-${c}`} key={group}><h3>{c?`Calendario ${c}`:'Otras fechas ICFES'} · {y}</h3><dl>{shown.filter(e=>e.anio===y&&(e.calendario||'')===c).map(e=><div key={e.codigo_evento}><dt>{e.titulo||stageNames[e.etapa]}</dt><dd>{formatEventDate(e.fecha_inicio)}{e.hora&&` · ${e.hora}`}</dd>{e.detalle&&<p className="small">{e.detalle}</p>}</div>)}</dl></article>;})}</div><a href="/calendario" className="cs-outline btn btn-outline-primary mt-3">Ver calendario académico completo →</a>
    </section>;
}
