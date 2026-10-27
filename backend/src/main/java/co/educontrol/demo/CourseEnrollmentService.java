package co.educontrol.demo;

import java.text.Normalizer;
import java.util.*;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service @Profile("demo")
public class CourseEnrollmentService {
 private final JdbcTemplate db;private final BulkDataService data;
 public CourseEnrollmentService(JdbcTemplate db,BulkDataService data){this.db=db;this.data=data;}
 private static final Map<String,String> LEGACY=Map.of("biologia","Biología","tecnologia","Tecnología e informática","etica","Ética y religión","matematicas","Matemáticas","ingles","Inglés","quimica","Química","paz","Cátedra de Paz");
 static String subjectKey(String subject){String value=LEGACY.getOrDefault(subject.strip().toLowerCase(Locale.ROOT),subject);return Normalizer.normalize(value,Normalizer.Form.NFD).replaceAll("\\p{M}","").strip().replaceAll("\\s+"," ").toLowerCase(Locale.ROOT);}
 static String subjectLabel(Map<String,Object> row){String name=Objects.toString(row.get("subject_name"),"");return name.isBlank()?LEGACY.getOrDefault(Objects.toString(row.get("subject"),""),Objects.toString(row.get("subject"),"")):name;}
 public record Enrollment(String code,String name,int year,List<String> subjects,String academicStatus,boolean levelingAllowed,String startDate,String endDate){}
 public List<Enrollment> courses(String studentId){
  var students=db.queryForList("SELECT document,document_type,program,academic_year,academic_status,account_active,deleted FROM demo_students WHERE id=?",studentId);
  if(students.isEmpty()||Boolean.TRUE.equals(students.getFirst().get("deleted")))return List.of();var student=students.getFirst();
  var enrollments=data.list("matriculas",null).stream().filter(r->Objects.equals(student.get("document"),r.get("documento"))&&Objects.equals(student.get("document_type"),r.get("tipo_documento"))).toList();
  var result=new ArrayList<Enrollment>();
  for(var course:data.list("cursos",null)){
   String code=course.get("codigo_curso");var years=new TreeSet<Integer>();var linked=enrollments.stream().filter(r->code.equals(r.get("codigo_curso"))).toList();linked.forEach(r->years.add(Integer.parseInt(r.get("anio"))));
   boolean primary=code.equals(student.get("program"))||course.get("nombre").equals(student.get("program"));int primaryYear=((Number)student.get("academic_year")).intValue();if(primary)years.add(primaryYear);
   for(int year:years){var registered=linked.stream().filter(r->Integer.toString(year).equals(r.get("anio"))).toList();String status=registered.isEmpty()?student.get("academic_status").toString():registered.stream().map(r->r.getOrDefault("estado_academico","EN_CURSO")).filter(s->!s.equals("RETIRADO")).findFirst().orElse("RETIRADO");
    boolean allowed=Boolean.TRUE.equals(student.get("account_active"))&&"Activo".equals(course.get("estado_registro"))&&!Set.of("RETIRADO","PENDIENTE_MATRICULA").contains(status);
    result.add(new Enrollment(code,course.get("nombre"),year,CourseGradeController.subjects(course),status,allowed,course.getOrDefault("fecha_inicio",""),course.getOrDefault("fecha_fin","")));
   }
  }return result;
 }
 public record Selection(Enrollment course,String subject){}
 public Selection require(String studentId,String code,int year,String subject){
  var memberships=courses(studentId).stream().filter(c->c.year()==year&&c.levelingAllowed()&&(code==null||code.isBlank()||code.equals(c.code()))).toList();
  if(memberships.isEmpty())throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Solo puedes solicitar nivelaciones de tus cursos inscritos y habilitados en ese año.");
  var matches=new ArrayList<Selection>();for(var course:memberships)for(String name:course.subjects())if(subjectKey(name).equals(subjectKey(subject)))matches.add(new Selection(course,name));
  if(matches.isEmpty())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"La materia no pertenece al curso o no está configurada.");
  if(matches.size()!=1)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Selecciona el curso de la nivelación.");return matches.getFirst();
 }
 public boolean knownSubject(String value){return data.list("cursos",null).stream().flatMap(c->CourseGradeController.subjects(c).stream()).anyMatch(s->subjectKey(s).equals(subjectKey(value)));}
}
