package tk.jaooo.osmanager.model.dto;

import tk.jaooo.osmanager.model.OrdemServico;

import java.time.LocalDate;

public record InternalOrderResponseDTO(
        Long id,
        String numeroOs,
        String defeito,
        String descricao,
        LocalDate dataEntrada,
        LocalDate dataSaida
) {
    public static InternalOrderResponseDTO fromEntity(OrdemServico os) {
        return new InternalOrderResponseDTO(
                os.getId(),
                os.getNumeroOs(),
                os.getDefeitoOriginal(),
                os.getDescricaoOriginal(),
                os.getDataEntrada(),
                os.getDataSaida()
        );
    }
}
