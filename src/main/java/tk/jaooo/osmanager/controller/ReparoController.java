package tk.jaooo.osmanager.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tk.jaooo.osmanager.model.Reparo;
import tk.jaooo.osmanager.model.dto.ReparoRequestDTO;
import tk.jaooo.osmanager.services.ReparoService;

import java.util.List;

@RestController
@RequestMapping("/api/reparos")
public class ReparoController {

    private final ReparoService reparoService;

    public ReparoController(ReparoService reparoService) {
        this.reparoService = reparoService;
    }

    @PostMapping
    public ResponseEntity<Reparo> adicionarReparo(@RequestBody ReparoRequestDTO reparoRequestDTO) {
        Reparo novoReparo = reparoService.adicionarReparo(
                reparoRequestDTO.osId(),
                reparoRequestDTO.tecnicoId(),
                reparoRequestDTO.descricaoReparo()
        );
        return ResponseEntity.ok(novoReparo);
    }

    @GetMapping("/os/{osId}")
    public ResponseEntity<List<Reparo>> listarReparosPorOs(@PathVariable Long osId) {
        return ResponseEntity.ok(reparoService.listarReparosPorOrdemServico(osId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Reparo> atualizarReparo(@PathVariable Long id, @RequestBody ReparoRequestDTO reparoRequestDTO) {
        Reparo reparoAtualizado = reparoService.atualizarReparo(id, reparoRequestDTO.tecnicoId(), reparoRequestDTO.descricaoReparo());
        return ResponseEntity.ok(reparoAtualizado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarReparo(@PathVariable Long id) {
        reparoService.deletarReparo(id);
        return ResponseEntity.noContent().build();
    }
}
