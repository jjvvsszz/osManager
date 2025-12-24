package tk.jaooo.osmanager.model.dto;

public record ConsultedOrderDetailsDTO(
        String osNumber,
        String requisitante,
        String tipoServico,
        String defeito,
        String descricao,
        String patrimonio,
        String situacao,
        String dataCadastro
) {}
