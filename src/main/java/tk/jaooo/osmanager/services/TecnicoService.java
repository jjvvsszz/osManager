package tk.jaooo.osmanager.services;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tk.jaooo.osmanager.exception.ResourceNotFoundException;
import tk.jaooo.osmanager.model.Tecnico;
import tk.jaooo.osmanager.repository.ReparoRepository;
import tk.jaooo.osmanager.repository.TecnicoRepository;

import java.util.List;

@Service
public class TecnicoService {

    private final TecnicoRepository tecnicoRepository;
    private final ReparoRepository reparoRepository;

    public TecnicoService(TecnicoRepository tecnicoRepository, ReparoRepository reparoRepository) {
        this.tecnicoRepository = tecnicoRepository;
        this.reparoRepository = reparoRepository;
    }

    public List<Tecnico> listarTodos() {
        return tecnicoRepository.findAll();
    }

    public Tecnico buscarPorId(Long id) {
        return tecnicoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Técnico não encontrado com o ID: " + id));
    }

    public Tecnico criarTecnico(Tecnico tecnico) {
        if (tecnicoRepository.findByNomeAndRemovidoIsFalse(tecnico.getNome()).isPresent()) {
            throw new IllegalArgumentException("Já existe um técnico ativo com o nome: " + tecnico.getNome());
        }

        return tecnicoRepository.save(tecnico);
    }

    public Tecnico atualizarTecnico(Long id, Tecnico dadosTecnico) {
        Tecnico tecnicoExistente = buscarPorId(id);

        if (!tecnicoExistente.getNome().equals(dadosTecnico.getNome()) &&
                tecnicoRepository.findByNomeAndRemovidoIsFalse(dadosTecnico.getNome()).isPresent()) {
            throw new IllegalArgumentException("Já existe um técnico ativo com o nome: " + dadosTecnico.getNome());
        }

        tecnicoExistente.setNome(dadosTecnico.getNome());
        tecnicoExistente.setEstagiario(dadosTecnico.isEstagiario());
        return tecnicoRepository.save(tecnicoExistente);
    }

    @Transactional
    public void deletarTecnico(Long id) {
        Tecnico tecnico = buscarPorId(id);

        boolean hasReparos = !reparoRepository.findByTecnico_Id(id).isEmpty();

        if (hasReparos) {
            tecnico.setRemovido(true);
            tecnicoRepository.save(tecnico);
        } else {
            tecnicoRepository.delete(tecnico);
        }
    }

    public Tecnico restaurarTecnico(Long id) {
        Tecnico tecnicoRemovido = buscarPorId(id);

        if (!tecnicoRemovido.isRemovido()) {
            throw new IllegalStateException("O técnico não está marcado como removido.");
        }

        tecnicoRepository.findByNomeAndRemovidoIsFalse(tecnicoRemovido.getNome()).ifPresent(t -> {
            throw new DataIntegrityViolationException("Não é possível restaurar, pois já existe um técnico ativo com o nome '" + t.getNome() + "'. Altere o nome antes de restaurar.");
        });

        tecnicoRemovido.setRemovido(false);
        return tecnicoRepository.save(tecnicoRemovido);
    }
}
