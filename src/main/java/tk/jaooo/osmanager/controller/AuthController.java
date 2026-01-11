package tk.jaooo.osmanager.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tk.jaooo.osmanager.model.dto.AuthenticationResponseDTO;
import tk.jaooo.osmanager.model.Tecnico;
import tk.jaooo.osmanager.model.dto.DemandanetAuthRequestDTO;
import tk.jaooo.osmanager.repository.TecnicoRepository;
import tk.jaooo.osmanager.services.DemandanetSessionManager;
import tk.jaooo.osmanager.services.JwtUtil;

@RestController
@RequestMapping("/api")
@Tag(name = "Autenticação", description = "Endpoints para login e obtenção de token JWT")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final DemandanetSessionManager sessionManager;
    private final JwtUtil jwtUtil;
    private final TecnicoRepository tecnicoRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${demandanet.idescola}")
    private String idEscola;

    public AuthController(DemandanetSessionManager sessionManager,
                          JwtUtil jwtUtil,
                          TecnicoRepository tecnicoRepository,
                          PasswordEncoder passwordEncoder) {
        this.sessionManager = sessionManager;
        this.jwtUtil = jwtUtil;
        this.tecnicoRepository = tecnicoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Operation(
            summary = "Realizar Login",
            description = "Autentica o usuário localmente e no sistema legado (Demandanet). " +
                    "Retorna um token JWT contendo a sessão do legado criptografada."
    )
    @ApiResponse(responseCode = "200", description = "Autenticação realizada com sucesso")
    @ApiResponse(responseCode = "401", description = "Usuário ou senha inválidos")
    @ApiResponse(responseCode = "502", description = "Falha de comunicação com o sistema legado")
    @PostMapping("/authenticate")
    public ResponseEntity<?> createAuthenticationToken(@RequestBody @Valid DemandanetAuthRequestDTO authRequest) {
        logger.info("Tentativa de autenticação local para: {}", authRequest.username());

        // 1. Busca o usuário no banco local
        var tecnicoOpt = tecnicoRepository.findByUsernameAndRemovidoIsFalse(authRequest.username());

        if (tecnicoOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuário não encontrado.");
        }

        Tecnico tecnico = tecnicoOpt.get();

        // 2. Valida a senha local (BCrypt)
        if (!passwordEncoder.matches(authRequest.password(), tecnico.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Senha incorreta.");
        }

        try {
            // 3. Resolve quem é o dono da credencial do legado (o próprio usuário ou o chefe)
            Tecnico donoCredencial = sessionManager.resolveCredentialOwner(tecnico);

            // 4. Obtém (ou cria) a sessão no Demandanet usando as credenciais do cofre
            // O sessionManager já lida com OCI Vault e cache de sessão
            String sessionCookie = sessionManager.getSessionForOwner(donoCredencial);

            // 5. Gera o JWT
            final String jwt = jwtUtil.generateToken(tecnico.getUsername(), sessionCookie, this.idEscola);

            logger.info("Login realizado com sucesso para: {}", tecnico.getNome());

            return ResponseEntity.ok(new AuthenticationResponseDTO(jwt));

        } catch (Exception e) {
            logger.error("Erro ao obter sessão do sistema legado", e);
            return ResponseEntity
                    .status(HttpStatus.BAD_GATEWAY)
                    .body("Login local OK, mas falha ao conectar no Demandanet: " + e.getMessage());
        }
    }
}
