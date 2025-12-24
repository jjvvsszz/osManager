package tk.jaooo.osmanager.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tk.jaooo.osmanager.model.Tecnico;
import tk.jaooo.osmanager.repository.TecnicoRepository;
import tk.jaooo.osmanager.services.JwtUtil;

import java.io.IOException;
import java.util.Optional;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtRequestFilter.class);

    private final JwtUtil jwtUtil;
    private final TecnicoRepository tecnicoRepository;

    public JwtRequestFilter(JwtUtil jwtUtil, TecnicoRepository tecnicoRepository) {
        this.jwtUtil = jwtUtil;
        this.tecnicoRepository = tecnicoRepository;
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
                String username = jwtUtil.extractUsername(jwt);

                if (username != null) {
                    Optional<Tecnico> tecnicoOpt = tecnicoRepository.findByUsernameAndRemovidoIsFalse(username);

                    if (tecnicoOpt.isPresent()) {
                        Tecnico tecnico = tecnicoOpt.get();

                        // Cria a autenticação usando o objeto TECNICO como Principal
                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                tecnico,
                                null,
                                tecnico.getAuthorities()
                        );

                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);

                        logger.debug("Usuário '{}' autenticado via JWT.", username);
                    } else {
                        logger.warn("Token válido, mas usuário '{}' não encontrado no banco.", username);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Erro ao processar token JWT: {}", e.getMessage());
        }

        chain.doFilter(request, response);
    }
}
