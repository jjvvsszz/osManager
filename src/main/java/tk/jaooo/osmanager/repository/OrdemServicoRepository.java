package tk.jaooo.osmanager.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tk.jaooo.osmanager.model.OrdemServico;

import java.util.List;

@Repository
public interface OrdemServicoRepository extends JpaRepository<OrdemServico, Long> {
    List<OrdemServico> findByPatrimonio(String patrimonio);

    List<OrdemServico> findByNumeroOs(String numeroOs);

    List<OrdemServico> findByDataEntradaBetween(java.time.LocalDate startDate, java.time.LocalDate endDate);
}
