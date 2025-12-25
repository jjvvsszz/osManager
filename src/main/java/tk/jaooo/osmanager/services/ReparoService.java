package tk.jaooo.osmanager.services;

import org.springframework.stereotype.Service;
import tk.jaooo.osmanager.exception.ResourceNotFoundException;
import tk.jaooo.osmanager.model.OrdemServico;
import tk.jaooo.osmanager.model.Reparo;
import tk.jaooo.osmanager.model.Tecnico;
import tk.jaooo.osmanager.repository.ReparoRepository;

import java.util.List;

@Service
public class ReparoService {

    private final ReparoRepository reparoRepository;
    private final OrdemServicoService ordemServicoService;
    private final TecnicoService tecnicoService;

    public ReparoService(ReparoRepository reparoRepository, OrdemServicoService ordemServicoService, TecnicoService tecnicoService) {
        this.reparoRepository = reparoRepository;
        this.ordemServicoService = ordemServicoService;
        this.tecnicoService = tecnicoService;
    }

    public Reparo adicionarReparo(Long osId, Long tecnicoId, String descricaoReparo) {
        OrdemServico os = ordemServicoService.buscarPorId(osId);
        Tecnico tecnico = tecnicoService.buscarPorId(tecnicoId);

        Reparo novoReparo = Reparo.builder()
                .ordemServico(os)
                .tecnico(tecnico)
                .descricaoReparo(descricaoReparo)
                .build();

        return reparoRepository.save(novoReparo);
    }

    public List<Reparo> listarReparosPorOrdemServico(Long osId) {
        ordemServicoService.buscarPorId(osId);
        return reparoRepository.findByOrdemServico_Id(osId);
    }

    public List<Reparo> listarReparosPorTecnico(Long tecnicoId) {
        tecnicoService.buscarPorId(tecnicoId);
        return reparoRepository.findByTecnico_Id(tecnicoId);
    }

    public List<Reparo> listarReparosPorPatrimonio(String patrimonio) {
        if (patrimonio == null || patrimonio.isBlank()) {
            throw new IllegalArgumentException("Patrimonio inválido.");
        } else {
            return reparoRepository.findByOrdemServico_Equipamento_Patrimonio(patrimonio);
        }
    }

    public Reparo atualizarReparo(Long reparoId, Long novoTecnicoId, String novaDescricao) {
        Reparo reparoExistente = reparoRepository.findById(reparoId)
                .orElseThrow(() -> new ResourceNotFoundException("Reparo não encontrado com o ID: " + reparoId));

        Tecnico novoTecnico = tecnicoService.buscarPorId(novoTecnicoId);

        reparoExistente.setTecnico(novoTecnico);
        reparoExistente.setDescricaoReparo(novaDescricao);
        return reparoRepository.save(reparoExistente);
    }

    public void deletarReparo(Long reparoId) {
        Reparo reparo = reparoRepository.findById(reparoId)
                .orElseThrow(() -> new ResourceNotFoundException("Reparo não encontrado com o ID: " + reparoId));
        reparoRepository.delete(reparo);
    }
}
