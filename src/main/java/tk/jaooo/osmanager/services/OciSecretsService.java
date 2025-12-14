package tk.jaooo.osmanager.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.secrets.SecretsClient;
import com.oracle.bmc.secrets.model.Base64SecretBundleContentDetails;
import com.oracle.bmc.secrets.requests.GetSecretBundleRequest;
import com.oracle.bmc.secrets.responses.GetSecretBundleResponse;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OciSecretsService {

    private static final Logger logger = LoggerFactory.getLogger(OciSecretsService.class);

    private final SecretsClient secretsClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String masterSecretOcid;

    // Cache simples: JSON Mestre Map<Key, Credentials>
    private Map<String, DemandanetCredentials> cachedCredentials;
    private Instant lastCacheUpdate = Instant.MIN;

    // Tempo de cache (ex: 10 minutos) para evitar chamadas excessivas ao Vault
    private static final int CACHE_MINUTES = 10;

    public OciSecretsService(
            @Value("${oci.config.path:~/.oci/config}") String configPath,
            @Value("${oci.config.profile:DEFAULT}") String profile,
            @Value("${oci.vault.master-secret-id}") String masterSecretOcid) throws IOException { // Novo ID global

        this.masterSecretOcid = masterSecretOcid;

        String resolvedPath = configPath;
        if (configPath.startsWith("~")) {
            resolvedPath = System.getProperty("user.home") + configPath.substring(1);
        }

        var provider = new ConfigFileAuthenticationDetailsProvider(resolvedPath, profile);
        this.secretsClient = SecretsClient.builder().build(provider);
    }

    // Busca uma credencial específica dentro do JSON Mestre.
    public synchronized DemandanetCredentials getCredentialByKey(String key) {
        if (shouldRefreshCache()) {
            refreshCache();
        }

        DemandanetCredentials creds = cachedCredentials.get(key);
        if (creds == null) {
            // Se não achou, pode ser que o cache esteja velho e adicionaram agora. Força refresh 1 vez.
            logger.warn("Chave '{}' não encontrada no cache. Forçando atualização do Vault...", key);
            refreshCache();
            creds = cachedCredentials.get(key);
        }

        if (creds == null) {
            throw new IllegalArgumentException("Credencial não encontrada no Vault para a chave: " + key);
        }

        return creds;
    }

    private boolean shouldRefreshCache() {
        return cachedCredentials == null || Instant.now().isAfter(lastCacheUpdate.plus(CACHE_MINUTES, ChronoUnit.MINUTES));
    }

    private void refreshCache() {
        logger.info("Atualizando cache de credenciais do Vault (Secret Mestre)...");
        try {
            GetSecretBundleRequest request = GetSecretBundleRequest.builder()
                    .secretId(masterSecretOcid)
                    .stage(GetSecretBundleRequest.Stage.Current)
                    .build();

            GetSecretBundleResponse response = secretsClient.getSecretBundle(request);
            var contentDetails = (Base64SecretBundleContentDetails) response.getSecretBundle().getSecretBundleContent();
            byte[] decoded = Base64.decodeBase64(contentDetails.getContent());
            String jsonString = new String(decoded);

            // Parseia o JSON grandão: { "adm1": {"username": "...", "password": "..."}, "adm2": ... }
            this.cachedCredentials = objectMapper.readValue(jsonString, new TypeReference<ConcurrentHashMap<String, DemandanetCredentials>>() {});
            this.lastCacheUpdate = Instant.now();

        } catch (Exception e) {
            throw new RuntimeException("Falha ao atualizar credenciais do OCI Vault.", e);
        }
    }

    public record DemandanetCredentials(String username, String password) {}
}
