import { useEffect, useState, type FormEvent, type ReactNode } from 'react';
import { api, programNames, type Student } from './api';
import { Brand } from './StudentPortal';
import AdminShell, { AdminDashboard } from './AdminShell';
import DataManagement from './DataManagement';
import CoursesAdmin from './CoursesAdmin';
import { usePrograms } from './programs';
import LevelingsAdmin, {type Leveling} from './Levelings';
import PageManagement from './PageManagement';
import { InquiryRecords, StudentRecords, type Inquiry } from './AdminRecords';
export default function AdminPortal({ templates }: {
    templates: ReactNode;
}) {
    usePrograms();
    const [revision, setRevision] = useState(0);
    const [levelings,setLevelings]=useState<Leveling[]>([]);
    const [logged, setLogged] = useState(false), [ready, setReady] = useState(false), [error, setError] = useState(''), [busy, setBusy] = useState(false), [tab, setTab] = useState('dashboard'), [leads, setLeads] = useState<Inquiry[]>([]), [students, setStudents] = useState<Student[]>([]), [selected, setSelected] = useState<Inquiry | null>(null), [notice, setNotice] = useState('');
    async function load() { const [l, s, n] = await Promise.all([api<Inquiry[]>('/admin/inquiries'), api<Student[]>('/admin/students'), api<Leveling[]>('/admin/levelings')]); setLeads(l); setStudents(s); setLevelings(n); }
    useEffect(() => { api<{
        role: string;
    }>('/session').then(async (s) => { if (s.role === 'ADMINISTRADOR') {
        await load();
        setLogged(true);
    } }).catch(() => { }).finally(() => setReady(true)); }, []);
    async function login(e: FormEvent<HTMLFormElement>) { e.preventDefault(); setBusy(true); setError(''); const f = new FormData(e.currentTarget); try {
        await api('/login/admin', { username: f.get('username'), password: f.get('password') });
        await load();
        setTab('dashboard');
        setSelected(null);
        setNotice('');
        setLogged(true);
    }
    catch (e) {
        setError((e as Error).message);
    }
    finally {
        setBusy(false);
    } }
    async function logout() { try {
        await api('/logout', {});
        setLogged(false);
        setLeads([]);
        setStudents([]);setLevelings([]);
    }
    catch (e) {
        setError((e as Error).message);
    } }
    async function create(e: FormEvent<HTMLFormElement>) { e.preventDefault(); if (!selected)
        return; setBusy(true); setError(''); const f = new FormData(e.currentTarget); try {
        await api(`/admin/inquiries/${selected.id}/student`, { document: f.get('document'), issued: f.get('issued'), year: Number(f.get('year')) });
        setNotice(`Estudiante y usuario creados para ${selected.name}. La cuenta queda inactiva hasta que la actives en Usuarios estudiantiles.`);
        setSelected(null);
        await load();
    }
    catch (e) {
        setError((e as Error).message);
    }
    finally {
        setBusy(false);
    } }
    if (!ready)
        return <p className="cs-loading">Comprobando sesión…</p>;
    if (!logged)
        return <div className="cs-site cs-admin"><header className="cs-header d-flex align-items-center"><Brand /><a href="/">Volver al portal estudiantil →</a></header><main className="cs-main container-fluid"><section className="cs-login-page"><div><p className="cs-eyebrow">GESTIÓN INSTITUCIONAL</p><h1>Administración<br /><em>Centro Saber.</em></h1><p>Consulta solicitudes, contacta interesados y crea usuarios.</p><p className="cs-demo-note">Cuenta de prueba: <b>adminN</b><br />Clave: <b>1234987A</b></p></div><form className="cs-form" onSubmit={login}><h2>Ingreso administrativo</h2><label className="form-label">Usuario<input name="username" required autoComplete="username" className="form-control"/></label><label className="form-label">Clave<input name="password" type="password" required autoComplete="current-password" className="form-control"/></label>{error && <p role="alert" className="cs-error alert alert-danger">{error}</p>}<button disabled={busy} className="cs-button btn btn-primary">{busy ? 'Ingresando…' : 'Entrar como administrador →'}</button></form></section></main></div>;
    const navigate = (section: string) => { if(section==='dashboard')load().catch(e=>setError(e.message)); setTab(section); setSelected(null); setError(''); setNotice(''); };
    const titles: Record<string, string> = { cursos: 'Cursos', dashboard: 'Dashboard', solicitudes: 'Solicitudes', usuarios: 'Usuarios estudiantiles', plantillas: 'Plantillas', informacion: 'Gestión de información', pagina: 'Gestión de página', nivelaciones: 'Nivelaciones' };
    return <AdminShell section={tab} onNavigate={navigate} onLogout={logout} pending={leads.filter(l => !l.student_id).length}>
 <main id="admin-content" className="cs-main admin-main container-fluid"><div className="admin-page-heading"><div><p className="cs-eyebrow">CENTRO SABER · ADMINISTRACIÓN</p><h1>{titles[tab]}</h1><p>{tab === 'dashboard' ? 'El resumen de tu gestión, en un solo lugar.' : tab === 'solicitudes' ? 'Revisa las solicitudes y crea la ficha del estudiante junto con su acceso al portal.' : tab === 'usuarios' ? 'Consulta los estudiantes registrados y su estado académico.' : tab === 'nivelaciones' ? 'Revisa solicitudes, confirma citas y registra resultados de nivelación.' : tab === 'pagina' ? 'Organiza las imágenes, fechas y configuración del portal estudiantil.' : tab === 'cursos' ? 'Consulta estudiantes activos, materias, notas y fechas por curso.' : tab === 'informacion' ? 'Consulta, importa y descarga los registros de la institución.' : 'Descarga y prepara tus archivos para organizar la información.'}</p></div><div className="admin-heading-actions">{tab==='dashboard'&&<a href="/api/demo/admin/data/export/all?format=xlsx" download className="btn dash-export">↓ Descargar Excel</a>}<button onClick={() => load().then(() => setRevision(r => r + 1)).catch(e => setError(e.message))} className="cs-outline btn btn-outline-primary">Actualizar datos</button></div></div>
 {tab === 'dashboard' && <AdminDashboard leads={leads} students={students} levelings={levelings} onNavigate={navigate}/>}{error && <p role="alert" className="cs-error alert alert-danger">{error}</p>}{notice && <p role="status" className="cs-success alert alert-success">{notice}</p>}{tab === 'solicitudes' && <InquiryRecords leads={leads} onReload={load} onError={setError} onNotice={setNotice} onCreate={lead => { setSelected(lead); setError(''); }}/>}{selected && <section className="cs-create-user"><div className="cs-section-head"><div><h2>Crear estudiante y usuario: {selected.name}</h2><p>Se usarán el nombre, correo, celular y programa de la solicitud para crear la ficha y el acceso.</p></div><button onClick={() => setSelected(null)} className="cs-outline btn btn-outline-primary">Cancelar</button></div><form className="cs-form" onSubmit={create}><div className="cs-form-row"><label className="form-label">Número de cédula<input name="document" defaultValue={selected.document || selected.student_document || ''} required pattern="[0-9]{5,30}" inputMode="numeric" className="form-control"/></label><label className="form-label">Fecha de expedición<input name="issued" type="date" required className="form-control"/></label></div><label className="form-label">Año / clave de acceso<input name="year" type="number" min={2000} max={2200} defaultValue={2026} required className="form-control"/></label><p>Se creará la ficha del estudiante y su usuario de acceso. La cuenta quedará desactivada. Después de activarla en Usuarios estudiantiles, podrá entrar con su documento, fecha de expedición y año configurados. La matrícula y las notas quedarán pendientes.</p><button disabled={busy} className="cs-button btn btn-primary">{busy ? 'Creando…' : 'Crear estudiante y usuario'}</button></form></section>}{tab === 'usuarios' && <StudentRecords students={students} onReload={load} onError={setError} onNotice={setNotice}/>}{tab === 'cursos' && <CoursesAdmin revision={revision} onChanged={load}/>} {tab === 'informacion' && <DataManagement onChanged={load} revision={revision}/>} {tab === 'pagina' && <PageManagement revision={revision} onChanged={load}/>} {tab === 'nivelaciones' && <LevelingsAdmin revision={revision}/>} {tab === 'plantillas' && templates}</main></AdminShell>;
}
