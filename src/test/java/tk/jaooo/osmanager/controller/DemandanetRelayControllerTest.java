package tk.jaooo.osmanager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
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
import tk.jaooo.osmanager.config.SecurityConfig;
import tk.jaooo.osmanager.model.Tecnico;
import tk.jaooo.osmanager.model.dto.ConcludeOrderRequestDTO;
import tk.jaooo.osmanager.services.DemandanetClientService;
import tk.jaooo.osmanager.services.DemandanetParserService;
import tk.jaooo.osmanager.services.DemandanetSessionManager;
import tk.jaooo.osmanager.services.JwtUtil;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DemandanetRelayController.class)
@AutoConfigureMockMvc
@Import({JacksonConfig.class, SecurityConfig.class})
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

    @BeforeEach
    void setUp() throws ServletException, IOException {
        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            HttpServletRequest request = invocation.getArgument(0);
            HttpServletResponse response = invocation.getArgument(1);
            chain.doFilter(request, response);
            return null;
        }).when(jwtRequestFilter).doFilter(any(), any(), any());
    }

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
    void deveFalharSeOsIsBlank() throws Exception {
        ConcludeOrderRequestDTO dto = new ConcludeOrderRequestDTO("", "Obs");

        Tecnico tecnicoMock = new Tecnico();
        tecnicoMock.setId(1L);
        tecnicoMock.setUsername("teste");

        when(sessionManager.resolveCredentialOwner(any())).thenReturn(tecnicoMock);
        when(sessionManager.getSessionForOwner(any())).thenReturn("PHPSESSID=abc");
        when(clientService.concludeOrder(any(), any(), any())).thenReturn(Mono.just("Sucesso"));
        when(sessionManager.isSessionExpiredResponse(any())).thenReturn(false);

        mockMvc.perform(post("/api/demandanet/concluir")
                        .with(user(tecnicoMock))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest()); // Espera 400
    }

    @Test
    void deveFalharSeObsIsNull() throws Exception {
        ConcludeOrderRequestDTO dto = new ConcludeOrderRequestDTO("12345", null);

        Tecnico tecnicoMock = new Tecnico();
        tecnicoMock.setId(1L);
        tecnicoMock.setUsername("teste");

        when(sessionManager.resolveCredentialOwner(any())).thenReturn(tecnicoMock);
        when(sessionManager.getSessionForOwner(any())).thenReturn("PHPSESSID=abc");
        when(clientService.concludeOrder(any(), any(), any())).thenReturn(Mono.just("Sucesso"));
        when(sessionManager.isSessionExpiredResponse(any())).thenReturn(false);

        mockMvc.perform(post("/api/demandanet/concluir")
                        .with(user(tecnicoMock))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest()); // Espera 400
    }
}
