package tk.jaooo.osmanager.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tk.jaooo.osmanager.model.OrdemServico;
import tk.jaooo.osmanager.services.OrdemServicoService;

import java.util.List;

@RestController
@RequestMapping("/api/os")
@Tag(name = "Ordens de Serviço Internas", description = "Gestão de OS no banco de dados local")
public class OrdemServicoController {

    private final OrdemServicoService ordemServicoService;

    public OrdemServicoController(OrdemServicoService ordemServicoService) {
        this.ordemServicoService = ordemServicoService;
    }

    @Operation(summary = "Listar todas as OS internas")
    @GetMapping
    public ResponseEntity<List<OrdemServico>> listarTodas() {
        return ResponseEntity.ok(ordemServicoService.listarTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrdemServico> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(ordemServicoService.buscarPorId(id));
    }

    @Operation(summary = "Abrir nova OS interna")
    @PostMapping
    public ResponseEntity<OrdemServico> abrirOrdemServico(@RequestBody OrdemServico os) {
        OrdemServico novaOs = ordemServicoService.abrirOrdemServico(os);
        return ResponseEntity.ok(novaOs);
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrdemServico> atualizarOrdemServico(@PathVariable Long id, @RequestBody OrdemServico dadosOs) {
        OrdemServico osAtualizada = ordemServicoService.atualizarOrdemServico(id, dadosOs);
        return ResponseEntity.ok(osAtualizada);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarOrdemServico(@PathVariable Long id) {
        ordemServicoService.deletarOrdemServico(id);
        return ResponseEntity.noContent().build();
    }
}
