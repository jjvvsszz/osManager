package tk.jaooo.osmanager.services;

import org.springframework.stereotype.Service;
import tk.jaooo.osmanager.exception.ResourceNotFoundException;
import tk.jaooo.osmanager.model.OrdemServico;
import tk.jaooo.osmanager.repository.OrdemServicoRepository;

import java.time.LocalDate;
import java.util.List;

@Service
public class OrdemServicoService {

    private final OrdemServicoRepository ordemServicoRepository;

    public OrdemServicoService(OrdemServicoRepository ordemServicoRepository) {
        this.ordemServicoRepository = ordemServicoRepository;
    }

    public List<OrdemServico> listarTodas() {
        return ordemServicoRepository.findAll();
    }

    public OrdemServico buscarPorId(Long id) {
        return ordemServicoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ordem de Serviço não encontrada com o ID: " + id));
    }

    public OrdemServico abrirOrdemServico(OrdemServico os) {
        os.setDataSaida(null);
        return ordemServicoRepository.save(os);
    }

    public OrdemServico finalizarOrdemServico(Long id) {
        OrdemServico os = buscarPorId(id);

        os.setDataSaida(LocalDate.now());

        return ordemServicoRepository.save(os);
    }
}
