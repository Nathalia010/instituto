import { useEffect, useState } from 'react';
import StudentPortal from './StudentPortal';
import AdminPortal from './AdminPortal';

const templates = [
  { id: 'estudiantes', name: 'Estudiantes', category: 'Personas', description: 'Identificación, nombres y datos de contacto.', fields: 'Tipo de documento · Documento · Nombres · Apellidos' },
  { id: 'docentes', name: 'Docentes', category: 'Personas', description: 'Identificación y contacto de los profesores.', fields: 'Tipo de documento · Documento · Nombre completo · Correo' },
  { id: 'cursos', name: 'Cursos', category: 'Académico', description: 'Validación de bachillerato, cursos y programas técnicos.', fields: 'Código · Nombre · Tipo de programa · Duración en horas' },
  { id: 'periodos', name: 'Periodos', category: 'Académico', description: 'Años, fechas y códigos de los periodos académicos.', fields: 'Código · Año · Nombre · Inicio · Fin' },
  { id: 'matriculas', name: 'Matrículas', category: 'Académico', description: 'Relaciona cada estudiante con su curso y periodo.', fields: 'Código de matrícula · Documento · Curso · Periodo · Estado' },
  { id: 'notas', name: 'Notas', category: 'Académico', description: 'Evaluaciones asociadas a una matrícula y un docente.', fields: 'Matrícula · Evaluación · Nota · Documento del docente' },
  { id: 'cobros', name: 'Cobros', category: 'Financiero', description: 'Conceptos y valores que debe pagar cada matrícula.', fields: 'Código de cobro · Matrícula · Concepto · Valor · Vencimiento' },
  { id: 'pagos', name: 'Pagos', category: 'Financiero', description: 'Abonos individuales con fecha y referencia.', fields: 'Referencia · Código de cobro · Fecha · Valor · Método' },
];

export default function App() {
  useEffect(() => {
    document.title = window.location.pathname.startsWith('/administracion')
      ? 'Centro Saber · Administración y docentes'
      : 'Centro Saber · Espacio del estudiante';
  }, []);
  return window.location.pathname.startsWith('/administracion') ? <AdminPortal templates={<AdminTemplates />} /> : <StudentPortal />;
}

function AdminTemplates() {
  const [search, setSearch] = useState('');
  const [category, setCategory] = useState('Todas');
  const visible = templates.filter(t => (category === 'Todas' || t.category === category) && `${t.name} ${t.description}`.toLocaleLowerCase('es').includes(search.toLocaleLowerCase('es')));

  return <div className="app">
    <div className="workspace">
      <section id="contenido" aria-label="Centro de plantillas">
        <div className="heading"><div><p className="eyebrow">TODO EMPIEZA CON DATOS ORGANIZADOS</p><h1>Centro de plantillas</h1><p className="lead">Prepara la información de tu institución, a tu ritmo.</p></div><span className="count">08 plantillas</span></div>
        <section className="hero" aria-labelledby="excel-title"><div><span className="tag">EXCEL EN BLANCO</span><h2 id="excel-title">Un lugar para ordenar<br/>cada nuevo comienzo.</h2><p>Descarga el libro completo, rellena las hojas que necesitas<br className="desktop-break"/> y conserva una copia de tus archivos originales.</p><a className="primary" href="/plantillas/educontrol-plantillas.xlsx" download>Descargar libro Excel <span aria-hidden="true">↓</span></a></div><div className="sheet-art" aria-hidden="true"><div className="sheet-top"><span>Centro Saber</span><span>XLSX</span></div><div className="sheet-line"/><div className="sheet-grid">{Array.from({ length: 24 }, (_, i) => <span key={i}/>)}</div><div className="sheet-bottom">8 hojas · Sin datos de estudiantes</div></div></section>
        <section className="steps" aria-label="Cómo usar las plantillas"><div><span>01</span><p><strong>Descarga</strong><small>Elige el libro o un archivo CSV.</small></p></div><div><span>02</span><p><strong>Completa</strong><small>Una fila por registro, sin cambiar encabezados.</small></p></div><div><span>03</span><p><strong>Revisa</strong><small>Consulta la guía antes de preparar tu carga.</small></p></div></section>
        <div className="catalog-heading"><h2>Plantillas por módulo</h2><label className="search"><span aria-hidden="true">⌕</span><input aria-label="Buscar plantillas" placeholder="Buscar una plantilla…" value={search} onChange={e => setSearch(e.target.value)}/></label></div>
        <div className="filters" role="group" aria-label="Categoría de plantilla">{['Todas', 'Personas', 'Académico', 'Financiero'].map(c => <button key={c} aria-pressed={category === c} className={category === c ? 'selected' : ''} onClick={() => setCategory(c)}>{c}</button>)}</div>
        <div className="cards">{visible.map((t, i) => <article className="card" key={t.id}><div className="card-top"><span className={`module-icon ${t.category === 'Financiero' ? 'money' : ''}`} aria-hidden="true">{t.category === 'Personas' ? '◉' : t.category === 'Financiero' ? '$' : '▤'}</span><span className="category">{t.category}</span></div><h3>{t.name}</h3><p>{t.description}</p><div className="fields">{t.fields}</div><a href={`/plantillas/${t.id}.csv`} download aria-label={`Descargar plantilla CSV de ${t.name}`}>Descargar CSV <span aria-hidden="true">↓</span></a><span className="sr-only">Plantilla {i + 1}</span></article>)}</div>
        {visible.length === 0 && <p role="status" className="empty">No encontramos plantillas. Prueba con otro nombre o categoría.</p>}
        <section className="guide"><div><h2>Datos listos para el siguiente paso</h2><p>La guía explica los campos, las relaciones y el orden de preparación.</p></div><a href="/plantillas/guia.html">Leer guía de llenado <span aria-hidden="true">↗</span></a></section>
        <p className="notice">La importación con vista previa se implementará en una siguiente etapa. Esta versión permite descargar plantillas; no guarda datos.</p>
        <footer><span>Centro Saber · Gestión educativa</span><span>Información académica y financiera, organizada.</span></footer>
      </section>
    </div>
  </div>;
}
