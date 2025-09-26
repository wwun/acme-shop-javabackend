package com.wwun.acme.user.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.wwun.acme.security.JwtService;
import com.wwun.acme.user.dto.auth.AuthRequestDTO;
import com.wwun.acme.user.dto.auth.AuthResponseDTO;
import com.wwun.acme.user.dto.auth.RegisterRequestDTO;
import com.wwun.acme.user.entity.Role;
import com.wwun.acme.user.entity.User;
import com.wwun.acme.user.enums.CategoriesEnum;
import com.wwun.acme.user.repository.RoleRepository;
import com.wwun.acme.user.repository.UserRepository;

@Service
public class AuthServiceImpl implements AuthService{

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    
    public AuthServiceImpl(UserRepository userRepository, RoleRepository roleRepository, AuthenticationManager authenticationManager, PasswordEncoder passwordEncoder, JwtService jwtService){
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.authenticationManager = authenticationManager;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;        
    }

    @Override
    public User register(RegisterRequestDTO registerRequestDTO) {

        if(userRepository.existsByEmail(registerRequestDTO.getEmail())){
            throw new RuntimeException("Email already in use"); //wwun
        }
        if(userRepository.existsByUsername(registerRequestDTO.getUsername())){
            throw new RuntimeException("Username already in use");  //wwun
        }

        Role defaultRole = roleRepository.findByName(CategoriesEnum.ROLE_USER.toString()).orElseThrow(() -> new RuntimeException("Default role not found"));

        User user = new User(registerRequestDTO.getUsername(), passwordEncoder.encode(registerRequestDTO.getPassword()), registerRequestDTO.getEmail(), List.of(defaultRole));

        return userRepository.save(user);
    }

    @Override
    public AuthResponseDTO login(AuthRequestDTO authRequestDTO) {
        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(authRequestDTO.getUsername(), authRequestDTO.getPassword()));
        
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow(() -> new RuntimeException("User not found"));

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId().toString());

        String token = jwtService.generateToken(userDetails, claims);

        return new AuthResponseDTO(token, userDetails.getUsername());
    }


}