package com.wwun.acme.order.metric;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.MeterRegistry;

@Component
public class OrderMetrics {

    private final MeterRegistry meterRegistry;

    public OrderMetrics(MeterRegistry meterRegistry){
        this.meterRegistry = meterRegistry;
    }

    public void incrementOrdersCreated(){
        meterRegistry.counter("orders_created_total").increment();
    }

}
