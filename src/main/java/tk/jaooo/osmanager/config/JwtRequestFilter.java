package tk.jaooo.osmanager.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tk.jaooo.osmanager.model.dto.DemandanetSessionDetailsDTO;
import tk.jaooo.osmanager.services.JwtUtil;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtRequestFilter.class);

    private final JwtUtil jwtUtil;

    public JwtRequestFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain)
            throws ServletException, IOException {

        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            chain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader("Authorization");
        final String jwt;

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);

        try {
            if (jwtUtil.validateToken(jwt)) {
                String sessionCookie = jwtUtil.extractSessionCookie(jwt);
                String idEscola = jwtUtil.extractIdEscola(jwt);

                DemandanetSessionDetailsDTO sessionDetails = new DemandanetSessionDetailsDTO(sessionCookie, idEscola);

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        sessionDetails,
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_ACTIVE_SESSION"))
                );

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authToken);
                logger.debug("Sessão do Demandanet validada e contexto de segurança populado para idEscola: {}", idEscola);
            } else {
                logger.warn("Token JWT recebido é inválido ou expirado.");
            }
        } catch (Exception e) {
            logger.error("Erro ao processar o token JWT: {}", e.getMessage());
        }

        chain.doFilter(request, response);
    }
}
