package com.wwun.acme.catalog.dto;

import java.util.List;
import java.util.UUID;

public record InventoryBatchRequestDTO(List<UUID> productIds) {
}