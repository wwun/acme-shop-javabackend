package com.wwun.acme.user.service;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.wwun.acme.user.dto.auth.RegisterRequestDTO;
import com.wwun.acme.user.entity.Role;
import com.wwun.acme.user.entity.User;
import com.wwun.acme.user.enums.RolesEnum;
import com.wwun.acme.user.repository.RoleRepository;
import com.wwun.acme.user.repository.UserRepository;

@Service
public class RegistrationServiceImpl implements RegistrationService{

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    
    public RegistrationServiceImpl(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder){
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public User register(RegisterRequestDTO registerRequestDTO) {

        if(userRepository.existsByEmail(registerRequestDTO.getEmail())){
            throw new RuntimeException("Email already in use"); //wwun
        }
        if(userRepository.existsByUsername(registerRequestDTO.getUsername())){
            throw new RuntimeException("Username already in use");  //wwun
        }

        Role defaultRole = roleRepository.findByName(RolesEnum.ROLE_USER.toString()).orElseThrow(() -> new RuntimeException("Default role not found"));

        User user = new User(registerRequestDTO.getUsername(), passwordEncoder.encode(registerRequestDTO.getPassword()), registerRequestDTO.getEmail(), List.of(defaultRole));

        return userRepository.save(user);
    }

}