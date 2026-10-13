package co.educontrol.demo;

import java.util.*;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController @Profile("demo") @RequestMapping("/api/demo")
public class RegistrationController {
 private final JdbcTemplate db;private final BulkDataService data;
 public RegistrationController(JdbcTemplate db,BulkDataService data){this.db=db;this.data=data;}
 public record Settings(String form1,String form2,String neliName,String neliPhone,String estivenName,String estivenPhone){}
 @GetMapping("/registration/settings") public ResponseEntity<Settings> settings(){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(db.queryForObject("SELECT * FROM demo_registration_settings WHERE id=1",(rs,n)->new Settings(rs.getString("form1"),rs.getString("form2"),rs.getString("neli_name"),rs.getString("neli_phone"),rs.getString("estiven_name"),rs.getString("estiven_phone"))));}
 private boolean formUrl(String value){if(!BulkDataService.https(value))return false;var u=java.net.URI.create(value);return u.getHost().equalsIgnoreCase("forms.gle")||u.getHost().equalsIgnoreCase("docs.google.com")&&u.getPath().startsWith("/forms/");}
 @PostMapping("/admin/registration/settings") @Transactional public Settings settings(@RequestBody Settings input,Authentication auth){
  if(input.form1()==null||input.form2()==null||input.neliName()==null||input.estivenName()==null||input.neliPhone()==null||input.estivenPhone()==null)throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
  var s=new Settings(input.form1().strip(),input.form2().strip(),input.neliName().strip(),input.neliPhone().strip(),input.estivenName().strip(),input.estivenPhone().strip());
  if(s.form1().length()>500||s.form2().length()>500||!formUrl(s.form1())||!formUrl(s.form2())||s.form1().equals(s.form2())||s.neliName().isBlank()||s.estivenName().isBlank()||s.neliName().length()>80||s.estivenName().length()>80||!s.neliPhone().matches("[0-9]{10}")||!s.estivenPhone().matches("[0-9]{10}"))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Usa dos formularios Google diferentes, nombres y celulares de diez dígitos.");
  db.update("UPDATE demo_registration_settings SET form1=?,form2=?,neli_name=?,neli_phone=?,estiven_name=?,estiven_phone=? WHERE id=1",s.form1(),s.form2(),s.neliName(),s.neliPhone(),s.estivenName(),s.estivenPhone());
  db.update("INSERT INTO demo_audit(id,actor,action,resource_id) VALUES (?,?,?,?)",UUID.randomUUID().toString(),auth.getName(),"EDITAR_REGISTRO_CONTACTOS","1");return s;
 }
 @GetMapping("/agenda") public ResponseEntity<List<Map<String,String>>> agenda(@RequestParam(required=false) Integer year){if(year!=null&&(year<2000||year>2200))throw new ResponseStatusException(HttpStatus.BAD_REQUEST);var images=new HashSet<>(db.queryForList("SELECT code FROM demo_event_images WHERE active=TRUE",String.class));return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(data.list("agenda",year).stream().filter(r->"PUBLICADO".equals(r.get("estado"))).peek(r->{if(db.queryForObject("SELECT COUNT(*) FROM demo_publications WHERE kind='event' AND id=?",Integer.class,r.get("codigo_evento"))>0)r.put("programacion_url","/eventos/"+r.get("codigo_evento"));if("OTRO".equals(r.get("tipo_evento"))&&images.contains(r.get("codigo_evento")))r.put("imagen_url","/api/demo/agenda/images/"+r.get("codigo_evento"));}).sorted(Comparator.comparing(r->r.get("fecha_inicio"))).toList());}
 @PostMapping("/admin/data/agenda") public Map<String,String> create(@RequestBody Map<String,String> input,Authentication auth){return data.saveEvent(null,input,auth.getName());}
 @PostMapping("/admin/data/agenda/{code}") public Map<String,String> edit(@PathVariable String code,@RequestBody Map<String,String> input,Authentication auth){return data.saveEvent(code,input,auth.getName());}
 @GetMapping("/admin/data/agenda/deleted") public List<Map<String,String>> deleted(){return db.query("SELECT payload FROM demo_data_records WHERE module='agenda' AND deleted=TRUE",(rs,n)->data.decode(rs.getString(1)));}
 @PostMapping("/admin/data/agenda/{code}/delete") @Transactional public Map<String,Boolean> delete(@PathVariable String code,Authentication auth){return visibility(code,true,auth);}
 @PostMapping("/admin/data/agenda/{code}/restore") @Transactional public Map<String,Boolean> restore(@PathVariable String code,Authentication auth){return visibility(code,false,auth);}
 private Map<String,Boolean> visibility(String code,boolean deleted,Authentication auth){db.queryForObject("SELECT id FROM demo_data_lock WHERE id=1 FOR UPDATE",Integer.class);if(db.update("UPDATE demo_data_records SET deleted=? WHERE module='agenda' AND record_key=? AND deleted=?",deleted,code,!deleted)==0)throw new ResponseStatusException(HttpStatus.NOT_FOUND);db.update("INSERT INTO demo_audit(id,actor,action,resource_id) VALUES (?,?,?,?)",UUID.randomUUID().toString(),auth.getName(),deleted?"ELIMINAR_EVENTO":"RESTAURAR_EVENTO",code);return Map.of("deleted",deleted);}
}
