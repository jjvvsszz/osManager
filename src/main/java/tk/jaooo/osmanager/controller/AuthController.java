package tk.jaooo.osmanager.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value; // <-- IMPORT NECESSÁRIO
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tk.jaooo.osmanager.model.AuthenticationResponse;
import tk.jaooo.osmanager.model.DemandanetAuthRequest;
import tk.jaooo.osmanager.services.DemandanetClientService;
import tk.jaooo.osmanager.services.JwtUtil;

@RestController
@RequestMapping("/api")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final DemandanetClientService demandanetClientService;
    private final JwtUtil jwtUtil;

    @Value("${demandanet.idescola}")
    private String idEscola;

    public AuthController(DemandanetClientService demandanetClientService, JwtUtil jwtUtil) {
        this.demandanetClientService = demandanetClientService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/authenticate")
    public ResponseEntity<?> createAuthenticationToken(@RequestBody DemandanetAuthRequest authRequest) {
        logger.info("Tentativa de autenticação para o usuário: {}", authRequest.getUsername());

        try {
            String sessionCookie = demandanetClientService.loginAndGetSessionCookie(
                    authRequest.getUsername(),
                    authRequest.getPassword()
            ).block();

            if (sessionCookie == null || sessionCookie.isEmpty()) {
                throw new RuntimeException("Não foi possível obter o cookie de sessão do Demandanet.");
            }

            logger.info("Login no Demandanet bem-sucedido para o idEscola configurado: {}", idEscola);

            final String jwt = jwtUtil.generateTokenForDemandanetSession(sessionCookie, this.idEscola);

            return ResponseEntity.ok(new AuthenticationResponse(jwt));

        } catch (Exception e) {
            logger.error("Falha na autenticação com o Demandanet para o usuário: {}. Erro: {}", authRequest.getUsername(), e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("Falha na autenticação: Verifique as credenciais do Demandanet ou o serviço pode estar indisponível.");
        }
    }
}
