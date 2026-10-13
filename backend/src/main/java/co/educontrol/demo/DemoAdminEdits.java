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
    private final JdbcTemplate db;
    private static final Set<String> PROGRAMS=Set.of("validacion","preparacion","preuniversitario","enfermeria","icfes");
    private static final Set<String> STATUSES=Set.of("EN_CURSO","FINALIZADO","NO_FINALIZADO","RETIRADO","PENDIENTE_EVALUACION","PENDIENTE_MATRICULA");
    public DemoAdminEdits(JdbcTemplate db) { this.db=db; }
    public record EditInquiry(@NotBlank @Size(max=180) String name, @NotNull @Pattern(regexp="[0-9]{5,30}|") String document,
        @NotBlank @Email @Size(max=254) String email, @NotBlank @Pattern(regexp="[0-9]{10}") String phone,
        @NotBlank String program, @NotBlank @Size(max=2000) String message, @NotNull @Size(max=2000) String notes) {}
    public record EditStudent(@NotBlank @Size(max=180) String name, @NotBlank @Pattern(regexp="[0-9]{5,30}") String document,
        @NotBlank @Email @Size(max=254) String email, @NotBlank @Pattern(regexp="[0-9]{10}") String phone,
        @NotBlank String program, @NotBlank String issued, @NotBlank String status, @NotNull @Size(max=500) String notes) {}
    public record InquiryNotes(@NotNull @Size(max=2000) String notes) {}
    private void validateProgram(String program) {
        if(!PROGRAMS.contains(program))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Programa inválido.");
    }
    private void audit(String action,String id) {
        db.update("INSERT INTO demo_audit(id,actor,action,resource_id) VALUES (?,?,?,?)",UUID.randomUUID().toString(),SecurityContextHolder.getContext().getAuthentication().getName(),action,id);
    }
    @PostMapping("/inquiries/{id}/edit") @Transactional
    public Map<String,Boolean> editInquiry(@PathVariable String id,@Valid @RequestBody EditInquiry input) {
        validateProgram(input.program());
        int changed=db.update("UPDATE demo_inquiries SET name=?,document=?,email=?,phone=?,program=?,message=?,admin_notes=? WHERE id=? AND deleted=FALSE",
            input.name().trim(),input.document(),input.email().trim(),input.phone(),input.program(),input.message().trim(),input.notes(),id);
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
    @PostMapping("/students/{id}/edit") @Transactional
    public Map<String,Boolean> editStudent(@PathVariable String id,@Valid @RequestBody EditStudent input) {
        validateProgram(input.program());
        if(!STATUSES.contains(input.status()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Estado inválido.");
        try { if(LocalDate.parse(input.issued()).isAfter(LocalDate.now()))throw new IllegalArgumentException(); }
        catch(Exception e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Fecha de expedición inválida."); }
        var before=db.queryForList("SELECT document,issued FROM demo_students WHERE id=? FOR UPDATE",id);
        if(before.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Estudiante no encontrado.");
        try {
            db.update("UPDATE demo_students SET name=?,document=?,email=?,phone=?,program=?,issued=?,academic_status=?,admin_notes=? WHERE id=?",
                input.name().trim(),input.document(),input.email().trim(),input.phone(),input.program(),input.issued(),input.status(),input.notes(),id);
        } catch(DuplicateKeyException e) {throw new ResponseStatusException(HttpStatus.CONFLICT,"El documento ya pertenece a otro usuario.");}
        audit("EDITAR_ESTUDIANTE",id);return Map.of("ok",true);
    }
}
