package tk.jaooo.osmanager.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tk.jaooo.osmanager.model.Tecnico;
import tk.jaooo.osmanager.repository.TecnicoRepository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DemandanetSessionManager {

    private static final Logger logger = LoggerFactory.getLogger(DemandanetSessionManager.class);

    // Cache: ID do Técnico -> Cookie PHPSESSID
    private final Map<Long, String> sessionPool = new ConcurrentHashMap<>();

    private final DemandanetClientService clientService;
    private final OciSecretsService ociSecretsService;
    private final TecnicoRepository tecnicoRepository;

    public DemandanetSessionManager(DemandanetClientService clientService,
                                    OciSecretsService ociSecretsService,
                                    TecnicoRepository tecnicoRepository) {
        this.clientService = clientService;
        this.ociSecretsService = ociSecretsService;
        this.tecnicoRepository = tecnicoRepository;
    }

    public Tecnico resolveCredentialOwner(Tecnico solicitante) {
        // Verifica se tem a chave (String) preenchida
        if (solicitante.getCredentialKey() != null && !solicitante.getCredentialKey().isBlank()) {
            return solicitante;
        }

        if (solicitante.getResponsavel() != null) {
            return tecnicoRepository.findById(solicitante.getResponsavel().getId())
                    .filter(t -> t.getCredentialKey() != null)
                    .orElseThrow(() -> new IllegalStateException("Responsável sem credencial configurada."));
        }

        throw new IllegalStateException("Usuário sem credencial e sem responsável.");
    }

    public String getSessionForOwner(Tecnico owner) {
        return sessionPool.computeIfAbsent(owner.getId(), id -> performLogin(owner));
    }

    public String refreshSessionForOwner(Tecnico owner) {
        logger.info("Renovando sessão para owner ID {} (Key: {})", owner.getId(), owner.getCredentialKey());
        sessionPool.remove(owner.getId());
        return getSessionForOwner(owner);
    }

    private String performLogin(Tecnico owner) {
        // Agora busca pela chave string (ex: "adm_ti")
        var credentials = ociSecretsService.getCredentialByKey(owner.getCredentialKey());

        String cookie = clientService.loginAndGetSessionCookie(credentials.username(), credentials.password())
                .block();

        if (cookie == null || cookie.isBlank()) {
            throw new RuntimeException("Login falhou para chave: " + owner.getCredentialKey());
        }

        return cookie;
    }

    public boolean isSessionExpiredResponse(String body) {
        if (body == null) return false;
        return body.contains("Erro no comando sql") ||
                body.contains("You have an error in your SQL syntax") ||
                body.contains("telaAcesso.php");
    }
}
