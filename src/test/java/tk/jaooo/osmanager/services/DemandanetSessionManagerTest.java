package tk.jaooo.osmanager.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import tk.jaooo.osmanager.model.Tecnico;
import tk.jaooo.osmanager.repository.TecnicoRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DemandanetSessionManagerTest {

    @Mock
    private DemandanetClientService clientService;

    @Mock
    private OciSecretsService ociSecretsService;

    @Mock
    private TecnicoRepository tecnicoRepository;

    @InjectMocks
    private DemandanetSessionManager sessionManager;

    @Test
    void deveDetectarErroSqlComoSessaoExpirada() {
        String htmlErro = "<html><b>Erro no comando sql</b>: SELECT...</html>";
        assertTrue(sessionManager.isSessionExpiredResponse(htmlErro));
    }

    @Test
    void deveDetectarRedirecionamentoLoginComoSessaoExpirada() {
        String htmlLogin = "<script>window.location='telaAcesso.php'</script>";
        assertTrue(sessionManager.isSessionExpiredResponse(htmlLogin));
    }

    @Test
    void naoDeveDetectarJsonNormalComoErro() {
        String json = "{\"status\": \"ok\", \"data\": []}";
        assertFalse(sessionManager.isSessionExpiredResponse(json));
    }

    @Test
    void deveRenovarSessaoCorretamente() {
        // Cenário
        Tecnico admin = Tecnico.builder().id(1L).credentialKey("adm_master").build();
        OciSecretsService.DemandanetCredentials creds =
                new OciSecretsService.DemandanetCredentials("user", "pass");

        // Mocks
        when(ociSecretsService.getCredentialByOcid("adm_master")).thenReturn(creds);
        when(clientService.loginAndGetSessionCookie("user", "pass"))
                .thenReturn(Mono.just("PHPSESSID=novo_cookie_123"));

        // Ação
        String cookie = sessionManager.refreshSessionForOwner(admin);

        // Asserts
        assertEquals("PHPSESSID=novo_cookie_123", cookie);
        verify(clientService).loginAndGetSessionCookie("user", "pass");
    }
}
