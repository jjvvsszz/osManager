package tk.jaooo.osmanager.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ORDENS_SERVICO")
public class OrdemServico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_os", nullable = false, unique = true, length = 20)
    private String numeroOs;

    @Column(length = 50)
    private String patrimonio;

    @Column(name = "defeito_original", nullable = false, columnDefinition = "TEXT")
    private String defeitoOriginal;

    @Column(name = "descricao_original", columnDefinition = "TEXT")
    private String descricaoOriginal;

    @CreationTimestamp
    @Column(name = "data_entrada", nullable = false, updatable = false)
    private LocalDate dataEntrada;

    @Column(name = "data_saida")
    private LocalDate dataSaida;

    @OneToMany(mappedBy = "ordemServico", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Reparo> reparos;
}
