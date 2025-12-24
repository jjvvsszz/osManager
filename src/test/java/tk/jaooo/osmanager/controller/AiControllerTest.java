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
import tk.jaooo.osmanager.config.JacksonConfig;
import tk.jaooo.osmanager.config.JwtRequestFilter;
import tk.jaooo.osmanager.model.dto.GenerateObservationRequestDTO;
import tk.jaooo.osmanager.services.GeminiService;
import tk.jaooo.osmanager.services.JwtUtil;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AiController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(JacksonConfig.class)
class AiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GeminiService geminiService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private JwtRequestFilter jwtRequestFilter;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void deveGerarObservacaoComSucesso() throws Exception {
        GenerateObservationRequestDTO request = new GenerateObservationRequestDTO("Tela quebrada", "Caiu no chão");
        String respostaEsperada = "Realizada troca de tela devido a queda.";

        when(geminiService.gerarObservacaoTecnica("Tela quebrada", "Caiu no chão"))
                .thenReturn(respostaEsperada);

        mockMvc.perform(post("/api/ai/generate-observation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string(respostaEsperada));
    }

    @Test
    void deveRetornarBadRequestSeFaltarDados() throws Exception {
        GenerateObservationRequestDTO request = new GenerateObservationRequestDTO(null, "Descricao");

        mockMvc.perform(post("/api/ai/generate-observation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
