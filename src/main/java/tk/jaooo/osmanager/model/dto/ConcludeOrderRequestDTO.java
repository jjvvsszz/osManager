package tk.jaooo.osmanager.model.dto;

import jakarta.validation.constraints.NotBlank;

public record ConcludeOrderRequestDTO(
        @NotBlank(message = "O número da OS é obrigatório")
        String osNumber,

        @NotBlank(message = "A observação é obrigatória")
        String observacao
) {}
