import { useEffect, useRef, useState } from 'react';
import { api } from './api';

const verbs = { activate: 'Activar', deactivate: 'Desactivar', delete: 'Eliminar', restore: 'Restaurar' };
type Action = keyof typeof verbs;
export default function DataLifecycleActions({ module, row, onChanged }: { module: string; row: Record<string,string>; onChanged: () => Promise<void> }) {
 const [action,setAction] = useState<Action|null>(null), [busy,setBusy] = useState(false), [error,setError] = useState('');
 const dialog = useRef<HTMLDialogElement>(null);
 useEffect(()=>{if(action)dialog.current?.showModal();},[action]);
 const deleted=row.estado_registro==='Eliminado';
 const name=row.nombre_completo||row.nombre||`${row.nombres} ${row.apellidos}`;
 async function save(){if(!action)return;setBusy(true);setError('');try{
  await api(`/admin/data/lifecycle/${module}`,{key:module==='cursos'?row.codigo_curso:`${row.tipo_documento}:${row.documento}`,action});
  setAction(null);await onChanged();
 }catch(e){setError((e as Error).message);}finally{setBusy(false);}}
 function open(next:Action){setError('');setAction(next);}
 return <><div className="d-flex flex-wrap gap-2 data-record-actions">{deleted?<button className="btn btn-sm btn-outline-primary" onClick={()=>open('restore')}>Restaurar</button>:<><button className="btn btn-sm btn-outline-primary" onClick={()=>open(row.estado_registro==='Activo'?'deactivate':'activate')}>{row.estado_registro==='Activo'?'Desactivar':'Activar'}</button><button className="btn btn-sm btn-outline-danger" onClick={()=>open('delete')}>Eliminar</button></>}</div>
 {action&&<dialog ref={dialog} className="record-dialog" aria-label={`${verbs[action]} registro`} onCancel={e=>{e.preventDefault();if(!busy)setAction(null);}}><h2>{verbs[action]} registro</h2><p><strong>{name}</strong></p><p>{action==='delete'?'El registro pasará a Eliminados y podrás restaurarlo. Sus matrículas, notas y pagos se conservarán.':action==='restore'?'El registro volverá al listado como inactivo. Puedes activarlo después.':action==='activate'?module==='estudiantes'?'El estudiante podrá acceder al portal. Debe tener su fecha de expedición completa.':'El registro volverá a estar activo. Los cursos publicados aparecerán en la página inicial.':module==='estudiantes'?'El estudiante no podrá acceder al portal y sus sesiones anteriores dejarán de funcionar.':'El registro quedará inactivo. Los cursos desactivados no aparecerán en la oferta pública.'}</p>{module==='estudiantes'&&action==='delete'&&<p>También se desactivará su cuenta y se cerrará el acceso a sus sesiones.</p>}{error&&<p role="alert" className="alert alert-danger">{error}</p>}<div className="d-flex gap-2"><button className="btn btn-outline-secondary" disabled={busy} onClick={()=>setAction(null)}>Cancelar</button><button className={`btn ${action==='delete'?'btn-danger':'btn-primary'}`} disabled={busy} onClick={save}>{busy?'Guardando…':`Confirmar: ${verbs[action].toLowerCase()}`}</button></div></dialog>}</>;
}
