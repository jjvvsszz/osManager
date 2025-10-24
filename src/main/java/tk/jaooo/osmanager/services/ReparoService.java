package tk.jaooo.osmanager.services;

import org.springframework.stereotype.Service;
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
        return reparoRepository.findByOrdemServicoId(osId);
    }

    public List<Reparo> listarReparosPorTecnico(Long tecnicoId) {
        tecnicoService.buscarPorId(tecnicoId);
        return reparoRepository.findByTecnicoId(tecnicoId);
    }
}
