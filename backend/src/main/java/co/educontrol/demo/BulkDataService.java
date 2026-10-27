package co.educontrol.demo;

import java.time.*;
import java.util.*;
import java.math.BigDecimal;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service @Profile("demo")
public class BulkDataService {
 private final JdbcTemplate db;private final ObjectMapper json;private final StudentProvisioningService provisioning;
 public BulkDataService(JdbcTemplate db,ObjectMapper json,StudentProvisioningService provisioning){this.db=db;this.json=json;this.provisioning=provisioning;}
 record Stored(String key,Integer year,Map<String,String> data) {}
 public record Checked(int row,boolean valid,List<String> errors,Map<String,String> data,Integer year) {}
 public record PreviewRequest(String module,String filename,List<List<String>> rows,Map<String,Integer> mapping,double minimum,double maximum) {}
 String encode(Object value){try{return json.writeValueAsString(value);}catch(Exception e){throw new IllegalArgumentException(e);}}
 Map<String,String> decode(String value){try{return json.readValue(value,new TypeReference<LinkedHashMap<String,String>>(){});}catch(Exception e){throw new IllegalStateException(e);}}
 private List<Stored> catalog(String module){return catalog(module,false);}
 private List<Stored> catalog(String module,boolean deleted){return db.query("SELECT record_key,academic_year,payload,active FROM demo_data_records WHERE module=? AND deleted=? ORDER BY created_at DESC",(rs,n)->{var row=decode(rs.getString(3));if(Set.of("estudiantes","docentes","cursos").contains(module))row.put("estado_registro",deleted?"Eliminado":rs.getBoolean(4)?"Activo":"Inactivo");return new Stored(rs.getString(1),(Integer)rs.getObject(2),row);},module,deleted);}
 private List<Stored> accounts(boolean deleted){
  return db.query("SELECT * FROM demo_students WHERE deleted=? ORDER BY created_at DESC",(rs,n)->{
   Map<String,String> r=new LinkedHashMap<>();
   r.put("tipo_documento",rs.getString("document_type"));r.put("documento",rs.getString("document"));r.put("nombre_completo",rs.getString("name"));r.put("correo",rs.getString("email"));r.put("telefono",rs.getString("phone"));r.put("fecha_expedicion",rs.getString("issued"));r.put("programa",rs.getString("program"));r.put("anio",rs.getString("academic_year"));r.put("estado_academico",rs.getString("academic_status"));r.put("notas_administrativas",rs.getString("admin_notes"));
  r.put("estado_registro",deleted?"Eliminado":rs.getBoolean("account_active")?"Activo":"Inactivo");return new Stored(rs.getString("document_type")+":"+rs.getString("document"),rs.getInt("academic_year"),r);
  },deleted);
 }
 private List<Stored> records(String module){return records(module,false);}
 private List<Stored> records(String module,boolean deleted){var rows=new LinkedHashMap<String,Stored>();catalog(module,deleted).forEach(r->rows.put(r.key,r));if(module.equals("estudiantes")){accounts(deleted).forEach(r->{var old=rows.get(r.key);var merged=new LinkedHashMap<String,String>();if(old!=null)merged.putAll(old.data);merged.putAll(r.data);rows.put(r.key,new Stored(r.key,r.year,merged));});var opposite=new HashSet<>(db.queryForList("SELECT document_type || ':' || document FROM demo_students WHERE deleted=?",String.class,!deleted));opposite.forEach(rows::remove);}return new ArrayList<>(rows.values());}
 public List<Map<String,String>> list(String module,Integer year){
  return list(module,year,false);
 }
 public List<Map<String,String>> list(String module,Integer year,boolean deleted){
  var definition=DataCatalog.get(module);var result=new ArrayList<Map<String,String>>();
  Set<String> enrolled=new HashSet<>();if(module.equals("estudiantes")&&year!=null)for(var r:catalog("matriculas"))if(year.equals(r.year))enrolled.add(r.data.get("tipo_documento")+":"+r.data.get("documento"));
  for(var r:records(module,deleted))if(year==null||!definition.annual()||year.equals(r.year)||enrolled.contains(r.key)){var row=new LinkedHashMap<>(r.data);if(r.year!=null&&!row.containsKey("anio"))row.put("anio",r.year.toString());result.add(row);}return result;
 }
 public List<Integer> years(){TreeSet<Integer> years=new TreeSet<>(Comparator.reverseOrder());db.queryForList("SELECT DISTINCT academic_year FROM demo_data_records WHERE academic_year IS NOT NULL",Integer.class).forEach(years::add);db.queryForList("SELECT DISTINCT academic_year FROM demo_students",Integer.class).forEach(years::add);return new ArrayList<>(years);}
 // Existing portal history has no enrollment/charge codes. Keep it visible in the
 // complete export without inventing relationships in the imported catalog.
 public List<Map<String,String>> portalHistory(boolean notes,Integer year){
  String sql=notes?"SELECT s.document,s.name,n.id,n.subject,n.grade,n.academic_year FROM demo_notes n JOIN demo_students s ON s.id=n.student_id":"SELECT s.document,s.name,m.id,m.concept,m.type,m.amount,m.movement_date FROM demo_movements m JOIN demo_students s ON s.id=m.student_id";
  return db.query(sql,(rs,n)->{var row=new LinkedHashMap<String,String>();row.put("documento",rs.getString("document"));row.put("nombre_completo",rs.getString("name"));row.put("codigo_registro",rs.getString("id"));if(notes){row.put("asignatura",rs.getString("subject"));row.put("nota",rs.getBigDecimal("grade").toPlainString());row.put("anio",rs.getString("academic_year"));}else{row.put("concepto",rs.getString("concept"));row.put("tipo",rs.getString("type"));row.put("valor_cop",rs.getBigDecimal("amount").toPlainString());row.put("fecha_movimiento",rs.getDate("movement_date").toLocalDate().toString());row.put("anio",String.valueOf(rs.getDate("movement_date").toLocalDate().getYear()));}return (Map<String,String>)row;}).stream().filter(r->year==null||year.toString().equals(r.get("anio"))).toList();
 }
 private Map<String,Stored> indexed(String module){Map<String,Stored> map=new HashMap<>();records(module).forEach(r->map.put(r.key,r));return map;}
 private Integer referenceYear(String module,String key,List<String> errors){var found=indexed(module).get(key);if(found==null){errors.add("No existe la referencia en "+DataCatalog.get(module).label()+": "+key);return null;}return found.year;}
 List<Checked> check(String module,List<Map<String,String>> input,double minimum,double maximum){
  var definition=DataCatalog.get(module);Set<String> seen=new HashSet<>();Set<String> seenDocuments=new HashSet<>();Set<String> accountDocuments=module.equals("estudiantes")?new HashSet<>(db.queryForList("SELECT document FROM demo_students",String.class)):Set.of();Set<String> existing=indexed(module).keySet();List<Checked> result=new ArrayList<>();
  existing=new HashSet<>(existing);existing.addAll(db.queryForList("SELECT record_key FROM demo_data_records WHERE module=? AND deleted=TRUE",String.class,module));
  for(int i=0;i<input.size();i++){
   if(input.get(i).containsKey("__skip"))continue;
   var r=new LinkedHashMap<String,String>();for(var f:definition.fields())r.put(f,input.get(i).getOrDefault(f,"").strip());
   for(String f:List.of("tipo_documento","tipo_documento_docente","documento","documento_docente"))if(r.containsKey(f))r.put(f,r.get(f).replaceAll("[\\s.\\-]","").toUpperCase(Locale.ROOT));
   var errors=new ArrayList<String>();Integer year=null;
   for(String f:definition.required())if(r.get(f).isBlank())errors.add("Falta "+f);
   for(var entry:r.entrySet()){if(entry.getValue().length()>2000)errors.add("Campo demasiado largo: "+entry.getKey());if(entry.getValue().stripLeading().startsWith("="))errors.add("No se admiten fórmulas: "+entry.getKey());}
   if(r.containsKey("tipo_documento")&&!Set.of("CC","TI","CE","PA","PPT","RC").contains(r.get("tipo_documento")))errors.add("Tipo de documento inválido");
   for(String f:List.of("documento","documento_docente"))if(r.containsKey(f)&&!r.get(f).matches("[A-Z0-9]{1,40}"))errors.add("Documento inválido: "+f);
   if(r.containsKey("correo")&&!r.get("correo").isBlank()&&!r.get("correo").matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))errors.add("Correo inválido");
   if(module.equals("estudiantes")&&r.get("nombre_completo").isBlank()&&(r.get("nombres").isBlank()||r.get("apellidos").isBlank()))errors.add("Completa nombres y apellidos o nombre_completo");
   if(module.equals("estudiantes")){
    if(accountDocuments.contains(r.get("documento"))||!seenDocuments.add(r.get("documento")))errors.add("El documento ya tiene una cuenta o está repetido en el archivo");
    if(r.get("documento").length()>30)errors.add("Documento: máximo 30 caracteres");
    String full=r.get("nombre_completo").isBlank()?(r.get("nombres")+" "+r.get("apellidos")).strip():r.get("nombre_completo");if(full.length()>180)errors.add("Nombre: máximo 180 caracteres");
    if(r.get("correo").length()>254)errors.add("Correo: máximo 254 caracteres");if(r.get("telefono").length()>20)errors.add("Teléfono: máximo 20 caracteres");if(r.get("programa").length()>50)errors.add("Programa: máximo 50 caracteres");
    if(!r.get("estado_academico").isBlank()&&!Set.of("EN_CURSO","FINALIZADO","NO_FINALIZADO","RETIRADO","PENDIENTE_EVALUACION","PENDIENTE_MATRICULA").contains(r.get("estado_academico")))errors.add("Estado académico inválido");
    if(!r.get("fecha_expedicion").isBlank())try{if(LocalDate.parse(r.get("fecha_expedicion")).isAfter(LocalDate.now(ZoneId.of("America/Bogota"))))errors.add("La expedición no puede ser futura");}catch(Exception ignored){}
   }
   if(module.equals("estudiantes")&&r.get("notas_administrativas").length()>500)errors.add("Notas administrativas: máximo 500 caracteres");
   for(String field:List.of("fecha_expedicion","fecha_inicio","fecha_fin","fecha_matricula","fecha_vencimiento","fecha_pago"))if(r.containsKey(field)&&!r.get(field).isBlank())try{LocalDate.parse(r.get(field));}catch(Exception e){errors.add("Fecha inválida (AAAA-MM-DD): "+field);}
   if(r.containsKey("anio")&&!r.get("anio").isBlank())try{year=Integer.parseInt(r.get("anio"));if(year<2000||year>2200)throw new Exception();}catch(Exception e){errors.add("Año inválido");}
   if(module.equals("cursos")){if(!r.get("codigo_curso").matches("[A-Za-z0-9_-]{1,50}"))errors.add("Código de curso: máximo 50 letras, números, guiones o guion bajo");if(r.get("nombre").length()>180)errors.add("Nombre: máximo 180 caracteres");if(!Set.of("VALIDACION_BACHILLERATO","CURSO","TECNICO").contains(r.get("tipo_programa")))errors.add("Tipo de programa inválido");if(!r.get("duracion_horas").isBlank())try{if(Integer.parseInt(r.get("duracion_horas"))<=0)throw new Exception();}catch(Exception e){errors.add("Duración debe ser un entero positivo");}if(r.get("publicado").isBlank())r.put("publicado","SI");if(!Set.of("SI","NO").contains(r.get("publicado")))errors.add("Publicado debe ser SI o NO");}
   if(module.equals("cursos")&&!r.get("fecha_inicio").isBlank()&&!r.get("fecha_fin").isBlank())try{if(LocalDate.parse(r.get("fecha_fin")).isBefore(LocalDate.parse(r.get("fecha_inicio"))))errors.add("La fecha final es anterior al inicio");}catch(Exception ignored){}
   if(module.equals("cursos")){
    try{CourseGradeController.subjects(r);}catch(ResponseStatusException ex){errors.add(ex.getReason());}
    if(!r.get("nota_aprobacion").isBlank())try{var value=new BigDecimal(r.get("nota_aprobacion"));if(value.signum()<0||value.compareTo(new BigDecimal("5"))>0||value.scale()>2)throw new IllegalArgumentException();}catch(Exception ex){errors.add("La nota de aprobación debe estar entre 0 y 5, con máximo dos decimales");}
   }
   if(module.equals("periodos"))try{var start=LocalDate.parse(r.get("fecha_inicio"));if(LocalDate.parse(r.get("fecha_fin")).isBefore(start))errors.add("La fecha final es anterior al inicio");if(year!=null&&start.getYear()!=year)errors.add("El año no coincide con la fecha inicial");}catch(Exception ignored){}
   if(module.equals("matriculas")){
    if(!indexed("estudiantes").containsKey(r.get("tipo_documento")+":"+r.get("documento")))errors.add("Estudiante no registrado");
    if(!indexed("cursos").containsKey(r.get("codigo_curso")))errors.add("Curso no registrado");
    else if("Inactivo".equals(indexed("cursos").get(r.get("codigo_curso")).data.get("estado_registro")))errors.add("El curso está inactivo");
    year=referenceYear("periodos",r.get("codigo_periodo"),errors);
    if(!Set.of("EN_CURSO","FINALIZADO","NO_FINALIZADO","RETIRADO","PENDIENTE_EVALUACION").contains(r.get("estado_academico")))errors.add("Estado académico inválido");
   }
   if(module.equals("notas")){year=referenceYear("matriculas",r.get("codigo_matricula"),errors);if(!indexed("docentes").containsKey(r.get("tipo_documento_docente")+":"+r.get("documento_docente")))errors.add("Docente no registrado");try{double grade=Double.parseDouble(r.get("nota"));if(!Double.isFinite(grade)||grade<minimum||grade>maximum)throw new Exception();}catch(Exception e){errors.add("Nota fuera de la escala "+minimum+" a "+maximum);}}
   if(module.equals("cobros")){year=referenceYear("matriculas",r.get("codigo_matricula"),errors);}
   if(module.equals("pagos")){year=referenceYear("cobros",r.get("codigo_cobro"),errors);if(!Set.of("EFECTIVO","TRANSFERENCIA","TARJETA","OTRO").contains(r.get("metodo")))errors.add("Método de pago inválido");if(r.get("estado_pago").isBlank())r.put("estado_pago","EN_REVISION");if(!r.get("estado_pago").equals("EN_REVISION"))errors.add("Los pagos importados deben quedar EN_REVISION");}
   if(module.equals("agenda")){
    if(!r.get("calendario").isBlank()&&!Set.of("A","B").contains(r.get("calendario")))errors.add("Calendario ICFES inválido: A o B");
    if(!r.get("etapa").isBlank()&&!Set.of("INSCRIPCION","CITACION","APLICACION","RESULTADOS","PAGO","OTRO").contains(r.get("etapa")))errors.add("Etapa ICFES inválida");
    if(!r.get("codigo_evento").matches("[A-Za-z0-9_-]{1,60}"))errors.add("Código de evento inválido (letras, números, guion o guion bajo, máximo 60)");
    if(!Set.of("INGRESO","CLASE","INSCRIPCION","ICFES","NIVELACION","PAGO","OTRO").contains(r.get("tipo_evento")))errors.add("Tipo de evento inválido");
    if(!Set.of("BORRADOR","PUBLICADO","CANCELADO").contains(r.get("estado")))errors.add("Estado de publicación inválido");
    if(!r.get("programa").isBlank()&&!Set.of("validacion","preparacion","preuniversitario","enfermeria","icfes","vinculacion","administrativo","infancia","refuerzo","todos").contains(r.get("programa")))errors.add("Programa inválido");
    if(!r.get("hora").isBlank())try{if(!r.get("hora").matches("[0-9]{2}:[0-9]{2}"))throw new Exception();LocalTime.parse(r.get("hora"));}catch(Exception e){errors.add("Hora inválida (HH:MM)");}
    try{var start=LocalDate.parse(r.get("fecha_inicio"));if(!r.get("fecha_fin").isBlank()&&LocalDate.parse(r.get("fecha_fin")).isBefore(start))errors.add("La fecha final es anterior al inicio");}catch(Exception ignored){}
    if(!r.get("enlace").isBlank()&&!https(r.get("enlace")))errors.add("El enlace debe usar HTTPS y tener un dominio válido");
   }
   if(r.containsKey("valor_cop"))try{var value=new BigDecimal(r.get("valor_cop"));if(value.signum()<=0||value.scale()>2||value.precision()>14)throw new Exception();}catch(Exception e){errors.add("Valor monetario inválido, positivo y máximo dos decimales");}
   String key=DataCatalog.key(module,r);if(existing.contains(key))errors.add("Registro existente: no se sobrescribirá");if(!seen.add(key))errors.add("Registro repetido en el archivo");
   result.add(new Checked(i+2,errors.isEmpty(),errors,r,year));
  }
  return result;
 }
 static boolean https(String value){try{var uri=java.net.URI.create(value);return "https".equalsIgnoreCase(uri.getScheme())&&uri.getHost()!=null&&uri.getUserInfo()==null;}catch(Exception e){return false;}}
 @Transactional public Map<String,String> saveRecord(String module,String originalKey,Map<String,String> input,String actor){
  DataCatalog.get(module);if(module.equals("agenda"))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Usa el editor de eventos.");if(input==null||input.containsKey("__skip")||input.values().stream().anyMatch(Objects::isNull))throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
  db.queryForObject("SELECT id FROM demo_data_lock WHERE id=1 FOR UPDATE",Integer.class);boolean editing=originalKey!=null;
  if(editing&&(!module.equals("cursos")||db.queryForObject("SELECT COUNT(*) FROM demo_data_records WHERE module=? AND record_key=? AND deleted=FALSE",Integer.class,module,originalKey)==0))throw new ResponseStatusException(HttpStatus.NOT_FOUND);
  var checked=check(module,List.of(input),0,5).getFirst();var errors=new ArrayList<>(checked.errors());if(editing){if(!originalKey.equals(DataCatalog.key(module,checked.data())))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"El código no se puede cambiar.");errors.remove("Registro existente: no se sobrescribirá");}if(!errors.isEmpty())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,String.join(" · ",errors));
  String key=DataCatalog.key(module,checked.data());if(editing)db.update("UPDATE demo_data_records SET academic_year=?,payload=? WHERE module=? AND record_key=?",checked.year(),encode(checked.data()),module,key);else db.update("INSERT INTO demo_data_records(module,record_key,academic_year,payload) VALUES (?,?,?,?)",module,key,checked.year(),encode(checked.data()));
  if(module.equals("estudiantes")&&!editing)provisioning.createInactive(checked.data(),actor);
  db.update("INSERT INTO demo_audit(id,actor,action,resource_id) VALUES (?,?,?,?)",UUID.randomUUID().toString(),actor,editing?"EDITAR_CURSO":"CREAR_"+module.toUpperCase(),key);return checked.data();
 }
 @Transactional public Map<String,String> saveEvent(String originalKey,Map<String,String> data,String actor){
  if(data==null||data.values().stream().anyMatch(Objects::isNull))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Completa los campos del evento.");
  db.queryForObject("SELECT id FROM demo_data_lock WHERE id=1 FOR UPDATE",Integer.class);
  boolean edit=originalKey!=null;
  if(edit&&db.queryForObject("SELECT COUNT(*) FROM demo_data_records WHERE module='agenda' AND deleted=FALSE AND record_key=?",Integer.class,originalKey)==0)throw new ResponseStatusException(HttpStatus.NOT_FOUND);
  if(edit&&!originalKey.equals(data.get("codigo_evento")))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"El código no se puede cambiar.");
  var checked=check("agenda",List.of(data),0,5).getFirst();var errors=new ArrayList<>(checked.errors());if(edit)errors.remove("Registro existente: no se sobrescribirá");
  if(!errors.isEmpty())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,String.join(" · ",errors));
  if(edit)db.update("UPDATE demo_data_records SET academic_year=?,payload=? WHERE module='agenda' AND record_key=?",checked.year(),encode(checked.data()),originalKey);
  else db.update("INSERT INTO demo_data_records(module,record_key,academic_year,payload) VALUES ('agenda',?,?,?)",checked.data().get("codigo_evento"),checked.year(),encode(checked.data()));
  db.update("INSERT INTO demo_audit(id,actor,action,resource_id) VALUES (?,?,?,?)",UUID.randomUUID().toString(),actor,edit?"EDITAR_EVENTO":"CREAR_EVENTO",checked.data().get("codigo_evento"));return checked.data();
 }
 public Map<String,Object> preview(PreviewRequest req,String sessionHash){
  var def=DataCatalog.get(req.module());if(req.rows()==null||req.rows().size()>2000||req.mapping()==null||req.filename()==null||req.filename().length()>255||!Double.isFinite(req.minimum())||!Double.isFinite(req.maximum())||req.maximum()<=req.minimum())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Archivo o escala inválidos.");
  long characters=0;List<Map<String,String>> input=new ArrayList<>();for(var cells:req.rows()){if(cells==null||cells.size()>100)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Demasiadas columnas");for(String cell:cells){characters+=cell==null?0:cell.length();if(characters>5_000_000)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Archivo demasiado grande");}var row=new LinkedHashMap<String,String>();if(cells.stream().allMatch(s->s==null||s.isBlank()))row.put("__skip","true");for(String field:def.fields()){Integer column=req.mapping().get(field);if(column!=null&&(column<0||column>=cells.size()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Mapeo inválido");row.put(field,column==null?"":Objects.toString(cells.get(column),""));}input.add(row);}
  var checked=check(req.module(),input,req.minimum(),req.maximum());String id=UUID.randomUUID().toString();
  db.update("INSERT INTO demo_data_previews(id,session_hash,module,filename,payload,scale_min,scale_max) VALUES (?,?,?,?,?,?,?)",id,sessionHash,req.module(),req.filename(),encode(input),req.minimum(),req.maximum());
  return Map.of("id",id,"rows",checked,"valid",checked.stream().filter(Checked::valid).count(),"invalid",checked.stream().filter(r->!r.valid()).count());
 }
 @Transactional public Map<String,Object> confirm(String id,String sessionHash){
  db.queryForObject("SELECT id FROM demo_data_lock WHERE id=1 FOR UPDATE",Integer.class);
  var previews=db.queryForList("SELECT id,module,filename,CAST(payload AS VARCHAR) AS payload,scale_min,scale_max,confirmed,created_at FROM demo_data_previews WHERE id=? AND session_hash=? FOR UPDATE",id,sessionHash);
  if(previews.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Vista previa no encontrada.");var p=previews.getFirst();
  if((Boolean)p.get("confirmed"))throw new ResponseStatusException(HttpStatus.CONFLICT,"Esta importación ya fue confirmada.");
  if(((java.sql.Timestamp)p.get("created_at")).toInstant().isBefore(Instant.now().minusSeconds(900)))throw new ResponseStatusException(HttpStatus.GONE,"Vista previa vencida. Vuelve a validar el archivo.");
  List<Map<String,String>> input;try{input=json.readValue(p.get("payload").toString(),new TypeReference<>(){});}catch(Exception e){throw new IllegalStateException(e);}
  String module=p.get("module").toString();var checked=check(module,input,((Number)p.get("scale_min")).doubleValue(),((Number)p.get("scale_max")).doubleValue());int saved=0;
  for(var row:checked)if(row.valid()){db.update("INSERT INTO demo_data_records(module,record_key,academic_year,payload) VALUES (?,?,?,?)",module,DataCatalog.key(module,row.data()),row.year(),encode(row.data()));if(module.equals("estudiantes"))provisioning.createInactive(row.data(),"adminN");saved++;}
  if(saved==0)throw new ResponseStatusException(HttpStatus.CONFLICT,"No quedan filas válidas. Vuelve a validar.");
  db.update("UPDATE demo_data_previews SET confirmed=TRUE WHERE id=?",id);db.update("INSERT INTO demo_audit(id,actor,action,resource_id) VALUES (?,?,?,?)",UUID.randomUUID().toString(),"adminN","IMPORTAR_"+module.toUpperCase(),id);
  return Map.of("saved",saved,"skipped",checked.size()-saved);
 }
}
