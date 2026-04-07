package com.wwun.acme.inventory.metric;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.MeterRegistry;

@Component
public class InventoryMetrics {

    private final MeterRegistry meterRegistry;

    public InventoryMetrics(MeterRegistry meterRegistry){
        this.meterRegistry = meterRegistry;
    }

    public void incrementReservationsSucceeded() {
        meterRegistry.counter("inventory.reservations", "result", "success").increment();
    }

    public void incrementReservationsFailed() {
        meterRegistry.counter("inventory.reservations", "result", "failed").increment();
    }

    public void incrementStockAdjustments(String type) {
        meterRegistry.counter("inventory.adjustments", "type", type).increment();
    }

}
