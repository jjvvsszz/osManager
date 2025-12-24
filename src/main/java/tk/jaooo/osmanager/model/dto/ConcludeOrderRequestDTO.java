package tk.jaooo.osmanager.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ConcludeOrderRequestDTO(
        @NotBlank(message = "O número da OS é obrigatório")
        String osNumber,

        @NotNull(message = "A String da observação pode ser vazia, mas não null")
        String observacao
) {}
