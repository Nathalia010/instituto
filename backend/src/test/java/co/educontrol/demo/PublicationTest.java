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
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:publications;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"}) @AutoConfigureMockMvc @ActiveProfiles("demo")
class PublicationTest {
 @Autowired MockMvc mvc;@Autowired ObjectMapper json;
 MockHttpSession admin()throws Exception{return (MockHttpSession)mvc.perform(post("/api/demo/login/admin").with(csrf()).contentType("application/json").content("{\"username\":\"adminN\",\"password\":\"1234987A\"}")).andExpect(status().isOk()).andReturn().getRequest().getSession(false);}
 @Test void publicationLifecycleAndImagePrivacy()throws Exception{
  var session=admin();var p=new HashMap<String,String>(Map.of("title","Ficticio","intro","Prueba","type","GENERAL","link","","event","","state","BORRADOR"));String path="/api/demo/admin/registration/announcements/TEST-PUBLICATION";
  mvc.perform(post(path).with(csrf()).contentType("application/json").content(json.writeValueAsString(p))).andExpect(status().isUnauthorized());
  mvc.perform(post(path).session(session).contentType("application/json").content(json.writeValueAsString(p))).andExpect(status().isForbidden());
  mvc.perform(post(path).session(session).with(csrf()).contentType("application/json").content(json.writeValueAsString(p))).andExpect(status().isOk());
  var image=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(5,5,java.awt.image.BufferedImage.TYPE_INT_RGB),"png",image);
  mvc.perform(multipart(path+"/image").file("file",image.toByteArray()).session(session).with(csrf())).andExpect(status().isOk());
  mvc.perform(get("/api/demo/announcements/images/TEST-PUBLICATION")).andExpect(status().isNotFound());p.put("state","PUBLICADO");
  mvc.perform(post(path).session(session).with(csrf()).contentType("application/json").content(json.writeValueAsString(p))).andExpect(status().isOk());
  mvc.perform(get("/api/demo/announcements/images/TEST-PUBLICATION")).andExpect(status().isOk());
  mvc.perform(post(path+"/delete").session(session).with(csrf())).andExpect(status().isOk());mvc.perform(get("/api/demo/announcements/images/TEST-PUBLICATION")).andExpect(status().isNotFound());
  mvc.perform(post(path+"/restore").session(session).with(csrf())).andExpect(status().isOk());mvc.perform(get("/api/demo/announcements/images/TEST-PUBLICATION")).andExpect(status().isOk());p.put("link","javascript:alert(1)");
  mvc.perform(post(path).session(session).with(csrf()).contentType("application/json").content(json.writeValueAsString(p))).andExpect(status().isBadRequest());
 }
 @Test void eventPageDependsOnParentAndAvailabilityHasNoPrivateData()throws Exception{
  String code="SALIDA-REFERENCIA-2026";var session=admin();mvc.perform(get("/api/demo/event-pages/"+code)).andExpect(status().isOk()).andExpect(jsonPath("$.meeting").isNotEmpty());
  mvc.perform(post("/api/demo/admin/data/agenda/"+code+"/delete").session(session).with(csrf())).andExpect(status().isOk());mvc.perform(get("/api/demo/event-pages/"+code)).andExpect(status().isNotFound());mvc.perform(post("/api/demo/admin/data/agenda/"+code+"/restore").session(session).with(csrf())).andExpect(status().isOk());
  mvc.perform(get("/api/demo/leveling-dates")).andExpect(status().isOk());
  var student=(MockHttpSession)mvc.perform(post("/api/demo/login/student").with(csrf()).contentType("application/json").content("{\"document\":\"1000000001\",\"issued\":\"2020-01-15\",\"year\":\"2026\"}")).andReturn().getRequest().getSession(false);
  mvc.perform(get("/api/demo/admin/registration/announcements").session(student)).andExpect(status().isForbidden());
 }

 @Test void simpleFieldsUpdateCalendarAndPhotoVisibility()throws Exception{
  var session=admin();String code="SALIDA-REFERENCIA-2026",path="/api/demo/admin/registration/event-pages/"+code;var r=json.readValue(mvc.perform(get(path).session(session)).andReturn().getResponse().getContentAsString(),new com.fasterxml.jackson.core.type.TypeReference<Map<String,String>>(){});
  var original=new HashMap<>(r);r.put("address","Dirección ficticia");r.put("mapUrl","https://maps.google.com/?q=prueba");r.put("date","2026-12-12");r.put("points","[{\"name\":\"Punto ficticio\",\"address\":\"Dirección\",\"mapUrl\":\"https://maps.google.com\",\"time\":\"04:15\"}]");r.put("prices","[{\"label\":\"Prueba\",\"amount\":\"110000\",\"deadline\":\"2026-12-01\"}]");
  mvc.perform(post(path).session(session).with(csrf()).contentType("application/json").content(json.writeValueAsString(r))).andExpect(status().isOk());mvc.perform(get("/api/demo/agenda")).andExpect(jsonPath("$[?(@.codigo_evento=='"+code+"')].fecha_inicio").value("2026-12-12"));
  var bytes=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(5,5,java.awt.image.BufferedImage.TYPE_INT_RGB),"png",bytes);
  mvc.perform(multipart(path+"/photos/1/image").file("file",bytes.toByteArray()).session(session).with(csrf())).andExpect(status().isOk());mvc.perform(get("/api/demo/event-pages/"+code+"/photos/1")).andExpect(status().isOk());
  mvc.perform(get("/api/demo/event-pages/"+code)).andExpect(jsonPath("$.photos").value("[\"/api/demo/event-pages/"+code+"/photos/1\"]"));
  r.put("state","BORRADOR");mvc.perform(post(path).session(session).with(csrf()).contentType("application/json").content(json.writeValueAsString(r))).andExpect(status().isOk());mvc.perform(get("/api/demo/event-pages/"+code+"/photos/1")).andExpect(status().isNotFound());
  r.put("points","[{\"mapUrl\":\"javascript:alert(1)\"}]");mvc.perform(post(path).session(session).with(csrf()).contentType("application/json").content(json.writeValueAsString(r))).andExpect(status().isBadRequest());mvc.perform(post(path).session(session).with(csrf()).contentType("application/json").content(json.writeValueAsString(original))).andExpect(status().isOk());
 }
}

