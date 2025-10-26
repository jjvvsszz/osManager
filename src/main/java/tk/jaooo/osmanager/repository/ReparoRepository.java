package tk.jaooo.osmanager.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tk.jaooo.osmanager.model.Reparo;

import java.util.List;

@Repository
public interface ReparoRepository extends JpaRepository<Reparo, Long> {

    List<Reparo> findByOrdemServico_Id(Long ordemServicoId);

    List<Reparo> findByTecnico_Id(Long tecnicoId);

    List<Reparo> findByOrdemServico_Equipamento_Patrimonio(String patrimonio);
}
