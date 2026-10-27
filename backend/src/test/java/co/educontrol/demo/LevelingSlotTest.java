package co.educontrol.demo;
import java.time.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:leveling_slots;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"})
@AutoConfigureMockMvc @ActiveProfiles("demo")
class LevelingSlotTest {
 @Autowired MockMvc mvc;@Autowired JdbcTemplate db;@Autowired ObjectMapper json;
 @Autowired BulkDataService data;
 @org.junit.jupiter.api.BeforeEach void configureSubjects(){var course=new java.util.HashMap<>(data.list("cursos",null).stream().filter(c->"validacion".equals(c.get("codigo_curso"))).findFirst().orElseThrow());course.put("materias","Biología\nInglés");data.saveRecord("cursos","validacion",course,"adminN");}
 MockHttpSession login(boolean admin,String document)throws Exception{String body=admin?"{\"username\":\"adminN\",\"password\":\"1234987A\"}":"{\"document\":\""+document+"\",\"issued\":\"2020-01-15\",\"year\":\"2026\"}";return (MockHttpSession)mvc.perform(post("/api/demo/login/"+(admin?"admin":"student")).with(csrf()).contentType("application/json").content(body)).andExpect(status().isOk()).andReturn().getRequest().getSession(false);}
 @Test void bookingCapacityDeletionAndRestorationAreTransactional()throws Exception{
  var admin=login(true,"");String date=LocalDate.now(ZoneId.of("America/Bogota")).plusDays(20).toString();String input="{\"date\":\""+date+"\",\"time\":\"10:00\",\"subject\":\"biologia\",\"teacher\":\"Docente de prueba\",\"location\":\"Virtual\",\"capacity\":1,\"active\":true}";
  var result=mvc.perform(post("/api/demo/admin/leveling-slots").session(admin).with(csrf()).contentType("application/json").content(input)).andExpect(status().isOk()).andReturn();String slot=json.readTree(result.getResponse().getContentAsString()).get("id").asText();
  db.update("INSERT INTO demo_students(id,name,email,phone,program,document,issued,academic_year,key_hash,academic_status,account_active) SELECT 'slot-second','Otro estudiante','other@example.invalid','3000000000','validacion','1000000099',issued,academic_year,key_hash,academic_status,TRUE FROM demo_students WHERE id='prueba-estudiante'");var first=login(false,"1000000001");var second=login(false,"1000000099");String booking="{\"slotId\":\""+slot+"\",\"subject\":\"biologia\",\"period\":\"SEGUNDO_SEMESTRE\",\"year\":2026,\"reason\":\"Prueba de cupos\"}";
  mvc.perform(post("/api/demo/student/levelings/book").session(first).with(csrf()).contentType("application/json").content(booking.replace("\"subject\":\"biologia\"","\"subject\":\"ingles\""))).andExpect(status().isBadRequest());
  result=mvc.perform(post("/api/demo/student/levelings/book").session(first).with(csrf()).contentType("application/json").content(booking)).andExpect(status().isCreated()).andReturn();String id=json.readTree(result.getResponse().getContentAsString()).get("id").asText();
  mvc.perform(get("/api/demo/student/leveling-slots").session(second)).andExpect(jsonPath("$.length()").value(0));mvc.perform(post("/api/demo/student/levelings/book").session(second).with(csrf()).contentType("application/json").content(booking)).andExpect(status().isConflict());
  mvc.perform(post("/api/demo/admin/leveling-slots/"+slot).session(admin).with(csrf()).contentType("application/json").content(input.replace("10:00","11:00"))).andExpect(status().isConflict());
  mvc.perform(post("/api/demo/admin/levelings/"+id+"/delete").session(admin).with(csrf())).andExpect(status().isOk());mvc.perform(get("/api/demo/student/levelings").session(first)).andExpect(jsonPath("$.length()").value(0));mvc.perform(get("/api/demo/student/leveling-slots").session(second)).andExpect(jsonPath("$.length()").value(1));
  result=mvc.perform(post("/api/demo/student/levelings/book").session(second).with(csrf()).contentType("application/json").content(booking)).andExpect(status().isCreated()).andReturn();String other=json.readTree(result.getResponse().getContentAsString()).get("id").asText();mvc.perform(post("/api/demo/admin/levelings/"+id+"/restore").session(admin).with(csrf())).andExpect(status().isConflict());
  mvc.perform(post("/api/demo/admin/levelings/"+other+"/delete").session(admin).with(csrf())).andExpect(status().isOk());mvc.perform(post("/api/demo/admin/levelings/"+id+"/restore").session(admin).with(csrf())).andExpect(status().isOk());
  mvc.perform(post("/api/demo/admin/leveling-slots/"+slot).session(admin).with(csrf()).contentType("application/json").content(input.replace("\"capacity\":1","\"capacity\":2").replace("\"active\":true","\"active\":false"))).andExpect(status().isOk());mvc.perform(post("/api/demo/student/levelings/book").session(second).with(csrf()).contentType("application/json").content(booking)).andExpect(status().isConflict());
  mvc.perform(post("/api/demo/admin/levelings/"+id+"/delete").session(first).with(csrf())).andExpect(status().isForbidden());mvc.perform(get("/api/demo/admin/leveling-slots").session(first)).andExpect(status().isForbidden());mvc.perform(post("/api/demo/student/levelings/book").session(first).contentType("application/json").content(booking)).andExpect(status().isForbidden());
  mvc.perform(post("/api/demo/admin/leveling-slots/"+slot+"/delete").session(first).with(csrf())).andExpect(status().isForbidden());
  mvc.perform(post("/api/demo/admin/leveling-slots/"+slot+"/delete").session(admin)).andExpect(status().isForbidden());
  mvc.perform(post("/api/demo/admin/leveling-slots/"+slot+"/delete").session(admin).with(csrf())).andExpect(status().isOk());
  mvc.perform(get("/api/demo/admin/leveling-slots").session(admin)).andExpect(jsonPath("$[?(@.id=='"+slot+"')]").isEmpty());
  mvc.perform(get("/api/demo/student/levelings").session(first)).andExpect(jsonPath("$.length()").value(1));
  mvc.perform(get("/api/demo/leveling-dates")).andExpect(jsonPath("$.length()").value(0));
  mvc.perform(post("/api/demo/student/levelings/book").session(second).with(csrf()).contentType("application/json").content(booking)).andExpect(status().isConflict());

 }
}
