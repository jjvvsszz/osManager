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
import tk.jaooo.osmanager.model.Equipamento;
import tk.jaooo.osmanager.services.EquipamentoService;
import tk.jaooo.osmanager.services.JwtUtil;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
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
    private JwtUtil jwtUtil;

    @MockitoBean
    private JwtRequestFilter jwtRequestFilter;

    @Autowired
    private ObjectMapper objectMapper;

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
}
