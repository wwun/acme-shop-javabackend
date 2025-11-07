package com.wwun.acme.cart.cqrs.query;

import java.util.UUID;

public class Queries {

    public record GetCartQuery(UUID userId){}
    public record GetCartSummaryQuery(UUID userId){}

}
