import { useState, type ReactNode } from 'react';
import { Brand } from './StudentPortal';
import { programNames, type Student } from './api';
import './admin.css';
const navigation = [
    { id: 'dashboard', label: 'Dashboard', icon: 'dashboard' },
    { id: 'solicitudes', label: 'Solicitudes', icon: 'mail' },
    { id: 'usuarios', label: 'Usuarios estudiantiles', icon: 'users' },
    { id: 'informacion', label: 'Gestión de información', icon: 'file' },
    { id: 'nivelaciones', label: 'Nivelaciones', icon: 'file' },
    { id: 'pagina', label: 'Gestión de página', icon: 'file' },
    { id: 'plantillas', label: 'Plantillas', icon: 'file' },
];
export function AdminIcon({ name }: {
    name: string;
}) {
    return <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    {name === 'dashboard' ? <><rect x="3" y="3" width="7" height="7" rx="1.5"/><rect x="14" y="3" width="7" height="7" rx="1.5"/><rect x="3" y="14" width="7" height="7" rx="1.5"/><rect x="14" y="14" width="7" height="7" rx="1.5"/></> : name === 'mail' ? <><rect x="3" y="5" width="18" height="14" rx="2"/><path d="m3 6 9 7 9-7"/></> : name === 'users' ? <><circle cx="9" cy="8" r="3"/><path d="M3 21v-2a6 6 0 0 1 12 0v2M17 5a3 3 0 0 1 0 6M18 15a5 5 0 0 1 3 4v2"/></> : name === 'file' ? <><path d="M14 3H5v18h14V8l-5-5Z"/><path d="M14 3v5h5M8 12h8M8 16h5"/></> : <><circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/></>}
  </svg>;
}
export default function AdminShell({ section, onNavigate, onLogout, pending, children }: {
    section: string;
    onNavigate: (section: string) => void;
    onLogout: () => void;
    pending: number;
    children: ReactNode;
}) {
    const [open, setOpen] = useState(false);
    const title = navigation.find(item => item.id === section)?.label || 'Dashboard';
    return <div className="cs-site cs-admin admin-layout">
    <a className="skip" href="#admin-content">Ir al contenido administrativo</a>
    <aside className={`admin-sidebar ${open ? 'is-open' : ''}`} id="admin-sidebar">
      <div className="admin-brand"><Brand /></div>
      <div className="admin-side-heading"><span className="admin-online"/> PANEL ADMINISTRATIVO</div>
      <nav aria-label="Menú administrativo" className="nav">{navigation.map(item => <button key={item.id} aria-current={section === item.id ? 'page' : undefined} onClick={() => { onNavigate(item.id); setOpen(false); }} className="btn"><AdminIcon name={item.icon}/><span>{item.label}</span>{item.id === 'solicitudes' && pending > 0 && <small aria-label={`${pending} solicitudes pendientes`}>{pending}</small>}</button>)}</nav>
      <div className="admin-side-help"><span>Todo en un solo lugar</span><p>Organiza las solicitudes y acompaña a tus estudiantes.</p></div>
      <div className="admin-side-bottom"><a href="/">Portal estudiantil <span aria-hidden="true">↗</span></a><div className="admin-user"><span className="admin-avatar">AN</span><div><strong>adminN</strong><small>Administrador</small></div></div><button onClick={onLogout} className="btn">Cerrar sesión <span aria-hidden="true">→</span></button></div>
    </aside>
    <div className="admin-workspace"><header className="admin-topbar d-flex align-items-center"><div><button aria-expanded={open} aria-controls="admin-sidebar" onClick={() => setOpen(!open)} className="admin-menu-toggle btn">{open ? 'Cerrar menú' : 'Menú'}</button><span>Administración <span className="admin-crumb">/ {title}</span></span></div><div className="admin-topbar-right"><span className="admin-local">Demostración local</span><span className="admin-avatar">AN</span></div></header>{children}</div>
  </div>;
}
export type DashboardLead = {
    id: string;
    name: string;
    program: string;
    student_id: string | null;
    created_at: string;
};
function DashboardShortcuts({ onNavigate }: {
    onNavigate: (section: string) => void;
}) {
    return <section className="admin-shortcuts" aria-label="Accesos rápidos"><h2>Accesos rápidos</h2><div>
    <a className="admin-shortcut-download" href="/api/demo/admin/data/export/all?format=xlsx" download>
      <span className="admin-shortcut-icon"><AdminIcon name="file"/></span>
      <span><strong>Descargar Excel completo</strong><small>Toda la información de todos los años en un único archivo Excel.</small></span>
      <span className="admin-shortcut-arrow" aria-hidden="true">↓</span>
    </a>
    {[
            { id: 'solicitudes', title: 'Atender solicitudes', description: 'Consulta los datos de contacto y crea usuarios.', icon: 'mail' },
            { id: 'usuarios', title: 'Usuarios estudiantiles', description: 'Edita las cuentas, estados y notas administrativas.', icon: 'users' },
            { id: 'informacion', title: 'Gestión de información', description: 'Revisa estudiantes, docentes y cursos; importa y descarga por año.', icon: 'file' },
            { id: 'nivelaciones', title: 'Revisar nivelaciones', description: 'Confirma citas, asigna docentes y registra resultados.', icon: 'file' },
            { id: 'pagina', title: 'Gestión de página', description: 'Organiza imágenes, fechas, formularios y contactos del portal.', icon: 'file' },
            { id: 'plantillas', title: 'Descargar plantillas', description: 'Obtén archivos en blanco para preparar la carga masiva.', icon: 'file' },
        ].map(item => <button key={item.id} onClick={() => onNavigate(item.id)} className="btn"><span className="admin-shortcut-icon"><AdminIcon name={item.icon}/></span><span><strong>{item.title}</strong><small>{item.description}</small></span><span className="admin-shortcut-arrow" aria-hidden="true">↗</span></button>)}
  </div></section>;
}
export function AdminDashboard({ leads, students, onNavigate }: {
    leads: DashboardLead[];
    students: Student[];
    onNavigate: (section: string) => void;
}) {
    const pending = leads.filter(lead => !lead.student_id).length;
    const waiting = students.filter(student => student.academic_status === 'PENDIENTE_MATRICULA').length;
    const groups = Object.entries(programNames).map(([id, label]) => ({ id, label, count: leads.filter(lead => lead.program === id).length })).filter(group => group.count > 0);
    const maximum = Math.max(1, ...groups.map(group => group.count));
    return <div className="admin-dashboard">
    <section className="admin-welcome"><div><span className="admin-welcome-label">BIENVENIDO, ADMINISTRADOR</span><h2>Una mirada a tu institución.</h2><p>Revisa lo que está pendiente y continúa desde donde lo necesitas.</p><button onClick={() => onNavigate('solicitudes')} className="btn">{pending > 0 ? 'Revisar solicitudes pendientes' : 'Consultar solicitudes'} <span aria-hidden="true">→</span></button></div><div className="admin-welcome-art" aria-hidden="true"><AdminIcon name="dashboard"/><span>CENTRO SABER</span></div></section>
    <DashboardShortcuts onNavigate={onNavigate}/>
    <div className="admin-metrics d-grid">{[
            { label: 'Solicitudes recibidas', value: leads.length, detail: 'Interesados en los programas', icon: 'mail', section: 'solicitudes' },
            { label: 'Pendientes de usuario', value: pending, detail: 'Solicitudes por revisar', icon: 'clock', section: 'solicitudes' },
            { label: 'Usuarios estudiantiles', value: students.length, detail: 'Cuentas registradas', icon: 'users', section: 'usuarios' },
            { label: 'Pendientes de matrícula', value: waiting, detail: 'Estudiantes por matricular', icon: 'file', section: 'usuarios' },
        ].map(metric => <button key={metric.label} onClick={() => onNavigate(metric.section)} className="admin-metric btn"><div><span>{metric.label}</span><AdminIcon name={metric.icon}/></div><strong>{metric.value}</strong><small>{metric.detail}</small></button>)}</div>
    <div className="admin-dashboard-grid d-grid"><section className="admin-panel"><div className="admin-panel-heading"><h2>Solicitudes recientes</h2><button onClick={() => onNavigate('solicitudes')} className="btn">Ver todas ↗</button></div>{leads.length ? <div className="admin-recent-list">{leads.slice(0, 5).map(lead => <button key={lead.id} onClick={() => onNavigate('solicitudes')} className="btn"><span className="admin-lead-avatar">{lead.name.trim().charAt(0).toUpperCase()}</span><span className="admin-recent-name"><strong>{lead.name}</strong><small>{programNames[lead.program]}</small></span><span className={`admin-request-status ${lead.student_id ? 'complete' : ''}`}>{lead.student_id ? 'Usuario creado' : 'Pendiente'}</span></button>)}</div> : <p className="admin-empty">Todavía no hay solicitudes. Aquí aparecerán los formularios recibidos.</p>}</section>
    <section className="admin-panel"><div className="admin-panel-heading"><h2>Interés por programa</h2><span>Solicitudes</span></div>{groups.length ? <div className="admin-program-interest">{groups.map(group => <div key={group.id}><div><span>{group.label}</span><strong>{group.count}</strong></div><div className="admin-bar" aria-hidden="true"><span style={{ width: `${group.count / maximum * 100}%` }}/></div></div>)}</div> : <p className="admin-empty">La distribución aparecerá al recibir solicitudes.</p>}</section></div>
  </div>;
}

