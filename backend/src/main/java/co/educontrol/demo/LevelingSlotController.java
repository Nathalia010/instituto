package co.educontrol.demo;
import java.time.*;
import java.util.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
@RestController @Profile("demo") @RequestMapping("/api/demo")
public class LevelingSlotController {
 private final JdbcTemplate db;
 public LevelingSlotController(JdbcTemplate db){this.db=db;}
 public record Input(@NotBlank String date,@NotBlank String time,@NotBlank String subject,@NotBlank @Size(max=180) String teacher,@NotBlank @Size(max=300) String location,@Min(1) @Max(200) int capacity,boolean active){}
 private List<Map<String,Object>> list(){return db.queryForList("SELECT s.*,(SELECT COUNT(*) FROM demo_levelings n WHERE n.slot_id=s.id AND n.deleted=FALSE AND n.status IN ('PENDIENTE','CONFIRMADA')) AS reserved FROM demo_leveling_slots s ORDER BY slot_date,slot_time");}
 @GetMapping("/admin/leveling-slots") public List<Map<String,Object>> all(){return list();}
 @GetMapping("/student/leveling-slots") public List<Map<String,Object>> available(){var now=LocalDateTime.now(ZoneId.of("America/Bogota"));return list().stream().filter(s->Boolean.TRUE.equals(s.get("active"))&&LocalDateTime.parse(s.get("slot_date")+"T"+s.get("slot_time")).isAfter(now)&&((Number)s.get("reserved")).intValue()<((Number)s.get("capacity")).intValue()).toList();}
 @GetMapping("/leveling-dates") public List<Map<String,Object>> publicDates(){return available().stream().map(s->{Map<String,Object> r=new HashMap<>();for(String k:List.of("slot_date","slot_time","subject","location"))r.put(k,s.get(k));return r;}).toList();}
 @PostMapping("/admin/leveling-slots") @Transactional public Map<String,String> create(@Valid @RequestBody Input input,Authentication auth){return save(null,input,auth);}
 @PostMapping("/admin/leveling-slots/{id}") @Transactional public Map<String,String> edit(@PathVariable String id,@Valid @RequestBody Input input,Authentication auth){return save(id,input,auth);}
 private Map<String,String> save(String id,Input input,Authentication auth){db.queryForObject("SELECT id FROM demo_data_lock WHERE id=1 FOR UPDATE",Integer.class);try{if(!input.time().matches("[0-9]{2}:[0-9]{2}"))throw new IllegalArgumentException();LocalTime.parse(input.time());LocalDate.parse(input.date());if(id==null&&LocalDateTime.parse(input.date()+"T"+input.time()).isBefore(LocalDateTime.now(ZoneId.of("America/Bogota"))))throw new IllegalArgumentException();}catch(Exception e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST);}if(!input.subject().equals("todas")&&!LevelingController.SUBJECTS.contains(input.subject()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
  if(id!=null){var existing=list().stream().filter(s->id.equals(s.get("id"))).findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));int reserved=((Number)existing.get("reserved")).intValue();if(input.capacity()<reserved)throw new ResponseStatusException(HttpStatus.CONFLICT,"La capacidad es menor a las reservas.");if(reserved>0&&(!input.date().equals(existing.get("slot_date"))||!input.time().equals(existing.get("slot_time"))||!input.subject().equals(existing.get("subject"))||!input.teacher().strip().equals(existing.get("teacher"))||!input.location().strip().equals(existing.get("location"))))throw new ResponseStatusException(HttpStatus.CONFLICT,"Una fecha con reservas conserva horario, materia, docente y lugar.");}
  String key=id==null?UUID.randomUUID().toString():id;if(id==null)db.update("INSERT INTO demo_leveling_slots(id,slot_date,slot_time,subject,teacher,location,capacity,active) VALUES (?,?,?,?,?,?,?,?)",key,input.date(),input.time(),input.subject(),input.teacher().strip(),input.location().strip(),input.capacity(),input.active());else db.update("UPDATE demo_leveling_slots SET slot_date=?,slot_time=?,subject=?,teacher=?,location=?,capacity=?,active=? WHERE id=?",input.date(),input.time(),input.subject(),input.teacher().strip(),input.location().strip(),input.capacity(),input.active(),key);db.update("INSERT INTO demo_audit(id,actor,action,resource_id) VALUES (?,?,?,?)",UUID.randomUUID().toString(),auth.getName(),"FECHA_NIVELACION",key);return Map.of("id",key);
 }
}
