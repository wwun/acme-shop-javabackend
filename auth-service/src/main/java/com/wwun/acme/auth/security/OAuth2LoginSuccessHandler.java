package com.wwun.acme.auth.security;

import java.io.IOException;
import java.security.AuthProvider;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wwun.acme.auth.dto.AuthResponseDTO;
import com.wwun.acme.auth.dto.OAuthUserUpsertRequestDTO;
import com.wwun.acme.auth.dto.RoleResponseDTO;
import com.wwun.acme.auth.dto.UserAuthResponseDTO;
import com.wwun.acme.auth.service.UserGatewayService;
import com.wwun.acme.security.AuthProviderEnum;
import com.wwun.acme.security.JwtService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler{

    private final UserGatewayService userGatewayService;
    private final JwtService jwtService;

    public OAuth2LoginSuccessHandler(UserGatewayService userGatewayService, JwtService jwtService){
        this.userGatewayService = userGatewayService;
        this.jwtService = jwtService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
                
                OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
                String registrationId = oauthToken.getAuthorizedClientRegistrationId();

                OAuth2User oAuthUser = (OAuth2User) oauthToken.getPrincipal();

                String sub = oAuthUser.getAttribute("sub");
                String email = oAuthUser.getAttribute("email");
                String name = oAuthUser.getAttribute("name");
                String picture = oAuthUser.getAttribute("picture");

                AuthProviderEnum provider = switch(registrationId.toLowerCase()){
                    case "google" -> AuthProviderEnum.GOOGLE;
                    case "github" -> AuthProviderEnum.GITHUB;
                    default -> AuthProviderEnum.LOCAL;
                };

                OAuthUserUpsertRequestDTO oAuthUserUpsertRequestDTO = new OAuthUserUpsertRequestDTO(provider, sub, email, name, picture);

                UserAuthResponseDTO user = userGatewayService.upsert(oAuthUserUpsertRequestDTO);

                List<String> roles = user.roles().stream().map(RoleResponseDTO::name).toList();

                String token = jwtService.generateToken(user.id(), user.username(), user.email(), roles);

                AuthResponseDTO authResponseDTO = new AuthResponseDTO(token, user.username(), user.email());

                response.setContentType("application/json");
                response.getWriter().write(new ObjectMapper().writeValueAsString(authResponseDTO));

    }    

}
