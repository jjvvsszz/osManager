package tk.jaooo.osmanager.model.dto;

import jakarta.validation.constraints.NotBlank;

public record DemandanetAuthRequestDTO(
        @NotBlank(message = "O usuário é obrigatório")
        String username,

        @NotBlank(message = "A senha é obrigatória")
        String password
) {}
