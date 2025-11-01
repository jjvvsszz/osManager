package tk.jaooo.osmanager.services;

import org.springframework.stereotype.Service;
import tk.jaooo.osmanager.exception.ResourceNotFoundException;
import tk.jaooo.osmanager.model.Tecnico;
import tk.jaooo.osmanager.repository.TecnicoRepository;

import java.util.List;

@Service
public class TecnicoService {

    private final TecnicoRepository tecnicoRepository;

    public TecnicoService(TecnicoRepository tecnicoRepository) {
        this.tecnicoRepository = tecnicoRepository;
    }

    public List<Tecnico> listarTodos() {
        return tecnicoRepository.findAll();
    }

    public Tecnico buscarPorId(Long id) {
        return tecnicoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Técnico não encontrado com o ID: " + id));
    }

    public Tecnico criarTecnico(Tecnico tecnico) {
        if (tecnicoRepository.findByNome(tecnico.getNome()).isPresent()) {
            throw new IllegalArgumentException("Já existe um técnico com o nome: " + tecnico.getNome());
        }

        return tecnicoRepository.save(tecnico);
    }

    public Tecnico atualizarTecnico(Long id, Tecnico dadosTecnico) {
        Tecnico tecnicoExistente = buscarPorId(id);
        tecnicoExistente.setNome(dadosTecnico.getNome());
        tecnicoExistente.setEstagiario(dadosTecnico.isEstagiario());
        return tecnicoRepository.save(tecnicoExistente);
    }

    public void deletarTecnico(Long id) {
        Tecnico tecnico = buscarPorId(id);
        tecnicoRepository.delete(tecnico);
    }
}
