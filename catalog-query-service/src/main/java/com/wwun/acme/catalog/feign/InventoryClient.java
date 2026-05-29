package com.wwun.acme.catalog.feign;

import java.util.List;
import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.wwun.acme.catalog.dto.client.inventory.InventoryAvailabilityDTO;

@FeignClient(name = "msvc-inventories")
public interface InventoryClient {

    @PostMapping("/api/inventories/batch")
    List<InventoryAvailabilityDTO> getInventoriesByProductIds(@RequestBody List<UUID> productIds);
}
