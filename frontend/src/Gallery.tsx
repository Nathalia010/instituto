import { useEffect, useState, type FormEvent } from 'react';
import { api } from './api';
import './gallery.css';
type Photo = {
    id: string;
    caption: string;
    active: boolean;
    deleted: boolean;
    url: string;
};
type Upload = {
    file: File;
    caption: string;
    url: string;
};
function ShowcaseImage({photo,decorative=false}:{photo:Photo;decorative?:boolean}) {
    const [wide,setWide]=useState(false);
    return <img src={photo.url} alt={decorative?'':photo.caption} loading={decorative?'lazy':'eager'} className={`img-fluid${wide?' gallery-image-wide':''}`} onLoad={event=>setWide(event.currentTarget.naturalWidth/event.currentTarget.naturalHeight>2)}/>;
}
export function PublicGallery() {
    const [photos, setPhotos] = useState<Photo[]>([]), [index, setIndex] = useState(0), [playing, setPlaying] = useState(() => !window.matchMedia('(prefers-reduced-motion: reduce)').matches), [hover, setHover] = useState(false), [visible, setVisible] = useState(!document.hidden);
    useEffect(() => { let active = true; const load = () => api<Photo[]>('/gallery').then(p => { if (active)
        setPhotos(p); }).catch(() => { }); load(); window.addEventListener('focus', load); return () => { active = false; window.removeEventListener('focus', load); }; }, []);
    useEffect(() => { const change = () => setVisible(!document.hidden); change(); document.addEventListener('visibilitychange', change); return () => document.removeEventListener('visibilitychange', change); }, []);
    useEffect(() => { if (!playing || hover || !visible || photos.length < 2)
        return; const timer = window.setInterval(() => setIndex(i => (i + 1) % photos.length), 5000); return () => window.clearInterval(timer); }, [playing, hover, visible, photos.length]);
    if (!photos.length)
        return null;
    const current = index % photos.length, photo = photos[current];
    const go = (next: number) => { setIndex((next + photos.length) % photos.length); setPlaying(false); };
    const previous = photos[(current - 1 + photos.length) % photos.length], next = photos[(current + 1) % photos.length];
    return <section className="public-gallery gallery-showcase carousel" role="region" aria-roledescription="carrusel" aria-label="Galería de Centro Saber" onMouseEnter={() => setHover(true)} onMouseLeave={() => setHover(false)} onFocusCapture={e => { if (!(e.target as HTMLElement).closest('[data-gallery-playback]'))
        setPlaying(false); }}>
  <div className="cs-section-head"><div><p className="cs-eyebrow">NUESTRA COMUNIDAD</p><h2>Momentos de Centro Saber</h2></div>{photos.length > 1 && <button data-gallery-playback="true" onClick={() => setPlaying(p => !p)} className="cs-outline btn btn-outline-primary">{playing ? 'Pausar imágenes' : 'Reproducir imágenes'}</button>}</div>
  <div className="gallery-stage d-flex align-items-center justify-content-center" aria-live={playing ? 'off' : 'polite'}>
   {photos.length > 1 && <button aria-label={`Ver imagen anterior: ${previous.caption}`} onClick={() => go(current - 1)} className="gallery-neighbor gallery-previous btn"><ShowcaseImage key={previous.id} photo={previous} decorative/></button>}
   <figure className="gallery-featured mb-0 shadow-lg"><ShowcaseImage key={photo.id} photo={photo}/><figcaption><span>{photo.caption}</span><small>{current + 1} / {photos.length}</small></figcaption></figure>
   {photos.length > 1 && <button aria-label={`Ver imagen siguiente: ${next.caption}`} onClick={() => go(current + 1)} className="gallery-neighbor gallery-next btn"><ShowcaseImage key={next.id} photo={next} decorative/></button>}
  </div>
  {photos.length > 1 && <div className="gallery-navigation"><button aria-label="Imagen anterior" onClick={() => go(current - 1)} className="cs-outline btn btn-outline-primary">← Anterior</button><div className="gallery-dots" aria-label="Seleccionar imagen">{photos.map((p, i) => <button key={p.id} aria-label={`Ver imagen ${i + 1}: ${p.caption}`} aria-current={i === current ? 'true' : undefined} onClick={() => go(i)} className="btn"/>)}</div><button aria-label="Imagen siguiente" onClick={() => go(current + 1)} className="cs-outline btn btn-outline-primary">Siguiente →</button></div>}
 </section>;
}
export default function GalleryAdmin({ revision }: {
    revision: number;
}) {
    const [photos, setPhotos] = useState<Photo[]>([]), [uploads, setUploads] = useState<Upload[]>([]), [busy, setBusy] = useState(false), [error, setError] = useState(''), [notice, setNotice] = useState('');
    const [removing,setRemoving]=useState<Photo|null>(null);
    useEffect(()=>{if(removing)document.querySelector('.gallery-delete-confirm')?.scrollIntoView({block:'center'});},[removing]);
    async function remove(){if(!removing)return;setBusy(true);setError('');setNotice('');try{await api(`/admin/gallery/${removing.id}/delete`,{});setRemoving(null);setNotice('Imagen eliminada definitivamente.');await load();}catch(e){setError((e as Error).message);}finally{setBusy(false);}}
    const load = async () => setPhotos(await api<Photo[]>('/admin/gallery'));
    useEffect(() => { let active = true; api<Photo[]>('/admin/gallery').then(p => { if (active)
        setPhotos(p); }).catch(e => { if (active)
        setError(e.message); }); return () => { active = false; }; }, [revision]);
    const previewUrls = uploads.map(u => u.url).join('|');
    useEffect(() => { const urls = previewUrls.split('|').filter(Boolean); return () => urls.forEach(url => URL.revokeObjectURL(url)); }, [previewUrls]);
    function choose(files: FileList | null) { setError(''); setNotice(''); if (!files)
        return; const list = Array.from(files); if (list.length > 10 || list.some(f => !['image/jpeg', 'image/png'].includes(f.type) || f.size > 5000000)) {
        setError('Selecciona hasta 10 imágenes JPG o PNG, de hasta 5 MB cada una.');
        return;
    } setUploads(list.map(file => ({ file, caption: file.name.replace(/\.[^.]+$/, '').slice(0, 180), url: URL.createObjectURL(file) }))); }
    async function publish(e: FormEvent) { e.preventDefault(); if (busy || !uploads.length)
        return; setBusy(true); setError(''); setNotice(''); let saved = 0; const failed: Upload[] = []; try {
        const csrf = await api<{
            header: string;
            token: string;
        }>('/csrf');
        for (const upload of uploads) {
            try {
                const body = new FormData();
                body.append('file', upload.file);
                body.append('caption', upload.caption);
                const response = await fetch('/api/demo/admin/gallery', { method: 'POST', credentials: 'same-origin', headers: { [csrf.header]: csrf.token }, body });
                if (!response.ok)
                    throw new Error(response.status === 409 ? 'Se alcanzó el límite de 30 imágenes publicadas.' : 'Revisa el formato, tamaño y descripción de la imagen.');
                saved++;
            }
            catch (e) {
                failed.push(upload);
                setError((e as Error).message);
            }
        }
        setUploads(failed.map(u => ({ ...u, url: URL.createObjectURL(u.file) })));
        setNotice(`${saved} imágenes publicadas en Sobre nosotros.`);
        await load();
    }
    catch (e) {
        setError((e as Error).message);
    }
    finally {
        setBusy(false);
    } }
    async function toggle(photo: Photo) { setBusy(true); setError(''); setNotice(''); try {
        await api(`/admin/gallery/${photo.id}/${photo.active ? 'hide' : 'restore'}`, {});
        setNotice(photo.active ? 'Imagen retirada de la galería. Puedes restaurarla cuando quieras.' : 'Imagen restaurada en Sobre nosotros.');
        await load();
    }
    catch (e) {
        setError((e as Error).message);
    }
    finally {
        setBusy(false);
    } }
    return <div className="gallery-admin"><div className="gallery-admin-intro"><p>Las imágenes publicadas pasan automáticamente en la galería de Sobre nosotros. Agrega fotos con una descripción o retíralas de la página.</p><a href="/nosotros" target="_blank" rel="noreferrer" className="cs-outline btn btn-outline-primary">Ver Sobre nosotros ↗</a></div>{error && <p role="alert" className="cs-error alert alert-danger">{error}</p>}{notice && <p role="status" className="cs-success alert alert-success">{notice}</p>}
 {removing&&<div className="gallery-delete-confirm alert alert-warning" role="region" aria-label="Confirmar eliminación de imagen"><p>¿Eliminar <strong>{removing.caption}</strong>? Se retirará de la lista y del carrusel. La imagen se borrará definitivamente y no se podrá recuperar.</p><button className="btn btn-danger me-2" disabled={busy} onClick={remove}>Confirmar eliminación</button><button className="btn btn-outline-secondary" disabled={busy} onClick={()=>setRemoving(null)}>Conservar imagen</button></div>}<form className="gallery-upload-form" onSubmit={publish}><h2>Agregar imágenes</h2><label className="form-label">Seleccionar imágenes<input key={uploads.length ? 'selected' : 'empty'} type="file" accept="image/jpeg,image/png" multiple disabled={busy} onChange={e => choose(e.target.files)} className="form-control"/></label><p>JPG o PNG · Hasta 5 MB y 16 megapíxeles por imagen · Máximo 30 publicadas.</p>{uploads.length > 0 && <><div className="gallery-upload-preview d-grid">{uploads.map((u, i) => <div key={u.url}><img src={u.url} alt="Vista previa de la imagen seleccionada" className="img-fluid"/><label className="form-label">Descripción de la imagen {i + 1}<input value={u.caption} required maxLength={180} disabled={busy} onChange={e => setUploads(current => current.map((v, n) => n === i ? { ...v, caption: e.target.value } : v))} className="form-control"/></label></div>)}</div><p>Revisa las imágenes y sus descripciones antes de publicarlas.</p><button disabled={busy} className="cs-button btn btn-primary">{busy ? 'Publicando…' : `Publicar ${uploads.length} imágenes`}</button><button type="button" disabled={busy} onClick={() => setUploads([])} className="cs-outline btn btn-outline-primary">Cancelar selección</button></>}</form>
 <section className="gallery-admin-list"><h2>Imágenes de la galería</h2><p>{photos.filter(p => p.active && !p.deleted).length} publicadas · {photos.filter(p => !p.active && !p.deleted).length} retiradas</p><div className="gallery-admin-grid d-grid">{photos.filter(photo => !photo.deleted).map(photo => <article key={photo.id} className="card"><img src={photo.url} alt={photo.caption} loading="lazy" className="img-fluid"/><div><span className={`gallery-status ${photo.active ? '' : 'hidden'}`}>{photo.active ? 'Publicada' : 'Retirada'}</span><h3>{photo.caption}</h3><button disabled={busy} onClick={() => toggle(photo)} className="cs-outline btn btn-outline-primary">{photo.active ? 'Quitar de la galería' : 'Restaurar imagen'}</button><button disabled={busy} onClick={()=>setRemoving(photo)} className="btn btn-outline-danger ms-2 mt-2">Eliminar imagen</button></div></article>)}</div></section></div>;
}
