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
public class EventImageController {
 private final JdbcTemplate db;private final BulkDataService data; private final PublicationController publications;
 public EventImageController(JdbcTemplate db,BulkDataService data,PublicationController publications){this.db=db;this.data=data;this.publications=publications;}
 private String pageKey(String kind,String code){if(kind.equals("home")&&code.equals("hero"))return "HOME-hero";if(kind.equals("course")&&code.matches("[A-Za-z0-9_-]{1,50}"))return "COURSE-"+code;throw new ResponseStatusException(HttpStatus.NOT_FOUND);}
 private Map<String,String> event(String code){if(code.equals("HOME-hero"))return Map.of("tipo_evento","OTRO","estado","PUBLICADO");if(code.startsWith("COURSE-")){var c=data.list("cursos",null).stream().filter(r->code.substring(7).equals(r.get("codigo_curso"))).findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));return Map.of("tipo_evento","OTRO","estado","SI".equals(c.get("publicado"))&&"Activo".equals(c.get("estado_registro"))?"PUBLICADO":"BORRADOR");}if(code.startsWith("PHOTO-")){String parent=code.substring(6,code.lastIndexOf('-'));var p=publications.row("event",parent);return Map.of("tipo_evento","OTRO","estado",publications.publishedEvent(parent)&&"PUBLICADO".equals(p.get("state"))?"PUBLICADO":"BORRADOR");}if(code.startsWith("PUB-")){var r=publications.row("announcement",code.substring(4));return Map.of("tipo_evento","OTRO","estado","false".equals(r.get("deleted"))?r.get("state"):"BORRADOR");}return data.list("agenda",null).stream().filter(e->code.equals(e.get("codigo_evento"))).findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));}
 @GetMapping({"/agenda/images/{code}","/admin/data/agenda/{code}/image","/announcements/images/{code}","/admin/registration/announcements/{code}/image","/event-pages/{code}/photos/{photo}","/admin/registration/event-pages/{code}/photos/{photo}/image","/page-images/{kind}/{code}","/admin/registration/page-images/{kind}/{code}/image"})
 public ResponseEntity<byte[]> image(@PathVariable String code,jakarta.servlet.http.HttpServletRequest request,@PathVariable(required=false) String photo,@PathVariable(required=false) String kind){
  if(kind!=null)code=pageKey(kind,code);
  if(photo!=null){if(!photo.matches("[1-6]"))throw new ResponseStatusException(HttpStatus.BAD_REQUEST);code="PHOTO-"+code+"-"+photo;}
  if((request.getRequestURI().startsWith("/api/demo/announcements/")||request.getRequestURI().startsWith("/api/demo/admin/registration/announcements/")))code="PUB-"+code;var e=event(code);if(!"OTRO".equals(e.get("tipo_evento"))||!request.getRequestURI().contains("/admin/")&&!"PUBLICADO".equals(e.get("estado")))throw new ResponseStatusException(HttpStatus.NOT_FOUND);
  var found=db.query("SELECT mime,image FROM demo_event_images WHERE code=? AND active=TRUE",(rs,n)->Map.entry(rs.getString(1),rs.getBytes(2)),code);
  if(found.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND);
  return ResponseEntity.ok().header("X-Content-Type-Options","nosniff").cacheControl(CacheControl.noStore()).contentType(MediaType.parseMediaType(found.getFirst().getKey())).body(found.getFirst().getValue());
 }
 @PostMapping(value={"/admin/data/agenda/{code}/image","/admin/registration/announcements/{code}/image","/admin/registration/event-pages/{code}/photos/{photo}/image","/admin/registration/page-images/{kind}/{code}/image"},consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @Transactional
 public Map<String,String> upload(@PathVariable String code,@RequestParam MultipartFile file,Authentication auth,jakarta.servlet.http.HttpServletRequest request,@PathVariable(required=false) String photo,@PathVariable(required=false) String kind){
  if(kind!=null)code=pageKey(kind,code);
  if(photo!=null){if(!photo.matches("[1-6]"))throw new ResponseStatusException(HttpStatus.BAD_REQUEST);code="PHOTO-"+code+"-"+photo;}
  if((request.getRequestURI().startsWith("/api/demo/announcements/")||request.getRequestURI().startsWith("/api/demo/admin/registration/announcements/")))code="PUB-"+code;
  db.queryForObject("SELECT id FROM demo_data_lock WHERE id=1 FOR UPDATE",Integer.class);
  if(!"OTRO".equals(event(code).get("tipo_evento")))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Solo los eventos Otro admiten imagen.");
  if(file.isEmpty()||file.getSize()>5_000_000)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Imagen de hasta 5 MB.");
  byte[] bytes;String mime;
  try(var stream=file.getInputStream();var input=ImageIO.createImageInputStream(stream)){
   var readers=ImageIO.getImageReaders(input);if(!readers.hasNext())throw new IOException();var reader=readers.next();
   try{reader.setInput(input);String format=reader.getFormatName().toLowerCase(Locale.ROOT);if(!Set.of("png","jpeg","jpg").contains(format))throw new IOException();int w=reader.getWidth(0),h=reader.getHeight(0);if(w<1||h<1||(long)w*h>16_000_000)throw new IOException();var image=reader.read(0);var output=new ByteArrayOutputStream();ImageIO.write(image,format.equals("png")?"png":"jpg",output);bytes=output.toByteArray();mime=format.equals("png")?"image/png":"image/jpeg";if(bytes.length==0||bytes.length>5_000_000)throw new IOException();}finally{reader.dispose();}
  }catch(Exception e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Usa JPG o PNG válido, hasta 5 MB y 16 megapíxeles.");}
  if(db.update("UPDATE demo_event_images SET mime=?,image=?,active=TRUE WHERE code=?",mime,bytes,code)==0)db.update("INSERT INTO demo_event_images(code,mime,image,active) VALUES (?,?,?,TRUE)",code,mime,bytes);
  db.update("INSERT INTO demo_audit(id,actor,action,resource_id) VALUES (?,?,?,?)",UUID.randomUUID().toString(),auth.getName(),"IMAGEN_EVENTO",code);return Map.of("url","/api/demo/admin/data/agenda/"+code+"/image");
 }
 @PostMapping({"/admin/data/agenda/{code}/image/hide","/admin/registration/announcements/{code}/image/hide","/admin/registration/event-pages/{code}/photos/{photo}/image/hide","/admin/registration/page-images/{kind}/{code}/image/hide"}) @Transactional
 public Map<String,Boolean> hide(@PathVariable String code,Authentication auth,jakarta.servlet.http.HttpServletRequest request,@PathVariable(required=false) String photo,@PathVariable(required=false) String kind){if(kind!=null)code=pageKey(kind,code);if(photo!=null){if(!photo.matches("[1-6]"))throw new ResponseStatusException(HttpStatus.BAD_REQUEST);code="PHOTO-"+code+"-"+photo;}if((request.getRequestURI().startsWith("/api/demo/announcements/")||request.getRequestURI().startsWith("/api/demo/admin/registration/announcements/")))code="PUB-"+code;db.queryForObject("SELECT id FROM demo_data_lock WHERE id=1 FOR UPDATE",Integer.class);event(code);db.update("UPDATE demo_event_images SET active=FALSE WHERE code=?",code);db.update("INSERT INTO demo_audit(id,actor,action,resource_id) VALUES (?,?,?,?)",UUID.randomUUID().toString(),auth.getName(),"QUITAR_IMAGEN_EVENTO",code);return Map.of("active",false);}
}
