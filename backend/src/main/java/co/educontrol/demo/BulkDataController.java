package co.educontrol.demo;

import java.util.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController @Profile("demo") @RequestMapping("/api/demo/admin/data")
public class BulkDataController {
 private final BulkDataService service;
 public BulkDataController(BulkDataService service){this.service=service;ZipSecureFile.setMaxEntrySize(10_000_000);ZipSecureFile.setMaxTextSize(10_000_000);}
 private String owner(HttpServletRequest req){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(req.getSession().getId().getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
 private void validateYear(Integer year){if(year!=null&&(year<2000||year>2200))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Año inválido");}
 @GetMapping("/catalog") public Map<String,Object> catalog(){return Map.of("modules",DataCatalog.ALL,"years",service.years());}
 @GetMapping("/template/{module}") public ResponseEntity<byte[]> template(@PathVariable String module){
  var definition=DataCatalog.get(module);byte[] csv=("\uFEFF"+String.join(";",definition.fields())+"\r\n").getBytes(StandardCharsets.UTF_8);
  return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\"plantilla-"+module+".csv\"").header(HttpHeaders.CACHE_CONTROL,"no-store").contentType(MediaType.parseMediaType("text/csv;charset=UTF-8")).body(csv);
 }
 @GetMapping("/records/{module}") public List<Map<String,String>> records(@PathVariable String module,@RequestParam(required=false) Integer year){validateYear(year);return service.list(module,year);}
 @PostMapping("/preview") public Map<String,Object> preview(@RequestBody BulkDataService.PreviewRequest input,HttpServletRequest req){return service.preview(input,owner(req));}
 @PostMapping("/confirm/{id}") public Map<String,Object> confirm(@PathVariable String id,HttpServletRequest req){return service.confirm(id,owner(req));}
 @PostMapping(value="/file",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
 public Map<String,Object> file(@RequestParam MultipartFile file,@RequestParam(required=false) String sheet) {
  if(file.isEmpty()||file.getSize()>5_000_000)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Archivo vacío o mayor de 5 MB");
  String name=Objects.toString(file.getOriginalFilename(),"").toLowerCase(Locale.ROOT);List<List<String>> rows;List<String> sheets=new ArrayList<>();String selected="";
  try {
   if(name.endsWith(".csv")){rows=parseCsv(new String(file.getBytes(),StandardCharsets.UTF_8));}
   else if(name.endsWith(".xlsx")){
    try(var workbook=new XSSFWorkbook(file.getInputStream())) {
     if(workbook.getAllEmbeddedParts().size()>0||workbook.getPackage().getParts().stream().anyMatch(p->p.getPartName().getName().toLowerCase(Locale.ROOT).contains("vbaproject")))throw new IllegalArgumentException("No se permiten macros o archivos incrustados");
     for(int i=0;i<workbook.getNumberOfSheets();i++)sheets.add(workbook.getSheetName(i));
     var selectedSheet=sheet==null?workbook.getSheetAt(0):workbook.getSheet(sheet);if(selectedSheet==null)throw new IllegalArgumentException("Hoja no encontrada");selected=selectedSheet.getSheetName();
     if(selectedSheet.getLastRowNum()>2000)throw new IllegalArgumentException("Máximo 2000 filas");
     var header=selectedSheet.getRow(0);if(header==null||header.getLastCellNum()>100||header.getLastCellNum()<1)throw new IllegalArgumentException("Encabezados inválidos");int width=header.getLastCellNum();rows=new ArrayList<>();var formatter=new DataFormatter(Locale.ROOT);
     for(int n=0;n<=selectedSheet.getLastRowNum();n++){var row=selectedSheet.getRow(n);var cells=new ArrayList<String>();for(int c=0;c<width;c++){var cell=row==null?null:row.getCell(c);if(cell==null)cells.add("");else if(cell.getCellType()==CellType.FORMULA)throw new IllegalArgumentException("No se admiten fórmulas");else if(cell.getCellType()==CellType.NUMERIC&&DateUtil.isCellDateFormatted(cell))cells.add(cell.getLocalDateTimeCellValue().toLocalDate().toString());else if(cell.getCellType()==CellType.NUMERIC)cells.add(java.math.BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros().toPlainString());else cells.add(formatter.formatCellValue(cell));}rows.add(cells);}
    }
   } else throw new IllegalArgumentException("Usa XLSX o CSV");
   if(rows.isEmpty()||rows.getFirst().size()>100)throw new IllegalArgumentException("Archivo sin encabezados");
   var headers=rows.removeFirst();if(headers.stream().allMatch(String::isBlank))throw new IllegalArgumentException("Archivo sin encabezados");
   return Map.of("headers",headers,"rows",rows,"sheets",sheets,"sheet",selected);
  } catch(Exception e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"No se pudo leer el archivo: "+e.getMessage());}
 }
 static List<List<String>> parseCsv(String raw){
  String text=raw.startsWith("\uFEFF")?raw.substring(1):raw;String header=text.lines().findFirst().orElse("");char delimiter=header.contains(";")?';':header.contains("\t")?'\t':',';
  List<List<String>> rows=new ArrayList<>();List<String> row=new ArrayList<>();StringBuilder field=new StringBuilder();boolean quoted=false;
  for(int i=0;i<text.length();i++){char ch=text.charAt(i);if(ch=='"'){if(quoted&&i+1<text.length()&&text.charAt(i+1)=='"'){field.append('"');i++;}else if(quoted||field.isEmpty())quoted=!quoted;else throw new IllegalArgumentException("Comillas CSV inválidas");}
   else if(!quoted&&(ch==delimiter||ch=='\n'||ch=='\r')){row.add(field.toString());field.setLength(0);if(ch!=delimiter){rows.add(row);row=new ArrayList<>();if(ch=='\r'&&i+1<text.length()&&text.charAt(i+1)=='\n')i++;if(rows.size()>2001)throw new IllegalArgumentException("Máximo 2000 filas");}}else field.append(ch);}
  if(quoted)throw new IllegalArgumentException("Comillas CSV sin cerrar");if(!field.isEmpty()||!row.isEmpty()){row.add(field.toString());rows.add(row);}if(rows.size()>2001)throw new IllegalArgumentException("Máximo 2000 filas");
  if(!rows.isEmpty()){int width=rows.getFirst().size();for(var cells:rows){if(cells.size()>width)throw new IllegalArgumentException("Fila con más columnas que el encabezado");while(cells.size()<width)cells.add("");}}return rows;
 }
 static String safeCsv(String value){String safe=value;if(!value.stripLeading().isEmpty()&&"=+-@".indexOf(value.stripLeading().charAt(0))>=0)safe="'"+value;return "\""+safe.replace("\"","\"\"")+"\"";}
 @GetMapping("/export/{module}") public ResponseEntity<byte[]> export(@PathVariable String module,@RequestParam(required=false) Integer year,@RequestParam(defaultValue="xlsx") String format)throws IOException {
  validateYear(year);var definitions=module.equals("all")?DataCatalog.ALL:List.of(DataCatalog.get(module));
  byte[] output;String extension;if(format.equals("csv")){if(definitions.size()!=1)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"El informe completo usa Excel");var d=definitions.getFirst();var fields=new ArrayList<>(d.fields());if(d.annual()&&!fields.contains("anio"))fields.add("anio");StringBuilder csv=new StringBuilder("\uFEFF");csv.append(String.join(";",fields)).append("\r\n");for(var r:service.list(d.id(),year)){csv.append(fields.stream().map(f->safeCsv(r.getOrDefault(f,""))).collect(java.util.stream.Collectors.joining(";"))).append("\r\n");}output=csv.toString().getBytes(StandardCharsets.UTF_8);extension="csv";}
  else if(format.equals("xlsx")){try(var book=new XSSFWorkbook();var bytes=new ByteArrayOutputStream()){
   var headerStyle=book.createCellStyle();headerStyle.setFillForegroundColor(IndexedColors.TEAL.getIndex());headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);var font=book.createFont();font.setBold(true);font.setColor(IndexedColors.WHITE.getIndex());headerStyle.setFont(font);
   var textStyle=book.createCellStyle();textStyle.setDataFormat(book.createDataFormat().getFormat("@"));var numberStyle=book.createCellStyle();numberStyle.setDataFormat(book.createDataFormat().getFormat("0.00"));var dateStyle=book.createCellStyle();dateStyle.setDataFormat(book.createDataFormat().getFormat("yyyy-mm-dd"));
   for(var d:definitions){var worksheet=book.createSheet(d.label());var fields=new ArrayList<>(d.fields());if(d.annual()&&!fields.contains("anio"))fields.add("anio");var header=worksheet.createRow(0);for(int c=0;c<fields.size();c++){var cell=header.createCell(c);cell.setCellValue(fields.get(c));cell.setCellStyle(headerStyle);worksheet.setColumnWidth(c,fields.get(c).contains("nombre")||fields.get(c).contains("notas")?8000:6000);}header.setHeightInPoints(28);worksheet.createFreezePane(0,1);int n=1;
    for(var data:service.list(d.id(),year)){var row=worksheet.createRow(n++);for(int c=0;c<fields.size();c++){String f=fields.get(c),v=data.getOrDefault(f,"");var cell=row.createCell(c);cell.setCellStyle(textStyle);if(!v.isBlank()&&Set.of("anio","duracion_horas","nota","valor_cop").contains(f)){try{cell.setCellValue(Double.parseDouble(v));if(f.equals("valor_cop")||f.equals("nota"))cell.setCellStyle(numberStyle);}catch(Exception e){cell.setCellValue(v);}}else if(!v.isBlank()&&f.startsWith("fecha_")){try{cell.setCellValue(LocalDate.parse(v).atStartOfDay());cell.setCellStyle(dateStyle);}catch(Exception e){cell.setCellValue(v);}}else cell.setCellValue(v);}}
   }if(module.equals("all")){for(boolean notes:List.of(true,false)){var history=service.portalHistory(notes,year);var sheet=book.createSheet(notes?"Notas del portal":"Movimientos del portal");var fields=notes?List.of("documento","nombre_completo","codigo_registro","asignatura","nota","anio"):List.of("documento","nombre_completo","codigo_registro","concepto","tipo","valor_cop","fecha_movimiento","anio");var head=sheet.createRow(0);for(int c=0;c<fields.size();c++){var cell=head.createCell(c);cell.setCellValue(fields.get(c));cell.setCellStyle(headerStyle);sheet.setColumnWidth(c,7000);}sheet.createFreezePane(0,1);int index=1;for(var record:history){var row=sheet.createRow(index++);for(int c=0;c<fields.size();c++){var cell=row.createCell(c);cell.setCellStyle(textStyle);cell.setCellValue(record.getOrDefault(fields.get(c),""));}}}}book.write(bytes);output=bytes.toByteArray();extension="xlsx";
  }}else throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Formato inválido");
  String filename="centro-saber-"+module+"-"+(year==null?"completo":year)+"."+extension;
  return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\""+filename+"\"").header(HttpHeaders.CACHE_CONTROL,"no-store").contentType(MediaType.parseMediaType(extension.equals("csv")?"text/csv;charset=UTF-8":"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")).body(output);
 }
}


