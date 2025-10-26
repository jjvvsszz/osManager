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
        os.setDataEntrada(LocalDate.now());
        os.setDataSaida(null);
        return ordemServicoRepository.save(os);
    }

    public OrdemServico atualizarOrdemServico(Long id, OrdemServico dadosOs) {
        OrdemServico osExistente = buscarPorId(id);

        osExistente.setNumeroOs(dadosOs.getNumeroOs());
        osExistente.setDefeitoOriginal(dadosOs.getDefeitoOriginal());
        osExistente.setDescricaoOriginal(dadosOs.getDescricaoOriginal());
        osExistente.setDataSaida(dadosOs.getDataSaida());
        osExistente.setDataEntrada(dadosOs.getDataEntrada());

        return ordemServicoRepository.save(osExistente);
    }

    public void deletarOrdemServico(Long id) {
        OrdemServico os = ordemServicoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("OS não encontrado com o ID: " + id));
        ordemServicoRepository.delete(os);
    }
}
