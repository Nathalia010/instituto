package co.educontrol.demo;

import java.util.*;
import java.io.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.CellType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:bulk_data;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"})
@AutoConfigureMockMvc @ActiveProfiles("demo")
class BulkDataTest {
 @Autowired MockMvc mvc;
 @Autowired ObjectMapper json;
 @Autowired BulkDataService service;
 MockHttpSession admin() throws Exception {
  return (MockHttpSession)mvc.perform(post("/api/demo/login/admin").with(csrf()).contentType("application/json").content("{\"username\":\"adminN\",\"password\":\"1234987A\"}")).andExpect(status().isOk()).andReturn().getRequest().getSession(false);
 }
 BulkDataService.PreviewRequest request(String module,List<List<String>> rows) {
  var mapping=new LinkedHashMap<String,Integer>();var fields=DataCatalog.get(module).fields();for(int i=0;i<fields.size();i++)mapping.put(fields.get(i),i);
  return new BulkDataService.PreviewRequest(module,"prueba.csv",rows,mapping,0,5);
 }
 String preview(MockHttpSession session,String module,List<List<String>> rows,int valid,int invalid) throws Exception {
  var result=mvc.perform(post("/api/demo/admin/data/preview").session(session).with(csrf()).contentType("application/json").content(json.writeValueAsString(request(module,rows)))).andExpect(status().isOk()).andExpect(jsonPath("$.valid").value(valid)).andExpect(jsonPath("$.invalid").value(invalid)).andReturn();
  return json.readTree(result.getResponse().getContentAsString()).get("id").asText();
 }
 void save(MockHttpSession session,String module,List<String> row) throws Exception {
  var id=preview(session,module,List.of(row),1,0);
  mvc.perform(post("/api/demo/admin/data/confirm/"+id).session(session).with(csrf())).andExpect(status().isOk()).andExpect(jsonPath("$.saved").value(1));
 }
 @Test void previewRequiresConfirmationAndRejectsDuplicatesAndOtherSessions() throws Exception {
  var session=admin();String code="PREVIEW-ONLY";
  var rows=List.of(List.of(code,"Curso de prueba","CURSO","40"),List.of("INVALID","Curso inválido","CURSO","-1"));
  String id=preview(session,"cursos",rows,1,1);
  assertTrue(service.list("cursos",null).stream().noneMatch(r->code.equals(r.get("codigo_curso"))));
  mvc.perform(post("/api/demo/admin/data/confirm/"+id).session(admin()).with(csrf())).andExpect(status().isNotFound());
  mvc.perform(post("/api/demo/admin/data/confirm/"+id).session(session)).andExpect(status().isForbidden());
  mvc.perform(post("/api/demo/admin/data/confirm/"+id).session(session).with(csrf())).andExpect(status().isOk()).andExpect(jsonPath("$.saved").value(1)).andExpect(jsonPath("$.skipped").value(1));
  mvc.perform(post("/api/demo/admin/data/confirm/"+id).session(session).with(csrf())).andExpect(status().isConflict());
  preview(session,"cursos",List.of(rows.getFirst()),0,1);
  assertEquals("Curso de prueba",service.list("cursos",null).stream().filter(r->code.equals(r.get("codigo_curso"))).findFirst().orElseThrow().get("nombre"));
 }
 @Test void annualRelationshipsAndWorkbookExportsPreserveTypes() throws Exception {
  var session=admin();
  save(session,"cursos",List.of("EXPORT-COURSE","Prueba anual","CURSO","80"));
  save(session,"docentes",List.of("CC","00003456","Docente prueba","docente@example.invalid","3000000000"));
  for(int year:List.of(2025,2026)) {
   save(session,"periodos",List.of("P"+year,""+year,"Periodo "+year,year+"-01-01",year+"-12-31"));
   save(session,"matriculas",List.of("M"+year,"CC","1000000001","EXPORT-COURSE","P"+year,"A",year+"-02-01","EN_CURSO"));
   save(session,"cobros",List.of("C"+year,"M"+year,"MATRICULA","150000.50",year+"-03-01"));
   save(session,"pagos",List.of("AB"+year,"C"+year,year+"-02-15","100000","TRANSFERENCIA","EN_REVISION"));
  }
  save(session,"notas",List.of("M2025","E1","4.5","CC","00003456"));
  preview(session,"notas",List.of(List.of("M2025","E2","6","CC","00003456")),0,1);
  assertEquals(1,service.list("pagos",2025).size());
  assertEquals("2025",service.list("pagos",2025).getFirst().get("anio"));
  assertEquals(2,service.list("matriculas",null).size());
  var bytes=mvc.perform(get("/api/demo/admin/data/export/all").param("year","2025").session(session)).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
  try(var book=new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
   assertEquals(10,book.getNumberOfSheets());
   assertEquals(0,book.getSheet("Notas del portal").getLastRowNum());
   assertEquals(1,book.getSheet("Matrículas").getLastRowNum());
   assertEquals("M2025",book.getSheet("Matrículas").getRow(1).getCell(0).getStringCellValue());
   assertEquals("00003456",book.getSheet("Docentes").getRow(1).getCell(1).getStringCellValue());
   assertEquals(CellType.NUMERIC,book.getSheet("Cobros").getRow(1).getCell(3).getCellType());
   assertEquals(150000.50,book.getSheet("Cobros").getRow(1).getCell(3).getNumericCellValue());
   assertEquals(CellType.NUMERIC,book.getSheet("Cobros").getRow(1).getCell(4).getCellType());
  }
  assertEquals("\"'@peligro\"",BulkDataController.safeCsv("@peligro"));
 }
 @Test void csvAndExcelUploadsHandleDatesNumbersAndRejectFormulas() throws Exception {
  var session=admin();
  var file=new MockMultipartFile("file","prueba.csv","text/csv","\uFEFFcodigo;nombre\r\n001;\"Curso; especial\"\r\n002;\"Texto\ncontinuado\"".getBytes(java.nio.charset.StandardCharsets.UTF_8));
  mvc.perform(multipart("/api/demo/admin/data/file").file(file).session(session).with(csrf())).andExpect(status().isOk()).andExpect(jsonPath("$.rows[0][0]").value("001")).andExpect(jsonPath("$.rows[0][1]").value("Curso; especial")).andExpect(jsonPath("$.rows[1][1]").value("Texto\ncontinuado"));
  try(var book=new XSSFWorkbook();var out=new ByteArrayOutputStream()) {
   var sheet=book.createSheet("Datos");var header=sheet.createRow(0);header.createCell(0).setCellValue("valor");header.createCell(1).setCellValue("fecha");
   var row=sheet.createRow(1);var number=row.createCell(0);number.setCellValue(150000.50);var numberStyle=book.createCellStyle();numberStyle.setDataFormat(book.createDataFormat().getFormat("#,##0.00"));number.setCellStyle(numberStyle);
   var date=row.createCell(1);date.setCellValue(java.time.LocalDate.of(2026,2,1));var dateStyle=book.createCellStyle();dateStyle.setDataFormat(book.createDataFormat().getFormat("dd/mm/yyyy"));date.setCellStyle(dateStyle);book.write(out);
   mvc.perform(multipart("/api/demo/admin/data/file").file(new MockMultipartFile("file","prueba.xlsx","application/octet-stream",out.toByteArray())).session(session).with(csrf())).andExpect(status().isOk()).andExpect(jsonPath("$.rows[0][0]").value("150000.5")).andExpect(jsonPath("$.rows[0][1]").value("2026-02-01"));
   row.getCell(0).setCellFormula("1+1");out.reset();book.write(out);
   mvc.perform(multipart("/api/demo/admin/data/file").file(new MockMultipartFile("file","prueba.xlsx","application/octet-stream",out.toByteArray())).session(session).with(csrf())).andExpect(status().isBadRequest());
  }
 }
 @Test void studentCannotAccessImportsOrExports() throws Exception {
  var result=mvc.perform(post("/api/demo/login/student").with(csrf()).contentType("application/json").content("{\"document\":\"1000000001\",\"issued\":\"2020-01-15\",\"year\":\"2026\"}")).andExpect(status().isOk()).andReturn();
  var student=(MockHttpSession)result.getRequest().getSession(false);
  mvc.perform(get("/api/demo/admin/data/export/all").session(student)).andExpect(status().isForbidden());
  mvc.perform(post("/api/demo/admin/data/preview").session(student).with(csrf()).contentType("application/json").content(json.writeValueAsString(request("cursos",List.of())))).andExpect(status().isForbidden());
 }
 @Test void concurrentPreviewsRevalidateAndBlankRowsKeepTheirNumbers() throws Exception {
  var session=admin();var row=List.of("REVALIDATE","Prueba","CURSO","10");
  var id=preview(session,"cursos",List.of(List.of("","","",""),row,row),1,1);
  var second=preview(session,"cursos",List.of(row),1,0);
  mvc.perform(post("/api/demo/admin/data/confirm/"+second).session(session).with(csrf())).andExpect(status().isOk());
  mvc.perform(post("/api/demo/admin/data/confirm/"+id).session(session).with(csrf())).andExpect(status().isConflict());
  var checked=service.check("cursos",List.of(Map.of("__skip","true"),Map.of("codigo_curso","ROW3","nombre","Prueba","tipo_programa","CURSO","duracion_horas","10")),0,5);
  assertEquals(3,checked.getFirst().row());
 }
}

