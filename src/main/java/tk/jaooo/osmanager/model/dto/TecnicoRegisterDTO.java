package tk.jaooo.osmanager.model.dto;

public record TecnicoRegisterDTO(
        String nome,
        String username,
        String password,
        boolean estagiario,
        String demandanetUser,
        String demandanetPassword
) {}
