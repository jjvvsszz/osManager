package tk.jaooo.osmanager.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tk.jaooo.osmanager.model.Tecnico;
import tk.jaooo.osmanager.services.TecnicoService;

import java.util.List;

@RestController
@RequestMapping("/api/tecnicos")
public class TecnicoController {

    private final TecnicoService tecnicoService;

    public TecnicoController(TecnicoService tecnicoService) {
        this.tecnicoService = tecnicoService;
    }

    @PostMapping
    public ResponseEntity<Tecnico> criarTecnico(@RequestBody Tecnico tecnico) {
        Tecnico novoTecnico = tecnicoService.criarTecnico(tecnico);
        return ResponseEntity.ok(novoTecnico);
    }

    @GetMapping
    public ResponseEntity<List<Tecnico>> listarTodos() {
        return ResponseEntity.ok(tecnicoService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Tecnico> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(tecnicoService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Tecnico> atualizarTecnico(@PathVariable Long id, @RequestBody Tecnico dadosTecnico) {
        Tecnico tecnicoAtualizado = tecnicoService.atualizarTecnico(id, dadosTecnico);
        return ResponseEntity.ok(tecnicoAtualizado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarTecnico(@PathVariable Long id) {
        tecnicoService.deletarTecnico(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/restore")
    public ResponseEntity<Tecnico> restaurarTecnico(@PathVariable Long id) {
        Tecnico tecnicoRestaurado = tecnicoService.restaurarTecnico(id);
        return ResponseEntity.ok(tecnicoRestaurado);
    }
}
