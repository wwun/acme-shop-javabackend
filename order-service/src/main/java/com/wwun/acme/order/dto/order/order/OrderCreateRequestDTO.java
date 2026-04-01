package com.wwun.acme.order.dto.order.order;

import java.util.List;

import com.wwun.acme.order.dto.order.orderItem.OrderItemCreateRequestDTO;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderCreateRequestDTO {

    @NotEmpty(message ="Order must have at least one item")
    private List<OrderItemCreateRequestDTO> items;

}
