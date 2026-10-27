package co.educontrol.demo;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.Normalizer;
import java.util.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController @Profile("demo") @RequestMapping("/api/demo/admin/data/courses")
public class CourseGradeController {
 private final JdbcTemplate db;private final CourseAdminController courses;
 public CourseGradeController(JdbcTemplate db,CourseAdminController courses){this.db=db;this.courses=courses;}
 static String subjectKey(String name){return Normalizer.normalize(name.strip().replaceAll("\\s+"," "),Normalizer.Form.NFC).toLowerCase(Locale.ROOT);}
 static List<String> subjects(Map<String,String> course){
  var result=new ArrayList<String>();var keys=new HashSet<String>();
  for(String raw:course.getOrDefault("materias","").split("[\\n;]+")){
   String name=raw.strip().replaceAll("\\s+"," ");if(name.isBlank())continue;
   if(name.length()>180||result.size()>=50)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Usa hasta 50 materias de máximo 180 caracteres.");
   if(!keys.add(subjectKey(name)))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Hay materias repetidas.");result.add(name);
  }return result;
 }
 public record LevelingInfo(String subject,String status,String date){}
 public record GradeRow(String studentId,String name,String document,String email,String phone,int year,String academicStatus,Map<String,BigDecimal> grades,BigDecimal average,String status,String version,List<LevelingInfo> levelings,int unassignedLevelings){}
 static BigDecimal average(Map<String,BigDecimal> grades){var values=grades.values().stream().filter(Objects::nonNull).toList();return values.isEmpty()?null:values.stream().reduce(BigDecimal.ZERO,BigDecimal::add).divide(BigDecimal.valueOf(values.size()),2,java.math.RoundingMode.HALF_UP);}
 public record GradeBook(Map<String,String> course,List<String> subjects,BigDecimal passing,List<GradeRow> rows){}
 static String result(List<String> subjects,Map<String,BigDecimal> grades,BigDecimal passing){
  if(subjects.isEmpty())return "SIN_MATERIAS";
  if(grades.values().stream().anyMatch(g->g!=null&&g.compareTo(passing)<0))return "NO_APRUEBA";
  return subjects.stream().allMatch(s->grades.get(s)!=null)?"APRUEBA":"PENDIENTE";
 }
 private String version(List<String> subjects,Map<String,BigDecimal> grades,BigDecimal passing){
  try{StringBuilder text=new StringBuilder(passing.toPlainString());for(String s:subjects)text.append('\n').append(s).append(':').append(grades.get(s));return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.toString().getBytes(StandardCharsets.UTF_8)));}catch(Exception ex){throw new IllegalStateException(ex);}
 }
 @SuppressWarnings("unchecked") private GradeBook book(Map<String,Object> bundle){
  var course=(Map<String,String>)bundle.get("course");var subjects=subjects(course);String code=course.get("codigo_curso");
  BigDecimal passing=new BigDecimal(course.getOrDefault("nota_aprobacion","").isBlank()?"3":course.get("nota_aprobacion"));
  var result=new ArrayList<GradeRow>();
  for(var member:(List<Map<String,Object>>)bundle.get("students"))for(String year:(Collection<String>)member.get("years")){
   String id=member.get("id").toString();int academicYear=Integer.parseInt(year);var values=new HashMap<String,BigDecimal>();
   // Existing portal notes are used only for the student's own program/year.
   var primary=db.queryForMap("SELECT program,academic_year FROM demo_students WHERE id=?",id);
   if((code.equals(primary.get("program"))||course.get("nombre").equals(primary.get("program")))&&academicYear==((Number)primary.get("academic_year")).intValue())
    db.queryForList("SELECT subject,grade FROM demo_notes WHERE student_id=? AND academic_year=? ORDER BY id",id,academicYear).forEach(n->values.put(subjectKey(n.get("subject").toString()),(BigDecimal)n.get("grade")));
   db.queryForList("SELECT subject_key,grade FROM demo_course_grades WHERE course_code=? AND student_id=? AND academic_year=?",code,id,academicYear).forEach(n->values.put(n.get("subject_key").toString(),(BigDecimal)n.get("grade")));
   var grades=new LinkedHashMap<String,BigDecimal>();subjects.forEach(s->grades.put(s,values.get(subjectKey(s))));
   var levelings=db.queryForList("SELECT subject,subject_name,status,preferred_date,confirmed_date FROM demo_levelings WHERE student_id=? AND course_code=? AND academic_year=? AND deleted=FALSE ORDER BY created_at DESC",id,code,academicYear).stream().map(n->new LevelingInfo(CourseEnrollmentService.subjectLabel(n),n.get("status").toString(),n.get("confirmed_date").toString().isBlank()?n.get("preferred_date").toString():n.get("confirmed_date").toString())).toList();
   int unassigned=db.queryForObject("SELECT COUNT(*) FROM demo_levelings WHERE student_id=? AND academic_year=? AND course_code='' AND deleted=FALSE",Integer.class,id,academicYear);
   result.add(new GradeRow(id,member.get("name").toString(),member.get("document").toString(),member.get("email").toString(),member.get("phone").toString(),academicYear,member.get("academic_status").toString(),grades,average(grades),result(subjects,grades,passing),version(subjects,grades,passing),levelings,unassigned));
  }
  return new GradeBook(course,subjects,passing,result);
 }
 @SuppressWarnings("unchecked") private Map<String,Object> bundle(String code,Integer year){return courses.courses(year).stream().filter(c->code.equals(((Map<String,String>)c.get("course")).get("codigo_curso"))).findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Curso no encontrado."));}
 @GetMapping("/{code}/grades") public GradeBook grades(@PathVariable String code,@RequestParam(required=false) Integer year){return book(bundle(code,year));}
 public record Edit(@NotBlank String studentId,@Min(2000) @Max(2200) int year,@NotNull Map<String,BigDecimal> grades,@NotBlank String version){}
 public List<Map<String,Object>> portalNotes(String id){
  var primary=db.queryForMap("SELECT program FROM demo_students WHERE id=?",id);var notes=new LinkedHashMap<String,Map<String,Object>>();
  for(var note:db.queryForList("SELECT subject,grade,academic_year AS \"year\" FROM demo_notes WHERE student_id=? ORDER BY academic_year,subject",id))notes.put(primary.get("program")+":"+note.get("year")+":"+subjectKey(note.get("subject").toString()),note);
  for(var note:db.queryForList("SELECT course_code,subject_key,subject_name,grade,academic_year FROM demo_course_grades WHERE student_id=? ORDER BY academic_year,course_code,subject_name",id)){
   String key=note.get("course_code")+":"+note.get("academic_year")+":"+note.get("subject_key");
   if(note.get("grade")==null){notes.remove(key);continue;}
   notes.put(key,new LinkedHashMap<>(Map.of("subject",note.get("subject_name"),"grade",note.get("grade"),"year",note.get("academic_year"),"course",note.get("course_code"))));
  }return new ArrayList<>(notes.values());
 }
 @PostMapping("/{code}/grades") @Transactional
 public GradeRow save(@PathVariable String code,@Valid @RequestBody Edit input,Authentication auth){
  db.queryForObject("SELECT id FROM demo_data_lock WHERE id=1 FOR UPDATE",Integer.class);
  var current=grades(code,input.year());var before=current.rows().stream().filter(r->r.studentId().equals(input.studentId())&&r.year()==input.year()).findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.BAD_REQUEST,"El estudiante debe estar activo y vinculado al curso en ese año."));
  if(!before.version().equals(input.version())||!input.grades().keySet().equals(new HashSet<>(current.subjects())))throw new ResponseStatusException(HttpStatus.CONFLICT,"Las notas o materias cambiaron. Actualiza antes de guardar.");
  for(var grade:input.grades().values())if(grade!=null&&(grade.signum()<0||grade.compareTo(new BigDecimal("5"))>0||grade.scale()>2))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Notas entre 0 y 5, con máximo dos decimales.");
  for(var entry:input.grades().entrySet()){
   BigDecimal grade=entry.getValue();
   db.update("MERGE INTO demo_course_grades(course_code,student_id,academic_year,subject_key,subject_name,grade) KEY(course_code,student_id,academic_year,subject_key) VALUES (?,?,?,?,?,?)",code,input.studentId(),input.year(),subjectKey(entry.getKey()),entry.getKey(),grade);
  }
  db.update("INSERT INTO demo_audit(id,actor,action,resource_id) VALUES (?,?,?,?)",UUID.randomUUID().toString(),auth.getName(),"EDITAR_NOTAS_CURSO",code+":"+input.studentId()+":"+input.year());
  return grades(code,input.year()).rows().stream().filter(r->r.studentId().equals(input.studentId())&&r.year()==input.year()).findFirst().orElseThrow();
 }
 static String statusLabel(String value){return switch(value){case "APRUEBA"->"Aprueba";case "NO_APRUEBA"->"No aprueba";case "SIN_MATERIAS"->"Sin materias";default->"Pendiente de notas";};}
 @GetMapping("/export") public ResponseEntity<byte[]> export(@RequestParam(required=false) String course,@RequestParam(required=false) Integer year)throws IOException{
  var bundles=courses.courses(year);if(course!=null)bundles=List.of(bundle(course,year));
  try(var workbook=new XSSFWorkbook();var output=new ByteArrayOutputStream()){
   var style=workbook.createCellStyle();style.setFillForegroundColor(IndexedColors.TEAL.getIndex());style.setFillPattern(FillPatternType.SOLID_FOREGROUND);var font=workbook.createFont();font.setBold(true);font.setColor(IndexedColors.WHITE.getIndex());style.setFont(font);
   var summary=workbook.createSheet("Resumen cursos");var fields=List.of("Código","Curso","Estado","Inicio","Finalización","Materias","Nota mínima","Estudiantes / año");writeRow(summary,0,fields,style);int index=1;
   for(var item:bundles){
    var book=book(item);var c=book.course();writeRow(summary,index,List.of(c.get("codigo_curso"),c.get("nombre"),c.get("estado_registro"),c.getOrDefault("fecha_inicio",""),c.getOrDefault("fecha_fin",""),String.join("; ",book.subjects()),book.passing(),book.rows().size()),null);
    var sheet=workbook.createSheet(WorkbookUtil.createSafeSheetName(index++ +" "+c.get("codigo_curso")));
    var headers=new ArrayList<String>(List.of("Estudiante","Documento","Correo","Teléfono","Año","Estado académico"));for(String subject:book.subjects()){headers.add(subject);headers.add(subject+" · Resultado");}headers.addAll(List.of("Solicitó nivelación","Detalle de nivelaciones","Promedio","Notas registradas / materias","Resultado general"));writeRow(sheet,0,headers,style);int row=1;
    for(var student:book.rows()){
     var cells=new ArrayList<Object>(List.of(student.name(),student.document(),student.email(),student.phone(),student.year(),student.academicStatus()));
     for(String subject:book.subjects()){var grade=student.grades().get(subject);cells.add(grade==null?"":grade);cells.add(grade==null?"Sin nota":grade.compareTo(book.passing())>=0?"Aprueba":"No aprueba");}cells.add(!student.levelings().isEmpty()?"Sí":student.unassignedLevelings()>0?"Por asociar":"No");cells.add(student.levelings().stream().map(n->n.subject()+": "+n.status()+" · "+n.date()).collect(java.util.stream.Collectors.joining("; "))+(student.unassignedLevelings()>0?" · Hay solicitudes anteriores sin curso registrado":""));cells.add(student.average()==null?"":student.average());cells.add(student.grades().values().stream().filter(Objects::nonNull).count()+" / "+book.subjects().size());cells.add(statusLabel(student.status()));writeRow(sheet,row++,cells,null);
    }
    sheet.createFreezePane(2,1);sheet.setAutoFilter(new CellRangeAddress(0,Math.max(0,row-1),0,headers.size()-1));for(int col=0;col<headers.size();col++)sheet.setColumnWidth(col,col==0?8500:5500);
   }
   summary.createFreezePane(0,1);for(int col=0;col<fields.size();col++)summary.setColumnWidth(col,col==1||col==5?10000:5500);workbook.write(output);
   return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL,"no-store").header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\"centro-saber-cursos-"+(course==null?"completo":"seleccionado")+"-"+(year==null?"todos":year)+".xlsx\"").contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")).body(output.toByteArray());
  }
 }
 private void writeRow(Sheet sheet,int index,List<?> values,CellStyle style){var row=sheet.createRow(index);for(int col=0;col<values.size();col++){var cell=row.createCell(col);Object value=values.get(col);if(value instanceof Number number)cell.setCellValue(number.doubleValue());else cell.setCellValue(Objects.toString(value,""));if(style!=null)cell.setCellStyle(style);}}
}
