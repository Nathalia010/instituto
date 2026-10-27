package co.educontrol.demo;
import java.time.*;
import java.util.*;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;
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

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:leveling_courses;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"}) @AutoConfigureMockMvc @ActiveProfiles("demo") @Transactional
class LevelingCourseTest {
 @Autowired MockMvc mvc;@Autowired JdbcTemplate db;@Autowired ObjectMapper json;@Autowired BulkDataService data;
 String date=LocalDate.now(ZoneId.of("America/Bogota")).plusDays(20).toString();
 @BeforeEach void configure(){
  var primary=new HashMap<>(data.list("cursos",null).stream().filter(c->c.get("codigo_curso").equals("validacion")).findFirst().orElseThrow());primary.put("materias","Biología\nMatemáticas");data.saveRecord("cursos","validacion",primary,"adminN");
  for(String code:List.of("extra","ajeno"))data.saveRecord("cursos",null,Map.of("codigo_curso",code,"nombre","Curso "+code,"tipo_programa","CURSO","materias","Biología\nFísica"),"adminN");
  data.saveRecord("periodos",null,Map.of("codigo_periodo","P2026","nombre","Semestre","anio","2026","fecha_inicio","2026-01-01","fecha_fin","2026-12-31"),"adminN");
  data.saveRecord("matriculas",null,Map.of("codigo_matricula","MEXTRA","tipo_documento","CC","documento","1000000001","codigo_curso","extra","codigo_periodo","P2026","grupo","A","fecha_matricula","2026-01-10","estado_academico","EN_CURSO"),"adminN");
 }
 MockHttpSession admin()throws Exception{return (MockHttpSession)mvc.perform(post("/api/demo/login/admin").with(csrf()).contentType("application/json").content("{\"username\":\"adminN\",\"password\":\"1234987A\"}")).andReturn().getRequest().getSession(false);}
 MockHttpSession student()throws Exception{return (MockHttpSession)mvc.perform(post("/api/demo/login/student").with(csrf()).contentType("application/json").content("{\"document\":\"1000000001\",\"issued\":\"2020-01-15\",\"year\":\"2026\"}")).andExpect(status().isOk()).andReturn().getRequest().getSession(false);}
 Map<String,Object> request(String course,String subject){return new HashMap<>(Map.of("courseCode",course,"subject",subject,"period","SEGUNDO_SEMESTRE","year",2026,"reason","Actividad pendiente","preferredDate",date,"preferredTime","10:00"));}
 String create(MockHttpSession student,String course,String subject)throws Exception{return json.readTree(mvc.perform(post("/api/demo/student/levelings").session(student).with(csrf()).contentType("application/json").content(json.writeValueAsString(request(course,subject)))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asText();}
 @Test void studentSeesSeveralOwnCoursesAndCanRequestTheSameSubjectInEach()throws Exception{
  var student=student();mvc.perform(get("/api/demo/student/courses").session(student)).andExpect(jsonPath("$.length()").value(2)).andExpect(jsonPath("$[?(@.code=='extra')].subjects[1]").value("Física")).andExpect(jsonPath("$[?(@.code=='ajeno')]").isEmpty());
  create(student,"validacion","Biología");create(student,"extra","biologia");
  mvc.perform(post("/api/demo/student/levelings").session(student).with(csrf()).contentType("application/json").content(json.writeValueAsString(request("extra","Biología")))).andExpect(status().isConflict());
  mvc.perform(get("/api/demo/admin/levelings").session(admin())).andExpect(jsonPath("$[?(@.course_code=='extra')].student_name").value("Estudiante de prueba")).andExpect(jsonPath("$[?(@.course_code=='extra')].subject_name").value("Biología")).andExpect(jsonPath("$[?(@.course_code=='extra')].course_name").value("Curso extra"));
  mvc.perform(get("/api/demo/student/courses")).andExpect(status().isUnauthorized());mvc.perform(get("/api/demo/student/courses").session(admin())).andExpect(status().isForbidden());
 }
 @Test void courseYearSubjectAndEnrollmentCannotBeForged()throws Exception{
  var student=student();mvc.perform(post("/api/demo/student/levelings").session(student).with(csrf()).contentType("application/json").content(json.writeValueAsString(request("ajeno","Biología")))).andExpect(status().isForbidden());
  var year=request("extra","Biología");year.put("year",2025);mvc.perform(post("/api/demo/student/levelings").session(student).with(csrf()).contentType("application/json").content(json.writeValueAsString(year))).andExpect(status().isForbidden());
  mvc.perform(post("/api/demo/student/levelings").session(student).with(csrf()).contentType("application/json").content(json.writeValueAsString(request("extra","Matemáticas")))).andExpect(status().isBadRequest());
  var missing=request("extra","Biología");missing.remove("courseCode");mvc.perform(post("/api/demo/student/levelings").session(student).with(csrf()).contentType("application/json").content(json.writeValueAsString(missing))).andExpect(status().isBadRequest());
  var row=data.decode(db.queryForObject("SELECT payload FROM demo_data_records WHERE module='matriculas' AND record_key='MEXTRA'",String.class));row.put("estado_academico","RETIRADO");db.update("UPDATE demo_data_records SET payload=? WHERE module='matriculas' AND record_key='MEXTRA'",data.encode(row));
  db.update("UPDATE demo_students SET program='extra' WHERE id='prueba-estudiante'");mvc.perform(post("/api/demo/student/levelings").session(student).with(csrf()).contentType("application/json").content(json.writeValueAsString(request("extra","Biología")))).andExpect(status().isForbidden());assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM demo_levelings",Integer.class));
 }
 @Test void bookedDatesValidateEnrollmentAndSupportCustomCourseSubjects()throws Exception{
  var admin=admin();var student=student();var slot=Map.of("date",date,"time","10:00","subject","Física","teacher","Docente prueba","location","Virtual","capacity",2,"active",true);
  String id=json.readTree(mvc.perform(post("/api/demo/admin/leveling-slots").session(admin).with(csrf()).contentType("application/json").content(json.writeValueAsString(slot))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("id").asText();
  var request=request("ajeno","Física");request.put("slotId",id);mvc.perform(post("/api/demo/student/levelings/book").session(student).with(csrf()).contentType("application/json").content(json.writeValueAsString(request))).andExpect(status().isForbidden());
  request.put("courseCode","validacion");mvc.perform(post("/api/demo/student/levelings/book").session(student).with(csrf()).contentType("application/json").content(json.writeValueAsString(request))).andExpect(status().isBadRequest());
  request.put("courseCode","extra");mvc.perform(post("/api/demo/student/levelings/book").session(student).with(csrf()).contentType("application/json").content(json.writeValueAsString(request))).andExpect(status().isCreated());assertEquals("extra",db.queryForObject("SELECT course_code FROM demo_levelings",String.class));
 }
 @Test void courseIndicatorIsScopedToCourseAndYearAndIgnoresDeletedRequests()throws Exception{
  var student=student();String id=create(student,"extra","Biología");var admin=admin();
  mvc.perform(get("/api/demo/admin/data/courses/extra/grades?year=2026").session(admin)).andExpect(jsonPath("$.rows[0].levelings.length()").value(1)).andExpect(jsonPath("$.rows[0].levelings[0].subject").value("Biología"));
  mvc.perform(get("/api/demo/admin/data/courses/validacion/grades?year=2026").session(admin)).andExpect(jsonPath("$.rows[0].levelings.length()").value(0));mvc.perform(get("/api/demo/admin/data/courses/extra/grades?year=2025").session(admin)).andExpect(jsonPath("$.rows.length()").value(0));
  mvc.perform(post("/api/demo/admin/levelings/"+id+"/delete").session(admin).with(csrf()).content("{}").contentType("application/json")).andExpect(status().isOk());mvc.perform(get("/api/demo/admin/data/courses/extra/grades").session(admin)).andExpect(jsonPath("$.rows[0].levelings.length()").value(0));
  mvc.perform(post("/api/demo/admin/levelings/"+id+"/restore").session(admin).with(csrf()).content("{}").contentType("application/json")).andExpect(status().isOk());mvc.perform(get("/api/demo/admin/data/courses/extra/grades").session(admin)).andExpect(jsonPath("$.rows[0].levelings.length()").value(1));
 }
 @Test void legacyRequestsRemainUnassignedUntilAdminChoosesAnEnrolledCourse()throws Exception{
  db.update("INSERT INTO demo_levelings(id,student_id,subject,period,academic_year,reason,preferred_date,preferred_time) VALUES ('legacy','prueba-estudiante','biologia','SEGUNDO_SEMESTRE',2026,'Anterior',?,'10:00')",date);
  var admin=admin();mvc.perform(get("/api/demo/admin/levelings").session(admin)).andExpect(jsonPath("$[0].course_code").value(""));mvc.perform(get("/api/demo/admin/data/courses/extra/grades").session(admin)).andExpect(jsonPath("$.rows[0].unassignedLevelings").value(1));
  var update=Map.of("status","PENDIENTE","courseCode","ajeno");mvc.perform(post("/api/demo/admin/levelings/legacy").session(admin).with(csrf()).contentType("application/json").content(json.writeValueAsString(update))).andExpect(status().isForbidden());
  mvc.perform(post("/api/demo/admin/levelings/legacy").session(admin).with(csrf()).contentType("application/json").content(json.writeValueAsString(Map.of("status","PENDIENTE","courseCode","extra")))).andExpect(status().isOk());assertEquals("extra",db.queryForObject("SELECT course_code FROM demo_levelings WHERE id='legacy'",String.class));
 }
}
