package co.educontrol.security;

import co.educontrol.health.HealthController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HealthController.class)
@Import(SecurityConfig.class)
class SecurityConfigTest {
    @Autowired MockMvc mvc;

    @Test void healthIsPublic() throws Exception {
        mvc.perform(get("/api/health")).andExpect(status().isOk());
    }
    @Test void administrativeRoutesAreClosedEvenWithARole() throws Exception {
        mvc.perform(get("/api/students").with(user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden());
    }
    @Test void writesRequireCsrf() throws Exception {
        mvc.perform(post("/api/students")).andExpect(status().isForbidden());
    }
    @Test void studentRoutesAreClosed() throws Exception {
        mvc.perform(get("/api/portal/me").with(user("student").roles("STUDENT")))
                .andExpect(status().isForbidden());
    }
}
