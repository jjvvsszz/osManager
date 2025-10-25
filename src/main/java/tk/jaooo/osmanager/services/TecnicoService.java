package tk.jaooo.osmanager.services;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import tk.jaooo.osmanager.exception.ResourceNotFoundException;
import tk.jaooo.osmanager.model.Tecnico;
import tk.jaooo.osmanager.repository.TecnicoRepository;

import java.util.List;

@Service
public class TecnicoService {

    private final TecnicoRepository tecnicoRepository;
    private final PasswordEncoder passwordEncoder;

    public TecnicoService(TecnicoRepository tecnicoRepository, PasswordEncoder passwordEncoder) {
        this.tecnicoRepository = tecnicoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<Tecnico> listarTodos() {
        return tecnicoRepository.findAll();
    }

    public Tecnico buscarPorId(Long id) {
        return tecnicoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Técnico não encontrado com o ID: " + id));
    }

    public Tecnico criarTecnico(Tecnico tecnico) {
        tecnicoRepository.findByUsername(tecnico.getUsername()).ifPresent(t -> {
            throw new IllegalArgumentException("Username '" + tecnico.getUsername() + "' já está em uso.");
        });

        tecnico.setPasswordHash(passwordEncoder.encode(tecnico.getPasswordHash()));

        return tecnicoRepository.save(tecnico);
    }

    public void deletarTecnico(Long id) {
        Tecnico tecnico = buscarPorId(id);
        tecnicoRepository.delete(tecnico);
    }
}
