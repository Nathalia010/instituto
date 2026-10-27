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
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:provisioning;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"}) @AutoConfigureMockMvc @ActiveProfiles("demo")
class StudentProvisioningTest {
 @Autowired MockMvc mvc;@Autowired JdbcTemplate db;@Autowired ObjectMapper json;@Autowired BulkDataService data;@Autowired StudentProvisioningService provisioning;
 MockHttpSession admin()throws Exception{return (MockHttpSession)mvc.perform(post("/api/demo/login/admin").with(csrf()).contentType("application/json").content("{\"username\":\"adminN\",\"password\":\"1234987A\"}")).andReturn().getRequest().getSession(false);}
 Map<String,String> values(String document){return new HashMap<>(Map.of("tipo_documento","CC","documento",document,"nombres","Estudiante","apellidos","Importado","correo","importado@example.invalid","telefono","3000000000","fecha_expedicion","2020-01-15","programa","validacion","anio","2026"));}
 @Test void manualStudentCreatesDisabledAccountAndAdminCanActivate()throws Exception{
  var session=admin();var values=values("8888100001");mvc.perform(post("/api/demo/admin/data/manual/estudiantes").session(session).with(csrf()).contentType("application/json").content(json.writeValueAsString(Map.of("values",values)))).andExpect(status().isOk());
  var row=db.queryForMap("SELECT * FROM demo_students WHERE document='8888100001'");assertEquals(false,row.get("account_active"));assertEquals("Estudiante Importado",row.get("name"));assertEquals("2020-01-15",row.get("issued"));String login="{\"document\":\"8888100001\",\"issued\":\"2020-01-15\",\"year\":\"2026\"}";
  mvc.perform(post("/api/demo/login/student").with(csrf()).contentType("application/json").content(login)).andExpect(status().isUnauthorized());
  mvc.perform(post("/api/demo/admin/students/"+row.get("id")+"/access").session(session).with(csrf()).contentType("application/json").content("{\"active\":true}")).andExpect(status().isOk());mvc.perform(post("/api/demo/login/student").with(csrf()).contentType("application/json").content(login)).andExpect(status().isOk());
  values.put("tipo_documento","TI");mvc.perform(post("/api/demo/admin/data/manual/estudiantes").session(session).with(csrf()).contentType("application/json").content(json.writeValueAsString(Map.of("values",values)))).andExpect(status().isBadRequest());assertEquals(true,db.queryForObject("SELECT account_active FROM demo_students WHERE document='8888100001'",Boolean.class));
 }
 @Test void previewDoesNotCreateAccountsAndConfirmCreatesOnlyValidRows()throws Exception{
  var session=admin();var v=values("8888100002");var fields=DataCatalog.get("estudiantes").fields();var mapping=new LinkedHashMap<String,Integer>();for(int i=0;i<fields.size();i++)mapping.put(fields.get(i),i);var row=fields.stream().map(f->v.getOrDefault(f,"")).toList();var request=new BulkDataService.PreviewRequest("estudiantes","estudiantes.xlsx",List.of(row,row),mapping,0,5);
  var preview=mvc.perform(post("/api/demo/admin/data/preview").session(session).with(csrf()).contentType("application/json").content(json.writeValueAsString(request))).andExpect(status().isOk()).andExpect(jsonPath("$.valid").value(1)).andExpect(jsonPath("$.invalid").value(1)).andReturn();String id=json.readTree(preview.getResponse().getContentAsString()).get("id").asText();assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM demo_students WHERE document='8888100002'",Integer.class));
  mvc.perform(post("/api/demo/admin/data/confirm/"+id).session(session).with(csrf())).andExpect(status().isOk()).andExpect(jsonPath("$.saved").value(1));assertEquals(false,db.queryForObject("SELECT account_active FROM demo_students WHERE document='8888100002'",Boolean.class));assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM demo_students WHERE document='8888100002'",Integer.class));mvc.perform(post("/api/demo/admin/data/confirm/"+id).session(session).with(csrf())).andExpect(status().isConflict());assertEquals(1,data.list("estudiantes",null).stream().filter(r->"8888100002".equals(r.get("documento"))).count());
 }
 @Test void missingIssueDateKeepsAccountDisabledUntilCompleted()throws Exception{
  var session=admin();var values=values("PA123456");values.put("tipo_documento","PA");values.put("fecha_expedicion","");data.saveRecord("estudiantes",null,values,"adminN");var account=db.queryForMap("SELECT * FROM demo_students WHERE document='PA123456'");assertEquals("",account.get("issued"));String id=account.get("id").toString();
  mvc.perform(post("/api/demo/admin/students/"+id+"/access").session(session).with(csrf()).contentType("application/json").content("{\"active\":true}")).andExpect(status().isBadRequest());assertEquals(false,db.queryForObject("SELECT account_active FROM demo_students WHERE id=?",Boolean.class,id));
  var edit=new HashMap<String,Object>(Map.of("name","Estudiante Importado","document","PA123456","email","importado@example.invalid","phone","3000000000","program","validacion","issued","2020-01-15","status","PENDIENTE_MATRICULA","notes","","year",2027));mvc.perform(post("/api/demo/admin/students/"+id+"/edit").session(session).with(csrf()).contentType("application/json").content(json.writeValueAsString(edit))).andExpect(status().isOk());mvc.perform(post("/api/demo/admin/students/"+id+"/access").session(session).with(csrf()).contentType("application/json").content("{\"active\":true}")).andExpect(status().isOk());mvc.perform(post("/api/demo/login/student").with(csrf()).contentType("application/json").content("{\"document\":\"pa123456\",\"issued\":\"2020-01-15\",\"year\":\"2027\"}")).andExpect(status().isOk());
 }

 @Test void existingImportedStudentsGetOneInactiveAccountWithoutChangingActiveAccounts(){
  String hash=db.queryForObject("SELECT key_hash FROM demo_students WHERE id='prueba-estudiante'",String.class);var v=values("8888100099");db.update("INSERT INTO demo_data_records(module,record_key,academic_year,payload) VALUES ('estudiantes','CC:8888100099',2026,?)",data.encode(v));var backfill=new StudentAccountBackfill(db,data,provisioning);backfill.run(null);backfill.run(null);
  assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM demo_students WHERE document='8888100099'",Integer.class));assertEquals(false,db.queryForObject("SELECT account_active FROM demo_students WHERE document='8888100099'",Boolean.class));assertEquals(true,db.queryForObject("SELECT account_active FROM demo_students WHERE id='prueba-estudiante'",Boolean.class));assertEquals(hash,db.queryForObject("SELECT key_hash FROM demo_students WHERE id='prueba-estudiante'",String.class));
 }
}
