package tk.jaooo.osmanager.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tk.jaooo.osmanager.model.Tecnico;

import java.util.Optional;

@Repository
public interface TecnicoRepository extends JpaRepository<Tecnico, Long> {
    Optional<Tecnico> findByNomeAndRemovidoIsFalse(String nome);

    Optional<Tecnico> findByUsernameAndRemovidoIsFalse(String cpf);
}
