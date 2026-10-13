package co.educontrol.demo;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/demo")
@Profile("demo")
public class DemoController {
    private final JdbcTemplate db;
    private final BCryptPasswordEncoder encoder;
    private final String adminHash;
    private final Map<String, Deque<Long>> attempts = new ConcurrentHashMap<>();
    private static final Set<String> PROGRAMS = Set.of("validacion","preparacion","preuniversitario","enfermeria","icfes");
    public DemoController(JdbcTemplate db, BCryptPasswordEncoder encoder) {
        this.db=db; this.encoder=encoder;
        this.adminHash=encoder.encode("1234987A");
    }
    @Bean ApplicationRunner seedDemo() {
        return args -> {
            if (db.queryForObject("SELECT count(*) FROM demo_students WHERE id='prueba-estudiante'", Integer.class)==0) {
                db.update("INSERT INTO demo_students(id,name,email,phone,program,document,issued,academic_year,key_hash,academic_status) VALUES (?,?,?,?,?,?,?,?,?,?)",
                    "prueba-estudiante","Estudiante de prueba","estudiante@example.invalid","3000000000","validacion","1000000001","2020-01-15",2026,encoder.encode("2026"),"EN_CURSO");
                db.update("INSERT INTO demo_notes VALUES (?,?,?,?,?)","nota-prueba-1","prueba-estudiante","Matemáticas",4.2,2026);
                db.update("INSERT INTO demo_notes VALUES (?,?,?,?,?)","nota-prueba-2","prueba-estudiante","Lenguaje",4.5,2026);
                db.update("INSERT INTO demo_movements VALUES (?,?,?,?,?,?)","cobro-prueba","prueba-estudiante","Matrícula de ejemplo","COBRO",500000,"2026-01-15");
                db.update("INSERT INTO demo_movements VALUES (?,?,?,?,?,?)","abono-prueba","prueba-estudiante","Abono de ejemplo","ABONO",200000,"2026-01-20");
            }
        };
    }
    public record AdminLogin(@NotBlank @Size(max=80) String username, @NotBlank @Size(max=100) String password) {}
    public record StudentLogin(@Pattern(regexp="[0-9]{5,30}") String document, @NotBlank String issued, @NotBlank @Size(max=4) String year) {}
    public record Inquiry(@NotBlank @Size(max=180) String name, @NotBlank @Email @Size(max=254) String email,
        @NotBlank @Pattern(regexp="[0-9]{10}") String phone, @NotBlank String program,
        @NotBlank @Size(max=2000) String message, @AssertTrue boolean consent) {}
    public record CreateStudent(@NotBlank @Pattern(regexp="[0-9]{5,30}") String document, @NotBlank String issued, @Min(2000) @Max(2200) int year) {}

    @GetMapping("/csrf") public Map<String,String> csrf(CsrfToken token) {
        return Map.of("header",token.getHeaderName(),"token",token.getToken());
    }
    @GetMapping("/session") public Map<String,Object> session() {
        var a=SecurityContextHolder.getContext().getAuthentication();
        String role=a.getAuthorities().stream().map(Object::toString).filter(s->s.startsWith("ROLE_")).findFirst().orElse("");
        return Map.of("role",role.replace("ROLE_",""),"demo",true);
    }
    private void rateLimit(HttpServletRequest req, String kind) {
        String key=kind+":"+req.getRemoteAddr();
        Deque<Long> queue=attempts.computeIfAbsent(key,k->new ArrayDeque<>());
        synchronized(queue) {
            long now=System.currentTimeMillis();
            while(!queue.isEmpty() && queue.peekFirst()<now-60000) queue.removeFirst();
            if(queue.size()>=15) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Espera un minuto antes de volver a intentar.");
            queue.addLast(now);
        }
    }
    private Map<String,String> signIn(String id,String role,HttpServletRequest req,HttpServletResponse res) {
        if(req.getSession(false)!=null) req.changeSessionId();
        var context=SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(id,null,List.of(new SimpleGrantedAuthority("ROLE_"+role))));
        SecurityContextHolder.setContext(context);
        new HttpSessionSecurityContextRepository().saveContext(context,req,res);
        return Map.of("role",role);
    }
    @PostMapping("/login/admin") public Map<String,String> adminLogin(@Valid @RequestBody AdminLogin login,HttpServletRequest req,HttpServletResponse res) {
        rateLimit(req,"login");
        if(!encoder.matches(login.password(),adminHash) || !"adminN".equals(login.username()))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Credenciales incorrectas.");
        return signIn("adminN","ADMINISTRADOR",req,res);
    }
    @PostMapping("/login/student") public Map<String,String> studentLogin(@Valid @RequestBody StudentLogin login,HttpServletRequest req,HttpServletResponse res) {
        rateLimit(req,"login");
        var rows=db.queryForList("SELECT id,issued,key_hash FROM demo_students WHERE document=?",login.document());
        boolean valid=!rows.isEmpty() && encoder.matches(login.year(),rows.getFirst().get("key_hash").toString()) && login.issued().equals(rows.getFirst().get("issued"));
        if(!valid) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Credenciales incorrectas.");
        return signIn(rows.getFirst().get("id").toString(),"ESTUDIANTE",req,res);
    }
    @PostMapping("/logout") public Map<String,Boolean> logout(HttpServletRequest req) {
        if(req.getSession(false)!=null) req.getSession(false).invalidate();
        SecurityContextHolder.clearContext(); return Map.of("ok",true);
    }
    @PostMapping("/inquiries") @ResponseStatus(HttpStatus.CREATED)
    public Map<String,String> inquiry(@Valid @RequestBody Inquiry input,HttpServletRequest req) {
        rateLimit(req,"inquiry");
        if(!PROGRAMS.contains(input.program())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Programa inválido.");
        String id=UUID.randomUUID().toString();
        db.update("INSERT INTO demo_inquiries(id,name,email,phone,program,message,consent) VALUES (?,?,?,?,?,?,?)",id,input.name().trim(),input.email().trim(),input.phone(),input.program(),input.message().trim(),input.consent());
        return Map.of("id",id,"message","Solicitud registrada. La institución podrá contactarte.");
    }
    @GetMapping("/admin/inquiries") public List<Map<String,Object>> inquiries() {
        return db.queryForList("SELECT i.*, coalesce(s.document,'') AS student_document FROM demo_inquiries i LEFT JOIN demo_students s ON s.id=i.student_id WHERE i.deleted=FALSE ORDER BY i.created_at DESC");
    }
    @GetMapping("/admin/students") public List<Map<String,Object>> students() {
        return db.queryForList("SELECT id,name,email,phone,program,document,issued,academic_year,academic_status,admin_notes FROM demo_students ORDER BY created_at DESC");
    }
    @PostMapping("/admin/inquiries/{id}/student") @Transactional @ResponseStatus(HttpStatus.CREATED)
    public Map<String,String> createStudent(@PathVariable String id,@Valid @RequestBody CreateStudent input) {
        try { if(LocalDate.parse(input.issued()).isAfter(LocalDate.now())) throw new IllegalArgumentException(); }
        catch(Exception e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Fecha de expedición inválida."); }
        var rows=db.queryForList("SELECT * FROM demo_inquiries WHERE id=? AND deleted=FALSE FOR UPDATE",id);
        if(rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Solicitud no encontrada.");
        var lead=rows.getFirst();
        if(lead.get("student_id")!=null) throw new ResponseStatusException(HttpStatus.CONFLICT,"Esta solicitud ya tiene usuario.");
        String studentId=UUID.randomUUID().toString();
        try {
            db.update("INSERT INTO demo_students(id,name,email,phone,program,document,issued,academic_year,key_hash,academic_status) VALUES (?,?,?,?,?,?,?,?,?,?)",
                studentId,lead.get("name"),lead.get("email"),lead.get("phone"),lead.get("program"),input.document(),input.issued(),input.year(),encoder.encode(Integer.toString(input.year())),"PENDIENTE_MATRICULA");
        } catch(DuplicateKeyException e) { throw new ResponseStatusException(HttpStatus.CONFLICT,"El documento ya tiene usuario."); }
        db.update("UPDATE demo_inquiries SET student_id=?,status='USUARIO_CREADO' WHERE id=?",studentId,id);
        db.update("INSERT INTO demo_audit(id,actor,action,resource_id) VALUES (?,?,?,?)",UUID.randomUUID().toString(),"adminN","CREAR_USUARIO_ESTUDIANTE",studentId);
        return Map.of("id",studentId,"message","Usuario creado. Puede entrar con el documento, la fecha y el año configurados.");
    }
    @GetMapping("/student/me") public Map<String,Object> me() {
        String id=SecurityContextHolder.getContext().getAuthentication().getName();
        var data=new HashMap<String,Object>(db.queryForMap("SELECT id,name,email,phone,program,document,issued,academic_year,academic_status FROM demo_students WHERE id=?",id));
        data.put("notes",db.queryForList("SELECT subject,grade,academic_year AS \"year\" FROM demo_notes WHERE student_id=?",id));
        data.put("movements",db.queryForList("SELECT concept,type,amount,movement_date AS \"date\" FROM demo_movements WHERE student_id=? ORDER BY movement_date",id));
        data.put("balance",db.queryForObject("SELECT coalesce(sum(CASE WHEN type='COBRO' THEN amount ELSE -amount END),0) FROM demo_movements WHERE student_id=?",java.math.BigDecimal.class,id));
        return data;
    }
}
