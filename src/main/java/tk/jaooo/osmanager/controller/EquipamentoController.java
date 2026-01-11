package tk.jaooo.osmanager.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tk.jaooo.osmanager.model.Equipamento;
import tk.jaooo.osmanager.model.OrdemServico;
import tk.jaooo.osmanager.model.Reparo;
import tk.jaooo.osmanager.model.Tecnico;
import tk.jaooo.osmanager.model.dto.AssetHistoryDTO;
import tk.jaooo.osmanager.model.dto.ConsultedOrderDTO;
import tk.jaooo.osmanager.model.dto.InternalOrderResponseDTO;
import tk.jaooo.osmanager.model.dto.ReparoResponseDTO;
import tk.jaooo.osmanager.services.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/equipamentos")
@Tag(name = "Gestão de Equipamentos", description = "Inventário e histórico híbrido")
public class EquipamentoController {

    private static final Logger logger = LoggerFactory.getLogger(EquipamentoController.class);

    private final EquipamentoService equipamentoService;
    private final DemandanetSessionManager sessionManager;
    private final DemandanetClientService demandanetClientService;
    private final OrdemServicoService ordemServicoService;
    private final ReparoService reparoService;

    public EquipamentoController(EquipamentoService equipamentoService,
                                 DemandanetSessionManager sessionManager,
                                 DemandanetClientService demandanetClientService,
                                 OrdemServicoService ordemServicoService,
                                 ReparoService reparoService) {
        this.equipamentoService = equipamentoService;
        this.sessionManager = sessionManager;
        this.demandanetClientService = demandanetClientService;
        this.ordemServicoService = ordemServicoService;
        this.reparoService = reparoService;
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

    @Operation(
            summary = "Histórico Completo do Ativo",
            description = "Retorna um histórico unificado contendo: " +
                    "1. Ordens de serviço do sistema legado (Web Scraping). " +
                    "2. Ordens de serviço internas. " +
                    "3. Reparos avulsos realizados no equipamento."
    )
    @GetMapping("/historico/{patrimonio}")
    public ResponseEntity<AssetHistoryDTO> consultarHistoricoPatrimonio(
            @PathVariable String patrimonio,
            @AuthenticationPrincipal Tecnico solicitante) {

        try {
            List<OrdemServico> ordensInternas = ordemServicoService.buscarPorPatrimonio(patrimonio);
            List<InternalOrderResponseDTO> ordensInternasDTO = ordensInternas.stream()
                    .map(InternalOrderResponseDTO::fromEntity)
                    .toList();

            List<Reparo> reparos = reparoService.listarReparosPorPatrimonio(patrimonio);
            List<ReparoResponseDTO> reparosDTO = reparos.stream()
                    .map(ReparoResponseDTO::fromEntity)
                    .toList();

            Tecnico owner = sessionManager.resolveCredentialOwner(solicitante);
            String cookie = sessionManager.getSessionForOwner(owner);

            List<ConsultedOrderDTO> ordensLegado;
            try {
                ordensLegado = demandanetClientService.searchOrdersByPatrimony(cookie, patrimonio).block();
            } catch (Exception e) {
                logger.warn("Falha na primeira tentativa de busca legado. Tentando renovar sessão.", e);
                cookie = sessionManager.refreshSessionForOwner(owner);
                ordensLegado = demandanetClientService.searchOrdersByPatrimony(cookie, patrimonio).block();
            }

            return ResponseEntity.ok(new AssetHistoryDTO(ordensLegado, ordensInternasDTO, reparosDTO));

        } catch (Exception e) {
            logger.error("Erro ao consultar histórico do patrimônio: {}", patrimonio, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
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
