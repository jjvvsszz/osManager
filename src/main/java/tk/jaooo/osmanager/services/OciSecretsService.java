package tk.jaooo.osmanager.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.secrets.SecretsClient;
import com.oracle.bmc.secrets.model.Base64SecretBundleContentDetails;
import com.oracle.bmc.secrets.requests.GetSecretBundleRequest;
import com.oracle.bmc.vault.VaultsClient; // NOVO CLIENTE
import com.oracle.bmc.vault.model.Base64SecretContentDetails;
import com.oracle.bmc.vault.model.CreateSecretDetails;
import com.oracle.bmc.vault.requests.CreateSecretRequest;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

@Service
public class OciSecretsService {

    private final SecretsClient secretsClient;
    private final VaultsClient vaultsClient;
    private final ObjectMapper objectMapper;

    @Value("${oci.vault.compartment-id}")
    private String compartmentId;

    @Value("${oci.vault.vault-id}")
    private String vaultId;

    @Value("${oci.vault.encryption-key-id}")
    private String encryptionKeyId;

    public OciSecretsService(
            @Value("${oci.config.path:~/.oci/config}") String configPath,
            @Value("${oci.config.profile:DEFAULT}") String profile,
            ObjectMapper objectMapper) throws IOException {

        this.objectMapper = objectMapper;

        String resolvedPath = configPath.replace("~", System.getProperty("user.home"));
        var provider = new ConfigFileAuthenticationDetailsProvider(resolvedPath, profile);

        this.secretsClient = SecretsClient.builder().build(provider);
        this.vaultsClient = VaultsClient.builder().build(provider);
    }

    /**
     * Cria um novo segredo no Vault e retorna o OCID.
     */
    public String createSecretForUser(String username, String demUser, String demPass) {
        try {
            // 1. Prepara o conteúdo JSON
            Map<String, String> contentMap = Map.of(
                    "username", demUser,
                    "password", demPass
            );
            String jsonContent = objectMapper.writeValueAsString(contentMap);

            // 2. Codifica em Base64 (Exigência do OCI)
            String base64Content = Base64.encodeBase64String(jsonContent.getBytes());

            // 3. Monta a requisição
            CreateSecretDetails details = CreateSecretDetails.builder()
                    .compartmentId(compartmentId)
                    .vaultId(vaultId)
                    .keyId(encryptionKeyId)
                    .secretName("osmanager-cred-" + username + "-" + System.currentTimeMillis()) // Nome único
                    .description("Credenciais Demandanet para " + username)
                    .secretContent(Base64SecretContentDetails.builder()
                            .content(base64Content)
                            .stage(Base64SecretContentDetails.Stage.Current)
                            .build())
                    .build();

            CreateSecretRequest request = CreateSecretRequest.builder()
                    .createSecretDetails(details)
                    .build();

            // 4. Envia e retorna o ID
            var response = vaultsClient.createSecret(request);
            return response.getSecret().getId();

        } catch (Exception e) {
            throw new RuntimeException("Erro ao criar segredo no OCI Vault: " + e.getMessage(), e);
        }
    }

    /**
     * Busca credencial usando o OCID (que estará salvo no banco no campo credentialKey)
     */
    public DemandanetCredentials getCredentialByOcid(String secretOcid) {
        try {
            GetSecretBundleRequest request = GetSecretBundleRequest.builder()
                    .secretId(secretOcid)
                    .stage(GetSecretBundleRequest.Stage.Current)
                    .build();

            var response = secretsClient.getSecretBundle(request);
            var contentDetails = (Base64SecretBundleContentDetails) response.getSecretBundle().getSecretBundleContent();

            byte[] decoded = Base64.decodeBase64(contentDetails.getContent());

            // Lê o JSON individual: {"username": "...", "password": "..."}
            return objectMapper.readValue(decoded, DemandanetCredentials.class);

        } catch (Exception e) {
            throw new RuntimeException("Falha ao ler segredo (" + secretOcid + ") do OCI Vault.", e);
        }
    }

    public record DemandanetCredentials(String username, String password) {}
}
