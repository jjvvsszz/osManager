package tk.jaooo.osmanager.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tk.jaooo.osmanager.model.dto.GenerateObservationRequestDTO;
import tk.jaooo.osmanager.services.GeminiService;

@RestController
@RequestMapping("/api/ai")
@Tag(name = "Inteligência Artificial", description = "Auxílio na escrita de laudos técnicos")
public class AiController {

    private final GeminiService geminiService;

    public AiController(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    @Operation(
            summary = "Gerar Observação Técnica",
            description = "Utiliza o Google Gemini para reescrever o defeito e a descrição em uma linguagem técnica formal e sucinta para encerramento de OS."
    )
    @PostMapping("/generate-observation")
    public ResponseEntity<String> generateObservation(@RequestBody GenerateObservationRequestDTO request) {
        if (request.defeito() == null || request.descricao() == null) {
            return ResponseEntity.badRequest().body("Defeito e Descrição são obrigatórios.");
        }

        String observacao = geminiService.gerarObservacaoTecnica(request.defeito(), request.descricao());
        return ResponseEntity.ok(observacao);
    }
}
