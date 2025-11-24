package com.wwun.acme.security;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

//@Component    //this class is being anotated in commonsconfig
public class JwtService {

    private final Key signInKey;
    private long jwtExpiration;

    public JwtService(String secretKey, long jwtExpiration){
        this.jwtExpiration = jwtExpiration;
        this.signInKey = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(UserDetails userDetails, Map<String, Object> extraClaims){
        List<String> roles = userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .claim("roles", roles)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(signInKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateToken(UUID userId, String username, String email, List<String> roles){
        
    }

    public boolean validateToken(String token, UserDetails userDetails){
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    public String extractUsername(String token){
        return extractClaim(token, Claims::getSubject);
    }

    public List<String> extractRoles(String token){
        Claims claims = getAllClaims(token);
        Object raw = claims.get("roles");

        if (raw instanceof Collection<?> col) {
            return col.stream().map(String::valueOf).toList();
        }
        if (raw instanceof String str) {
            return Arrays.stream(str.split(","))
                         .map(String::trim)
                         .filter(s -> !s.isEmpty())
                         .toList();
        }
        return List.of();
    }

    public String extractUserId(String token){
        return extractClaim(token, claims -> claims.get("userId", String.class));
    }

    public boolean isTokenExpired(String token){
        return extractExpiration(token).before(new Date());
    }

    public Date extractExpiration(String token){
        return extractClaim(token, Claims::getExpiration);
    }

    public boolean isTokenValid(String token){
        try{
            Jwts.parserBuilder().setSigningKey(signInKey).build().parseClaimsJws(token);
            return true;
        }catch(Exception ex){
            return false;
        }
    }

    public <T> T extractClaim(String token, Function<Claims, T> resolver){
        final Claims claims = getAllClaims(token); //Se parsea el token JWT utilizando el método parseClaimsJws(token), se utiliza para verificar la firma del token JWT y extraer el payload (carga útil) que contiene los claims
        return resolver.apply(claims);  //función que se aplica al objeto Claims para extraer el claim deseado
    }

    private Claims getAllClaims(String token){
        return Jwts.parserBuilder()
                .setSigningKey(signInKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
    
}
