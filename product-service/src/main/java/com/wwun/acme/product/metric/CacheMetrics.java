package com.wwun.acme.product.metric;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.MeterRegistry;

@Component
public class CacheMetrics {

    private final MeterRegistry meterRegistry;

    public CacheMetrics(MeterRegistry meterRegistry){
        this.meterRegistry = meterRegistry;
    }

    public void incrementCacheHit(){
        meterRegistry.counter("cache_hits_total").increment();
    }

    public void incrementCacheMiss(){
        meterRegistry.counter("cache_misses_total").increment();
    }

}
