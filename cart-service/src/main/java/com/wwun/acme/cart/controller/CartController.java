package com.wwun.acme.cart.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wwun.acme.cart.cqrs.command.CartCommandHandler;
import com.wwun.acme.cart.cqrs.command.Commands.AddItemToCartCommand;
import com.wwun.acme.cart.cqrs.command.Commands.RemoveItemFromCartCommand;
import com.wwun.acme.cart.cqrs.command.Commands.ClearCartCommand;
import com.wwun.acme.cart.cqrs.command.Commands.SetItemQuantityCommand;
import com.wwun.acme.cart.cqrs.query.CartQueryHandler;
import com.wwun.acme.cart.cqrs.query.Queries.GetCartQuery;
import com.wwun.acme.cart.cqrs.query.Queries.GetCartSummaryQuery;
import com.wwun.acme.cart.dto.request.AddItemRequestDTO;
import com.wwun.acme.cart.dto.response.CartResponseDTO;
import com.wwun.acme.cart.dto.response.CartSummaryDTO;
import com.wwun.acme.security.SecurityUtils;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/carts")
public class CartController {

    private final CartCommandHandler command;
    private final CartQueryHandler query;

    public CartController(CartCommandHandler command, CartQueryHandler query){
        this.command = command;
        this.query = query;
    }

    private UUID userId(){
        return SecurityUtils.getCurrentUserId();
    }

    @PostMapping("/items")
    public ResponseEntity<?> addItem(@Valid @RequestBody AddItemRequestDTO addItemRequestDTO){
        command.handle(new AddItemToCartCommand(userId(), addItemRequestDTO.productId(), addItemRequestDTO.quantity()));
        return ResponseEntity.accepted().build();
    }

    @PutMapping("/items")
    public ResponseEntity<Void> setQty(@Valid @RequestBody SetItemQuantityCommand setItemQuantityCommand) {
        command.handle(new SetItemQuantityCommand(userId(), setItemQuantityCommand.productId(), setItemQuantityCommand.quantity()));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/items/{productId}")
    public ResponseEntity<Void> remove(@PathVariable UUID productId) {
        command.handle(new RemoveItemFromCartCommand(userId(), productId));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> clear() {
        command.handle(new ClearCartCommand(userId()));
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public CartResponseDTO getCart() {
        return query.handle(new GetCartQuery(userId()));
    }

    @GetMapping("/summary")
    public CartSummaryDTO getSummary() {
        return query.handle(new GetCartSummaryQuery(userId()));
    }

}
