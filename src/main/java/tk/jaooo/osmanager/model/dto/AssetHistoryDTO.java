package tk.jaooo.osmanager.model.dto;

import java.util.List;

public record AssetHistoryDTO(
        List<ConsultedOrderDTO> ordensLegado,
        List<InternalOrderResponseDTO> ordensInternas,
        List<ReparoResponseDTO> reparos
) {}
