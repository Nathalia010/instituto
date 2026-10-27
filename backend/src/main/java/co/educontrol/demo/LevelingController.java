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
public class LevelingController {
 private final JdbcTemplate db;private final CourseEnrollmentService enrollments;
 static final Set<String> SUBJECTS=Set.of("biologia","tecnologia","etica","matematicas","ingles","quimica","paz");
 public LevelingController(JdbcTemplate db,CourseEnrollmentService enrollments){this.db=db;this.enrollments=enrollments;}
 public record Request(@NotBlank @Size(max=180) String subject,@NotBlank @Size(max=80) String period,@Min(2000) @Max(2200) int year,@NotBlank @Size(max=500) String reason,@NotBlank String preferredDate,@NotBlank String preferredTime,@Size(max=50) String courseCode){}
 public record Update(@NotBlank String status,@Size(max=180) String teacher,@Size(max=300) String location,String confirmedDate,String confirmedTime,@Size(max=500) String result,@Size(max=500) String response,@Size(max=50) String courseCode){}
 private String text(String value){return value==null?"":value.strip();}
 private void dateTime(String date,String time,boolean future){try{var d=LocalDate.parse(date);LocalTime.parse(time);if(!time.matches("[0-9]{2}:[0-9]{2}")||future&&d.isBefore(LocalDate.now(ZoneId.of("America/Bogota"))))throw new IllegalArgumentException();}catch(Exception e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Revisa la fecha y hora de Colombia.");}}
 private void lock(){db.queryForObject("SELECT id FROM demo_data_lock WHERE id=1 FOR UPDATE",Integer.class);}
 private List<Map<String,Object>> labels(List<Map<String,Object>> rows){for(var row:rows){row.put("subject_name",CourseEnrollmentService.subjectLabel(row));if(Objects.toString(row.get("course_name"),"").isBlank())row.put("course_name","Sin curso registrado · solicitud anterior");}return rows;}
 @GetMapping("/student/levelings") public List<Map<String,Object>> mine(Authentication auth){return labels(db.queryForList("SELECT n.*,s.name AS student_name FROM demo_levelings n JOIN demo_students s ON s.id=n.student_id WHERE n.student_id=? AND n.deleted=FALSE ORDER BY n.created_at DESC",auth.getName()));}
 @PostMapping("/student/levelings") @ResponseStatus(HttpStatus.CREATED) @Transactional public Map<String,String> create(@Valid @RequestBody Request input,Authentication auth){
  lock();
  if(!Set.of("PRIMER_SEMESTRE","SEGUNDO_SEMESTRE","CICLOS").contains(input.period()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST);dateTime(input.preferredDate(),input.preferredTime(),true);
  var selected=enrollments.require(auth.getName(),input.courseCode(),input.year(),input.subject());String subject=CourseEnrollmentService.subjectKey(selected.subject());String course=selected.course().code();
  db.queryForObject("SELECT id FROM demo_students WHERE id=? FOR UPDATE",String.class,auth.getName());
  if(db.queryForObject("SELECT COUNT(*) FROM demo_levelings WHERE student_id=? AND course_code=? AND subject=? AND academic_year=? AND period=? AND deleted=FALSE AND status IN ('PENDIENTE','CONFIRMADA')",Integer.class,auth.getName(),course,subject,input.year(),input.period())>0)throw new ResponseStatusException(HttpStatus.CONFLICT,"Ya tienes una solicitud activa para esta materia, curso y periodo.");
  String id=UUID.randomUUID().toString();db.update("INSERT INTO demo_levelings(id,student_id,subject,period,academic_year,reason,preferred_date,preferred_time,course_code,course_name,subject_name) VALUES (?,?,?,?,?,?,?,?,?,?,?)",id,auth.getName(),subject,input.period(),input.year(),input.reason().strip(),input.preferredDate(),input.preferredTime(),course,selected.course().name(),selected.subject());audit(auth,"SOLICITAR_NIVELACION",id);return Map.of("id",id,"status","PENDIENTE");
 }
 @GetMapping("/admin/levelings") public List<Map<String,Object>> all(){var rows=labels(db.queryForList("SELECT n.*,s.name AS student_name,s.document AS student_document,s.phone AS student_phone,s.program AS student_program FROM demo_levelings n JOIN demo_students s ON s.id=n.student_id ORDER BY n.created_at DESC"));for(var row:rows)if(row.get("course_code").toString().isBlank())row.put("course_options",enrollments.courses(row.get("student_id").toString()).stream().filter(c->c.year()==((Number)row.get("academic_year")).intValue()&&c.levelingAllowed()).toList());return rows;}
 @PostMapping("/admin/levelings/{id}") @Transactional public Map<String,Boolean> update(@PathVariable String id,@Valid @RequestBody Update input,Authentication auth){
  lock();var rows=db.queryForList("SELECT * FROM demo_levelings WHERE id=? AND deleted=FALSE FOR UPDATE",id);if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND);var row=rows.getFirst();
  if(!Set.of("PENDIENTE","CONFIRMADA","FINALIZADA","CANCELADA").contains(input.status()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
  String date=text(input.confirmedDate()),time=text(input.confirmedTime()),teacher=text(input.teacher()),location=text(input.location()),result=text(input.result());
  if(!date.isBlank()||!time.isBlank())dateTime(date,time,false);
  if(Set.of("CONFIRMADA","FINALIZADA").contains(input.status())&&(date.isBlank()||time.isBlank()||teacher.isBlank()||location.isBlank()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Para confirmar, completa docente, fecha, hora y lugar o modalidad.");
  if(input.status().equals("FINALIZADA")&&result.isBlank())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Registra el resultado de la nivelación.");
  if(input.courseCode()!=null&&!input.courseCode().isBlank()){
   if(!row.get("course_code").toString().isBlank()&&!input.courseCode().equals(row.get("course_code")))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"La solicitud ya tiene un curso asociado.");
   var selected=enrollments.require(row.get("student_id").toString(),input.courseCode(),((Number)row.get("academic_year")).intValue(),row.get("subject").toString());
   if(Set.of("PENDIENTE","CONFIRMADA").contains(input.status())&&db.queryForObject("SELECT COUNT(*) FROM demo_levelings WHERE id<>? AND student_id=? AND course_code=? AND subject=? AND academic_year=? AND period=? AND deleted=FALSE AND status IN ('PENDIENTE','CONFIRMADA')",Integer.class,id,row.get("student_id"),selected.course().code(),CourseEnrollmentService.subjectKey(selected.subject()),row.get("academic_year"),row.get("period"))>0)throw new ResponseStatusException(HttpStatus.CONFLICT,"Ya hay una solicitud activa para ese curso y materia.");
   db.update("UPDATE demo_levelings SET course_code=?,course_name=?,subject=?,subject_name=? WHERE id=?",selected.course().code(),selected.course().name(),CourseEnrollmentService.subjectKey(selected.subject()),selected.subject(),id);
  }
  if(Set.of("PENDIENTE","CONFIRMADA").contains(input.status())){
   var effective=db.queryForMap("SELECT * FROM demo_levelings WHERE id=?",id);
   if(db.queryForObject("SELECT COUNT(*) FROM demo_levelings WHERE id<>? AND student_id=? AND course_code=? AND subject=? AND academic_year=? AND period=? AND deleted=FALSE AND status IN ('PENDIENTE','CONFIRMADA')",Integer.class,id,effective.get("student_id"),effective.get("course_code"),effective.get("subject"),effective.get("academic_year"),effective.get("period"))>0)throw new ResponseStatusException(HttpStatus.CONFLICT,"Ya existe una solicitud activa para ese curso y materia.");
   if(!Set.of("PENDIENTE","CONFIRMADA").contains(row.get("status"))&&row.get("slot_id")!=null&&reserved(row.get("slot_id").toString())>=db.queryForObject("SELECT capacity FROM demo_leveling_slots WHERE id=?",Integer.class,row.get("slot_id")))throw new ResponseStatusException(HttpStatus.CONFLICT,"La fecha está completa.");
  }
  db.update("UPDATE demo_levelings SET status=?,teacher=?,location=?,confirmed_date=?,confirmed_time=?,result=?,response=? WHERE id=?",input.status(),teacher,location,date,time,result,text(input.response()),id);audit(auth,"GESTIONAR_NIVELACION",id);return Map.of("saved",true);
 }
 private void audit(Authentication auth,String action,String id){db.update("INSERT INTO demo_audit(id,actor,action,resource_id) VALUES (?,?,?,?)",UUID.randomUUID().toString(),auth.getName(),action,id);}
 public record Booking(@NotBlank String slotId,@NotBlank @Size(max=180) String subject,@NotBlank String period,@Min(2000) @Max(2200) int year,@NotBlank @Size(max=500) String reason,@Size(max=50) String courseCode){}
 @PostMapping("/student/levelings/book") @ResponseStatus(HttpStatus.CREATED) @Transactional public Map<String,String> book(@Valid @RequestBody Booking input,Authentication auth){
  lock();var slots=db.queryForList("SELECT * FROM demo_leveling_slots WHERE id=? AND active=TRUE FOR UPDATE",input.slotId());if(slots.isEmpty())throw new ResponseStatusException(HttpStatus.CONFLICT,"La fecha ya no está disponible.");var slot=slots.getFirst();dateTime(slot.get("slot_date").toString(),slot.get("slot_time").toString(),true);
  if(!LocalDateTime.parse(slot.get("slot_date")+"T"+slot.get("slot_time")).isAfter(LocalDateTime.now(ZoneId.of("America/Bogota"))))throw new ResponseStatusException(HttpStatus.CONFLICT);
  enrollments.require(auth.getName(),input.courseCode(),input.year(),input.subject());
  if(!slot.get("subject").equals("todas")&&!CourseEnrollmentService.subjectKey(slot.get("subject").toString()).equals(CourseEnrollmentService.subjectKey(input.subject())))throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
  if(reserved(input.slotId())>=((Number)slot.get("capacity")).intValue())throw new ResponseStatusException(HttpStatus.CONFLICT,"Se agotaron los cupos.");
  var result=create(new Request(input.subject(),input.period(),input.year(),input.reason(),slot.get("slot_date").toString(),slot.get("slot_time").toString(),input.courseCode()),auth);db.update("UPDATE demo_levelings SET slot_id=?,teacher=?,location=? WHERE id=?",input.slotId(),slot.get("teacher"),slot.get("location"),result.get("id"));return result;
 }
 private int reserved(String slot){return db.queryForObject("SELECT COUNT(*) FROM demo_levelings WHERE slot_id=? AND deleted=FALSE AND status IN ('PENDIENTE','CONFIRMADA')",Integer.class,slot);}
 @PostMapping("/admin/levelings/{id}/delete") @Transactional public Map<String,Boolean> delete(@PathVariable String id,Authentication auth){lock();if(db.update("UPDATE demo_levelings SET deleted=TRUE WHERE id=? AND deleted=FALSE",id)==0)throw new ResponseStatusException(HttpStatus.NOT_FOUND);audit(auth,"ELIMINAR_NIVELACION",id);return Map.of("deleted",true);}
 @PostMapping("/admin/levelings/{id}/restore") @Transactional public Map<String,Boolean> restore(@PathVariable String id,Authentication auth){lock();var rows=db.queryForList("SELECT * FROM demo_levelings WHERE id=? AND deleted=TRUE",id);if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND);var row=rows.getFirst();if(Set.of("PENDIENTE","CONFIRMADA").contains(row.get("status"))){if(db.queryForObject("SELECT COUNT(*) FROM demo_levelings WHERE student_id=? AND course_code=? AND subject=? AND period=? AND academic_year=? AND deleted=FALSE AND status IN ('PENDIENTE','CONFIRMADA')",Integer.class,row.get("student_id"),row.get("course_code"),row.get("subject"),row.get("period"),row.get("academic_year"))>0)throw new ResponseStatusException(HttpStatus.CONFLICT);if(row.get("slot_id")!=null){String slot=row.get("slot_id").toString();if(reserved(slot)>=db.queryForObject("SELECT capacity FROM demo_leveling_slots WHERE id=?",Integer.class,slot))throw new ResponseStatusException(HttpStatus.CONFLICT,"La fecha está completa.");}}db.update("UPDATE demo_levelings SET deleted=FALSE WHERE id=?",id);audit(auth,"RESTAURAR_NIVELACION",id);return Map.of("deleted",false);}
}
