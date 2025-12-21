package tk.jaooo.osmanager.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tk.jaooo.osmanager.model.Tecnico;
import tk.jaooo.osmanager.repository.ReparoRepository;
import tk.jaooo.osmanager.repository.TecnicoRepository;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TecnicoServiceTest {

    @Mock
    private TecnicoRepository tecnicoRepository;

    @Mock
    private ReparoRepository reparoRepository;

    @InjectMocks
    private TecnicoService tecnicoService;

    @Test
    void deveCriarTecnicoComSucesso() {
        Tecnico novo = Tecnico.builder().nome("João").username("joao").build();

        when(tecnicoRepository.findByNomeAndRemovidoIsFalse("João")).thenReturn(Optional.empty());
        when(tecnicoRepository.save(any(Tecnico.class))).thenReturn(novo);

        Tecnico criado = tecnicoService.criarTecnico(novo);

        assertNotNull(criado);
        verify(tecnicoRepository).save(novo);
    }

    @Test
    void naoDeveCriarTecnicoComNomeDuplicado() {
        Tecnico novo = Tecnico.builder().nome("João").build();

        when(tecnicoRepository.findByNomeAndRemovidoIsFalse("João")).thenReturn(Optional.of(new Tecnico()));

        assertThrows(IllegalArgumentException.class, () -> tecnicoService.criarTecnico(novo));
        verify(tecnicoRepository, never()).save(any());
    }

    @Test
    void deveFazerSoftDeleteSeTiverReparos() {
        Long id = 1L;
        Tecnico tecnico = Tecnico.builder().id(id).removido(false).build();

        when(tecnicoRepository.findById(id)).thenReturn(Optional.of(tecnico));
        // Simula que existem reparos vinculados
        when(reparoRepository.findByTecnico_Id(id)).thenReturn(Collections.singletonList(new tk.jaooo.osmanager.model.Reparo()));

        tecnicoService.deletarTecnico(id);

        assertTrue(tecnico.isRemovido()); // Verifica se virou true
        verify(tecnicoRepository).save(tecnico); // Verifica se atualizou em vez de deletar
        verify(tecnicoRepository, never()).delete(any());
    }
}
