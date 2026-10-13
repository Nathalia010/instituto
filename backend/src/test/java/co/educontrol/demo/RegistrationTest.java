package co.educontrol.demo;

import java.util.*;
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
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:registration;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"})
@AutoConfigureMockMvc @ActiveProfiles("demo")
class RegistrationTest {
 @Autowired MockMvc mvc;@Autowired ObjectMapper json;@Autowired BulkDataService data;
 MockHttpSession admin()throws Exception{return (MockHttpSession)mvc.perform(post("/api/demo/login/admin").with(csrf()).contentType("application/json").content("{\"username\":\"adminN\",\"password\":\"1234987A\"}")).andExpect(status().isOk()).andReturn().getRequest().getSession(false);}
 Map<String,String> event(String code,String state){var e=new HashMap<String,String>();DataCatalog.get("agenda").fields().forEach(f->e.put(f,""));e.putAll(Map.of("codigo_evento",code,"anio","2027","tipo_evento","INGRESO","titulo","Ingreso de prueba","programa","todos","fecha_inicio","2026-12-15","estado",state));return e;}
 @Test void eventPublicationEditingAndAnnualExports()throws Exception{
  var session=admin();var e=event("EVENT-TEST","BORRADOR");
  mvc.perform(post("/api/demo/admin/data/agenda").session(session).with(csrf()).contentType("application/json").content(json.writeValueAsString(e))).andExpect(status().isOk());
  mvc.perform(get("/api/demo/agenda")).andExpect(jsonPath("$[?(@.codigo_evento=='EVENT-TEST')]").isEmpty());
  e.put("estado","PUBLICADO");e.put("hora","08:30");
  mvc.perform(post("/api/demo/admin/data/agenda/EVENT-TEST").session(session).with(csrf()).contentType("application/json").content(json.writeValueAsString(e))).andExpect(status().isOk());
  mvc.perform(get("/api/demo/agenda").param("year","2027")).andExpect(jsonPath("$[?(@.codigo_evento=='EVENT-TEST')].hora").value("08:30"));
  mvc.perform(get("/api/demo/agenda").param("year","2026")).andExpect(jsonPath("$[?(@.codigo_evento=='EVENT-TEST')]").isEmpty());
  assertEquals(1,data.list("agenda",2027).stream().filter(r->r.get("codigo_evento").equals("EVENT-TEST")).count());
  e.put("estado","CANCELADO");mvc.perform(post("/api/demo/admin/data/agenda/EVENT-TEST").session(session).with(csrf()).contentType("application/json").content(json.writeValueAsString(e))).andExpect(status().isOk());
  mvc.perform(get("/api/demo/agenda")).andExpect(jsonPath("$[?(@.codigo_evento=='EVENT-TEST')]").isEmpty());
 }
 @Test void eventAndFormValidationAndCsrf()throws Exception{
  var session=admin();var e=event("INVALID-DATE","PUBLICADO");e.put("fecha_fin","2026-01-01");
  mvc.perform(post("/api/demo/admin/data/agenda").session(session).with(csrf()).contentType("application/json").content(json.writeValueAsString(e))).andExpect(status().isBadRequest());
  e.put("fecha_fin","");e.put("enlace","javascript:alert(1)");assertFalse(data.check("agenda",List.of(e),0,5).getFirst().valid());
  e.put("enlace","");mvc.perform(post("/api/demo/admin/data/agenda").session(session).contentType("application/json").content(json.writeValueAsString(e))).andExpect(status().isForbidden());
  var result=mvc.perform(get("/api/demo/registration/settings")).andExpect(status().isOk()).andReturn();var s=json.readTree(result.getResponse().getContentAsString());assertNotEquals(s.get("form1"),s.get("form2"));assertEquals("3045822782",s.get("neliPhone").asText());
  var input=json.convertValue(s,new com.fasterxml.jackson.core.type.TypeReference<Map<String,String>>(){});input.put("form2",input.get("form1"));
  mvc.perform(post("/api/demo/admin/registration/settings").session(session).with(csrf()).contentType("application/json").content(json.writeValueAsString(input))).andExpect(status().isBadRequest());
 }
 @Test void studentsCannotChangeRegistrationOrAgenda()throws Exception{
  var result=mvc.perform(post("/api/demo/login/student").with(csrf()).contentType("application/json").content("{\"document\":\"1000000001\",\"issued\":\"2020-01-15\",\"year\":\"2026\"}")).andExpect(status().isOk()).andReturn();var student=(MockHttpSession)result.getRequest().getSession(false);
  mvc.perform(post("/api/demo/admin/data/agenda").session(student).with(csrf()).contentType("application/json").content(json.writeValueAsString(event("FORBIDDEN","PUBLICADO")))).andExpect(status().isForbidden());
  mvc.perform(post("/api/demo/admin/registration/settings").session(student).with(csrf()).contentType("application/json").content("{}")).andExpect(status().isForbidden());
  mvc.perform(multipart("/api/demo/admin/data/agenda/ANY/image").file("file",new byte[]{1}).session(student).with(csrf())).andExpect(status().isForbidden());
 }
 @Test void eventImagesRequireOtherAndPublishedEvent()throws Exception{
  var session=admin();var e=event("IMAGE-EVENT","BORRADOR");e.put("tipo_evento","OTRO");data.saveEvent(null,e,"test");
  var png=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(5,5,java.awt.image.BufferedImage.TYPE_INT_RGB),"png",png);
  mvc.perform(multipart("/api/demo/admin/data/agenda/IMAGE-EVENT/image").file("file",png.toByteArray()).session(session)).andExpect(status().isForbidden());
  mvc.perform(multipart("/api/demo/admin/data/agenda/IMAGE-EVENT/image").file("file",new byte[]{1,2}).session(session).with(csrf())).andExpect(status().isBadRequest());
  mvc.perform(multipart("/api/demo/admin/data/agenda/IMAGE-EVENT/image").file("file",png.toByteArray()).session(session).with(csrf())).andExpect(status().isOk());
  mvc.perform(get("/api/demo/agenda/images/IMAGE-EVENT")).andExpect(status().isNotFound());
  mvc.perform(get("/api/demo/admin/data/agenda/IMAGE-EVENT/image").session(session)).andExpect(status().isOk()).andExpect(content().contentType("image/png"));
  e.put("estado","PUBLICADO");data.saveEvent("IMAGE-EVENT",e,"test");
  mvc.perform(get("/api/demo/agenda")).andExpect(jsonPath("$[?(@.codigo_evento=='IMAGE-EVENT')].imagen_url").value("/api/demo/agenda/images/IMAGE-EVENT"));
  mvc.perform(get("/api/demo/agenda/images/IMAGE-EVENT")).andExpect(status().isOk());
  mvc.perform(post("/api/demo/admin/data/agenda/IMAGE-EVENT/image/hide").session(session).with(csrf())).andExpect(status().isOk());
  mvc.perform(get("/api/demo/agenda/images/IMAGE-EVENT")).andExpect(status().isNotFound());
  e.put("tipo_evento","ICFES");e.put("calendario","B");e.put("etapa","APLICACION");data.saveEvent("IMAGE-EVENT",e,"test");
  mvc.perform(multipart("/api/demo/admin/data/agenda/IMAGE-EVENT/image").file("file",png.toByteArray()).session(session).with(csrf())).andExpect(status().isBadRequest());
  e.put("calendario","INVALIDO");assertFalse(data.check("agenda",List.of(e),0,5).getFirst().valid());
 }
}
