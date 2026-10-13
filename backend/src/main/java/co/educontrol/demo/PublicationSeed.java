package co.educontrol.demo;
import java.util.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.JdbcTemplate;
@Component @Profile("demo")
public class PublicationSeed implements ApplicationRunner {
 private final JdbcTemplate db;private final BulkDataService data;private final ObjectMapper json;
 public PublicationSeed(JdbcTemplate db,BulkDataService data,ObjectMapper json){this.db=db;this.data=data;this.json=json;}
 private void seed(String kind,String id,Map<String,String> r){if(db.queryForObject("SELECT COUNT(*) FROM demo_publications WHERE kind=? AND id=?",Integer.class,kind,id)==0)db.update("INSERT INTO demo_publications(kind,id,payload) VALUES (?,?,?)",kind,id,data.encode(r));}
 public void run(ApplicationArguments args)throws Exception{
  String code="SALIDA-REFERENCIA-2026";
  if(db.queryForObject("SELECT COUNT(*) FROM demo_data_records WHERE module='agenda' AND record_key=?",Integer.class,code)==0){var event=new HashMap<String,String>();DataCatalog.get("agenda").fields().forEach(k->event.put(k,""));event.putAll(Map.of("codigo_evento",code,"anio","2026","tipo_evento","OTRO","titulo","Salida pedagógica · Referencia 2026","programa","todos","fecha_inicio","2026-10-12","estado","PUBLICADO","detalle","Programación histórica del 12 de octubre de 2026."));data.saveEvent(null,event,"sistema");}
  try(var stream=getClass().getResourceAsStream("/outing-template.json")){seed("event",code,json.readValue(stream,new TypeReference<Map<String,String>>(){}));}
  String album=json.readTree(getClass().getResourceAsStream("/outing-template.json")).get("album").asText();
  seed("announcement","feria",Map.of("title","Feria empresarial","intro","Participa y registra tu emprendimiento.","type","FERIA","link","https://docs.google.com/forms/d/e/1FAIpQLSfheDfvtuSYi18PzKXMQrBwLWqzNpSuw2pA2gLeD4FckITEBQ/viewform","event","","state","PUBLICADO"));
  seed("announcement","nivelaciones",Map.of("title","Nivelaciones","intro","Consulta las guías y las fechas habilitadas. Agenda desde tu espacio estudiantil.","type","NIVELACIONES","link","","event","","state","PUBLICADO"));
  seed("announcement","salida",Map.of("title","Salida pedagógica","intro","Conoce los puntos de encuentro y la programación de referencia de octubre de 2026.","type","SALIDA","link","","event",code,"state","PUBLICADO"));
  seed("announcement","album",Map.of("title","Álbum 2026","intro","Revive los momentos de nuestra comunidad.","type","ALBUM","link",album,"event","","state","PUBLICADO"));
 }
}
