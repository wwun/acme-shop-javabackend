package com.wwun.acme.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static com.wwun.acme.security.RolesEnum.*;

import java.util.UUID;

public final class SecurityUtils {

    public static boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream()
                   .anyMatch(authies -> authies.getAuthority().equals(ROLE_ADMIN.toString()));
    }

    public static String getCurrentUsername() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if(principal instanceof AuthUserPrincipal userPrincipal){
            return userPrincipal.getUsername();
        }

        return String.valueOf(principal);
    }

    public static UUID getCurrentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if(principal instanceof AuthUserPrincipal userPrincipal){
            return userPrincipal.getUserId();
        }

        throw new IllegalStateException("No userId found in principal");
    }
}
