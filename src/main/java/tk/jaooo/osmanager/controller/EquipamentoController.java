package tk.jaooo.osmanager.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tk.jaooo.osmanager.model.Equipamento;
import tk.jaooo.osmanager.services.EquipamentoService;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/equipamentos")
public class EquipamentoController {

    private final EquipamentoService equipamentoService;

    public EquipamentoController(EquipamentoService equipamentoService) {
        this.equipamentoService = equipamentoService;
    }

    @GetMapping
    public ResponseEntity<List<Equipamento>> listarTodos() {
        List<Equipamento> equipamentos = equipamentoService.listarTodos();
        return ResponseEntity.ok(equipamentos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Equipamento> buscarPorId(@PathVariable Long id) {
        Equipamento equipamento = equipamentoService.buscarPorId(id);
        return ResponseEntity.ok(equipamento);
    }

    @GetMapping("/patrimonio/{patrimonio}")
    public ResponseEntity<Equipamento> buscarPorPatrimonio(@PathVariable String patrimonio) {
        Equipamento equipamento = equipamentoService.buscarPorPatrimonio(patrimonio);
        return ResponseEntity.ok(equipamento);
    }

    @PostMapping
    public ResponseEntity<Equipamento> criarEquipamento(@RequestBody Equipamento equipamento) {
        Equipamento novoEquipamento = equipamentoService.criarEquipamento(equipamento);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest().path("/{id}")
                .buildAndExpand(novoEquipamento.getId()).toUri();

        return ResponseEntity.created(location).body(novoEquipamento);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Equipamento> atualizarEquipamento(@PathVariable Long id, @RequestBody Equipamento dadosEquipamento) {
        Equipamento equipamentoAtualizado = equipamentoService.atualizarEquipamento(id, dadosEquipamento);
        return ResponseEntity.ok(equipamentoAtualizado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarEquipamento(@PathVariable Long id) {
        equipamentoService.deletarEquipamento(id);
        return ResponseEntity.noContent().build();
    }
}
