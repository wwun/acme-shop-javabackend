package com.wwun.acme.cart.metric;

import java.util.function.Supplier;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.*;

@Component
public class CartMetrics {

    private final Counter addCounter;
    private final Counter removeCounter;
    private final Counter setQtyCounter;
    private final Counter clearCounter;

    private final Counter getCartCounter;
    private final Counter getSummaryCounter;

    private final Counter productFallbacks;

    // Timer: latencia de resumen
    private final Timer summaryTimer;

    public CartMetrics(MeterRegistry registry) {
        this.addCounter = Counter.builder("cart_commands_total").tag("type", "add").register(registry);
        this.removeCounter = Counter.builder("cart_commands_total").tag("type", "remove").register(registry);
        this.setQtyCounter = Counter.builder("cart_commands_total").tag("type", "setQty").register(registry);
        this.clearCounter = Counter.builder("cart_commands_total").tag("type", "clear").register(registry);

        this.getCartCounter = Counter.builder("cart_queries_total").tag("type", "getCart").register(registry);
        this.getSummaryCounter = Counter.builder("cart_queries_total").tag("type", "getSummary").register(registry);

        this.productFallbacks = Counter.builder("cart_product_fallbacks_total").register(registry);

        this.summaryTimer = Timer.builder("cart_query_latency_seconds").tag("type", "summary").publishPercentileHistogram().register(registry);
    }

    // commands
    public void incrementAdd(){ addCounter.increment(); }
    public void incrementRemove(){ removeCounter.increment(); }
    public void incrementSetQty(){ setQtyCounter.increment(); }
    public void incrementClear(){ clearCounter.increment(); }

    // queries
    public void incrementGetCart(){ getCartCounter.increment(); }
    public void incrementGetSummary(){ getSummaryCounter.increment(); }

    // fallbacks
    public void incrementProductFallbacks(){ productFallbacks.increment(); }

    // timers
    public <T> T timeSummary(Supplier<T> supplier) {
        return summaryTimer.record(supplier);
    }
}
