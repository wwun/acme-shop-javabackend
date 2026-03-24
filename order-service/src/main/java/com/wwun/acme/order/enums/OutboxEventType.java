package com.wwun.acme.order.enums;

public enum OutboxEventType {
    ORDER_CREATED,
    ORDER_CANCELLED,
    ORDER_PAID,
    ORDER_SHIPPED
}
