package co.educontrol.demo;
import java.util.*;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
@RestController @Profile("demo") @RequestMapping("/api/demo")
public class ManualDataController {
 private final BulkDataService data;
 public ManualDataController(BulkDataService data){this.data=data;}
 public record Input(String originalKey,Map<String,String> values){}
 @PostMapping("/admin/data/manual/{module}") public Map<String,String> save(@PathVariable String module,@RequestBody Input input,Authentication auth){return data.saveRecord(module,input.originalKey(),input.values(),auth.getName());}
 @GetMapping("/programs") public ResponseEntity<List<Map<String,String>>> programs(){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(data.list("cursos",null).stream().filter(r->"SI".equals(r.get("publicado"))).map(r->Map.of("id",r.get("codigo_curso"),"name",r.get("nombre"),"description",r.getOrDefault("descripcion",""),"hours",r.getOrDefault("duracion_horas",""))).toList());}
}
