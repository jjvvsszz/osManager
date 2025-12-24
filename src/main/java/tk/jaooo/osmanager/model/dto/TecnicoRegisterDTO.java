package tk.jaooo.osmanager.model.dto;

import jakarta.validation.constraints.NotBlank;

public record TecnicoRegisterDTO(
        @NotBlank(message = "O nome é obrigatório")
        String nome,

        @NotBlank(message = "O username é obrigatório")
        String username,

        @NotBlank(message = "A senha é obrigatória")
        String password,

        boolean estagiario,

        // Campos opcionais (usados apenas no cadastro público/self-registration)
        String demandanetUser,
        String demandanetPassword
) {}
