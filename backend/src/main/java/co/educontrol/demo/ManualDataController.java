package co.educontrol.demo;
import java.util.*;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
@RestController @Profile("demo") @RequestMapping("/api/demo")
public class ManualDataController {
 private final BulkDataService data;private final org.springframework.jdbc.core.JdbcTemplate db;
 public ManualDataController(BulkDataService data,org.springframework.jdbc.core.JdbcTemplate db){this.data=data;this.db=db;}
 private boolean image(String code){return db.queryForObject("SELECT COUNT(*) FROM demo_event_images WHERE code=? AND active=TRUE",Integer.class,code)>0;}
 @GetMapping("/home/appearance") public Map<String,String> appearance(){return Map.of("heroImage",image("HOME-hero")?"/api/demo/page-images/home/hero":"");}
 public record Input(String originalKey,Map<String,String> values){}
 @PostMapping("/admin/data/manual/{module}") public Map<String,String> save(@PathVariable String module,@RequestBody Input input,Authentication auth){return data.saveRecord(module,input.originalKey(),input.values(),auth.getName());}
 @GetMapping("/programs") public ResponseEntity<List<Map<String,String>>> programs(){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(data.list("cursos",null).stream().filter(r->"SI".equals(r.get("publicado"))&&"Activo".equals(r.get("estado_registro"))).map(r->Map.of("id",r.get("codigo_curso"),"name",r.get("nombre"),"description",r.getOrDefault("descripcion",""),"hours",r.getOrDefault("duracion_horas",""),"image",image("COURSE-"+r.get("codigo_curso"))?"/api/demo/page-images/course/"+r.get("codigo_curso"):"")).toList());}
}
