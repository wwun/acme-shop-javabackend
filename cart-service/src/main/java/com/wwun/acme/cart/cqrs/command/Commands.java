package com.wwun.acme.cart.cqrs.command;

import java.util.UUID;

public class Commands {

    public record AddItemToCartCommand(UUID userId, UUID productId, int quantity){}
    public record SetItemQuantityCommand(UUID userId, UUID productId, int quantity){}
    public record RemoveItemFromCartCommand(UUID userId, UUID productId){}
    public record ClearCartCommand(UUID userId){}

}
