package tk.jaooo.osmanager.model.dto;

import java.util.List;

public record DemandanetStatusCountDTO(
        String result,
        Data data
) {
    public record Data(
            List<StatusItem> listaSituacao
    ) {}

    public record StatusItem(
            String numero,
            String qtd
    ) {}
}
