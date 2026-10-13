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

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:manual_data;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"})
@AutoConfigureMockMvc @ActiveProfiles("demo")
class ManualDataTest {
 @Autowired MockMvc mvc;@Autowired ObjectMapper json;
 MockHttpSession admin()throws Exception{return (MockHttpSession)mvc.perform(post("/api/demo/login/admin").with(csrf()).contentType("application/json").content("{\"username\":\"adminN\",\"password\":\"1234987A\"}")).andReturn().getRequest().getSession(false);}
 String payload(String key,Map<String,String> values)throws Exception{var result=new HashMap<String,Object>();result.put("originalKey",key);result.put("values",values);return json.writeValueAsString(result);}
 @Test void coursesEditTheSamePublishedCatalogAndCodesRemainStable()throws Exception{
  var session=admin();var course=new HashMap<>(Map.of("codigo_curso","manual-course","nombre","Curso de prueba público","tipo_programa","CURSO","descripcion","Descripción de prueba","duracion_horas","","publicado","SI"));
  mvc.perform(post("/api/demo/admin/data/manual/cursos").session(session).with(csrf()).contentType("application/json").content(payload(null,course))).andExpect(status().isOk());mvc.perform(get("/api/demo/programs")).andExpect(jsonPath("$[?(@.id=='manual-course')].name").value("Curso de prueba público"));
  course.put("nombre","Curso editado");mvc.perform(post("/api/demo/admin/data/manual/cursos").session(session).with(csrf()).contentType("application/json").content(payload("manual-course",course))).andExpect(status().isOk());mvc.perform(get("/api/demo/programs")).andExpect(jsonPath("$[?(@.id=='manual-course')].name").value("Curso editado"));
  mvc.perform(post("/api/demo/inquiries").with(csrf()).contentType("application/json").content("{\"name\":\"Interés de prueba\",\"email\":\"test@example.invalid\",\"phone\":\"3000000000\",\"program\":\"manual-course\",\"message\":\"Consulta\",\"consent\":true}")).andExpect(status().isCreated());
  course.put("publicado","NO");mvc.perform(post("/api/demo/admin/data/manual/cursos").session(session).with(csrf()).contentType("application/json").content(payload("manual-course",course))).andExpect(status().isOk());mvc.perform(get("/api/demo/programs")).andExpect(jsonPath("$[?(@.id=='manual-course')]").isEmpty());
  course.put("codigo_curso","changed");mvc.perform(post("/api/demo/admin/data/manual/cursos").session(session).with(csrf()).contentType("application/json").content(payload("manual-course",course))).andExpect(status().isBadRequest());
 }
 @Test void individualCreationValidatesReferencesDuplicatesAndPermissions()throws Exception{
  var session=admin();var teacher=Map.of("tipo_documento","CC","documento","000888","nombre_completo","Docente ficticio","correo","teacher@example.invalid");String request=payload(null,teacher);
  mvc.perform(post("/api/demo/admin/data/manual/docentes").session(session).contentType("application/json").content(request)).andExpect(status().isForbidden());mvc.perform(post("/api/demo/admin/data/manual/docentes").session(session).with(csrf()).contentType("application/json").content(request)).andExpect(status().isOk());mvc.perform(post("/api/demo/admin/data/manual/docentes").session(session).with(csrf()).contentType("application/json").content(request)).andExpect(status().isBadRequest());
  mvc.perform(post("/api/demo/admin/data/manual/matriculas").session(session).with(csrf()).contentType("application/json").content(payload(null,Map.of("codigo_matricula","BAD","tipo_documento","CC","documento","000888","codigo_curso","missing","codigo_periodo","missing","fecha_matricula","2026-10-01","estado_academico","EN_CURSO")))).andExpect(status().isBadRequest());
  var student=(MockHttpSession)mvc.perform(post("/api/demo/login/student").with(csrf()).contentType("application/json").content("{\"document\":\"1000000001\",\"issued\":\"2020-01-15\",\"year\":\"2026\"}")).andReturn().getRequest().getSession(false);mvc.perform(post("/api/demo/admin/data/manual/docentes").session(student).with(csrf()).contentType("application/json").content(request)).andExpect(status().isForbidden());
 }
}
