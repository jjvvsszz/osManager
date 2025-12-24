package tk.jaooo.osmanager.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tk.jaooo.osmanager.model.Tecnico;
import tk.jaooo.osmanager.model.dto.TecnicoRegisterDTO;
import tk.jaooo.osmanager.services.TecnicoService;

import java.util.List;

@RestController
@RequestMapping("/api/tecnicos")
public class TecnicoController {

    private final TecnicoService tecnicoService;

    public TecnicoController(TecnicoService tecnicoService) {
        this.tecnicoService = tecnicoService;
    }

    /**
     * Endpoint PÚBLICO: Qualquer um pode chamar, mas precisa passar credenciais válidas do Demandanet.
     */
    @PostMapping("/register")
    public ResponseEntity<Tecnico> registrarPublico(@RequestBody @Valid TecnicoRegisterDTO dto) {
        Tecnico novoTecnico = tecnicoService.registrarTecnicoComDemandanet(dto);
        return ResponseEntity.ok(novoTecnico);
    }

    /**
     * Endpoint PROTEGIDO: Criação interna. O "responsável" será o usuário logado.
     */
    @PostMapping
    public ResponseEntity<Tecnico> criarInterno(
            @RequestBody @Valid TecnicoRegisterDTO dto,
            @AuthenticationPrincipal Tecnico usuarioLogado) {

        Tecnico novoTecnico = tecnicoService.criarTecnicoInterno(dto, usuarioLogado);
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
