package co.educontrol.demo;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:demo_test;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"})
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class DemoFlowTest {
 @Autowired MockMvc mvc;
 @Autowired ObjectMapper json;
 @Test void anonymousCannotReadLeads() throws Exception {
  mvc.perform(get("/api/demo/admin/inquiries")).andExpect(status().isUnauthorized());
 }
 @Test void publicFormRequiresCsrfAndConsent() throws Exception {
  String body="{\"name\":\"Prueba\",\"email\":\"prueba@example.invalid\",\"phone\":\"3000000000\",\"program\":\"enfermeria\",\"message\":\"Información ficticia\",\"consent\":true}";
  mvc.perform(post("/api/demo/inquiries").contentType("application/json").content(body)).andExpect(status().isForbidden());
  mvc.perform(post("/api/demo/inquiries").with(csrf()).contentType("application/json").content(body.replace("true","false"))).andExpect(status().isBadRequest());
 }
 @Test void studentOnlySeesOwnDataAndCannotReadAdmin() throws Exception {
  var result=mvc.perform(post("/api/demo/login/student").with(csrf()).contentType("application/json").content("{\"document\":\"1000000001\",\"issued\":\"2020-01-15\",\"year\":\"2026\"}")).andExpect(status().isOk()).andReturn();
  var session=(MockHttpSession)result.getRequest().getSession(false);
  mvc.perform(get("/api/demo/student/me").session(session)).andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Estudiante de prueba")).andExpect(jsonPath("$.balance").value(300000)).andExpect(jsonPath("$.key_hash").doesNotExist());
  mvc.perform(get("/api/demo/admin/students").session(session)).andExpect(status().isForbidden());
  mvc.perform(get("/api/demo/student/otro-id").session(session)).andExpect(status().isForbidden());
 }
 @Test void leadCanBeConvertedOnceAndNewStudentHasNoOtherGrades() throws Exception {
  var leadResult=mvc.perform(post("/api/demo/inquiries").with(csrf()).contentType("application/json").content("{\"name\":\"Interesado ficticio\",\"email\":\"nuevo@example.invalid\",\"phone\":\"3000000001\",\"program\":\"enfermeria\",\"message\":\"Solicito información de prueba\",\"consent\":true}")).andExpect(status().isCreated()).andReturn();
  String leadId=json.readTree(leadResult.getResponse().getContentAsString()).get("id").asText();
  var adminResult=mvc.perform(post("/api/demo/login/admin").with(csrf()).contentType("application/json").content("{\"username\":\"adminN\",\"password\":\"1234987A\"}")).andExpect(status().isOk()).andReturn();
  var adminSession=(MockHttpSession)adminResult.getRequest().getSession(false);
  String body="{\"document\":\"1000000002\",\"issued\":\"2021-02-03\",\"year\":2026}";
  mvc.perform(post("/api/demo/admin/inquiries/"+leadId+"/student").session(adminSession).with(csrf()).contentType("application/json").content(body)).andExpect(status().isCreated());
  mvc.perform(post("/api/demo/admin/inquiries/"+leadId+"/student").session(adminSession).with(csrf()).contentType("application/json").content(body)).andExpect(status().isConflict());
  var studentResult=mvc.perform(post("/api/demo/login/student").with(csrf()).contentType("application/json").content("{\"document\":\"1000000002\",\"issued\":\"2021-02-03\",\"year\":\"2026\"}")).andExpect(status().isOk()).andReturn();
  var studentSession=(MockHttpSession)studentResult.getRequest().getSession(false);
  mvc.perform(get("/api/demo/student/me").session(studentSession)).andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Interesado ficticio")).andExpect(jsonPath("$.notes").isEmpty()).andExpect(jsonPath("$.movements").isEmpty());
  mvc.perform(post("/api/demo/logout").session(studentSession).with(csrf())).andExpect(status().isOk());
  assertTrue(studentSession.isInvalid());
 }
 @Test void incorrectCredentialsAreRejected() throws Exception {
  mvc.perform(post("/api/demo/login/admin").with(csrf()).contentType("application/json").content("{\"username\":\"adminN\",\"password\":\"incorrecta\"}")).andExpect(status().isUnauthorized());
 }
}
