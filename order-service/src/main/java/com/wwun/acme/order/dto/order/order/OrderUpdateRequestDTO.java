package com.wwun.acme.order.dto.order.order;

import java.util.List;

import com.wwun.acme.order.dto.order.orderItem.OrderItemUpdateRequestDTO;

import jakarta.validation.constraints.NotEmpty;
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
public class OrderUpdateRequestDTO {

    @NotEmpty(message = "Order must have at least one item")
    private List<OrderItemUpdateRequestDTO> items;

}
