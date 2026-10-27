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
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:course_admin;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"}) @AutoConfigureMockMvc @ActiveProfiles("demo") @Transactional
class CourseAdminTest {
 @Autowired MockMvc mvc;@Autowired JdbcTemplate db;@Autowired BulkDataService data;@Autowired ObjectMapper json;
 MockHttpSession admin()throws Exception{return (MockHttpSession)mvc.perform(post("/api/demo/login/admin").with(csrf()).contentType("application/json").content("{\"username\":\"adminN\",\"password\":\"1234987A\"}")).andReturn().getRequest().getSession(false);}
 void course(String code){data.saveRecord("cursos",null,Map.of("codigo_curso",code,"nombre",code,"tipo_programa","CURSO","publicado","SI"),"adminN");}
 void record(String module,String key,int year,Map<String,String> row){db.update("INSERT INTO demo_data_records(module,record_key,academic_year,payload) VALUES (?,?,?,?)",module,key,year,data.encode(row));}
 String student(String doc,String program,boolean active){data.saveRecord("estudiantes",null,Map.of("tipo_documento","CC","documento",doc,"nombre_completo","Alumno "+doc,"anio","2026","programa",program,"fecha_expedicion","2020-01-15"),"adminN");db.update("UPDATE demo_students SET account_active=? WHERE document=?",active,doc);return db.queryForObject("SELECT id FROM demo_students WHERE document=?",String.class,doc);}
 @Test void accessIsAdminOnlyAndYearIsValidated()throws Exception{
  mvc.perform(get("/api/demo/admin/data/courses")).andExpect(status().isUnauthorized());
  var student=(MockHttpSession)mvc.perform(post("/api/demo/login/student").with(csrf()).contentType("application/json").content("{\"document\":\"1000000001\",\"issued\":\"2020-01-15\",\"year\":\"2026\"}")).andReturn().getRequest().getSession(false);
  mvc.perform(get("/api/demo/admin/data/courses").session(student)).andExpect(status().isForbidden());mvc.perform(get("/api/demo/admin/data/courses?year=1900").session(admin())).andExpect(status().isBadRequest());
 }
 @Test void catalogAndMembershipFollowCreationActivationDeletionAndYear()throws Exception{
  var admin=admin();course("nuevo");student("55880001","nuevo",true);student("55880002","nuevo",false);String deleted=student("55880003","nuevo",true);db.update("UPDATE demo_students SET deleted=TRUE WHERE id=?",deleted);
  mvc.perform(get("/api/demo/admin/data/courses?year=2026").session(admin)).andExpect(jsonPath("$[?(@.course.codigo_curso=='nuevo')].students.length()").value(1)).andExpect(jsonPath("$[?(@.course.codigo_curso=='nuevo')].students[0].paymentStatus").value("SIN_COBROS"));
  mvc.perform(get("/api/demo/admin/data/courses?year=2025").session(admin)).andExpect(jsonPath("$[?(@.course.codigo_curso=='nuevo')].students.length()").value(0));
  db.update("UPDATE demo_students SET account_active=TRUE WHERE document='55880002'");mvc.perform(get("/api/demo/admin/data/courses").session(admin)).andExpect(jsonPath("$[?(@.course.codigo_curso=='nuevo')].students.length()").value(2));
  db.update("UPDATE demo_data_records SET deleted=TRUE WHERE module='cursos' AND record_key='nuevo'");mvc.perform(get("/api/demo/admin/data/courses").session(admin)).andExpect(jsonPath("$[?(@.course.codigo_curso=='nuevo')]").isEmpty());
 }
 @Test void enrollmentPaymentsAreScopedDeduplicatedAndPendingPaymentsDoNotSettleDebt()throws Exception{
  var admin=admin();course("primario");course("segundo");String id=student("55880004","primario",true);
  record("periodos","P",2025,Map.of("codigo_periodo","P","nombre","Semestre 1","anio","2025","fecha_inicio","2025-01-10","fecha_fin","2025-06-10"));
  for(String key:List.of("M1","M2"))record("matriculas",key,2025,Map.of("codigo_matricula",key,"tipo_documento","CC","documento","55880004","codigo_curso","segundo","codigo_periodo","P","estado_academico","EN_CURSO"));
  record("cobros","C1",2025,Map.of("codigo_cobro","C1","codigo_matricula","M1","valor_cop","500"));
  record("pagos","R1",2025,Map.of("codigo_cobro","C1","valor_cop","200","estado_pago","CONFIRMADO"));record("pagos","R2",2025,Map.of("codigo_cobro","C1","valor_cop","150","estado_pago","EN_REVISION"));
  db.update("INSERT INTO demo_movements(id,student_id,concept,type,amount,movement_date) VALUES ('portal-other',?,'Otro programa','COBRO',1000,'2026-02-01')",id);
  var response=json.readTree(mvc.perform(get("/api/demo/admin/data/courses?year=2025").session(admin)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());var target=java.util.stream.StreamSupport.stream(response.spliterator(),false).filter(c->c.get("course").get("codigo_curso").asText().equals("segundo")).findFirst().orElseThrow();assertEquals(1,target.get("students").size());var member=target.get("students").get(0);assertEquals(500,member.get("charged").asInt());assertEquals(200,member.get("paid").asInt());assertEquals(300,member.get("balance").asInt());assertEquals(150,member.get("review").asInt());assertEquals("EN_REVISION",member.get("paymentStatus").asText());assertEquals("2025",member.get("years").get(0).asText());assertEquals(1,target.get("periods").size());
  record("pagos","R3",2025,Map.of("codigo_cobro","C1","valor_cop","300","estado_pago","APROBADO"));mvc.perform(get("/api/demo/admin/data/courses?year=2025").session(admin)).andExpect(jsonPath("$[?(@.course.codigo_curso=='segundo')].students[0].paymentStatus").value("AL_DIA"));
 }
 @Test void portalPaymentsBelongOnlyToPrimaryProgramAndAcademicYear()throws Exception{
  var admin=admin();course("portal");String id=student("55880005","portal",true);
  db.update("INSERT INTO demo_movements(id,student_id,concept,type,amount,movement_date) VALUES ('bill',?,'Curso','COBRO',500,'2026-01-01'),('pay',?,'Pago','ABONO',500,'2026-02-01'),('old',?,'Anterior','COBRO',900,'2025-01-01')",id,id,id);
  mvc.perform(get("/api/demo/admin/data/courses?year=2026").session(admin)).andExpect(jsonPath("$[?(@.course.codigo_curso=='portal')].students[0].paymentStatus").value("AL_DIA")).andExpect(jsonPath("$[?(@.course.codigo_curso=='portal')].students[0].charged").value(500.0));
 }
 @Test void courseSubjectsAndDatesUseSharedValidatedEditor()throws Exception{
  var admin=admin();course("fechas");var values=new HashMap<>(data.list("cursos",null).stream().filter(r->"fechas".equals(r.get("codigo_curso"))).findFirst().orElseThrow());values.put("materias","Biología\nMatemáticas");values.put("fecha_inicio","2026-02-01");values.put("fecha_fin","2026-06-30");
  mvc.perform(post("/api/demo/admin/data/manual/cursos").session(admin).with(csrf()).contentType("application/json").content(json.writeValueAsString(Map.of("originalKey","fechas","values",values)))).andExpect(status().isOk());mvc.perform(get("/api/demo/admin/data/courses").session(admin)).andExpect(jsonPath("$[?(@.course.codigo_curso=='fechas')].course.materias").value("Biología\nMatemáticas")).andExpect(jsonPath("$[?(@.course.codigo_curso=='fechas')].course.fecha_fin").value("2026-06-30"));
  values.put("fecha_fin","2026-01-01");mvc.perform(post("/api/demo/admin/data/manual/cursos").session(admin).with(csrf()).contentType("application/json").content(json.writeValueAsString(Map.of("originalKey","fechas","values",values)))).andExpect(status().isBadRequest());
 }
}
