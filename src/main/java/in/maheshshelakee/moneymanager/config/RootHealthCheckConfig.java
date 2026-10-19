package in.maheshshelakee.moneymanager.config;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.servlet.ServletException;
import java.io.IOException;

/**
 * Ensures that root health-check probes (such as GET / or GET /health on port 8080)
 * return HTTP 200 OK even when server.servlet.context-path is /api/v1.0.
 * This prevents Render deployment timeouts when the default health check probes /.
 */
@Configuration
public class RootHealthCheckConfig {

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatHealthCheckCustomizer() {
        return factory -> factory.addEngineValves(new ValveBase() {
            @Override
            public void invoke(Request request, Response response) throws IOException, ServletException {
                String uri = request.getRequestURI();
                if ("/".equals(uri) || "/health".equals(uri) || "/status".equals(uri) || "/actuator/health".equals(uri)) {
                    response.setStatus(200);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"status\":\"UP\"}");
                    return;
                }
                getNext().invoke(request, response);
            }
        });
    }
}
