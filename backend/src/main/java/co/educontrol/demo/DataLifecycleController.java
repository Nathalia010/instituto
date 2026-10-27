package co.educontrol.demo;

import java.util.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController @Profile("demo") @RequestMapping("/api/demo/admin/data/lifecycle")
public class DataLifecycleController {
 private final JdbcTemplate db;private final BulkDataService data;private final DemoAdminEdits access;
 public DataLifecycleController(JdbcTemplate db,BulkDataService data,DemoAdminEdits access){this.db=db;this.data=data;this.access=access;}
 private void validate(String module){if(!Set.of("estudiantes","docentes","cursos").contains(module))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Este catálogo no admite esta operación.");}
 @GetMapping("/{module}/deleted") public List<Map<String,String>> deleted(@PathVariable String module,@RequestParam(required=false) Integer year){validate(module);if(year!=null&&(year<2000||year>2200))throw new ResponseStatusException(HttpStatus.BAD_REQUEST);return data.list(module,year,true);}
 public record Change(@NotBlank String key,@NotBlank String action){}
 @PostMapping("/{module}") @Transactional
 public Map<String,Boolean> change(@PathVariable String module,@Valid @RequestBody Change input,Authentication auth){
  validate(module);if(!Set.of("activate","deactivate","delete","restore").contains(input.action()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Acción inválida.");
  db.queryForObject("SELECT id FROM demo_data_lock WHERE id=1 FOR UPDATE",Integer.class);
  boolean restore=input.action().equals("restore"),remove=input.action().equals("delete"),active=input.action().equals("activate");
  var records=db.queryForList("SELECT deleted FROM demo_data_records WHERE module=? AND record_key=? FOR UPDATE",module,input.key());
  var accounts=module.equals("estudiantes")?db.queryForList("SELECT id,deleted FROM demo_students WHERE document_type || ':' || document=? FOR UPDATE",input.key()):List.<Map<String,Object>>of();
  if(records.isEmpty()&&accounts.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Registro no encontrado.");
  boolean deleted=Boolean.TRUE.equals((accounts.isEmpty()?records:accounts).getFirst().get("deleted"));
  if(deleted!=restore)throw new ResponseStatusException(HttpStatus.CONFLICT,deleted?"Restaura el registro antes de activarlo.":"El registro no está eliminado.");
  if(module.equals("estudiantes")&&accounts.isEmpty())throw new ResponseStatusException(HttpStatus.CONFLICT,"La cuenta del estudiante todavía no está disponible.");
  if(!accounts.isEmpty()){
   String id=accounts.getFirst().get("id").toString();
   if(remove||restore)db.update("UPDATE demo_students SET deleted=?,account_active=FALSE,account_version=account_version+1 WHERE id=?",remove,id);
   else access.studentAccess(id,new DemoAdminEdits.StudentAccess(active));
  }
  if(remove||restore)db.update("UPDATE demo_data_records SET deleted=?,active=FALSE WHERE module=? AND record_key=?",remove,module,input.key());
  else db.update("UPDATE demo_data_records SET active=? WHERE module=? AND record_key=?",active,module,input.key());
  db.update("INSERT INTO demo_audit(id,actor,action,resource_id) VALUES (?,?,?,?)",UUID.randomUUID().toString(),auth.getName(),input.action().toUpperCase(Locale.ROOT)+"_"+module.toUpperCase(Locale.ROOT),input.key());
  return Map.of("ok",true);
 }
}
