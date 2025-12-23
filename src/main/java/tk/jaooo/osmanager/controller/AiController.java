package tk.jaooo.osmanager.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tk.jaooo.osmanager.model.dto.GenerateObservationRequestDTO;
import tk.jaooo.osmanager.services.GeminiService;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final GeminiService geminiService;

    public AiController(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    @PostMapping("/generate-observation")
    public ResponseEntity<String> generateObservation(@RequestBody GenerateObservationRequestDTO request) {
        if (request.defeito() == null || request.descricao() == null) {
            return ResponseEntity.badRequest().body("Defeito e Descrição são obrigatórios.");
        }

        String observacao = geminiService.gerarObservacaoTecnica(request.defeito(), request.descricao());
        return ResponseEntity.ok(observacao);
    }
}
