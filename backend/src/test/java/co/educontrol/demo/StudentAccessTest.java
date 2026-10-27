package co.educontrol.demo;
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
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:student_access;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"}) @AutoConfigureMockMvc @ActiveProfiles("demo")
class StudentAccessTest {
 @Autowired MockMvc mvc;@Autowired JdbcTemplate db;
 private static final String PATH="/api/demo/admin/students/prueba-estudiante/access";
 private static final String LOGIN="{\"document\":\"1000000001\",\"issued\":\"2020-01-15\",\"year\":\"2026\"}";
 MockHttpSession admin()throws Exception{return (MockHttpSession)mvc.perform(post("/api/demo/login/admin").with(csrf()).contentType("application/json").content("{\"username\":\"adminN\",\"password\":\"1234987A\"}")).andExpect(status().isOk()).andReturn().getRequest().getSession(false);}
 MockHttpSession student()throws Exception{return (MockHttpSession)mvc.perform(post("/api/demo/login/student").with(csrf()).contentType("application/json").content(LOGIN)).andExpect(status().isOk()).andReturn().getRequest().getSession(false);}
 @Test void accessRequiresAdminAndCsrf()throws Exception{
  var admin=admin();var student=student();String body="{\"active\":false}";
  mvc.perform(post(PATH).with(csrf()).contentType("application/json").content(body)).andExpect(status().isUnauthorized());
  mvc.perform(post(PATH).session(student).with(csrf()).contentType("application/json").content(body)).andExpect(status().isForbidden());
  mvc.perform(post(PATH).session(admin).contentType("application/json").content(body)).andExpect(status().isForbidden());
  mvc.perform(post(PATH).session(admin).with(csrf()).contentType("application/json").content("{}")).andExpect(status().isBadRequest());
  mvc.perform(post("/api/demo/admin/students/missing/access").session(admin).with(csrf()).contentType("application/json").content(body)).andExpect(status().isNotFound());
  mvc.perform(get("/api/demo/student/me").session(student)).andExpect(status().isOk());
 }
 @Test void disableBlocksLoginAndExistingSessionsAndReactivationRequiresFreshLogin()throws Exception{
  var admin=admin();var session1=student();var session2=student();var before=db.queryForMap("SELECT name,academic_status,admin_notes FROM demo_students WHERE id='prueba-estudiante'");int notes=db.queryForObject("SELECT COUNT(*) FROM demo_notes WHERE student_id='prueba-estudiante'",Integer.class);
  mvc.perform(post(PATH).session(admin).with(csrf()).contentType("application/json").content("{\"active\":false}")).andExpect(status().isOk());
  mvc.perform(get("/api/demo/admin/students").session(admin)).andExpect(jsonPath("$[?(@.id=='prueba-estudiante')].account_active").value(false));
  mvc.perform(post("/api/demo/login/student").with(csrf()).contentType("application/json").content(LOGIN)).andExpect(status().isUnauthorized());
  mvc.perform(get("/api/demo/student/me").session(session1)).andExpect(status().isUnauthorized());assertTrue(session1.isInvalid());
  mvc.perform(post(PATH).session(admin).with(csrf()).contentType("application/json").content("{\"active\":true}")).andExpect(status().isOk());
  mvc.perform(get("/api/demo/student/levelings").session(session2)).andExpect(status().isUnauthorized());assertTrue(session2.isInvalid());
  mvc.perform(get("/api/demo/student/me").session(student())).andExpect(status().isOk());
  assertEquals(before,db.queryForMap("SELECT name,academic_status,admin_notes FROM demo_students WHERE id='prueba-estudiante'"));assertEquals(notes,db.queryForObject("SELECT COUNT(*) FROM demo_notes WHERE student_id='prueba-estudiante'",Integer.class));
  assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM demo_audit WHERE action='DESACTIVAR_CUENTA_ESTUDIANTE' AND resource_id='prueba-estudiante'",Integer.class));
 }
}
