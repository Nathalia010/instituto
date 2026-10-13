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
public class SiteController {
 private final JdbcTemplate db;
 public SiteController(JdbcTemplate db){this.db=db;}
 public record Site(int id,String name,String address,String phone,String hours,String mapUrl,String imageUrl){}
 @GetMapping("/sites") public ResponseEntity<List<Site>> list(){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(db.query("SELECT * FROM demo_sites ORDER BY id",(rs,n)->new Site(rs.getInt("id"),rs.getString("name"),rs.getString("address"),rs.getString("phone"),rs.getString("hours"),rs.getString("map_url"),rs.getBoolean("image_active")?"/api/demo/sites/images/"+rs.getInt("id"):"")));}
 private void require(int id){if(id<1||id>3||db.queryForObject("SELECT COUNT(*) FROM demo_sites WHERE id=?",Integer.class,id)==0)throw new ResponseStatusException(HttpStatus.NOT_FOUND);}
 @PostMapping("/admin/registration/sites/{id}") @Transactional public Map<String,Boolean> save(@PathVariable int id,@RequestBody Site site,Authentication auth){require(id);if(site.name()==null||site.address()==null||site.phone()==null||site.hours()==null||site.mapUrl()==null)throw new ResponseStatusException(HttpStatus.BAD_REQUEST);if(site.name().isBlank()||site.name().length()>120||site.address().length()>300||site.hours().length()>300||site.mapUrl().length()>500||!site.phone().isBlank()&&!site.phone().matches("[0-9]{10}")||!site.mapUrl().isBlank()&&!BulkDataService.https(site.mapUrl()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST);db.update("UPDATE demo_sites SET name=?,address=?,phone=?,hours=?,map_url=? WHERE id=?",site.name().strip(),site.address().strip(),site.phone().strip(),site.hours().strip(),site.mapUrl().strip(),id);audit(auth,"EDITAR_SEDE",id);return Map.of("saved",true);}
 @GetMapping({"/sites/images/{id}","/admin/registration/sites/{id}/image"}) public ResponseEntity<byte[]> image(@PathVariable int id){require(id);var rows=db.query("SELECT mime,image FROM demo_sites WHERE id=? AND image_active=TRUE",(rs,n)->Map.entry(rs.getString(1),rs.getBytes(2)),id);if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND);return ResponseEntity.ok().contentType(MediaType.parseMediaType(rows.getFirst().getKey())).header("X-Content-Type-Options","nosniff").cacheControl(CacheControl.noStore()).body(rows.getFirst().getValue());}
 @PostMapping(value="/admin/registration/sites/{id}/image",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @Transactional public Map<String,Boolean> image(@PathVariable int id,@RequestParam MultipartFile file,Authentication auth){require(id);if(file.isEmpty()||file.getSize()>5_000_000)throw new ResponseStatusException(HttpStatus.BAD_REQUEST);byte[] bytes;String mime;
  try(var stream=file.getInputStream();var input=ImageIO.createImageInputStream(stream)){var readers=ImageIO.getImageReaders(input);if(!readers.hasNext())throw new IOException();var reader=readers.next();try{reader.setInput(input);String format=reader.getFormatName().toLowerCase(Locale.ROOT);if(!Set.of("png","jpeg","jpg").contains(format))throw new IOException();int w=reader.getWidth(0),h=reader.getHeight(0);if(w<1||h<1||(long)w*h>16_000_000)throw new IOException();var decoded=reader.read(0);var output=new ByteArrayOutputStream();ImageIO.write(decoded,format.equals("png")?"png":"jpg",output);bytes=output.toByteArray();mime=format.equals("png")?"image/png":"image/jpeg";if(bytes.length==0||bytes.length>5_000_000)throw new IOException();}finally{reader.dispose();}}catch(Exception e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Usa JPG o PNG válido.");}
  db.update("UPDATE demo_sites SET mime=?,image=?,image_active=TRUE WHERE id=?",mime,bytes,id);audit(auth,"IMAGEN_SEDE",id);return Map.of("saved",true);
 }
 @PostMapping("/admin/registration/sites/{id}/image/hide") @Transactional public Map<String,Boolean> hide(@PathVariable int id,Authentication auth){require(id);db.update("UPDATE demo_sites SET image_active=FALSE WHERE id=?",id);audit(auth,"QUITAR_IMAGEN_SEDE",id);return Map.of("active",false);}
 private void audit(Authentication auth,String action,int id){db.update("INSERT INTO demo_audit(id,actor,action,resource_id) VALUES (?,?,?,?)",UUID.randomUUID().toString(),auth.getName(),action,String.valueOf(id));}
}
