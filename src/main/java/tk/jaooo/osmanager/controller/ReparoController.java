package tk.jaooo.osmanager.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tk.jaooo.osmanager.model.Reparo;
import tk.jaooo.osmanager.repository.TecnicoRepository;
import tk.jaooo.osmanager.services.ReparoService;

import java.util.List;

@RestController
@RequestMapping("/api/reparos")
public class ReparoController {

    private final ReparoService reparoService;

    public ReparoController(ReparoService reparoService, TecnicoRepository tecnicoRepository) {
        this.reparoService = reparoService;
    }

    @PostMapping
    public ResponseEntity<Reparo> adicionarReparo(@RequestBody ReparoRequest reparoRequest) {
        Reparo novoReparo = reparoService.adicionarReparo(
                reparoRequest.getOsId(),
                reparoRequest.getTecnicoId(),
                reparoRequest.getDescricaoReparo()
        );
        return ResponseEntity.ok(novoReparo);
    }

    @GetMapping("/os/{osId}")
    public ResponseEntity<List<Reparo>> listarReparosPorOs(@PathVariable Long osId) {
        return ResponseEntity.ok(reparoService.listarReparosPorOrdemServico(osId));
    }

    static class ReparoRequest {
        private Long osId;
        private Long tecnicoId;
        private String descricaoReparo;

        // Getters e Setters
        public Long getOsId() { return osId; }
        public void setOsId(Long osId) { this.osId = osId; }
        public Long getTecnicoId() { return tecnicoId; }
        public void setTecnicoId(Long tecnicoId) { this.tecnicoId = tecnicoId; }
        public String getDescricaoReparo() { return descricaoReparo; }
        public void setDescricaoReparo(String descricaoReparo) { this.descricaoReparo = descricaoReparo; }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Reparo> atualizarReparo(@PathVariable Long id, @RequestBody ReparoRequest reparoRequest) {
        Reparo reparoAtualizado = reparoService.atualizarReparo(id, reparoRequest.getTecnicoId(), reparoRequest.getDescricaoReparo());
        return ResponseEntity.ok(reparoAtualizado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarReparo(@PathVariable Long id) {
        reparoService.deletarReparo(id);
        return ResponseEntity.noContent().build();
    }
}
