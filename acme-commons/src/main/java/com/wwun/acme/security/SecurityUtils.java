package com.wwun.acme.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static com.wwun.acme.security.CategoriesEnum.*;

public class SecurityUtils {

    public static boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream()
                   .anyMatch(authies -> authies.getAuthority().equals(ROLE_ADMIN.toString()));
    }

    public static String getCurrentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
