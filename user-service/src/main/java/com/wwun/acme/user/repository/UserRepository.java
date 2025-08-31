package com.wwun.acme.user.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wwun.acme.user.entity.User;
import java.util.List;
import java.util.Optional;


public interface UserRepository extends JpaRepository<User, UUID>{

    List<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);

}
