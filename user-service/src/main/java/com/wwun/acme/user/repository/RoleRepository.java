package com.wwun.acme.user.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wwun.acme.user.entity.Role;
import java.util.List;
import java.util.Optional;


public interface RoleRepository extends JpaRepository<Role, UUID>{

    List<Role> findByNameIn(List<String> names);
    Optional<Role> findByName(String name);
    boolean existsByName(String name);

}
