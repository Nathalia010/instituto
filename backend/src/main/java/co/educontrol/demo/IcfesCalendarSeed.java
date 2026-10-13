package co.educontrol.demo;
import java.util.*;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component @Profile("demo")
public class IcfesCalendarSeed implements ApplicationRunner {
 private final JdbcTemplate db;private final BulkDataService data;
 public IcfesCalendarSeed(JdbcTemplate db,BulkDataService data){this.db=db;this.data=data;}
 public void run(ApplicationArguments args){
  String[][] entries={{"A","2026","CITACION","Publicación de citaciones","2026-07-10"},{"A","2026","APLICACION","Aplicación del examen","2026-07-26"},{"A","2026","RESULTADOS","Resultados individuales","2026-09-25"},{"B","2027","CITACION","Publicación de citaciones","2027-02-27"},{"B","2027","APLICACION","Aplicación del examen","2027-03-15"},{"B","2027","RESULTADOS","Resultados","2027-05-15"}};
  for(var e:entries){String code="ICFES-"+e[0]+"-"+e[1]+"-"+e[2];var row=new LinkedHashMap<String,String>();DataCatalog.get("agenda").fields().forEach(f->row.put(f,""));row.putAll(Map.of("codigo_evento",code,"anio",e[1],"tipo_evento","ICFES","titulo",e[3],"programa","icfes","calendario",e[0],"etapa",e[2],"fecha_inicio",e[4],"estado","PUBLICADO"));row.put("detalle",e[0].equals("A")?"Referencia oficial consultada en octubre de 2026. Periodo finalizado.":"Anuncio institucional de Centro Saber. Fecha por confirmar con el cronograma oficial de 2027.");db.update("INSERT INTO demo_data_records(module,record_key,academic_year,payload) SELECT 'agenda',?,?,? WHERE NOT EXISTS (SELECT 1 FROM demo_data_records WHERE module='agenda' AND record_key=?)",code,Integer.parseInt(e[1]),data.encode(row),code);}
 }
}
