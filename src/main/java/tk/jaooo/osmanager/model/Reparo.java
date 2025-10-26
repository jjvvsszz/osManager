package tk.jaooo.osmanager.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString(exclude = "ordemServico")
@EqualsAndHashCode(exclude = "ordemServico")
@Entity
@Table(name = "REPAROS")
public class Reparo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonBackReference("os-reparo")
    @JoinColumn(name = "id_ordem_servico", nullable = false)
    private OrdemServico ordemServico;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tecnico", nullable = false)
    private Tecnico tecnico;

    @Column(name = "descricao_reparo", nullable = false)
    private String descricaoReparo;

    @CreationTimestamp
    @Column(name = "data_reparo", nullable = false, updatable = false)
    private LocalDateTime dataReparo;

    @JsonProperty("ordemServicoId")
    public Long getOrdemServicoId() {
        return ordemServico != null ? ordemServico.getId() : null;
    }

    @JsonProperty("tecnicoId")
    public Long getTecnicoId() {
        return tecnico != null ? tecnico.getId() : null;
    }
}
