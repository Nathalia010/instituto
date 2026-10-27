package co.educontrol.demo;

import java.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/** Revokes old student sessions even when an account is subsequently reactivated. */
final class StudentAccessFilter extends OncePerRequestFilter {
 private final JdbcTemplate db;
 StudentAccessFilter(JdbcTemplate db){this.db=db;}
 @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws ServletException,IOException{
  var auth=SecurityContextHolder.getContext().getAuthentication();
  if(auth!=null&&auth.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_ESTUDIANTE"))){
   var rows=db.queryForList("SELECT account_active,account_version FROM demo_students WHERE id=? AND deleted=FALSE",auth.getName());
   boolean valid=!rows.isEmpty()&&Boolean.TRUE.equals(rows.getFirst().get("account_active"))&&auth.getDetails() instanceof Integer version&&version.equals(((Number)rows.getFirst().get("account_version")).intValue());
   if(!valid){if(request.getSession(false)!=null)request.getSession(false).invalidate();SecurityContextHolder.clearContext();if(request.getRequestURI().startsWith("/api/demo/student/")){response.sendError(401);return;}}
  }
  chain.doFilter(request,response);
 }
}
