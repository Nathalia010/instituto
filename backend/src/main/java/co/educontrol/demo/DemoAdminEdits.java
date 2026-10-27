package co.educontrol.demo;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/demo/admin")
@Profile("demo")
public class DemoAdminEdits {
    private final JdbcTemplate db;private final org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder;private final BulkDataService data;
    private static final Set<String> PROGRAMS=Set.of("validacion","preparacion","preuniversitario","enfermeria","icfes","vinculacion","administrativo","infancia","refuerzo");
    private static final Set<String> STATUSES=Set.of("EN_CURSO","FINALIZADO","NO_FINALIZADO","RETIRADO","PENDIENTE_EVALUACION","PENDIENTE_MATRICULA");
    public DemoAdminEdits(JdbcTemplate db,org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder,BulkDataService data) { this.db=db;this.encoder=encoder;this.data=data; }
    public record EditInquiry(@NotBlank @Size(max=180) String name, @NotNull @Pattern(regexp="[0-9]{5,30}|") String document,
        @NotBlank @Email @Size(max=254) String email, @NotBlank @Pattern(regexp="[0-9]{10}") String phone,
        @NotBlank String program, @NotBlank @Size(max=2000) String message, @NotNull @Size(max=2000) String notes) {}
    public record EditStudent(@NotBlank @Size(max=180) String name, @NotBlank @Pattern(regexp="[A-Za-z0-9]{1,30}") String document,
        @NotBlank @Email @Size(max=254) String email, @NotBlank @Pattern(regexp="[0-9]{10}") String phone,
        @NotBlank String program, @NotBlank String issued, @NotBlank String status, @NotNull @Size(max=500) String notes, @Min(2000) @Max(2200) Integer year) {}
    public record InquiryNotes(@NotNull @Size(max=2000) String notes) {}
    private void validateProgram(String program) {
        if(!PROGRAMS.contains(program)&&db.queryForObject("SELECT COUNT(*) FROM demo_data_records WHERE module='cursos' AND record_key=? AND deleted=FALSE",Integer.class,program)==0)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Programa inválido.");
    }
    private void audit(String action,String id) {
        db.update("INSERT INTO demo_audit(id,actor,action,resource_id) VALUES (?,?,?,?)",UUID.randomUUID().toString(),SecurityContextHolder.getContext().getAuthentication().getName(),action,id);
    }
    @PostMapping("/inquiries/{id}/edit") @Transactional
    public Map<String,Boolean> editInquiry(@PathVariable String id,@Valid @RequestBody EditInquiry input) {
        validateProgram(input.program());
        int changed=db.update("UPDATE demo_inquiries SET name=?,document=?,email=?,phone=?,program=?,message=?,admin_notes=? WHERE id=? AND deleted=FALSE",
            input.name().trim(),input.document().toUpperCase(java.util.Locale.ROOT),input.email().trim(),input.phone(),input.program(),input.message().trim(),input.notes(),id);
        if(changed==0)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Solicitud no encontrada.");
        audit("EDITAR_SOLICITUD",id);return Map.of("ok",true);
    }
    @PostMapping("/inquiries/{id}/delete") @Transactional
    public Map<String,Boolean> deleteInquiry(@PathVariable String id) {
        if(db.update("UPDATE demo_inquiries SET deleted=TRUE WHERE id=? AND deleted=FALSE",id)==0)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Solicitud no encontrada.");
        audit("QUITAR_SOLICITUD",id);return Map.of("ok",true);
    }
    @PostMapping("/inquiries/{id}/notes") @Transactional
    public Map<String,Boolean> inquiryNotes(@PathVariable String id,@Valid @RequestBody InquiryNotes input) {
        if(db.update("UPDATE demo_inquiries SET admin_notes=? WHERE id=? AND deleted=FALSE",input.notes(),id)==0)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Solicitud no encontrada.");
        audit("NOTA_SOLICITUD",id);return Map.of("ok",true);
    }
    @PostMapping("/inquiries/{id}/restore") @Transactional
    public Map<String,Boolean> restoreInquiry(@PathVariable String id) {
        if(db.update("UPDATE demo_inquiries SET deleted=FALSE WHERE id=? AND deleted=TRUE",id)==0)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Solicitud no encontrada.");
        audit("RESTAURAR_SOLICITUD",id);return Map.of("ok",true);
    }
    public record StudentAccess(@NotNull Boolean active) {}
    @PostMapping("/students/{id}/access") @Transactional
    public Map<String,Boolean> studentAccess(@PathVariable String id,@Valid @RequestBody StudentAccess input){
        db.queryForObject("SELECT id FROM demo_data_lock WHERE id=1 FOR UPDATE",Integer.class);
        var rows=db.queryForList("SELECT account_active,issued,academic_year,key_hash FROM demo_students WHERE id=? AND deleted=FALSE FOR UPDATE",id);
        if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Estudiante no encontrado.");
        var account=rows.getFirst();if(input.active()){try{if(LocalDate.parse(account.get("issued").toString()).isAfter(LocalDate.now()))throw new IllegalArgumentException();}catch(Exception ex){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Completa la fecha de expedición antes de activar.");}if(account.get("key_hash").toString().isBlank())db.update("UPDATE demo_students SET key_hash=? WHERE id=?",encoder.encode(account.get("academic_year").toString()),id);}
        if(!input.active().equals(rows.getFirst().get("account_active"))){db.update("UPDATE demo_students SET account_active=?,account_version=account_version+1 WHERE id=?",input.active(),id);audit(input.active()?"ACTIVAR_CUENTA_ESTUDIANTE":"DESACTIVAR_CUENTA_ESTUDIANTE",id);}
        return Map.of("active",input.active());
    }
    @PostMapping("/students/{id}/edit") @Transactional
    public Map<String,Boolean> editStudent(@PathVariable String id,@Valid @RequestBody EditStudent input) {
        validateProgram(input.program());
        if(!STATUSES.contains(input.status()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Estado inválido.");
        try { if(LocalDate.parse(input.issued()).isAfter(LocalDate.now()))throw new IllegalArgumentException(); }
        catch(Exception e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Fecha de expedición inválida."); }
        db.queryForObject("SELECT id FROM demo_data_lock WHERE id=1 FOR UPDATE",Integer.class);
        var before=db.queryForList("SELECT document,document_type,issued,academic_year FROM demo_students WHERE id=? AND deleted=FALSE FOR UPDATE",id);
        if(before.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Estudiante no encontrado.");
        int year=input.year()==null?((Number)before.getFirst().get("academic_year")).intValue():input.year();
        if(year!=((Number)before.getFirst().get("academic_year")).intValue())db.update("UPDATE demo_students SET academic_year=?,key_hash=?,account_version=account_version+1 WHERE id=?",year,encoder.encode(Integer.toString(year)),id);
        try {
            db.update("UPDATE demo_students SET name=?,document=?,email=?,phone=?,program=?,issued=?,academic_status=?,admin_notes=? WHERE id=?",
                input.name().trim(),input.document().toUpperCase(java.util.Locale.ROOT),input.email().trim(),input.phone(),input.program(),input.issued(),input.status(),input.notes(),id);
        } catch(DuplicateKeyException e) {throw new ResponseStatusException(HttpStatus.CONFLICT,"El documento ya pertenece a otro usuario.");}
        String oldKey=before.getFirst().get("document_type")+":"+before.getFirst().get("document");
        var imported=db.queryForList("SELECT payload FROM demo_data_records WHERE module='estudiantes' AND record_key=? AND deleted=FALSE",String.class,oldKey);
        if(!imported.isEmpty()){
            var row=data.decode(imported.getFirst());row.putAll(Map.of("documento",input.document().toUpperCase(java.util.Locale.ROOT),"nombre_completo",input.name().trim(),"correo",input.email().trim(),"telefono",input.phone(),"programa",input.program(),"fecha_expedicion",input.issued(),"estado_academico",input.status(),"notas_administrativas",input.notes(),"anio",Integer.toString(year)));
            row.put("nombres","");row.put("apellidos","");
            db.update("UPDATE demo_data_records SET record_key=?,academic_year=?,payload=? WHERE module='estudiantes' AND record_key=?",before.getFirst().get("document_type")+":"+input.document().toUpperCase(java.util.Locale.ROOT),year,data.encode(row),oldKey);
        }
        audit("EDITAR_ESTUDIANTE",id);return Map.of("ok",true);
    }
}
