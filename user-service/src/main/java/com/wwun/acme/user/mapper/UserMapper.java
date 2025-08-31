package com.wwun.acme.user.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import com.wwun.acme.user.dto.User.UserCreateRequestDTO;
import com.wwun.acme.user.dto.User.UserResponseDTO;
import com.wwun.acme.user.dto.User.UserUpdateRequestDTO;
import com.wwun.acme.user.entity.User;

@Mapper(componentModel = "spring", uses = RoleMapper.class)  //wwun debo implementar o poner RoleMapper.class?
public interface UserMapper {

    @Mappings({@Mapping(target = "id", ignore = true), @Mapping(target = "roles", ignore = true)})
    User toEntity(UserCreateRequestDTO userCreateRequestDTO);

    @Mappings({@Mapping(target = "id", ignore = true), @Mapping(target = "username", ignore = true), @Mapping(target = "roles", ignore = true)})
    User toEntity(UserUpdateRequestDTO userUpdateRequestDTO);

    UserResponseDTO toResponseDTO(User user);

}