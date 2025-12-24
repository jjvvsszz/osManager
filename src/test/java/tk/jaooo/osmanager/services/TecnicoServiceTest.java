package tk.jaooo.osmanager.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Mono;
import tk.jaooo.osmanager.model.Reparo;
import tk.jaooo.osmanager.model.Tecnico;
import tk.jaooo.osmanager.model.dto.TecnicoRegisterDTO;
import tk.jaooo.osmanager.repository.ReparoRepository;
import tk.jaooo.osmanager.repository.TecnicoRepository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TecnicoServiceTest {

    @Mock
    private TecnicoRepository tecnicoRepository;

    @Mock
    private ReparoRepository reparoRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private DemandanetClientService demandanetClientService;

    @Mock
    private OciSecretsService ociSecretsService;

    @InjectMocks
    private TecnicoService tecnicoService;

    // --- TESTES DE CADASTRO PÚBLICO (Auto-Registro) ---

    @Test
    @DisplayName("Deve registrar técnico público com sucesso se credenciais do Demandanet forem válidas")
    void deveRegistrarTecnicoPublicoComSucesso() {
        // Arrange
        TecnicoRegisterDTO dto = new TecnicoRegisterDTO(
                "João da Silva", "joao.silva", "senhaApp123", false,
                "joao_demanda", "senha_demanda"
        );

        // Mock validação de unicidade (Use lenient para evitar UnnecessaryStubbing se a ordem de chamada mudar)
        lenient().when(tecnicoRepository.findByNomeAndRemovidoIsFalse(dto.username())).thenReturn(Optional.empty());

        // Mock Login no Demandanet (Sucesso)
        when(demandanetClientService.loginAndGetSessionCookie(dto.demandanetUser(), dto.demandanetPassword()))
                .thenReturn(Mono.just("PHPSESSID=cookie_valido"));

        // Mock Criação do Segredo no Vault
        when(ociSecretsService.createSecretForUser(dto.username(), dto.demandanetUser(), dto.demandanetPassword()))
                .thenReturn("ocid1.vault.secret.123");

        // Mock Encoder de senha
        when(passwordEncoder.encode(dto.password())).thenReturn("hash_senha_segura");

        // Mock salvamento no banco
        when(tecnicoRepository.save(any(Tecnico.class))).thenAnswer(invocation -> {
            Tecnico t = invocation.getArgument(0);
            t.setId(1L); // Simula ID gerado pelo banco
            return t;
        });

        // Act
        Tecnico resultado = tecnicoService.registrarTecnicoComDemandanet(dto);

        // Assert
        assertNotNull(resultado);
        assertEquals("ocid1.vault.secret.123", resultado.getCredentialKey(), "Deve salvar o OCID do segredo");
        assertEquals("hash_senha_segura", resultado.getPassword(), "A senha deve ser hashada");
        assertNull(resultado.getResponsavel(), "Técnico público não deve ter responsável");

        verify(demandanetClientService).loginAndGetSessionCookie("joao_demanda", "senha_demanda");
        verify(ociSecretsService).createSecretForUser("joao.silva", "joao_demanda", "senha_demanda");
        verify(tecnicoRepository).save(any(Tecnico.class));
    }

    @Test
    @DisplayName("Deve falhar registro público se login no Demandanet falhar")
    void deveFalharRegistroPublicoSeDemandanetInvalido() {
        // Arrange
        TecnicoRegisterDTO dto = new TecnicoRegisterDTO(
                "João", "joao", "123", false, "user_errado", "pass_errada"
        );

        // Lenient: pode não ser chamado se a validação de campos ocorrer antes
        lenient().when(tecnicoRepository.findByNomeAndRemovidoIsFalse(dto.username())).thenReturn(Optional.empty());

        // Simula erro no login legado
        when(demandanetClientService.loginAndGetSessionCookie(anyString(), anyString()))
                .thenReturn(Mono.error(new RuntimeException("Credenciais rejeitadas")));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> tecnicoService.registrarTecnicoComDemandanet(dto));

        // Verifica que NUNCA tentou criar segredo ou salvar no banco
        verify(ociSecretsService, never()).createSecretForUser(anyString(), anyString(), anyString());
        verify(tecnicoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve falhar registro público se campos do Demandanet estiverem vazios")
    void deveFalharRegistroPublicoSemCredenciaisLegado() {
        TecnicoRegisterDTO dto = new TecnicoRegisterDTO(
                "João", "joao", "123", false, null, "" // Campos vazios
        );

        // Lenient aqui é crucial: se o serviço valida NULL antes de ir no banco, este mock não será usado.
        lenient().when(tecnicoRepository.findByNomeAndRemovidoIsFalse(dto.username())).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> tecnicoService.registrarTecnicoComDemandanet(dto));

        verify(demandanetClientService, never()).loginAndGetSessionCookie(any(), any());
    }

    // --- TESTES DE CADASTRO INTERNO (Hierárquico) ---

    @Test
    @DisplayName("Deve criar técnico interno vinculado ao responsável")
    void deveCriarTecnicoInternoComSucesso() {
        // Arrange
        Tecnico chefe = Tecnico.builder().id(99L).nome("Chefe").build();
        TecnicoRegisterDTO dto = new TecnicoRegisterDTO(
                "Estagiário", "estag", "senha123", true, null, null
        );

        lenient().when(tecnicoRepository.findByNomeAndRemovidoIsFalse(dto.username())).thenReturn(Optional.empty());
        when(passwordEncoder.encode("senha123")).thenReturn("hash_estagiario");
        when(tecnicoRepository.save(any(Tecnico.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        Tecnico resultado = tecnicoService.criarTecnicoInterno(dto, chefe);

        // Assert
        assertNotNull(resultado);
        assertNull(resultado.getCredentialKey(), "Técnico interno não deve ter credencial do Demandanet");
        assertTrue(resultado.isEstagiario());
        assertEquals(chefe, resultado.getResponsavel(), "Deve estar vinculado ao chefe");
    }

    @Test
    @DisplayName("Não deve permitir usuário duplicado (validação comum)")
    void naoDeveCriarUsuarioDuplicado() {
        // Arrange
        String usernameDuplicado = "joao.dup";
        TecnicoRegisterDTO dto = new TecnicoRegisterDTO("João", usernameDuplicado, "123", false, null, null);

        // Simula que já existe alguém com esse username
        when(tecnicoRepository.findByUsernameAndRemovidoIsFalse(usernameDuplicado))
                .thenReturn(Optional.of(new Tecnico()));

        // Act & Assert
        // Tenta criar (fluxo interno ou público deve falhar igual)
        assertThrows(IllegalArgumentException.class,
                () -> tecnicoService.criarTecnicoInterno(dto, new Tecnico()));

        verify(tecnicoRepository, never()).save(any());
    }

    // --- TESTES DE REMOÇÃO ---

    @Test
    @DisplayName("Deve fazer Soft Delete (marcar removido) se técnico tiver reparos")
    void deveFazerSoftDeleteSeTiverReparos() {
        Long id = 1L;
        Tecnico tecnico = Tecnico.builder().id(id).removido(false).build();

        when(tecnicoRepository.findById(id)).thenReturn(Optional.of(tecnico));
        // Simula existência de reparos
        when(reparoRepository.findByTecnico_Id(id)).thenReturn(List.of(new Reparo()));

        // Act
        tecnicoService.deletarTecnico(id);

        // Assert
        assertTrue(tecnico.isRemovido());
        verify(tecnicoRepository).save(tecnico); // Atualizou flag
        verify(tecnicoRepository, never()).delete(any()); // Não deletou físico
    }

    @Test
    @DisplayName("Deve fazer Hard Delete (apagar do banco) se técnico não tiver reparos")
    void deveFazerHardDeleteSeSemReparos() {
        Long id = 2L;
        Tecnico tecnico = Tecnico.builder().id(id).removido(false).build();

        when(tecnicoRepository.findById(id)).thenReturn(Optional.of(tecnico));
        when(reparoRepository.findByTecnico_Id(id)).thenReturn(Collections.emptyList());

        // Act
        tecnicoService.deletarTecnico(id);

        // Assert
        verify(tecnicoRepository).delete(tecnico); // Deletou físico
        verify(tecnicoRepository, never()).save(any());
    }
}
