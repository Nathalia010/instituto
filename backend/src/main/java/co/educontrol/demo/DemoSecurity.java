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
    @Bean SecurityFilterChain demoFilter(HttpSecurity http,org.springframework.jdbc.core.JdbcTemplate db) throws Exception {
        return http.addFilterBefore(new StudentAccessFilter(db),org.springframework.security.web.access.intercept.AuthorizationFilter.class).authorizeHttpRequests(a -> a
            .requestMatchers(HttpMethod.GET, "/api/health", "/api/demo/csrf", "/api/demo/session").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/demo/login/admin", "/api/demo/login/student", "/api/demo/inquiries").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/demo/gallery", "/api/demo/gallery/images/*").permitAll()
            .requestMatchers(HttpMethod.GET,"/api/demo/registration/settings","/api/demo/agenda","/api/demo/agenda/images/*","/api/demo/sites","/api/demo/sites/images/*").permitAll()
            .requestMatchers(HttpMethod.GET,"/api/demo/announcements","/api/demo/announcements/images/*","/api/demo/event-pages/*","/api/demo/event-pages/*/photos/*","/api/demo/leveling-dates").permitAll()
            .requestMatchers("/api/demo/admin/registration/**").hasRole("ADMINISTRADOR")
            .requestMatchers("/api/demo/admin/gallery/**").hasRole("ADMINISTRADOR")
            .requestMatchers(HttpMethod.GET, "/api/demo/admin/inquiries", "/api/demo/admin/students").hasRole("ADMINISTRADOR")
            .requestMatchers("/api/demo/admin/data/**").hasRole("ADMINISTRADOR")
            .requestMatchers("/api/demo/admin/levelings/**","/api/demo/admin/levelings").hasRole("ADMINISTRADOR")
            .requestMatchers("/api/demo/student/levelings").hasRole("ESTUDIANTE")
            .requestMatchers(HttpMethod.GET,"/api/demo/programs","/api/demo/home/appearance","/api/demo/page-images/*/*").permitAll()
            .requestMatchers("/api/demo/student/leveling-slots","/api/demo/student/levelings/book").hasRole("ESTUDIANTE")
            .requestMatchers("/api/demo/admin/leveling-slots/**","/api/demo/admin/leveling-slots").hasRole("ADMINISTRADOR")
            .requestMatchers(HttpMethod.POST, "/api/demo/admin/inquiries/*/student", "/api/demo/admin/inquiries/*/edit", "/api/demo/admin/inquiries/*/notes", "/api/demo/admin/inquiries/*/delete", "/api/demo/admin/inquiries/*/restore", "/api/demo/admin/students/*/edit", "/api/demo/admin/students/*/access").hasRole("ADMINISTRADOR")
            .requestMatchers(HttpMethod.GET, "/api/demo/student/me", "/api/demo/student/courses").hasRole("ESTUDIANTE")
            .requestMatchers("/api/demo/logout").authenticated()
            .anyRequest().denyAll())
            .exceptionHandling(e -> e.authenticationEntryPoint((req,res,error) -> res.sendError(401)))
            .build();
    }
}
