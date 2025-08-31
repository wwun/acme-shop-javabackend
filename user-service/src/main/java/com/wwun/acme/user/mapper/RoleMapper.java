package com.wwun.acme.user.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import com.wwun.acme.user.dto.role.RoleCreateRequestDTO;
import com.wwun.acme.user.dto.role.RoleResponseDTO;
import com.wwun.acme.user.dto.role.RoleUpdateRequestDTO;
import com.wwun.acme.user.entity.Role;

@Mapper(componentModel = "spring")
public interface RoleMapper {

    @Mappings({@Mapping(target = "id", ignore = true)})
    Role toEntity(RoleCreateRequestDTO roleCreateRequestDTO);

    @Mappings({@Mapping(target = "id", ignore = true)})
    Role toEntity(RoleUpdateRequestDTO roleUpdateRequestDTO);
    
    RoleResponseDTO toResponseDTO(Role role);

}
