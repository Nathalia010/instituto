package co.educontrol.demo;
import java.time.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:leveling;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"})
@AutoConfigureMockMvc @ActiveProfiles("demo")
class LevelingTest {
 @Autowired MockMvc mvc;@Autowired ObjectMapper json;@Autowired JdbcTemplate db;
 MockHttpSession student(String document)throws Exception{return (MockHttpSession)mvc.perform(post("/api/demo/login/student").with(csrf()).contentType("application/json").content("{\"document\":\""+document+"\",\"issued\":\"2020-01-15\",\"year\":\"2026\"}")).andExpect(status().isOk()).andReturn().getRequest().getSession(false);}
 MockHttpSession admin()throws Exception{return (MockHttpSession)mvc.perform(post("/api/demo/login/admin").with(csrf()).contentType("application/json").content("{\"username\":\"adminN\",\"password\":\"1234987A\"}")).andExpect(status().isOk()).andReturn().getRequest().getSession(false);}
 String request(String subject){return "{\"subject\":\""+subject+"\",\"period\":\"SEGUNDO_SEMESTRE\",\"year\":2026,\"reason\":\"Actividad ficticia pendiente\",\"preferredDate\":\""+LocalDate.now(ZoneId.of("America/Bogota")).plusDays(10)+"\",\"preferredTime\":\"10:00\"}";}
 @Test void requestRemainsPendingUntilAdminConfirmsAndResultIsVisibleOnlyToOwner()throws Exception{
  var owner=student("1000000001");var response=mvc.perform(post("/api/demo/student/levelings").session(owner).with(csrf()).contentType("application/json").content(request("biologia"))).andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("PENDIENTE")).andReturn();String id=json.readTree(response.getResponse().getContentAsString()).get("id").asText();
  mvc.perform(post("/api/demo/student/levelings").session(owner).with(csrf()).contentType("application/json").content(request("biologia"))).andExpect(status().isConflict());
  db.update("INSERT INTO demo_students(id,name,email,phone,program,document,issued,academic_year,key_hash,academic_status) SELECT 'other-leveling','Otro estudiante','other@example.invalid','3000000000','validacion','1000000088',issued,academic_year,key_hash,academic_status FROM demo_students WHERE id='prueba-estudiante'");var other=student("1000000088");
  mvc.perform(get("/api/demo/student/levelings").session(other)).andExpect(jsonPath("$.length()").value(0));mvc.perform(get("/api/demo/student/levelings").session(owner)).andExpect(jsonPath("$[?(@.id=='"+id+"')].status").value("PENDIENTE"));
  var administrator=admin();String update="{\"status\":\"CONFIRMADA\",\"teacher\":\"Docente de prueba\",\"location\":\"Virtual\",\"confirmedDate\":\""+LocalDate.now().plusDays(12)+"\",\"confirmedTime\":\"11:00\",\"response\":\"Cita de prueba confirmada\",\"result\":\"\"}";
  mvc.perform(post("/api/demo/admin/levelings/"+id).session(administrator).with(csrf()).contentType("application/json").content(update)).andExpect(status().isOk());mvc.perform(get("/api/demo/student/levelings").session(owner)).andExpect(jsonPath("$[?(@.id=='"+id+"')].confirmed_time").value("11:00"));
  mvc.perform(post("/api/demo/admin/levelings/"+id).session(administrator).with(csrf()).contentType("application/json").content(update.replace("CONFIRMADA","FINALIZADA"))).andExpect(status().isBadRequest());
  mvc.perform(post("/api/demo/admin/levelings/"+id).session(administrator).with(csrf()).contentType("application/json").content(update.replace("CONFIRMADA","FINALIZADA").replace("\"result\":\"\"","\"result\":\"Actividad revisada\""))).andExpect(status().isOk());mvc.perform(get("/api/demo/student/levelings").session(owner)).andExpect(jsonPath("$[?(@.id=='"+id+"')].result").value("Actividad revisada"));mvc.perform(get("/api/demo/student/levelings").session(other)).andExpect(jsonPath("$.length()").value(0));
 }
 @Test void validationAndPermissionsCannotBeBypassed()throws Exception{
  mvc.perform(get("/api/demo/student/levelings")).andExpect(status().isUnauthorized());var owner=student("1000000001");
  mvc.perform(post("/api/demo/student/levelings").session(owner).contentType("application/json").content(request("ingles"))).andExpect(status().isForbidden());
  mvc.perform(post("/api/demo/student/levelings").session(owner).with(csrf()).contentType("application/json").content(request("unknown"))).andExpect(status().isBadRequest());
  mvc.perform(post("/api/demo/student/levelings").session(owner).with(csrf()).contentType("application/json").content(request("ingles").replace(LocalDate.now(ZoneId.of("America/Bogota")).plusDays(10).toString(),"2000-01-01"))).andExpect(status().isBadRequest());
  mvc.perform(get("/api/demo/admin/levelings").session(owner)).andExpect(status().isForbidden());mvc.perform(post("/api/demo/admin/levelings/ANY").session(owner).with(csrf()).contentType("application/json").content("{}")).andExpect(status().isForbidden());
  mvc.perform(post("/api/demo/student/levelings").session(admin()).with(csrf()).contentType("application/json").content(request("ingles"))).andExpect(status().isForbidden());
 }
}
