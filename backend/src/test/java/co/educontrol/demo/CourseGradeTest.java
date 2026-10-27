package co.educontrol.demo;

import java.io.ByteArrayInputStream;
import java.util.*;
import com.fasterxml.jackson.databind.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.CellType;
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

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:course_grades;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"}) @AutoConfigureMockMvc @ActiveProfiles("demo") @Transactional
class CourseGradeTest {
 @Test void averageIncludesZeroExcludesMissingAndRoundsToTwoDecimals(){
  var values=new HashMap<String,java.math.BigDecimal>();values.put("Pendiente",null);assertNull(CourseGradeController.average(values));values.put("Cero",java.math.BigDecimal.ZERO);values.put("Otra",new java.math.BigDecimal("4.25"));assertEquals(new java.math.BigDecimal("2.13"),CourseGradeController.average(values));
 }
 @Autowired MockMvc mvc;@Autowired JdbcTemplate db;@Autowired ObjectMapper json;@Autowired BulkDataService data;
 static final String PATH="/api/demo/admin/data/courses/validacion/grades";
 MockHttpSession admin()throws Exception{return (MockHttpSession)mvc.perform(post("/api/demo/login/admin").with(csrf()).contentType("application/json").content("{\"username\":\"adminN\",\"password\":\"1234987A\"}")).andExpect(status().isOk()).andReturn().getRequest().getSession(false);}
 MockHttpSession student()throws Exception{return (MockHttpSession)mvc.perform(post("/api/demo/login/student").with(csrf()).contentType("application/json").content("{\"document\":\"1000000001\",\"issued\":\"2020-01-15\",\"year\":\"2026\"}")).andExpect(status().isOk()).andReturn().getRequest().getSession(false);}
 void configure(String subjects,String passing){var course=new HashMap<>(data.list("cursos",null).stream().filter(c->c.get("codigo_curso").equals("validacion")).findFirst().orElseThrow());course.put("materias",subjects);course.put("nota_aprobacion",passing);data.saveRecord("cursos","validacion",course,"adminN");}
 JsonNode book(MockHttpSession admin,int year)throws Exception{return json.readTree(mvc.perform(get(PATH+"?year="+year).session(admin)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());}
 String input(JsonNode row,Map<String,?> grades)throws Exception{return json.writeValueAsString(Map.of("studentId",row.get("studentId").asText(),"year",row.get("year").asInt(),"version",row.get("version").asText(),"grades",grades));}
 JsonNode save(MockHttpSession admin,JsonNode row,Map<String,?> grades)throws Exception{return json.readTree(mvc.perform(post(PATH).session(admin).with(csrf()).contentType("application/json").content(input(row,grades))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());}
 @Test void gradesAndExportsRequireAdminAndWritesRequireCsrf()throws Exception{
  var admin=admin();var student=student();configure("Matemáticas\nBiología","3");var row=book(admin,2026).get("rows").get(0);String request=input(row,Map.of("Matemáticas",4,"Biología",3));
  mvc.perform(get(PATH)).andExpect(status().isUnauthorized());mvc.perform(get(PATH).session(student)).andExpect(status().isForbidden());mvc.perform(get("/api/demo/admin/data/courses/export").session(student)).andExpect(status().isForbidden());
  mvc.perform(post(PATH).session(admin).contentType("application/json").content(request)).andExpect(status().isForbidden());mvc.perform(post(PATH).session(student).with(csrf()).contentType("application/json").content(request)).andExpect(status().isForbidden());
  mvc.perform(post(PATH).session(admin).with(csrf()).contentType("application/json").content(input(row,Map.of("Matemáticas",4,"Biología",5.01)))).andExpect(status().isBadRequest());assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM demo_course_grades",Integer.class));
  db.update("UPDATE demo_students SET account_active=FALSE WHERE id='prueba-estudiante'");mvc.perform(post(PATH).session(admin).with(csrf()).contentType("application/json").content(request)).andExpect(status().isBadRequest());
 }
 @Test void saveEditClearAndStaleWritesUseRealNotesAndCalculatedResults()throws Exception{
  var admin=admin();configure("Matemáticas\nBiología","3");var original=book(admin,2026).get("rows").get(0);assertEquals(4.2,original.get("grades").get("Matemáticas").asDouble());assertTrue(original.get("grades").get("Biología").isNull());
  var failed=save(admin,original,Map.of("Matemáticas",4,"Biología",2));assertEquals("NO_APRUEBA",failed.get("status").asText());assertEquals(3,failed.get("average").asDouble());mvc.perform(post(PATH).session(admin).with(csrf()).contentType("application/json").content(input(original,Map.of("Matemáticas",5,"Biología",5)))).andExpect(status().isConflict());
  var passed=save(admin,failed,Map.of("Matemáticas",3,"Biología",3));assertEquals("APRUEBA",passed.get("status").asText());var clear=new HashMap<String,Object>();clear.put("Matemáticas",null);clear.put("Biología",3);var pending=save(admin,passed,clear);assertEquals("PENDIENTE",pending.get("status").asText());assertTrue(book(admin,2026).get("rows").get(0).get("grades").get("Matemáticas").isNull());assertEquals(3,db.queryForObject("SELECT COUNT(*) FROM demo_audit WHERE action='EDITAR_NOTAS_CURSO'",Integer.class));
 }
 @Test void subjectsAddColumnsAndRemovingThemPreservesNotesAndThresholdChangesResult()throws Exception{
  var admin=admin();configure("Matemáticas\nBiología","3");save(admin,book(admin,2026).get("rows").get(0),Map.of("Matemáticas",3,"Biología",3));configure("Matemáticas\nBiología\nQuímica","4");var added=book(admin,2026);assertEquals(3,added.get("subjects").size());assertTrue(added.get("rows").get(0).get("grades").get("Química").isNull());assertEquals("NO_APRUEBA",added.get("rows").get(0).get("status").asText());
  configure("Matemáticas","3");configure("Matemáticas\nBiología","3");assertEquals(3,book(admin,2026).get("rows").get(0).get("grades").get("Biología").asInt());assertThrows(org.springframework.web.server.ResponseStatusException.class,()->configure("Biología\nbiología","3"));
 }
 @Test void gradesRemainSeparateByYearAndAreVisibleInStudentPortal()throws Exception{
  var admin=admin();configure("Matemáticas","3");save(admin,book(admin,2026).get("rows").get(0),Map.of("Matemáticas",3.5));
  db.update("INSERT INTO demo_data_records(module,record_key,academic_year,payload) VALUES ('matriculas','M2027',2027,?)",data.encode(Map.of("codigo_matricula","M2027","tipo_documento","CC","documento","1000000001","codigo_curso","validacion","codigo_periodo","P2027")));
  save(admin,book(admin,2027).get("rows").get(0),Map.of("Matemáticas",1));assertEquals(3.5,book(admin,2026).get("rows").get(0).get("grades").get("Matemáticas").asDouble());assertEquals(2,bookAll(admin).get("rows").size());
  mvc.perform(get("/api/demo/student/me").session(student())).andExpect(jsonPath("$.notes[?(@.year==2026 && @.subject=='Matemáticas')].grade").value(3.5)).andExpect(jsonPath("$.notes[?(@.year==2027 && @.subject=='Matemáticas')].grade").value(1.0));
 }
 JsonNode bookAll(MockHttpSession admin)throws Exception{return json.readTree(mvc.perform(get(PATH).session(admin)).andReturn().getResponse().getContentAsString());}
 @Test void exportsOneCourseOrWholeWorkbookWithNumericGradesAndNoFinancialColumns()throws Exception{
  var admin=admin();configure("Matemáticas\nBiología","3");save(admin,book(admin,2026).get("rows").get(0),Map.of("Matemáticas",4,"Biología",2));
  byte[] bytes=mvc.perform(get("/api/demo/admin/data/courses/export?course=validacion&year=2026").session(admin)).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store")).andReturn().getResponse().getContentAsByteArray();
  try(var excel=new XSSFWorkbook(new ByteArrayInputStream(bytes))){assertEquals(2,excel.getNumberOfSheets());var sheet=excel.getSheetAt(1);assertEquals("Matemáticas",sheet.getRow(0).getCell(6).getStringCellValue());assertEquals(CellType.NUMERIC,sheet.getRow(1).getCell(6).getCellType());assertEquals(4,sheet.getRow(1).getCell(6).getNumericCellValue());assertEquals("No aprueba",sheet.getRow(1).getCell(9).getStringCellValue());for(var cell:sheet.getRow(0))assertFalse(Set.of("Cobros","Pagado","Saldo","Estado de pago").contains(cell.getStringCellValue()));}
  byte[] all=mvc.perform(get("/api/demo/admin/data/courses/export").session(admin)).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();try(var excel=new XSSFWorkbook(new ByteArrayInputStream(all))){assertEquals(data.list("cursos",null).size()+1,excel.getNumberOfSheets());}
  byte[] filtered=mvc.perform(get("/api/demo/admin/data/courses/export?course=validacion&year=2025").session(admin)).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();try(var excel=new XSSFWorkbook(new ByteArrayInputStream(filtered))){assertEquals(0,excel.getSheetAt(1).getLastRowNum());}
  mvc.perform(get("/api/demo/admin/data/courses/export?course=missing").session(admin)).andExpect(status().isNotFound());
 }
}
