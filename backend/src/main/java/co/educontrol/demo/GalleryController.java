package co.educontrol.demo;

import java.io.*;
import java.util.*;
import javax.imageio.ImageIO;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController @Profile("demo") @RequestMapping("/api/demo")
public class GalleryController {
 private final JdbcTemplate db;
 public GalleryController(JdbcTemplate db){this.db=db;}
 public record Photo(String id,String caption,boolean active,String url,boolean deleted){}
 private List<Photo> photos(boolean admin){return db.query("SELECT id,caption,active,built_in,deleted FROM demo_gallery"+(admin?"":" WHERE active=TRUE AND deleted=FALSE")+" ORDER BY created_at,id",(rs,n)->new Photo(rs.getString("id"),rs.getString("caption"),rs.getBoolean("active"),rs.getBoolean("built_in")?"/marca/perro-graduado.png":"/api/demo/"+(admin?"admin/":"")+"gallery/images/"+rs.getString("id"),rs.getBoolean("deleted")));}
 @GetMapping("/gallery") public ResponseEntity<List<Photo>> gallery(){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(photos(false));}
 @GetMapping("/admin/gallery") public List<Photo> all(){return photos(true);}
 @GetMapping({"/gallery/images/{id}","/admin/gallery/images/{id}"}) public ResponseEntity<byte[]> image(@PathVariable String id,jakarta.servlet.http.HttpServletRequest request){
  boolean admin=request.getRequestURI().contains("/admin/");
  var found=db.query("SELECT mime,image FROM demo_gallery WHERE id=? AND built_in=FALSE"+(admin?"":" AND active=TRUE AND deleted=FALSE"),(rs,n)->Map.entry(rs.getString(1),rs.getBytes(2)),id);
  if(found.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND);
  return ResponseEntity.ok().header("X-Content-Type-Options","nosniff").cacheControl(CacheControl.noStore()).contentType(MediaType.parseMediaType(found.getFirst().getKey())).body(found.getFirst().getValue());
 }
 @PostMapping(value="/admin/gallery",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @Transactional
 public ResponseEntity<Photo> add(@RequestParam MultipartFile file,@RequestParam String caption,Authentication auth){
  caption=caption.strip();if(caption.isEmpty()||caption.length()>180||file.isEmpty()||file.getSize()>5_000_000)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Descripción requerida (máximo 180 caracteres) e imagen de hasta 5 MB.");
  byte[] bytes;String mime;
  try(var stream=file.getInputStream();var input=ImageIO.createImageInputStream(stream)){
   var readers=ImageIO.getImageReaders(input);if(!readers.hasNext())throw new IOException();var reader=readers.next();
   try{reader.setInput(input);String format=reader.getFormatName().toLowerCase(Locale.ROOT);if(!Set.of("png","jpeg","jpg").contains(format))throw new IOException();int width=reader.getWidth(0),height=reader.getHeight(0);if(width<1||height<1||(long)width*height>16_000_000)throw new IOException();var image=reader.read(0);var output=new ByteArrayOutputStream();ImageIO.write(image,format.equals("png")?"png":"jpg",output);bytes=output.toByteArray();mime=format.equals("png")?"image/png":"image/jpeg";if(bytes.length==0||bytes.length>5_000_000)throw new IOException();}finally{reader.dispose();}
  }catch(Exception e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Usa una imagen JPG o PNG válida, hasta 5 MB y 16 megapíxeles.");}
  lock();if(db.queryForObject("SELECT COUNT(*) FROM demo_gallery WHERE active=TRUE AND deleted=FALSE",Integer.class)>=30)throw new ResponseStatusException(HttpStatus.CONFLICT,"Máximo 30 imágenes publicadas. Quita una antes de agregar otra.");
  String id=UUID.randomUUID().toString();db.update("INSERT INTO demo_gallery(id,caption,mime,image) VALUES (?,?,?,?)",id,caption,mime,bytes);audit(auth,"PUBLICAR_IMAGEN",id);
  return ResponseEntity.status(HttpStatus.CREATED).body(new Photo(id,caption,true,"/api/demo/admin/gallery/images/"+id,false));
 }
 private void lock(){db.queryForObject("SELECT id FROM demo_data_lock WHERE id=1 FOR UPDATE",Integer.class);}
 @PostMapping("/admin/gallery/{id}/delete") @Transactional public Map<String,Boolean> delete(@PathVariable String id,Authentication auth){lock();if(db.update("UPDATE demo_gallery SET deleted=TRUE WHERE id=? AND deleted=FALSE",id)==0)throw new ResponseStatusException(HttpStatus.NOT_FOUND);audit(auth,"ELIMINAR_IMAGEN",id);return Map.of("deleted",true);}
 @PostMapping("/admin/gallery/{id}/undelete") @Transactional public Map<String,Boolean> undelete(@PathVariable String id,Authentication auth){lock();var rows=db.queryForList("SELECT active FROM demo_gallery WHERE id=? AND deleted=TRUE",id);if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND);if(Boolean.TRUE.equals(rows.getFirst().get("active"))&&db.queryForObject("SELECT COUNT(*) FROM demo_gallery WHERE active=TRUE AND deleted=FALSE",Integer.class)>=30)throw new ResponseStatusException(HttpStatus.CONFLICT,"Máximo 30 imágenes publicadas.");db.update("UPDATE demo_gallery SET deleted=FALSE WHERE id=?",id);audit(auth,"RECUPERAR_IMAGEN",id);return Map.of("deleted",false);}
 private void audit(Authentication auth,String action,String id){db.update("INSERT INTO demo_audit(id,actor,action,resource_id) VALUES (?,?,?,?)",UUID.randomUUID().toString(),auth.getName(),action,id);}
 @PostMapping("/admin/gallery/{id}/hide") @Transactional public Map<String,Boolean> hide(@PathVariable String id,Authentication auth){lock();if(db.update("UPDATE demo_gallery SET active=FALSE WHERE id=? AND active=TRUE AND deleted=FALSE",id)==0)throw new ResponseStatusException(HttpStatus.NOT_FOUND);audit(auth,"QUITAR_IMAGEN",id);return Map.of("active",false);}
 @PostMapping("/admin/gallery/{id}/restore") @Transactional public Map<String,Boolean> restore(@PathVariable String id,Authentication auth){lock();if(db.queryForObject("SELECT COUNT(*) FROM demo_gallery WHERE active=TRUE AND deleted=FALSE",Integer.class)>=30)throw new ResponseStatusException(HttpStatus.CONFLICT,"Máximo 30 imágenes publicadas.");if(db.update("UPDATE demo_gallery SET active=TRUE WHERE id=? AND active=FALSE AND deleted=FALSE",id)==0)throw new ResponseStatusException(HttpStatus.NOT_FOUND);audit(auth,"RESTAURAR_IMAGEN",id);return Map.of("active",true);}
}

