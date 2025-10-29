package tk.jaooo.osmanager.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.util.List;

//@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString(exclude = {"equipamento", "reparos"})
@EqualsAndHashCode(exclude = {"equipamento", "reparos"})
@Entity
@Table(name = "ORDENS_SERVICO")
public class OrdemServico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_os", nullable = false, unique = true, length = 20)
    private String numeroOs;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonBackReference("equipamento-os")
    @JoinColumn(name = "id_equipamento", nullable = false)
    private Equipamento equipamento;

    @Column(name = "defeito_original", nullable = false)
    private String defeitoOriginal;

    @Column(name = "descricao_original")
    private String descricaoOriginal;

    @Column(name = "data_entrada", nullable = false)
    private LocalDate dataEntrada;

    @Column(name = "data_saida")
    private LocalDate dataSaida;

    @OneToMany(mappedBy = "ordemServico", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("os-reparo")
    private List<Reparo> reparos;

    @JsonProperty("equipamentoId")
    public Long getEquipamentoId() {
        return equipamento != null ? equipamento.getId() : null;
    }
}
