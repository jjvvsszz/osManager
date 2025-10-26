package tk.jaooo.osmanager.services;

import org.springframework.stereotype.Service;
import tk.jaooo.osmanager.exception.ResourceNotFoundException;
import tk.jaooo.osmanager.model.Equipamento;
import tk.jaooo.osmanager.repository.EquipamentoRepository;

import java.util.List;

@Service
public class EquipamentoService {

    private final EquipamentoRepository equipamentoRepository;

    public EquipamentoService(EquipamentoRepository equipamentoRepository) {
        this.equipamentoRepository = equipamentoRepository;
    }

    public List<Equipamento> listarTodos() {
        return equipamentoRepository.findAll();
    }

    public Equipamento buscarPorId(Long id) {
        return equipamentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Equipamento não encontrado com o ID: " + id));
    }

    public Equipamento buscarPorPatrimonio(String patrimonio) {
        return equipamentoRepository.findByPatrimonio(patrimonio)
                .orElseThrow(() -> new ResourceNotFoundException("Equipamento não encontrado com o patrimônio: " + patrimonio));
    }

    public Equipamento criarEquipamento(Equipamento equipamento) {
        equipamentoRepository.findByPatrimonio(equipamento.getPatrimonio()).ifPresent(e -> {
            throw new IllegalArgumentException("Patrimônio '" + equipamento.getPatrimonio() + "' já cadastrado.");
        });
        return equipamentoRepository.save(equipamento);
    }

    public Equipamento atualizarEquipamento(Long id, Equipamento dadosEquipamento) {
        Equipamento equipamento = buscarPorId(id);

        equipamento.setNome(dadosEquipamento.getNome());
        equipamento.setDescricao(dadosEquipamento.getDescricao());
        equipamento.setMarca(dadosEquipamento.getMarca());
        equipamento.setModelo(dadosEquipamento.getModelo());

        return equipamentoRepository.save(equipamento);
    }

    public void deletarEquipamento(Long id) {
        Equipamento equipamento = buscarPorId(id);
        equipamentoRepository.delete(equipamento);
    }
}
