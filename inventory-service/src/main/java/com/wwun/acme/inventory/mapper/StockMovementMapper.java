package com.wwun.acme.inventory.mapper;

import org.mapstruct.Mapper;

import com.wwun.acme.inventory.dto.response.StockMovementResponseDTO;
import com.wwun.acme.inventory.entity.StockMovement;

@Mapper(componentModel = "spring")
public interface StockMovementMapper {

    StockMovementResponseDTO toResponseDTO(StockMovement stockMovement);

}
