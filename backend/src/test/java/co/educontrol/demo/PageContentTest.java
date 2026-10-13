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
import org.springframework.jdbc.core.JdbcTemplate;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:page_content;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"})
@AutoConfigureMockMvc @ActiveProfiles("demo")
class PageContentTest {
 @Autowired MockMvc mvc;@Autowired ObjectMapper json;@Autowired BulkDataService data;@Autowired JdbcTemplate db;
 MockHttpSession admin()throws Exception{return (MockHttpSession)mvc.perform(post("/api/demo/login/admin").with(csrf()).contentType("application/json").content("{\"username\":\"adminN\",\"password\":\"1234987A\"}")).andExpect(status().isOk()).andReturn().getRequest().getSession(false);}
 @Test void deletionIsRecoverableAndDoesNotRecreateKeys()throws Exception{
  var session=admin();var e=new HashMap<String,String>();DataCatalog.get("agenda").fields().forEach(f->e.put(f,""));e.putAll(Map.of("codigo_evento","REMOVE-TEST","anio","2026","tipo_evento","OTRO","titulo","Evento ficticio","fecha_inicio","2026-12-01","estado","PUBLICADO"));data.saveEvent(null,e,"test");
  mvc.perform(post("/api/demo/admin/data/agenda/REMOVE-TEST/delete").session(session)).andExpect(status().isForbidden());
  mvc.perform(post("/api/demo/admin/data/agenda/REMOVE-TEST/delete").session(session).with(csrf())).andExpect(status().isOk());
  assertTrue(data.list("agenda",null).stream().noneMatch(r->"REMOVE-TEST".equals(r.get("codigo_evento"))));assertFalse(data.check("agenda",List.of(e),0,5).getFirst().valid());
  mvc.perform(get("/api/demo/agenda")).andExpect(jsonPath("$[?(@.codigo_evento=='REMOVE-TEST')]").isEmpty());
  mvc.perform(get("/api/demo/admin/data/agenda/deleted").session(session)).andExpect(jsonPath("$[?(@.codigo_evento=='REMOVE-TEST')]").isNotEmpty());
  mvc.perform(post("/api/demo/admin/data/agenda/REMOVE-TEST/restore").session(session).with(csrf())).andExpect(status().isOk());
  mvc.perform(get("/api/demo/agenda")).andExpect(jsonPath("$[?(@.codigo_evento=='REMOVE-TEST')].estado").value("PUBLICADO"));
 }
 @Test void sitesAndImagesAreAdminEditableAndValidated()throws Exception{
  mvc.perform(get("/api/demo/sites")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3));var session=admin();String site="{\"name\":\"Sede de prueba\",\"address\":\"Dirección ficticia\",\"phone\":\"3000000000\",\"hours\":\"Lunes\",\"mapUrl\":\"https://example.invalid/map\"}";
  mvc.perform(post("/api/demo/admin/registration/sites/2").session(session).contentType("application/json").content(site)).andExpect(status().isForbidden());
  mvc.perform(post("/api/demo/admin/registration/sites/2").session(session).with(csrf()).contentType("application/json").content(site.replace("https://example.invalid/map","javascript:alert(1)"))).andExpect(status().isBadRequest());
  mvc.perform(post("/api/demo/admin/registration/sites/2").session(session).with(csrf()).contentType("application/json").content(site)).andExpect(status().isOk());
  mvc.perform(get("/api/demo/sites")).andExpect(jsonPath("$[?(@.id==2)].address").value("Dirección ficticia"));
  var bytes=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(5,5,java.awt.image.BufferedImage.TYPE_INT_RGB),"png",bytes);
  mvc.perform(multipart("/api/demo/admin/registration/sites/2/image").file("file",new byte[]{1}).session(session).with(csrf())).andExpect(status().isBadRequest());
  mvc.perform(multipart("/api/demo/admin/registration/sites/2/image").file("file",bytes.toByteArray()).session(session).with(csrf())).andExpect(status().isOk());
  mvc.perform(get("/api/demo/sites/images/2")).andExpect(status().isOk()).andExpect(content().contentType("image/png"));
  mvc.perform(post("/api/demo/admin/registration/sites/2/image/hide").session(session).with(csrf())).andExpect(status().isOk());
  mvc.perform(get("/api/demo/sites/images/2")).andExpect(status().isNotFound());
 }
 @Test void inquiriesPreserveSeparateNamesAndLocationAndStudentsCannotEditContent()throws Exception{
  String input="{\"name\":\"Nombre Apellido\",\"firstName\":\"Nombre\",\"lastName\":\"Apellido\",\"city\":\"Ciudad ficticia\",\"neighborhood\":\"Barrio ficticio\",\"phone\":\"3000000000\",\"email\":\"test@example.invalid\",\"program\":\"infancia\",\"message\":\"Prueba\",\"consent\":true}";
  mvc.perform(post("/api/demo/inquiries").with(csrf()).contentType("application/json").content(input.replace("\"lastName\":\"Apellido\"","\"lastName\":\"\""))).andExpect(status().isBadRequest());
  var response=mvc.perform(post("/api/demo/inquiries").with(csrf()).contentType("application/json").content(input)).andExpect(status().isCreated()).andReturn();String id=json.readTree(response.getResponse().getContentAsString()).get("id").asText();var row=db.queryForMap("SELECT * FROM demo_inquiries WHERE id=?",id);assertEquals("Nombre",row.get("first_name"));assertEquals("Apellido",row.get("last_name"));assertEquals("Ciudad ficticia",row.get("city"));assertEquals("Barrio ficticio",row.get("neighborhood"));
  var student=(MockHttpSession)mvc.perform(post("/api/demo/login/student").with(csrf()).contentType("application/json").content("{\"document\":\"1000000001\",\"issued\":\"2020-01-15\",\"year\":\"2026\"}")).andReturn().getRequest().getSession(false);
  mvc.perform(post("/api/demo/admin/data/agenda/X/delete").session(student).with(csrf())).andExpect(status().isForbidden());mvc.perform(get("/api/demo/admin/data/agenda/deleted").session(student)).andExpect(status().isForbidden());mvc.perform(post("/api/demo/admin/registration/sites/1").session(student).with(csrf()).contentType("application/json").content("{}")).andExpect(status().isForbidden());
 }
}
