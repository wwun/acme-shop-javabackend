package com.wwun.acme.order.event;

import java.util.UUID;

public record StockFailedEvent (UUID orderId, UUID userId, String reason){

}
