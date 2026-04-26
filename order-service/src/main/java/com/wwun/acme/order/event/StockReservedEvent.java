package com.wwun.acme.order.event;

import java.util.UUID;

public record StockReservedEvent (UUID orderId, UUID userId){

}
