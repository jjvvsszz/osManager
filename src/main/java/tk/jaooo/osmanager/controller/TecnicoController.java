package tk.jaooo.osmanager.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Gestão de Técnicos", description = "Gerenciamento de usuários, estagiários e hierarquia")
public class TecnicoController {

    private final TecnicoService tecnicoService;

    public TecnicoController(TecnicoService tecnicoService) {
        this.tecnicoService = tecnicoService;
    }

    @Operation(
            summary = "Auto-cadastro Público",
            description = "Permite que um técnico se cadastre fornecendo suas credenciais do Demandanet. " +
                    "As credenciais do legado são validadas e armazenadas no cofre (OCI Vault)."
    )
    @PostMapping("/register")
    public ResponseEntity<Tecnico> registrarPublico(@RequestBody @Valid TecnicoRegisterDTO dto) {
        Tecnico novoTecnico = tecnicoService.registrarTecnicoComDemandanet(dto);
        return ResponseEntity.ok(novoTecnico);
    }

    @Operation(
            summary = "Cadastro Interno (Estagiários)",
            description = "Cria um novo técnico vinculado ao usuário logado (Responsável). " +
                    "Ideal para cadastrar estagiários que usarão a sessão do chefe para acessar o legado."
    )
    @PostMapping
    public ResponseEntity<Tecnico> criarInterno(
            @RequestBody @Valid TecnicoRegisterDTO dto,
            @AuthenticationPrincipal Tecnico usuarioLogado) {

        Tecnico novoTecnico = tecnicoService.criarTecnicoInterno(dto, usuarioLogado);
        return ResponseEntity.ok(novoTecnico);
    }

    @Operation(summary = "Listar todos os técnicos")
    @GetMapping
    public ResponseEntity<List<Tecnico>> listarTodos() {
        return ResponseEntity.ok(tecnicoService.listarTodos());
    }

    @Operation(summary = "Buscar técnico por ID")
    @GetMapping("/{id}")
    public ResponseEntity<Tecnico> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(tecnicoService.buscarPorId(id));
    }

    @Operation(summary = "Atualizar dados do técnico")
    @PutMapping("/{id}")
    public ResponseEntity<Tecnico> atualizarTecnico(@PathVariable Long id, @RequestBody Tecnico dadosTecnico) {
        Tecnico tecnicoAtualizado = tecnicoService.atualizarTecnico(id, dadosTecnico);
        return ResponseEntity.ok(tecnicoAtualizado);
    }

    @Operation(
            summary = "Remover Técnico",
            description = "Se o técnico não possuir reparos vinculados, realiza exclusão física (Hard Delete). " +
                    "Se possuir histórico, realiza exclusão lógica (Soft Delete, marca como removido)."
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarTecnico(@PathVariable Long id) {
        tecnicoService.deletarTecnico(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Restaurar Técnico", description = "Reativa um técnico que foi removido logicamente.")
    @PostMapping("/{id}/restore")
    public ResponseEntity<Tecnico> restaurarTecnico(@PathVariable Long id) {
        Tecnico tecnicoRestaurado = tecnicoService.restaurarTecnico(id);
        return ResponseEntity.ok(tecnicoRestaurado);
    }
}
