package tk.jaooo.osmanager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import reactor.core.publisher.Mono;
import tk.jaooo.osmanager.config.JacksonConfig;
import tk.jaooo.osmanager.config.JwtRequestFilter;
import tk.jaooo.osmanager.model.Tecnico;
import tk.jaooo.osmanager.model.dto.ConcludeOrderRequestDTO;
import tk.jaooo.osmanager.services.DemandanetClientService;
import tk.jaooo.osmanager.services.DemandanetParserService;
import tk.jaooo.osmanager.services.DemandanetSessionManager;
import tk.jaooo.osmanager.services.JwtUtil;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DemandanetRelayController.class)
@AutoConfigureMockMvc
@Import(JacksonConfig.class)
class DemandanetRelayControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DemandanetSessionManager sessionManager;

    @MockitoBean
    private DemandanetParserService parserService;

    @MockitoBean
    private DemandanetClientService clientService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private JwtRequestFilter jwtRequestFilter;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void deveConcluirOrdemComSucesso() throws Exception {
        ConcludeOrderRequestDTO dto = new ConcludeOrderRequestDTO("12345", "Serviço finalizado");

        Tecnico tecnicoMock = new Tecnico();
        tecnicoMock.setId(1L);
        tecnicoMock.setUsername("teste");

        // Mock da sessão
        when(sessionManager.resolveCredentialOwner(any())).thenReturn(tecnicoMock);
        when(sessionManager.getSessionForOwner(any())).thenReturn("PHPSESSID=abc");

        // Mock da chamada ao legado
        when(clientService.concludeOrder("PHPSESSID=abc", "12345", "Serviço finalizado"))
                .thenReturn(Mono.just("Sucesso"));

        // Mock da verificação de expiração
        when(sessionManager.isSessionExpiredResponse("Sucesso")).thenReturn(false);

        mockMvc.perform(post("/api/demandanet/concluir")
                        .with(user(tecnicoMock)) // Simula usuário logado
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    void deveFalharSeDtoInvalido() throws Exception {
        ConcludeOrderRequestDTO dto = new ConcludeOrderRequestDTO("", ""); // Dados inválidos

        mockMvc.perform(post("/api/demandanet/concluir")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest()); // 400 devido ao @Valid
    }
}
