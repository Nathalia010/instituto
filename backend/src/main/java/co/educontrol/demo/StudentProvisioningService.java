package co.educontrol.demo;
import java.time.*;
import java.util.*;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Profile("demo")
public class StudentProvisioningService {
 private final JdbcTemplate db;
 public StudentProvisioningService(JdbcTemplate db){this.db=db;}
 @Transactional public void createInactive(Map<String,String> r,String actor){
  db.queryForObject("SELECT id FROM demo_data_lock WHERE id=1 FOR UPDATE",Integer.class);
  if(db.queryForObject("SELECT COUNT(*) FROM demo_students WHERE document=?",Integer.class,r.get("documento"))>0)return;
  String id=UUID.randomUUID().toString(),name=r.getOrDefault("nombre_completo","");
  if(name.isBlank())name=(r.getOrDefault("nombres","")+" "+r.getOrDefault("apellidos","")).strip();
  int year=r.getOrDefault("anio","").isBlank()?LocalDate.now(ZoneId.of("America/Bogota")).getYear():Integer.parseInt(r.get("anio"));
  String status=r.getOrDefault("estado_academico","");if(status.isBlank())status="PENDIENTE_MATRICULA";
  db.update("INSERT INTO demo_students(id,name,email,phone,program,document,document_type,issued,academic_year,key_hash,academic_status,admin_notes,account_active) VALUES (?,?,?,?,?,?,?,?,?,'',?,?,FALSE)",id,name,r.getOrDefault("correo",""),r.getOrDefault("telefono",""),r.getOrDefault("programa",""),r.get("documento"),r.getOrDefault("tipo_documento","CC"),r.getOrDefault("fecha_expedicion",""),year,status,r.getOrDefault("notas_administrativas",""));
  db.update("INSERT INTO demo_audit(id,actor,action,resource_id) VALUES (?,?,?,?)",UUID.randomUUID().toString(),actor,"CREAR_CUENTA_INACTIVA",id);
 }
}
