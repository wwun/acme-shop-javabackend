package com.wwun.acme.cart.cqrs.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

public class CartQueryHandlerTest {

    // handleGetCart_shouldReturnCartResponseWithItemsFromRedis
    // handleGetCart_shouldReturnEmptyItemsWhenCartDoesNotExist
    // handleGetCart_shouldConvertRedisEntriesToCartItemResponseDTO
    // handleGetCart_shouldIncrementGetCartMetric
    // handleGetCartSummary_shouldReturnSummaryWithSingleItem
    // handleGetCartSummary_shouldReturnSummaryWithMultipleItems
    // handleGetCartSummary_shouldCalculateTotalItemsCorrectly
    // handleGetCartSummary_shouldCalculateTotalAmountCorrectly
    // handleGetCartSummary_shouldCallProductGatewayForEachCartItem
    // handleGetCartSummary_shouldReturnEmptySummaryWhenCartIsEmpty
    // handleGetCartSummary_shouldRecordSummaryLatency
    // handleGetCartSummary_shouldPropagateExceptionWhenProductGatewayFails

}
