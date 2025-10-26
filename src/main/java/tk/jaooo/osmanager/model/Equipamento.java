package tk.jaooo.osmanager.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString(exclude = "ordensServico")
@EqualsAndHashCode(exclude = "ordensServico")
@Entity
@Table(name = "EQUIPAMENTOS")
public class Equipamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String patrimonio;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column
    private String descricao;

    @Column(length = 100)
    private String marca;

    @Column(length = 100)
    private String modelo;

    @OneToMany(mappedBy = "equipamento", cascade = CascadeType.REMOVE, orphanRemoval = true)
    @JsonManagedReference("equipamento-os")
    private List<OrdemServico> ordensServico;
}
