package com.wwun.acme.user.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import com.wwun.acme.security.SecurityUtils;
import com.wwun.acme.user.dto.User.UserCreateRequestDTO;
import com.wwun.acme.user.dto.User.UserUpdateRequestDTO;
import com.wwun.acme.user.entity.Role;
import com.wwun.acme.user.entity.User;
import com.wwun.acme.user.mapper.UserMapper;
import com.wwun.acme.user.repository.RoleRepository;
import com.wwun.acme.user.repository.UserRepository;

import static com.wwun.acme.user.enums.CategoriesEnum.*;

import jakarta.validation.Valid;

@Service
public class UserServiceImpl implements UserService{

    private UserRepository userRepository;
    private RoleRepository roleRepository;
    private UserMapper userMapper;
    
    private final PasswordEncoder passwordEncoder;
    
    public UserServiceImpl(UserRepository userRepository, UserMapper userMapper, RoleRepository roleRepository, PasswordEncoder passwordEncoder){
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public User save(@Valid @RequestBody UserCreateRequestDTO userCreateRequestDTO) {
        // Validar email único Username/email únicos → con existsByEmail y existsByUsername, Password seguro → puedes añadir un validador custom (mínimo 8 caracteres, mayúscula, número, etc, Roles válidos → no confiar en lo que envía el cliente, siempre validar con tu tabla roles
        if(userRepository.existsByEmail(userCreateRequestDTO.getEmail())){
            throw new RuntimeException("Email already in use");
        }
        if(userRepository.existsByUsername(userCreateRequestDTO.getUsername())){
            throw new RuntimeException("Username already in use");
        }

        List<Role> roles = roleRepository.findAllById(userCreateRequestDTO.getRoleIds());
        if(roles.size() != userCreateRequestDTO.getRoleIds().size()){
            throw new RuntimeException("One or more roles are invalid");
        }

        User user = userMapper.toEntity(userCreateRequestDTO);
        user.setPassword(passwordEncoder.encode(userCreateRequestDTO.getPassword()));   //wwun
        user.setRoles(roles);

        return userRepository.save(user);
    }

    @Override
    public Optional<User> findById(UUID id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        System.out.println(""+SecurityUtils.isAdmin(auth));
        if (!SecurityUtils.isAdmin(auth)) {
            UUID currentUserId = SecurityUtils.getCurrentUserId();

            if (!id.equals(currentUserId)) {
                throw new RuntimeException("User doesn't exist or is not allowed to access");
            }else{
                System.out.println("inside");
            }
        }

        return userRepository.findById(id);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public Optional<User> update(UUID id, UserUpdateRequestDTO userUpdateRequestDTO) {

        User user = this.findById(id).orElseThrow(() -> new RuntimeException("User doesn't exist or is not allowed to access"));

        if(!user.getEmail().equals(userUpdateRequestDTO.getEmail()) && userRepository.existsByEmail(userUpdateRequestDTO.getEmail())){
            throw new RuntimeException("Email already in use");
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        List<Role> roles = user.getRoles();
        if (SecurityUtils.isAdmin(auth)) {
            roles = roleRepository.findAllById(userUpdateRequestDTO.getRoleIds());
            if (roles.size() != userUpdateRequestDTO.getRoleIds().size()) {
                throw new RuntimeException("One or more roles are invalid");
            }
        }

        // Actualizar email
        user.setEmail(userUpdateRequestDTO.getEmail());

        // Actualizar contraseña solo si es distinta de null/vacía
        if (userUpdateRequestDTO.getPassword() != null && !userUpdateRequestDTO.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(userUpdateRequestDTO.getPassword()));
        }
        user.setRoles(roles);

        return Optional.of(userRepository.save(user));
    }

    @Override
    public void delete(UUID id) {
        this.findById(id).orElseThrow(() -> new RuntimeException("User doesn't exist or is not allowed to access"));
        userRepository.deleteById(id);;
    }

}
