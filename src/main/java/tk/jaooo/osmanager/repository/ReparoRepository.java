package tk.jaooo.osmanager.repository;

import tk.jaooo.osmanager.model.Reparo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReparoRepository extends JpaRepository<Reparo, Long> {
    List<Reparo> findByOrdemServicoId(Long ordemServicoId);

    List<Reparo> findByTecnicoId(Long tecnicoId);

    List<Reparo> findByOrdemServicoPatrimonio(String patrimonio);
}
