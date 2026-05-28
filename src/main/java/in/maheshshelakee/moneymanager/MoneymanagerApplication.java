package in.maheshshelakee.moneymanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

// FIX: Added @EnableAsync so that @Async in AdminAuditService actually works
// FIX: Excluded UserDetailsServiceAutoConfiguration — this app uses JWT auth, not
//      Spring's built-in username/password. Without this exclusion, Spring creates
//      an InMemoryUserDetailsManager and logs "Using generated security password"
//      on every startup (confusing in production).
@SpringBootApplication(exclude = { UserDetailsServiceAutoConfiguration.class })
@EnableAsync
@EnableScheduling
public class MoneymanagerApplication {

	public static void main(String[] args) {
		SpringApplication.run(MoneymanagerApplication.class, args);
	}

}
