package tk.jaooo.osmanager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import reactor.core.publisher.Mono;
import tk.jaooo.osmanager.config.JacksonConfig;
import tk.jaooo.osmanager.config.JwtRequestFilter;
import tk.jaooo.osmanager.model.Equipamento;
import tk.jaooo.osmanager.model.OrdemServico;
import tk.jaooo.osmanager.model.Reparo;
import tk.jaooo.osmanager.model.Tecnico;
import tk.jaooo.osmanager.model.dto.ConsultedOrderDTO;
import tk.jaooo.osmanager.services.*;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EquipamentoController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(JacksonConfig.class)
class EquipamentoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EquipamentoService equipamentoService;

    @MockitoBean
    private DemandanetSessionManager sessionManager;

    @MockitoBean
    private DemandanetClientService demandanetClientService;

    @MockitoBean
    private OrdemServicoService ordemServicoService;

    @MockitoBean
    private ReparoService reparoService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private JwtRequestFilter jwtRequestFilter;

    @Autowired
    private ObjectMapper objectMapper;

    private Tecnico mockTecnico;

    @BeforeEach
    void setUp() {
        mockTecnico = new Tecnico();
        mockTecnico.setId(1L);
        mockTecnico.setUsername("admin");
    }

    @Test
    void deveListarEquipamentos() throws Exception {
        when(equipamentoService.listarTodos()).thenReturn(List.of(
                Equipamento.builder().id(1L).nome("PC Dell").build()
        ));

        mockMvc.perform(get("/api/equipamentos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("PC Dell"));
    }

    @Test
    void deveCriarEquipamento() throws Exception {
        Equipamento novo = Equipamento.builder().patrimonio("123").nome("PC").build();
        Equipamento salvo = Equipamento.builder().id(10L).patrimonio("123").nome("PC").build();

        when(equipamentoService.criarEquipamento(any(Equipamento.class))).thenReturn(salvo);

        mockMvc.perform(post("/api/equipamentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(novo)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    @DisplayName("Deve retornar histórico completo (Local + Legado) com sucesso")
    void deveConsultarHistoricoComSucesso() throws Exception {
        String patrimonio = "P-100";

        when(ordemServicoService.buscarPorPatrimonio(patrimonio))
                .thenReturn(List.of(OrdemServico.builder().id(1L).numeroOs("LOC-001").build()));

        when(reparoService.listarReparosPorPatrimonio(patrimonio))
                .thenReturn(List.of(Reparo.builder().id(2L).descricaoReparo("Troca de fonte").build()));

        when(sessionManager.resolveCredentialOwner(any())).thenReturn(mockTecnico);
        when(sessionManager.getSessionForOwner(mockTecnico)).thenReturn("COOKIE_VALIDO");

        List<ConsultedOrderDTO> ordensLegado = List.of(
                new ConsultedOrderDTO("LEG-999", "Escola A", "Manutenção", "Quebrado", patrimonio, "Resp")
        );
        when(demandanetClientService.searchOrdersByPatrimony("COOKIE_VALIDO", patrimonio))
                .thenReturn(Mono.just(ordensLegado));

        mockMvc.perform(get("/api/equipamentos/historico/{patrimonio}", patrimonio)
                        .with(user(mockTecnico)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ordensInternas[0].numeroOs").value("LOC-001"))
                .andExpect(jsonPath("$.reparos[0].descricao").value("Troca de fonte"))
                .andExpect(jsonPath("$.ordensLegado[0].id").value("LEG-999"));
    }

    @Test
    @DisplayName("Deve renovar sessão e tentar novamente se busca no legado falhar na primeira tentativa")
    void deveRenovarSessaoSeLegadoFalhar() throws Exception {
        String patrimonio = "P-200";

        when(ordemServicoService.buscarPorPatrimonio(anyString())).thenReturn(Collections.emptyList());
        when(reparoService.listarReparosPorPatrimonio(anyString())).thenReturn(Collections.emptyList());

        when(sessionManager.resolveCredentialOwner(any())).thenReturn(mockTecnico);
        when(sessionManager.getSessionForOwner(mockTecnico)).thenReturn("COOKIE_EXPIRADO");
        when(sessionManager.refreshSessionForOwner(mockTecnico)).thenReturn("COOKIE_NOVO");

        when(demandanetClientService.searchOrdersByPatrimony("COOKIE_EXPIRADO", patrimonio))
                .thenThrow(new RuntimeException("Sessão inválida"));

        List<ConsultedOrderDTO> ordensLegado = List.of(new ConsultedOrderDTO("LEG-NEW", "Escola B", "Rede", "Sem net", patrimonio, "Eu"));
        when(demandanetClientService.searchOrdersByPatrimony("COOKIE_NOVO", patrimonio))
                .thenReturn(Mono.just(ordensLegado));

        mockMvc.perform(get("/api/equipamentos/historico/{patrimonio}", patrimonio)
                        .with(user(mockTecnico)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ordensLegado[0].id").value("LEG-NEW"));

        verify(sessionManager).refreshSessionForOwner(mockTecnico); // Garante que houve renovação
        verify(demandanetClientService, times(2)).searchOrdersByPatrimony(anyString(), eq(patrimonio));
    }

    @Test
    @DisplayName("Deve retornar Erro 500 se o legado falhar mesmo após renovação")
    void deveRetornar500SeLegadoFalharTotalmente() throws Exception {
        String patrimonio = "P-ERR";

        when(sessionManager.resolveCredentialOwner(any())).thenReturn(mockTecnico);
        when(sessionManager.getSessionForOwner(mockTecnico)).thenReturn("COOKIE_EXPIRADO");
        when(sessionManager.refreshSessionForOwner(mockTecnico)).thenReturn("COOKIE_NOVO");

        when(demandanetClientService.searchOrdersByPatrimony(anyString(), anyString()))
                .thenThrow(new RuntimeException("Sistema legado fora do ar"));

        mockMvc.perform(get("/api/equipamentos/historico/{patrimonio}", patrimonio)
                        .with(user(mockTecnico)))
                .andExpect(status().isInternalServerError());
    }
}
