package co.educontrol.demo;
import java.util.*;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
@Component @Profile("demo")
public class CourseSeed implements ApplicationRunner {
 private final JdbcTemplate db;private final BulkDataService data;
 public CourseSeed(JdbcTemplate db,BulkDataService data){this.db=db;this.data=data;}
 public void run(ApplicationArguments args){String[][] courses={{"vinculacion","Vinculación menor (CJK)"},{"validacion","Preparación validación Bachillerato 299 (ADULTOS)"},{"administrativo","Auxiliar Administrativo (Curso preparación)"},{"infancia","Primera Infancia (Curso preparación)"},{"enfermeria","Enfermería (Preuniversitario)"},{"icfes","PRE ICFES · Preparación"},{"refuerzo","Refuerzo escolar"}};for(var c:courses){var row=new LinkedHashMap<String,String>();DataCatalog.get("cursos").fields().forEach(f->row.put(f,""));row.putAll(Map.of("codigo_curso",c[0],"nombre",c[1],"tipo_programa",c[0].equals("validacion")?"VALIDACION_BACHILLERATO":"CURSO","descripcion","Solicita información sobre este programa, sus requisitos y disponibilidad.","publicado","SI"));db.update("INSERT INTO demo_data_records(module,record_key,payload) SELECT 'cursos',?,? WHERE NOT EXISTS (SELECT 1 FROM demo_data_records WHERE module='cursos' AND record_key=?)",c[0],data.encode(row),c[0]);}}
}
