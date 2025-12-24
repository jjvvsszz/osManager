package tk.jaooo.osmanager.model.dto;

public record ReparoRequestDTO(
        Long osId,
        Long tecnicoId,
        String descricaoReparo
) {}
