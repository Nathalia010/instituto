import StudentPortal from '../StudentPortal';
import { LevelingGuides } from '../Levelings';
export default function TemporaryApp(){
 const route=new URLSearchParams(location.search).get('route')||'/';
 if(['/administracion','/estudiantes','/informacion'].includes(route))return <div className="cs-site student-site"><main className="cs-main py-5"><a href="/" className="cs-outline">← Volver a Centro Saber</a><h1 className="mt-4">{route==='/informacion'?'¿Quieres estudiar con nosotros?':route==='/estudiantes'?'Mi espacio estudiantil':'Acceso administrativo'}</h1><p>Estás en una vista pública temporal. El ingreso a cuentas, las solicitudes y los registros estarán disponibles cuando se conecte el servidor de la institución.</p><a className="cs-button" href="/contacto">Comunicarme con Centro Saber</a>{route==='/estudiantes'&&<LevelingGuides/>}</main></div>;
 return <><div className="alert alert-info mb-0 rounded-0 text-center py-2 small" role="note">Vista pública temporal · Información de referencia. Confirma fechas y disponibilidad con la institución.</div><StudentPortal/></>;
}
