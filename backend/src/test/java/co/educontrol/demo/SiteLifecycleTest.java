package co.educontrol.demo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:site_lifecycle;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"})
@AutoConfigureMockMvc @ActiveProfiles("demo")
class SiteLifecycleTest {
 @Autowired MockMvc mvc;@Autowired ObjectMapper json;
 @Test void newSitesCanBeEditedAndRemovedFromPublicContact()throws Exception{
  var session=(MockHttpSession)mvc.perform(post("/api/demo/login/admin").with(csrf()).contentType("application/json").content("{\"username\":\"adminN\",\"password\":\"1234987A\"}")).andExpect(status().isOk()).andReturn().getRequest().getSession(false);
  mvc.perform(post("/api/demo/admin/registration/sites").with(csrf())).andExpect(status().isUnauthorized());
  mvc.perform(post("/api/demo/admin/registration/sites").session(session)).andExpect(status().isForbidden());
  var result=mvc.perform(post("/api/demo/admin/registration/sites").session(session).with(csrf())).andExpect(status().isOk()).andReturn();int id=json.readTree(result.getResponse().getContentAsString()).get("id").asInt();
  mvc.perform(post("/api/demo/admin/registration/sites/"+id).session(session).with(csrf()).contentType("application/json").content("{\"name\":\"Sede nueva\",\"address\":\"Calle 1\",\"phone\":\"3000000000\",\"hours\":\"Lunes\",\"mapUrl\":\"https://maps.google.com/\"}")).andExpect(status().isOk());
  mvc.perform(get("/api/demo/sites")).andExpect(jsonPath("$[?(@.id=="+id+")].name").value("Sede nueva"));
  mvc.perform(post("/api/demo/admin/registration/sites/"+id+"/delete").session(session).with(csrf())).andExpect(status().isOk());
  mvc.perform(get("/api/demo/sites")).andExpect(jsonPath("$[?(@.id=="+id+")]").isEmpty());
  mvc.perform(get("/api/demo/sites/images/"+id)).andExpect(status().isNotFound());
  mvc.perform(post("/api/demo/admin/registration/sites/"+id).session(session).with(csrf()).contentType("application/json").content("{}")).andExpect(status().isNotFound());
 }
}
