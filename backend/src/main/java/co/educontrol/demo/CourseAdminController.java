package co.educontrol.demo;

import java.math.BigDecimal;
import java.util.*;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** Course membership uses the shared catalog and active portal accounts. */
@RestController @Profile("demo") @RequestMapping("/api/demo/admin/data/courses")
public class CourseAdminController {
 private final BulkDataService data;private final JdbcTemplate db;
 public CourseAdminController(BulkDataService data,JdbcTemplate db){this.data=data;this.db=db;}
 private BigDecimal amount(Map<String,String> row){return new BigDecimal(row.getOrDefault("valor_cop","0"));}
 @GetMapping public List<Map<String,Object>> courses(@RequestParam(required=false) Integer year){
  if(year!=null&&(year<2000||year>2200))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Año inválido.");
  var students=db.queryForList("SELECT id,name,document,document_type,program,academic_year,academic_status,email,phone FROM demo_students WHERE deleted=FALSE AND account_active=TRUE ORDER BY name");
  var enrollments=data.list("matriculas",year);var charges=data.list("cobros",null);var payments=data.list("pagos",null);var periods=data.list("periodos",null);
  var movements=db.queryForList("SELECT student_id,type,amount,YEAR(movement_date) AS movement_year FROM demo_movements");
  var result=new ArrayList<Map<String,Object>>();
  for(var course:data.list("cursos",null)){
   String code=course.get("codigo_curso");var courseEnrollments=enrollments.stream().filter(r->code.equals(r.get("codigo_curso"))).toList();
   var members=new ArrayList<Map<String,Object>>();
   for(var student:students){
    var linked=courseEnrollments.stream().filter(r->Objects.equals(student.get("document"),r.get("documento"))&&Objects.equals(student.get("document_type"),r.get("tipo_documento"))).toList();
    boolean primary=code.equals(student.get("program"))||course.get("nombre").equals(student.get("program"));
    boolean primaryYear=primary&&(year==null||year.equals(((Number)student.get("academic_year")).intValue()));
    if(!primaryYear&&linked.isEmpty())continue;
    var row=new LinkedHashMap<String,Object>(student);row.remove("program");Set<String> memberYears=new TreeSet<>();linked.forEach(r->memberYears.add(r.getOrDefault("anio","")));if(primaryYear)memberYears.add(student.get("academic_year").toString());memberYears.remove("");row.put("years",memberYears);
    Set<String> ids=new HashSet<>();linked.forEach(r->ids.add(r.get("codigo_matricula")));
    var bills=charges.stream().filter(r->ids.contains(r.get("codigo_matricula"))).toList();Set<String> billIds=new HashSet<>();bills.forEach(r->billIds.add(r.get("codigo_cobro")));
    BigDecimal charged=BigDecimal.ZERO,paid=BigDecimal.ZERO,review=BigDecimal.ZERO;String source="Sin movimientos";
    if(!bills.isEmpty()){
     source="Matrículas del curso";for(var bill:bills)charged=charged.add(amount(bill));
     for(var payment:payments)if(billIds.contains(payment.get("codigo_cobro"))){if(Set.of("APROBADO","CONFIRMADO").contains(payment.get("estado_pago")))paid=paid.add(amount(payment));else if("EN_REVISION".equals(payment.get("estado_pago")))review=review.add(amount(payment));}
    }else if(primaryYear){
     // Legacy portal movements have no enrollment id. Attribute them only to
     // the student's own program and academic year, never to a second course.
     for(var movement:movements)if(student.get("id").equals(movement.get("student_id"))&&((Number)student.get("academic_year")).intValue()==((Number)movement.get("movement_year")).intValue()){
      source="Portal · programa y año";BigDecimal value=(BigDecimal)movement.get("amount");if("COBRO".equals(movement.get("type")))charged=charged.add(value);else paid=paid.add(value);
     }
    }
    BigDecimal balance=charged.subtract(paid).max(BigDecimal.ZERO);
    row.put("charged",charged);row.put("paid",paid);row.put("review",review);row.put("balance",balance);row.put("paymentSource",source);
    row.put("paymentStatus",charged.signum()==0?"SIN_COBROS":balance.signum()==0?"AL_DIA":review.signum()>0?"EN_REVISION":paid.signum()>0?"PARCIAL":"PENDIENTE");
    row.put("enrollments",linked);members.add(row);
   }
   Set<String> periodCodes=new HashSet<>();courseEnrollments.forEach(r->periodCodes.add(r.get("codigo_periodo")));
   var item=new LinkedHashMap<String,Object>();item.put("course",course);item.put("students",members);item.put("periods",periods.stream().filter(r->periodCodes.contains(r.get("codigo_periodo"))).toList());result.add(item);
  }
  return result;
 }
}
