import PublicationsAdmin from './Publications';
import { useState } from 'react';
import SitesAdmin from './Sites';
import GalleryAdmin from './Gallery';
import DataManagement from './DataManagement';
import { RegistrationSettingsAdmin } from './RegistrationAdmin';

export default function PageManagement({ revision, onChanged }: {
    revision: number;
    onChanged: () => Promise<void>;
}) {
    const [section, setSection] = useState('imagenes');
    const sections = [
        { id: 'comunicados', title: 'Comunicados' },
        { id: 'imagenes', title: 'Imágenes de Sobre nosotros' },
        { id: 'fechas', title: 'Fechas y eventos' },
        { id: 'registro', title: 'Registro y contactos' },
        { id: 'sedes', title: 'Sedes y contacto' },
    ];
    return <div>
        <nav className="nav nav-pills gap-2 mb-4" aria-label="Secciones de gestión de página">
            {sections.map(item => <button key={item.id} className={`nav-link btn ${section === item.id ? 'active' : ''}`} aria-current={section === item.id ? 'page' : undefined} onClick={() => setSection(item.id)}>{item.title}</button>)}
        </nav>
        {section === 'comunicados' && <PublicationsAdmin/>}
        {section === 'sedes' && <SitesAdmin revision={revision}/>}
        {section === 'imagenes' && <GalleryAdmin revision={revision}/>}
        {section === 'fechas' && <DataManagement scope="agenda" revision={revision} onChanged={onChanged}/>}
        {section === 'registro' && <><p>Configura los formularios y las personas de contacto que aparecen en Registro.</p><RegistrationSettingsAdmin revision={revision}/></>}
    </div>;
}
