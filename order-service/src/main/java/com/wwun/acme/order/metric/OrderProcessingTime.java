package com.wwun.acme.order.metric;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

public class OrderProcessingTime {

    private final Timer orderProcessingTimer;

    public OrderProcessingTime(MeterRegistry registry){
        this.orderProcessingTimer = registry.timer("order_processing_duration_seconds");
    }

    public void processOrder(){
        orderProcessingTimer.record(() -> {
            try{

            }catch(Exception ex){
                Thread.currentThread().interrupt();
            }
        });
    }

}
