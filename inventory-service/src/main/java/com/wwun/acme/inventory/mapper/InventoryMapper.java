package com.wwun.acme.inventory.mapper;

import org.mapstruct.Mapper;

import com.wwun.acme.inventory.dto.response.InventoryResponseDTO;
import com.wwun.acme.inventory.entity.Inventory;

@Mapper(componentModel = "spring")
public interface InventoryMapper {

    InventoryResponseDTO toResponseDTO(Inventory inventory);

}
