package co.educontrol.demo;
import java.io.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:landing_images;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"}) @AutoConfigureMockMvc @ActiveProfiles("demo") @Transactional
class LandingImageTest {
 @Autowired MockMvc mvc;@Autowired JdbcTemplate db;
 MockHttpSession admin()throws Exception{return (MockHttpSession)mvc.perform(post("/api/demo/login/admin").with(csrf()).contentType("application/json").content("{\"username\":\"adminN\",\"password\":\"1234987A\"}")).andExpect(status().isOk()).andReturn().getRequest().getSession(false);}
 MockMultipartFile image()throws Exception{var bytes=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(10,10,BufferedImage.TYPE_INT_RGB),"png",bytes);return new MockMultipartFile("file","portada.png","image/png",bytes.toByteArray());}
 @Test void heroHasDefaultUntilExplicitUploadAndHideRestoresDefault()throws Exception{
  var admin=admin();String path="/api/demo/admin/registration/page-images/home/hero/image";
  mvc.perform(get("/api/demo/home/appearance")).andExpect(jsonPath("$.heroImage").value(""));
  mvc.perform(multipart(path).file(image()).session(admin).with(csrf())).andExpect(status().isOk());
  mvc.perform(get("/api/demo/home/appearance")).andExpect(jsonPath("$.heroImage").value("/api/demo/page-images/home/hero"));
  mvc.perform(get("/api/demo/page-images/home/hero")).andExpect(status().isOk()).andExpect(content().contentType("image/png")).andExpect(header().string("X-Content-Type-Options","nosniff"));
  mvc.perform(post(path+"/hide").session(admin).with(csrf()).contentType("application/json").content("{}")).andExpect(status().isOk());mvc.perform(get("/api/demo/home/appearance")).andExpect(jsonPath("$.heroImage").value(""));mvc.perform(get("/api/demo/page-images/home/hero")).andExpect(status().isNotFound());
 }
 @Test void courseImagesArePublicOnlyForPublishedActiveCourses()throws Exception{
  var admin=admin();String path="/api/demo/admin/registration/page-images/course/validacion/image";
  mvc.perform(multipart(path).file(image()).session(admin).with(csrf())).andExpect(status().isOk());mvc.perform(get("/api/demo/programs")).andExpect(jsonPath("$[?(@.id=='validacion')].image").value("/api/demo/page-images/course/validacion"));mvc.perform(get("/api/demo/page-images/course/validacion")).andExpect(status().isOk());
  db.update("UPDATE demo_data_records SET active=FALSE WHERE module='cursos' AND record_key='validacion'");mvc.perform(get("/api/demo/page-images/course/validacion")).andExpect(status().isNotFound());mvc.perform(get(path).session(admin)).andExpect(status().isOk());
  db.update("UPDATE demo_data_records SET deleted=TRUE WHERE module='cursos' AND record_key='validacion'");mvc.perform(get("/api/demo/page-images/course/validacion")).andExpect(status().isNotFound());
 }
 @Test void imageChangesRequireAdminCsrfAndValidImage()throws Exception{
  String path="/api/demo/admin/registration/page-images/home/hero/image";var admin=admin();
  mvc.perform(multipart(path).file(image()).with(csrf())).andExpect(status().isUnauthorized());mvc.perform(multipart(path).file(image()).session(admin)).andExpect(status().isForbidden());mvc.perform(multipart(path).file(new MockMultipartFile("file","imagen.png","image/png",new byte[]{1,2,3})).session(admin).with(csrf())).andExpect(status().isBadRequest());
  mvc.perform(multipart("/api/demo/page-images/home/hero").file(image()).with(csrf())).andExpect(status().isUnauthorized());mvc.perform(multipart("/api/demo/admin/registration/page-images/course/missing/image").file(image()).session(admin).with(csrf())).andExpect(status().isNotFound());
 }
}
