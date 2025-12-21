package tk.jaooo.osmanager.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import tk.jaooo.osmanager.model.Tecnico;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class TecnicoRepositoryTest {

    @Autowired
    private TecnicoRepository tecnicoRepository;

    @Test
    void deveEncontrarTecnicoAtivoPorNome() {
        // Arrange
        Tecnico t1 = Tecnico.builder().nome("Jose").username("jose").password("123").removido(false).estagiario(false).build();
        Tecnico t2 = Tecnico.builder().nome("Maria").username("maria").password("123").removido(true).estagiario(false).build();

        tecnicoRepository.save(t1);
        tecnicoRepository.save(t2);

        // Act
        Optional<Tecnico> resultAtivo = tecnicoRepository.findByNomeAndRemovidoIsFalse("Jose");
        Optional<Tecnico> resultRemovido = tecnicoRepository.findByNomeAndRemovidoIsFalse("Maria");

        // Assert
        assertThat(resultAtivo).isPresent();
        assertThat(resultRemovido).isEmpty();
    }
}
