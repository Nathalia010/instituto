import { useState, type ReactNode } from 'react';
import { Brand } from './StudentPortal';
export {default as AdminDashboard} from './Dashboard';
import './admin.css';
const navigation = [
    { id: 'dashboard', label: 'Dashboard', icon: 'dashboard' },
    { id: 'solicitudes', label: 'Solicitudes', icon: 'mail' },
    { id: 'usuarios', label: 'Usuarios estudiantiles', icon: 'users' },
    { id: 'cursos', label: 'Cursos', icon: 'file' },
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
    return <div className={`cs-site cs-admin admin-layout section-${section}`}>
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
