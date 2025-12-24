package tk.jaooo.osmanager.model.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record BatchStatusUpdateRequestDTO(
        @NotEmpty(message = "A lista de IDs não pode estar vazia")
        List<String> ids,

        @NotNull(message = "A situação é obrigatória")
        Integer situacao,

        // Opcional: usado apenas quando situacao == 0 (Agendada)
        String funcionarioId,
        String dataPrevisao,

        // Opcional: usado apenas quando situacao == 2 (Concluída)
        String observacao
) {}
