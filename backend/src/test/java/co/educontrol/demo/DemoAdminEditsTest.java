package co.educontrol.demo;

import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:admin_edits;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"})
@AutoConfigureMockMvc @ActiveProfiles("demo")
class DemoAdminEditsTest {
 @Autowired MockMvc mvc;
 @Autowired ObjectMapper json;
 private MockHttpSession admin() throws Exception {
  return (MockHttpSession)mvc.perform(post("/api/demo/login/admin").with(csrf()).contentType("application/json").content("{\"username\":\"adminN\",\"password\":\"1234987A\"}")).andExpect(status().isOk()).andReturn().getRequest().getSession(false);
 }
 private String lead() throws Exception {
  var r=mvc.perform(post("/api/demo/inquiries").with(csrf()).contentType("application/json").content("{\"name\":\"Solicitud de prueba\",\"email\":\"editar@example.invalid\",\"phone\":\"3000000000\",\"program\":\"icfes\",\"message\":\"Información de prueba\",\"consent\":true}")).andExpect(status().isCreated()).andReturn();
  return json.readTree(r.getResponse().getContentAsString()).get("id").asText();
 }
 @Test void inquiryEditsNotesRemovalAndRestorePersist() throws Exception {
  String id=lead();var session=admin();
  String body=json.writeValueAsString(Map.of("name","Nombre corregido","document","12345678","email","editar@example.invalid","phone","3000000000","program","icfes","message","Consulta corregida","notes","Llamar el lunes"));
  mvc.perform(post("/api/demo/admin/inquiries/"+id+"/edit").session(session).contentType("application/json").content(body)).andExpect(status().isForbidden());
  mvc.perform(post("/api/demo/admin/inquiries/"+id+"/edit").session(session).with(csrf()).contentType("application/json").content(body)).andExpect(status().isOk());
  mvc.perform(get("/api/demo/admin/inquiries").session(session)).andExpect(jsonPath("$[?(@.id=='"+id+"')].document").value("12345678")).andExpect(jsonPath("$[?(@.id=='"+id+"')].admin_notes").value("Llamar el lunes"));
  mvc.perform(post("/api/demo/admin/inquiries/"+id+"/delete").session(session).with(csrf())).andExpect(status().isOk());
  mvc.perform(get("/api/demo/admin/inquiries").session(session)).andExpect(jsonPath("$[?(@.id=='"+id+"')]").isEmpty());
  mvc.perform(post("/api/demo/admin/inquiries/"+id+"/student").session(session).with(csrf()).contentType("application/json").content("{\"document\":\"11111111\",\"issued\":\"2020-01-15\",\"year\":2026}")).andExpect(status().isNotFound());
  mvc.perform(post("/api/demo/admin/inquiries/"+id+"/restore").session(session).with(csrf())).andExpect(status().isOk());
  mvc.perform(get("/api/demo/admin/inquiries").session(session)).andExpect(jsonPath("$[?(@.id=='"+id+"')].admin_notes").value("Llamar el lunes"));
 }
 @Test void studentNoteLimitAndPrivacy() throws Exception {
  var session=admin();
  var input=new java.util.HashMap<String,Object>(Map.of("name","Estudiante de prueba","document","1000000001","email","estudiante@example.invalid","phone","3000000000","program","validacion","issued","2020-01-15","status","EN_CURSO","notes","x".repeat(500)));
  mvc.perform(post("/api/demo/admin/students/prueba-estudiante/edit").session(session).with(csrf()).contentType("application/json").content(json.writeValueAsString(input))).andExpect(status().isOk());
  input.put("notes","x".repeat(501));
  mvc.perform(post("/api/demo/admin/students/prueba-estudiante/edit").session(session).with(csrf()).contentType("application/json").content(json.writeValueAsString(input))).andExpect(status().isBadRequest());
  mvc.perform(get("/api/demo/admin/students").session(session)).andExpect(jsonPath("$[?(@.id=='prueba-estudiante')].admin_notes").value("x".repeat(500)));
  var result=mvc.perform(post("/api/demo/login/student").with(csrf()).contentType("application/json").content("{\"document\":\"1000000001\",\"issued\":\"2020-01-15\",\"year\":\"2026\"}")).andExpect(status().isOk()).andReturn();
  var studentSession=(MockHttpSession)result.getRequest().getSession(false);
  mvc.perform(get("/api/demo/student/me").session(studentSession)).andExpect(jsonPath("$.admin_notes").doesNotExist());
  mvc.perform(post("/api/demo/admin/students/prueba-estudiante/edit").session(studentSession).with(csrf()).contentType("application/json").content(json.writeValueAsString(input))).andExpect(status().isForbidden());
 }
 @Test void removingLinkedInquiryDoesNotRemoveStudent() throws Exception {
  var session=admin();String id=lead();
  mvc.perform(post("/api/demo/admin/inquiries/"+id+"/student").session(session).with(csrf()).contentType("application/json").content("{\"document\":\"77777777\",\"issued\":\"2020-01-15\",\"year\":2026}")).andExpect(status().isCreated());
  mvc.perform(post("/api/demo/admin/inquiries/"+id+"/delete").session(session).with(csrf())).andExpect(status().isOk());
  mvc.perform(get("/api/demo/admin/students").session(session)).andExpect(jsonPath("$[?(@.document=='77777777')]").isNotEmpty());
 }
}
