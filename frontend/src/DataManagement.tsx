import { useEffect, useState, type FormEvent } from 'react';
import { api } from './api';
import './data-management.css';
import { eventTypes } from './AcademicCalendar';
import ManualDataEditor from './ManualDataEditor';
import RecordsAndDetail from './RecordsAndDetail';
import DataLifecycleActions from './DataLifecycleActions';
import './page-content.css';
import { AgendaEditor } from './RegistrationAdmin';
type Definition = {
    id: string;
    label: string;
    annual: boolean;
    fields: string[];
    required: string[];
};
type Parsed = {
    headers: string[];
    rows: string[][];
    sheets: string[];
    sheet: string;
};
type Checked = {
    row: number;
    valid: boolean;
    errors: string[];
    data: Record<string, string>;
    year: number | null;
};
type Preview = {
    id: string;
    rows: Checked[];
    valid: number;
    invalid: number;
};
const label = (field: string) => field === 'anio' ? 'Año' : field.replaceAll('_', ' ').replace(/^./, s => s.toUpperCase());
function csvCell(v: string) { return '"' + v.replaceAll('"', '""') + '"'; }
export default function DataManagement({ onChanged, revision, scope = 'records' }: {
    onChanged: () => Promise<void>;
    revision: number;
    scope?: 'records' | 'agenda';
}) {
    const [modules, setModules] = useState<Definition[]>([]), [module, setModule] = useState(scope === 'agenda' ? 'agenda' : 'estudiantes'), [years, setYears] = useState<number[]>([]), [year, setYear] = useState(''), [records, setRecords] = useState<Record<string, string>[]>([]), [search, setSearch] = useState('');
    const [file, setFile] = useState<File | null>(null), [parsed, setParsed] = useState<Parsed | null>(null), [mapping, setMapping] = useState<Record<string, number>>({}), [preview, setPreview] = useState<Preview | null>(null), [minimum, setMinimum] = useState(0), [maximum, setMaximum] = useState(5), [busy, setBusy] = useState(false), [error, setError] = useState(''), [notice, setNotice] = useState(''), [selected, setSelected] = useState<Record<string, string> | null>(null);
    useEffect(() => { if (selected)
        document.querySelector('.data-detail')?.scrollIntoView({ behavior: 'smooth', block: 'start' }); }, [selected]);
    const [eventType, setEventType] = useState('');
    const [recordState,setRecordState]=useState('');
    const manageable=['estudiantes','docentes','cursos'].includes(module);
    const deletedView=manageable&&recordState==='Eliminado';
    const definition = modules.find(m => m.id === module);
    const yearQuery = year && definition?.annual ? `?year=${year}` : '';
    const recordsPath=deletedView?`/admin/data/lifecycle/${module}/deleted${yearQuery}`:`/admin/data/records/${module}${yearQuery}`;
    useEffect(() => { let active = true; api<{
        modules: Definition[];
        years: number[];
    }>('/admin/data/catalog').then(r => { if (active) {
        setModules(r.modules);
        setYears(r.years);
    } }).catch(e => { if (active)
        setError(e.message); }); return () => { active = false; }; }, [revision]);
    useEffect(() => { let active = true; setError(''); setRecords([]); api<Record<string, string>[]>(recordsPath).then(r => { if (active)
        setRecords(r); }).catch(e => { if (active)
        setError(e.message); }); return () => { active = false; }; }, [module, recordsPath, revision]);
    function changeModule(id: string) { setModule(id); setRecordState(''); setYear(''); setSearch(''); setParsed(null); setPreview(null); setFile(null); setNotice(''); setError(''); setSelected(null); }
    async function refresh() { setSelected(null); setRecords(await api<Record<string, string>[]>(recordsPath)); const c = await api<{
        years: number[];
    }>('/admin/data/catalog'); setYears(c.years); await onChanged(); }
    async function readFile(target: File, sheet = '') {
        setBusy(true);
        setError('');
        setPreview(null);
        try {
            const c = await api<{
                header: string;
                token: string;
            }>('/csrf');
            const data = new FormData();
            data.append('file', target);
            if (sheet)
                data.append('sheet', sheet);
            const response = await fetch('/api/demo/admin/data/file', { method: 'POST', credentials: 'same-origin', headers: { [c.header]: c.token }, body: data });
            if (!response.ok)
                throw new Error('No se pudo leer el archivo. Usa XLSX o CSV sin fórmulas, hasta 5 MB y 2.000 filas.');
            const result: Parsed = await response.json();
            setParsed(result);
            const match: Record<string, number> = {};
            for (const field of definition?.fields || []) {
                const i = result.headers.findIndex(h => h.trim().toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '').replaceAll(' ', '_') === field);
                if (i >= 0)
                    match[field] = i;
            }
            setMapping(match);
            setFile(target);
        }
        catch (e) {
            setError((e as Error).message);
            setParsed(null);
        }
        finally {
            setBusy(false);
        }
    }
    async function validate(e: FormEvent) { e.preventDefault(); if (!parsed || !file)
        return; setBusy(true); setError(''); setNotice(''); try {
        setPreview(await api<Preview>('/admin/data/preview', { module, filename: file.name, rows: parsed.rows, mapping, minimum, maximum }));
    }
    catch (e) {
        setError((e as Error).message);
    }
    finally {
        setBusy(false);
    } }
    async function confirm() { if (!preview)
        return; setBusy(true); setError(''); try {
        const r = await api<{
            saved: number;
            skipped: number;
        }>(`/admin/data/confirm/${preview.id}`, {});
        setNotice(`${r.saved} registros guardados. ${r.skipped} filas omitidas por errores o duplicados.`);
        setPreview(null);
        setParsed(null);
        setFile(null);
        await refresh();
    }
    catch (e) {
        setError((e as Error).message);
    }
    finally {
        setBusy(false);
    } }
    function errorsCsv() { if (!preview)
        return; const rows = ['fila;errores', ...preview.rows.filter(r => !r.valid).map(r => `${r.row};${csvCell(r.errors.join(' · '))}`)]; const url = URL.createObjectURL(new Blob(['\uFEFF' + rows.join('\r\n')], { type: 'text/csv;charset=utf-8' })); const link = document.createElement('a'); link.href = url; link.download = `errores-${module}.csv`; link.click(); URL.revokeObjectURL(url); }
    const visible = records.filter(row => (!manageable || !recordState || deletedView || row.estado_registro===recordState) && (!eventType || row.tipo_evento === eventType) && Object.values(row).some(v => v.toLowerCase().includes(search.toLowerCase())));
    const columns = definition ? [...definition.fields, ...(definition.annual && !definition.fields.includes('anio') ? ['anio'] : [])] : [];
    const exportLink = (target: string, format = 'xlsx') => `/api/demo/admin/data/export/${target}?format=${format}${year && definition?.annual ? `&year=${year}` : ''}`;
    return <div className="data-management"><div className="data-module-tabs" role="group" aria-label="Tipos de información" style={scope === 'agenda' ? { display: 'none' } : undefined}>{modules.filter(m => scope === 'agenda' ? m.id === 'agenda' : m.id !== 'agenda').map(m => <button key={m.id} aria-pressed={module === m.id} onClick={() => changeModule(m.id)} disabled={busy} className="btn">{m.label}</button>)}</div>
 <section className="data-toolbar"><div><h2>{definition?.label || 'Información'}</h2><p>Consulta los registros y descarga su información.</p></div><div className="data-export-actions">{scope === 'agenda' && <label className="form-label">Tipo de actividad<select className="form-select" value={eventType} onChange={e => setEventType(e.target.value)}><option value="">Todos los tipos</option>{Object.entries(eventTypes).map(([id,name]) => <option key={id} value={id}>{name}</option>)}</select></label>}{definition?.annual && <label className="form-label">Año<select value={year} onChange={e => setYear(e.target.value)} className="form-select"><option value="">Todos los años</option>{years.map(y => <option key={y} value={y}>{y}</option>)}</select></label>}<a href={exportLink(module)} className="cs-button btn btn-primary">Descargar Excel</a><a href={exportLink(module, 'csv')} className="cs-outline btn btn-outline-primary">CSV</a><a href={exportLink('all')} className="cs-outline btn btn-outline-primary">Descargar todo ({modules.length + 2} hojas)</a></div></section>
 <p className="data-help">{definition?.annual ? 'El filtro de año se aplica a los registros asociados a ese periodo.' : 'Este catálogo no tiene año: se consulta y descarga completo.'} En el libro completo, docentes y cursos se incluyen sin filtro de año. También se añaden dos hojas con las notas y movimientos existentes del portal. Cada estudiante nuevo recibe automáticamente una cuenta inactiva al confirmar la carga. Actívala en Usuarios estudiantiles después de completar sus datos de acceso.</p>
 {error && <p role="alert" className="cs-error alert alert-danger">{error}</p>}{notice && <p role="status" className="cs-success alert alert-success">{notice}</p>}
 {manageable&&<div className="d-flex flex-wrap align-items-center gap-3 mb-4"><label className="form-label mb-0">Estado del registro<select className="form-select" value={recordState} onChange={e=>{setRecordState(e.target.value);setSelected(null);}}><option value="">Todos los registros</option><option value="Activo">Activos</option><option value="Inactivo">Inactivos</option><option value="Eliminado">Eliminados</option></select></label><p className="small text-secondary mb-0">Eliminar permite restaurar después. Al restaurar, el registro queda inactivo.{module==='estudiantes'&&' El estado indica si su cuenta puede ingresar al portal.'}</p></div>}{module!=="agenda"&&!deletedView&&definition&&<ManualDataEditor key={module} definition={definition} records={records} onChanged={refresh}/>} {module==="agenda"&&<AgendaEditor records={visible} onChanged={refresh}/>}<RecordsAndDetail records={visible} total={records.length} columns={module === "agenda" ? ["codigo_evento","titulo","anio","tipo_evento","fecha_inicio","estado"] : module==="estudiantes"?["nombre_completo","documento","programa","anio","estado_registro"]:module==="docentes"?["nombre_completo","documento","correo","estado_registro"]:module==="cursos"?["codigo_curso","nombre","publicado","estado_registro"]:columns} actions={manageable?(row)=><DataLifecycleActions module={module} row={row} onChanged={refresh}/>:undefined} search={search} onSearch={setSearch} selected={selected} onSelect={setSelected} onClose={()=>setSelected(null)}/>
 <section className="data-import"><div className="data-import-heading"><div><span className="cs-eyebrow">CARGA MASIVA</span><h2>Importar {definition?.label.toLowerCase()}</h2><p>Selecciona un archivo, relaciona sus columnas y revisa los datos antes de guardar.</p></div><a href={`/api/demo/admin/data/template/${module}`} download className="cs-outline btn btn-outline-primary">Plantilla CSV ↓</a></div><p className="data-help">Orden recomendado: estudiantes/docentes, cursos, periodos, matrículas, notas/cobros y pagos. Documentos y códigos deben conservarse como texto. Los pagos quedan en revisión.</p><label className="data-upload form-label">Archivo Excel o CSV<input key={module + (file ? 'loaded' : 'empty')} type="file" accept=".xlsx,.csv" disabled={busy} onChange={e => { const f = e.target.files?.[0]; if (f)
        readFile(f); }} className="form-control"/><small>Máximo 5 MB y 2.000 filas por carga. No se admiten fórmulas ni macros.</small></label>
 {parsed && <form onSubmit={validate}><div className="data-file-info"><strong>{file?.name}</strong><span>{parsed.rows.filter(r => r.some(c => c.trim())).length} filas con datos</span>{parsed.sheets.length > 0 && <label className="form-label">Hoja a importar<select value={parsed.sheet} disabled={busy} onChange={e => file && readFile(file, e.target.value)} className="form-select">{parsed.sheets.map(s => <option key={s}>{s}</option>)}</select></label>}</div><h3>Relacionar columnas</h3><div className="data-mapping d-grid">{definition?.fields.map(field => <label key={field} className="form-label">{label(field)}{definition.required.includes(field) ? ' *' : ''}<select disabled={busy} value={mapping[field] ?? ''} onChange={e => { const next = { ...mapping }; if (e.target.value === '')
        delete next[field];
    else
        next[field] = Number(e.target.value); setMapping(next); setPreview(null); }} className="form-select"><option value="">Sin asignar</option>{parsed.headers.map((h, i) => <option key={i} value={i}>{h || `Columna ${i + 1}`}</option>)}</select></label>)}</div><p className="data-help">Solo se importan las columnas asignadas. En estudiantes completa nombre completo o nombres y apellidos. Las filas vacías se omiten.</p>{module === 'notas' && <div className="data-grade-scale"><label className="form-label">Nota mínima<input type="number" step="0.01" value={minimum} onChange={e => { setMinimum(Number(e.target.value)); setPreview(null); }} className="form-control"/></label><label className="form-label">Nota máxima<input type="number" step="0.01" value={maximum} onChange={e => { setMaximum(Number(e.target.value)); setPreview(null); }} className="form-control"/></label><p>Configura la escala aplicable antes de validar.</p></div>}<button disabled={busy} className="cs-button btn btn-primary">{busy ? 'Validando…' : 'Previsualizar y validar'}</button></form>}
 {preview && <div className="data-preview"><div className="data-preview-heading"><h3>Vista previa: no se ha guardado nada</h3><span>{preview.valid} válidas · {preview.invalid} con errores</span></div><div className="cs-table-wrap table-responsive"><table className="table table-hover align-middle mb-0"><thead><tr><th>Fila</th><th>Resultado</th><th>Datos</th><th>Errores</th></tr></thead><tbody>{preview.rows.map(row => <tr key={row.row}><td>{row.row}</td><td><span className={`data-validation ${row.valid ? 'valid' : 'invalid'}`}>{row.valid ? 'Válida' : 'Revisar'}</span></td><td className="data-preview-values">{Object.entries(row.data).filter(([, v]) => v).map(([f, v]) => <span key={f}><b>{label(f)}:</b> {v}</span>)}</td><td className="data-preview-errors">{row.errors.join(' · ') || 'Sin errores'}</td></tr>)}</tbody></table></div><div className="data-confirm"><p>Guardar solo las {preview.valid} filas válidas. Los errores y duplicados se omiten; ningún registro existente se sobrescribe.</p>{preview.invalid > 0 && <button onClick={errorsCsv} className="cs-outline btn btn-outline-primary">Descargar errores</button>}<button disabled={busy || preview.valid === 0} onClick={confirm} className="cs-button btn btn-primary">{busy ? 'Guardando…' : `Confirmar carga de ${preview.valid} registros`}</button><button disabled={busy} onClick={() => setPreview(null)} className="cs-outline btn btn-outline-primary">Cancelar vista previa</button></div></div>}
 </section></div>;
}

