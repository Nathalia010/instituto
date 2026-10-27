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
import org.springframework.transaction.annotation.Transactional;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:data_lifecycle;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"})
@AutoConfigureMockMvc @ActiveProfiles("demo") @Transactional
class DataLifecycleTest {
 @Autowired MockMvc mvc;@Autowired JdbcTemplate db;@Autowired BulkDataService data;@Autowired ObjectMapper json;@Autowired StudentAccountBackfill backfill;
 MockHttpSession admin()throws Exception{return (MockHttpSession)mvc.perform(post("/api/demo/login/admin").with(csrf()).contentType("application/json").content("{\"username\":\"adminN\",\"password\":\"1234987A\"}")).andExpect(status().isOk()).andReturn().getRequest().getSession(false);}
 MockHttpSession login(String document)throws Exception{return (MockHttpSession)mvc.perform(post("/api/demo/login/student").with(csrf()).contentType("application/json").content(json.writeValueAsString(Map.of("document",document,"issued","2020-01-15","year","2026")))).andExpect(status().isOk()).andReturn().getRequest().getSession(false);}
 void change(MockHttpSession admin,String module,String key,String action)throws Exception{mvc.perform(post("/api/demo/admin/data/lifecycle/"+module).session(admin).with(csrf()).contentType("application/json").content(json.writeValueAsString(Map.of("key",key,"action",action)))).andExpect(status().isOk());}
 Map<String,String> course(String code){return Map.of("codigo_curso",code,"nombre","Curso de prueba","tipo_programa","CURSO","publicado","SI");}
 @Test void permissionsAndCsrfRequired()throws Exception{
  var admin=admin();var student=login("1000000001");String path="/api/demo/admin/data/lifecycle/cursos",body="{\"key\":\"icfes\",\"action\":\"delete\"}";
  mvc.perform(post(path).with(csrf()).contentType("application/json").content(body)).andExpect(status().isUnauthorized());
  mvc.perform(post(path).session(student).with(csrf()).contentType("application/json").content(body)).andExpect(status().isForbidden());
  mvc.perform(get(path+"/deleted").session(student)).andExpect(status().isForbidden());
  mvc.perform(post(path).session(admin).contentType("application/json").content(body)).andExpect(status().isForbidden());
  mvc.perform(post(path).session(admin).with(csrf()).contentType("application/json").content("{}")).andExpect(status().isBadRequest());
 }
 @Test void courseHidesPubliclyAndRestoresInactiveWithoutLosingHistoryOrPublication()throws Exception{
  var admin=admin();data.saveRecord("cursos",null,course("lifecycle"),"adminN");
  db.update("INSERT INTO demo_data_records(module,record_key,academic_year,payload) VALUES ('matriculas','historia',2026,?)",data.encode(Map.of("codigo_matricula","historia","codigo_curso","lifecycle")));
  mvc.perform(get("/api/demo/programs")).andExpect(jsonPath("$[?(@.id=='lifecycle')]").isNotEmpty());
  change(admin,"cursos","lifecycle","deactivate");mvc.perform(get("/api/demo/programs")).andExpect(jsonPath("$[?(@.id=='lifecycle')]").isEmpty());
  change(admin,"cursos","lifecycle","activate");change(admin,"cursos","lifecycle","delete");
  assertTrue(data.list("cursos",null).stream().noneMatch(r->"lifecycle".equals(r.get("codigo_curso"))));
  assertEquals("lifecycle",data.list("matriculas",2026).getFirst().get("codigo_curso"));
  mvc.perform(get("/api/demo/admin/data/lifecycle/cursos/deleted").session(admin)).andExpect(jsonPath("$[?(@.codigo_curso=='lifecycle')].estado_registro").value("Eliminado"));
  assertThrows(org.springframework.web.server.ResponseStatusException.class,()->data.saveRecord("cursos",null,course("lifecycle"),"adminN"));
  change(admin,"cursos","lifecycle","restore");var restored=data.list("cursos",null).stream().filter(r->"lifecycle".equals(r.get("codigo_curso"))).findFirst().orElseThrow();assertEquals("Inactivo",restored.get("estado_registro"));assertEquals("SI",restored.get("publicado"));
  mvc.perform(get("/api/demo/programs")).andExpect(jsonPath("$[?(@.id=='lifecycle')]").isEmpty());change(admin,"cursos","lifecycle","activate");
  mvc.perform(get("/api/demo/programs")).andExpect(jsonPath("$[?(@.id=='lifecycle')]").isNotEmpty());
 }
 @Test void teachersCanDeactivateDeleteRestoreAndCannotBeReimportedOverTrash()throws Exception{
  var admin=admin();var teacher=Map.of("tipo_documento","CC","documento","44556677","nombre_completo","Docente prueba","correo","docente@example.invalid");data.saveRecord("docentes",null,teacher,"adminN");
  change(admin,"docentes","CC:44556677","deactivate");assertEquals("Inactivo",data.list("docentes",null).getFirst().get("estado_registro"));change(admin,"docentes","CC:44556677","delete");assertTrue(data.list("docentes",null).isEmpty());
  assertFalse(data.check("docentes",List.of(teacher),0,5).getFirst().valid());change(admin,"docentes","CC:44556677","restore");assertEquals("Inactivo",data.list("docentes",null).getFirst().get("estado_registro"));change(admin,"docentes","CC:44556677","activate");assertEquals("Activo",data.list("docentes",null).getFirst().get("estado_registro"));
 }
 @Test void importedStudentDeletionRevokesAccessAndBackfillDoesNotResurrect()throws Exception{
  var admin=admin();data.saveRecord("estudiantes",null,Map.of("tipo_documento","CC","documento","66778899","nombre_completo","Estudiante prueba","fecha_expedicion","2020-01-15","anio","2026"),"adminN");
  change(admin,"estudiantes","CC:66778899","activate");var student=login("66778899");change(admin,"estudiantes","CC:66778899","delete");backfill.run(null);
  mvc.perform(get("/api/demo/student/me").session(student)).andExpect(status().isUnauthorized());
  mvc.perform(get("/api/demo/admin/students").session(admin)).andExpect(jsonPath("$[?(@.document=='66778899')]").isEmpty());
  assertTrue(data.list("estudiantes",null).stream().noneMatch(r->"66778899".equals(r.get("documento"))));assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM demo_students WHERE document='66778899' AND deleted=TRUE",Integer.class));
  String id=db.queryForObject("SELECT id FROM demo_students WHERE document='66778899'",String.class);mvc.perform(post("/api/demo/admin/students/"+id+"/access").session(admin).with(csrf()).contentType("application/json").content("{\"active\":true}")).andExpect(status().isNotFound());
  change(admin,"estudiantes","CC:66778899","restore");assertEquals("Inactivo",data.list("estudiantes",2026).stream().filter(r->"66778899".equals(r.get("documento"))).findFirst().orElseThrow().get("estado_registro"));change(admin,"estudiantes","CC:66778899","activate");login("66778899");
 }
 @Test void accountOnlyStudentCanBeDeletedAndRestoredWithHistoryIntact()throws Exception{
  var admin=admin();int notes=db.queryForObject("SELECT COUNT(*) FROM demo_notes WHERE student_id='prueba-estudiante'",Integer.class);change(admin,"estudiantes","CC:1000000001","delete");assertTrue(data.list("estudiantes",null).stream().noneMatch(r->"1000000001".equals(r.get("documento"))));assertEquals(notes,db.queryForObject("SELECT COUNT(*) FROM demo_notes WHERE student_id='prueba-estudiante'",Integer.class));change(admin,"estudiantes","CC:1000000001","restore");assertEquals(false,db.queryForObject("SELECT account_active FROM demo_students WHERE id='prueba-estudiante'",Boolean.class));
 }
 @Test void editingImportedDocumentKeepsCatalogAndLifecycleInSync()throws Exception{
  var admin=admin();data.saveRecord("estudiantes",null,Map.of("tipo_documento","CC","documento","77889900","nombre_completo","Antes","anio","2026"),"adminN");String id=db.queryForObject("SELECT id FROM demo_students WHERE document='77889900'",String.class);
  mvc.perform(post("/api/demo/admin/students/"+id+"/edit").session(admin).with(csrf()).contentType("application/json").content(json.writeValueAsString(Map.of("name","Después","document","77889901","email","prueba@example.invalid","phone","3000000000","program","validacion","issued","2020-01-15","status","EN_CURSO","notes","Nota conservada","year",2027)))).andExpect(status().isOk());
  assertTrue(data.list("estudiantes",null).stream().noneMatch(r->"77889900".equals(r.get("documento"))));assertEquals("Nota conservada",data.list("estudiantes",2027).stream().filter(r->"77889901".equals(r.get("documento"))).findFirst().orElseThrow().get("notas_administrativas"));change(admin,"estudiantes","CC:77889901","delete");assertTrue(data.list("estudiantes",null).stream().noneMatch(r->"77889901".equals(r.get("documento"))));
 }
}
