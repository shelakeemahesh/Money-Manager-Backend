package in.maheshshelakee.moneymanager.security;

import in.maheshshelakee.moneymanager.entity.User;
import in.maheshshelakee.moneymanager.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    private static class CachedUser {
        final String email;
        final String status;
        final String password;
        final long expiryTime;

        CachedUser(String email, String status, String password, long ttlMs) {
            this.email = email;
            this.status = status;
            this.password = password;
            this.expiryTime = System.currentTimeMillis() + ttlMs;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }
    }

    private final java.util.concurrent.ConcurrentHashMap<String, CachedUser> userCache = 
            new java.util.concurrent.ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 10000; // 10 seconds cache

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (jwtUtil.isTokenValid(token)) {
                String email = jwtUtil.extractEmail(token);
                
                // Retrieve user details from cache or DB to check if the session is still valid
                CachedUser cached = userCache.get(email);
                if (cached == null || cached.isExpired()) {
                    User user = userRepository.findByEmail(email).orElse(null);
                    if (user == null) {
                        userCache.remove(email);
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "User not found");
                        return;
                    }
                    String dbStatus = user.getStatus() != null ? user.getStatus().name() : "ACTIVE";
                    cached = new CachedUser(email, dbStatus, user.getPassword(), CACHE_TTL_MS);
                    userCache.put(email, cached);
                }

                // Check for banned/suspended status
                if ("BANNED".equals(cached.status) || "SUSPENDED".equals(cached.status)) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN,
                            "User account is " + cached.status.toLowerCase());
                    return;
                }

                // Verify password version from token against the actual version in cache
                String tokenPassVersion = jwtUtil.extractPasswordVersion(token);
                String actualPassVersion = jwtUtil.getPasswordVersion(cached.password);
                if (tokenPassVersion == null || !tokenPassVersion.equals(actualPassVersion)) {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                            "Session invalidated. Please log in again.");
                    return;
                }

                if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    String role = jwtUtil.extractRole(token);
                    String resolvedRole = (role != null) ? role : "USER";
                    SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + resolvedRole);
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(email, null,
                                    Collections.singletonList(authority));
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
