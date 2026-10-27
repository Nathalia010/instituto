package co.educontrol.demo;
import java.util.*;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
@Component @Profile("demo")
class StudentAccountBackfill implements ApplicationRunner {
 private final JdbcTemplate db;private final BulkDataService data;private final StudentProvisioningService provisioning;
 StudentAccountBackfill(JdbcTemplate db,BulkDataService data,StudentProvisioningService provisioning){this.db=db;this.data=data;this.provisioning=provisioning;}
 public void run(ApplicationArguments args){
  for(String payload:db.queryForList("SELECT payload FROM demo_data_records WHERE module='estudiantes' AND deleted=FALSE",String.class)){
   var r=data.decode(payload);if(r.getOrDefault("documento","").length()>30||r.getOrDefault("nombre_completo","").length()>180)throw new IllegalStateException("Revisa los límites de identificación y nombre de estudiantes importados antes de crear sus cuentas.");
   provisioning.createInactive(r,"sistema");
  }
 }
}
