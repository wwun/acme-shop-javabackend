package com.wwun.acme.catalog.service;

import java.util.List;
import java.util.UUID;

import com.wwun.acme.catalog.dto.response.CatalogProductResponseDTO;

public interface CatalogQueryService {

    List<CatalogProductResponseDTO> getProductsAndInventories(List<UUID> productIds);

}
