package com.wwun.acme.user.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.wwun.acme.user.dto.role.RoleCreateRequestDTO;
import com.wwun.acme.user.dto.role.RoleUpdateRequestDTO;
import com.wwun.acme.user.entity.Role;
import com.wwun.acme.user.mapper.RoleMapper;
import com.wwun.acme.user.repository.RoleRepository;

@Service
public class RoleServiceImpl implements RoleService{

    private final RoleRepository roleRepository;
    private final RoleMapper roleMapper;

    public RoleServiceImpl(RoleRepository roleRepository, RoleMapper roleMapper){
        this.roleRepository = roleRepository;
        this.roleMapper = roleMapper;
    }

    @Override
    public Role save(RoleCreateRequestDTO roleCreateRequestDTO) {
        
        if(roleRepository.existsByName(roleCreateRequestDTO.getName())){
            throw new RuntimeException("rol " + roleCreateRequestDTO.getName() + " already exists");
        }

        Role role = roleMapper.toEntity(roleCreateRequestDTO);
        
        return roleRepository.save(role);
    }

    @Override
    public Optional<Role> findById(UUID id) {
        return roleRepository.findById(id);
    }

    @Override
    public Optional<Role> update(UUID id, RoleUpdateRequestDTO roleUpdateRequestDTO) {

        Role role = roleRepository.findById(id).orElseThrow(() -> new RuntimeException("Role not found with id: " + id));

        if(!role.getName().equals(roleUpdateRequestDTO.getName())){
            if(roleRepository.existsByName(roleUpdateRequestDTO.getName()))
                throw new RuntimeException("rol " + roleUpdateRequestDTO.getName() + " already exists");
        }

        role.setName(roleUpdateRequestDTO.getName());
        role.setDescription(roleUpdateRequestDTO.getDescription());
        
        return Optional.of(roleRepository.save(role));
    }

    @Override
    public void delete(UUID id) {
        roleRepository.deleteById(id);;
    }


}
