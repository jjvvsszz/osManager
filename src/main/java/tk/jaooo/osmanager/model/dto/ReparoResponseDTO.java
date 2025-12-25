package tk.jaooo.osmanager.model.dto;

import tk.jaooo.osmanager.model.Reparo;

import java.time.LocalDateTime;

public record ReparoResponseDTO(
        Long id,
        Long osId,
        String descricao,
        LocalDateTime dataReparo,
        String nomeTecnico
) {
    public static ReparoResponseDTO fromEntity(Reparo reparo) {
        return new ReparoResponseDTO(
                reparo.getId(),
                reparo.getOrdemServico() != null ? reparo.getOrdemServico().getId() : null,
                reparo.getDescricaoReparo(),
                reparo.getDataReparo(),
                reparo.getTecnico() != null ? reparo.getTecnico().getNome() : "Desconhecido"
        );
    }
}
