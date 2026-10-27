package co.educontrol.demo;

import java.io.ByteArrayOutputStream;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:gallery;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"})
@AutoConfigureMockMvc @ActiveProfiles("demo")
class GalleryTest {
 @Autowired MockMvc mvc;
 @Autowired ObjectMapper json;
 MockHttpSession admin() throws Exception{return (MockHttpSession)mvc.perform(post("/api/demo/login/admin").with(csrf()).contentType("application/json").content("{\"username\":\"adminN\",\"password\":\"1234987A\"}")).andExpect(status().isOk()).andReturn().getRequest().getSession(false);}
 MockMultipartFile image() throws Exception{var bytes=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(8,8,BufferedImage.TYPE_INT_RGB),"png",bytes);return new MockMultipartFile("file","prueba.png","image/png",bytes.toByteArray());}
 @Test void publicGalleryAndAdminPublishingHideRestore() throws Exception{
  mvc.perform(get("/api/demo/gallery")).andExpect(status().isOk()).andExpect(jsonPath("$[?(@.id=='perro-centro-saber')].url").value("/marca/perro-graduado.png"));
  var session=admin();var result=mvc.perform(multipart("/api/demo/admin/gallery").file(image()).param("caption","Momento de prueba").session(session).with(csrf())).andExpect(status().isCreated()).andReturn();
  String id=json.readTree(result.getResponse().getContentAsString()).get("id").asText();
  mvc.perform(get("/api/demo/gallery/images/"+id)).andExpect(status().isOk()).andExpect(content().contentType("image/png"));
  mvc.perform(post("/api/demo/admin/gallery/"+id+"/hide").session(session)).andExpect(status().isForbidden());
  mvc.perform(post("/api/demo/admin/gallery/"+id+"/hide").session(session).with(csrf())).andExpect(status().isOk());
  mvc.perform(get("/api/demo/gallery")).andExpect(jsonPath("$[?(@.id=='"+id+"')]").isEmpty());
  mvc.perform(get("/api/demo/gallery/images/"+id)).andExpect(status().isNotFound());
  mvc.perform(get("/api/demo/admin/gallery/images/"+id).session(session)).andExpect(status().isOk());
  mvc.perform(post("/api/demo/admin/gallery/"+id+"/restore").session(session).with(csrf())).andExpect(status().isOk());
  mvc.perform(get("/api/demo/gallery")).andExpect(jsonPath("$[?(@.id=='"+id+"')].caption").value("Momento de prueba"));
 }
 @Test void filesAndCaptionsAreValidated() throws Exception{
  var session=admin();
  mvc.perform(multipart("/api/demo/admin/gallery").file(new MockMultipartFile("file","fake.png","image/png","<svg onload='alert(1)'/>".getBytes())).param("caption","No válida").session(session).with(csrf())).andExpect(status().isBadRequest());
  mvc.perform(multipart("/api/demo/admin/gallery").file(image()).param("caption"," ").session(session).with(csrf())).andExpect(status().isBadRequest());
  mvc.perform(multipart("/api/demo/admin/gallery").file(image()).param("caption","x".repeat(181)).session(session).with(csrf())).andExpect(status().isBadRequest());
  mvc.perform(multipart("/api/demo/admin/gallery").file(image()).param("caption","Prueba").session(session)).andExpect(status().isForbidden());
 }
 @Test void deletionPermanentlyRemovesStoredImage()throws Exception{
  var session=admin();var result=mvc.perform(multipart("/api/demo/admin/gallery").file(image()).param("caption","Imagen eliminable de prueba").session(session).with(csrf())).andExpect(status().isCreated()).andReturn();String id=json.readTree(result.getResponse().getContentAsString()).get("id").asText();
  mvc.perform(post("/api/demo/admin/gallery/"+id+"/delete").session(session)).andExpect(status().isForbidden());
  mvc.perform(post("/api/demo/admin/gallery/"+id+"/delete").session(session).with(csrf())).andExpect(status().isOk());
  mvc.perform(get("/api/demo/gallery/images/"+id)).andExpect(status().isNotFound());mvc.perform(get("/api/demo/admin/gallery/images/"+id).session(session)).andExpect(status().isNotFound());mvc.perform(get("/api/demo/admin/gallery").session(session)).andExpect(jsonPath("$[?(@.id=='"+id+"')]").isEmpty());
  mvc.perform(post("/api/demo/admin/gallery/"+id+"/restore").session(session).with(csrf())).andExpect(status().isNotFound());
 }
 @Test void onlyAdminsCanManageImages() throws Exception{
  mvc.perform(get("/api/demo/admin/gallery")).andExpect(status().isUnauthorized());
  var result=mvc.perform(post("/api/demo/login/student").with(csrf()).contentType("application/json").content("{\"document\":\"1000000001\",\"issued\":\"2020-01-15\",\"year\":\"2026\"}")).andExpect(status().isOk()).andReturn();
  var student=(MockHttpSession)result.getRequest().getSession(false);
  mvc.perform(get("/api/demo/admin/gallery").session(student)).andExpect(status().isForbidden());
  mvc.perform(multipart("/api/demo/admin/gallery").file(image()).param("caption","Prueba").session(student).with(csrf())).andExpect(status().isForbidden());
  mvc.perform(post("/api/demo/admin/gallery/perro-centro-saber/hide").session(student).with(csrf())).andExpect(status().isForbidden());
  mvc.perform(post("/api/demo/admin/gallery/perro-centro-saber/delete").session(student).with(csrf())).andExpect(status().isForbidden());
  mvc.perform(post("/api/demo/admin/gallery/perro-centro-saber/undelete").session(student).with(csrf())).andExpect(status().isForbidden());
 }
}
