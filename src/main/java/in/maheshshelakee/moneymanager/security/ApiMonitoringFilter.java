package in.maheshshelakee.moneymanager.security;

import in.maheshshelakee.moneymanager.service.SystemMonitoringService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Component
@Order(1) // Run first to time the entire request cycle
@RequiredArgsConstructor
public class ApiMonitoringFilter extends OncePerRequestFilter {

    private final SystemMonitoringService monitoringService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String path = request.getRequestURI();
        
        // Exclude static resources, web sockets, or actuator endpoints to prevent cluttering logs
        if (path.contains("/assets/") || path.contains("/favicon") || path.contains("/ws")) {
            filterChain.doFilter(request, response);
            return;
        }

        long startTime = System.currentTimeMillis();
        Throwable exception = null;

        try {
            filterChain.doFilter(request, response);
        } catch (Throwable t) {
            exception = t;
            throw t;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            int status = response.getStatus();
            
            String userId = "anonymous";
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
                userId = auth.getName();
            }

            monitoringService.recordRequest(path, request.getMethod(), status, duration, userId, exception);
        }
    }
}
