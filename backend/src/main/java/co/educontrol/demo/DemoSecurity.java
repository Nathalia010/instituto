package co.educontrol.demo;

import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
@Profile("demo")
public class DemoSecurity {
    @Bean BCryptPasswordEncoder demoEncoder() { return new BCryptPasswordEncoder(); }
    @Bean SecurityFilterChain demoFilter(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(a -> a
            .requestMatchers(HttpMethod.GET, "/api/health", "/api/demo/csrf", "/api/demo/session").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/demo/login/admin", "/api/demo/login/student", "/api/demo/inquiries").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/demo/admin/inquiries", "/api/demo/admin/students").hasRole("ADMINISTRADOR")
            .requestMatchers("/api/demo/admin/data/**").hasRole("ADMINISTRADOR")
            .requestMatchers(HttpMethod.POST, "/api/demo/admin/inquiries/*/student", "/api/demo/admin/inquiries/*/edit", "/api/demo/admin/inquiries/*/notes", "/api/demo/admin/inquiries/*/delete", "/api/demo/admin/inquiries/*/restore", "/api/demo/admin/students/*/edit").hasRole("ADMINISTRADOR")
            .requestMatchers(HttpMethod.GET, "/api/demo/student/me").hasRole("ESTUDIANTE")
            .requestMatchers("/api/demo/logout").authenticated()
            .anyRequest().denyAll())
            .exceptionHandling(e -> e.authenticationEntryPoint((req,res,error) -> res.sendError(401)))
            .build();
    }
}
