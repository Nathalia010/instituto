package co.educontrol.demo;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
@RestController @Profile("demo")
public class StudentCoursesController {
 private final CourseEnrollmentService enrollments;
 public StudentCoursesController(CourseEnrollmentService enrollments){this.enrollments=enrollments;}
 @GetMapping("/api/demo/student/courses") public List<CourseEnrollmentService.Enrollment> mine(Authentication auth){return enrollments.courses(auth.getName());}
}
