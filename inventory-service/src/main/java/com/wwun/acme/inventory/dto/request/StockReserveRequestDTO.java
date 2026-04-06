package com.wwun.acme.inventory.dto.request;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockReserveRequestDTO {
    
    @NotNull
    UUID orderId;
    
    List<StockAdjustRequestDTO> items;

}
