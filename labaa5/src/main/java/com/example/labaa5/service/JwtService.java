package com.example.labaa5.service;

import com.example.labaa5.config.JwtProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    private final SecretKey key;
    private final long accessExpiration;
    private final long refreshExpiration;


    public JwtService(JwtProperties properties) {
        this.key = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
        this.accessExpiration = properties.getAccessTokenExpirationMs();
        this.refreshExpiration = properties.getRefreshTokenExpirationMs();
    }


    public String generateAccessToken(String username, Map<String, Object> extraClaims) {
        return buildToken(extraClaims, username, accessExpiration);
    }


    public String generateRefreshToken(String username, Map<String, Object> extraClaims) {
        // В refresh токен можно добавить тип claim, чтобы отличать его от access
        Map<String, Object> claimsWithRefreshFlag = new HashMap<>(extraClaims);
        claimsWithRefreshFlag.put("type", "refresh");
        return buildToken(claimsWithRefreshFlag, username, refreshExpiration);
    }


    private String buildToken(Map<String, Object> extraClaims, String subject, long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }


    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }


    public boolean isTokenValid(String token, String username) {
        try {
            final String extractedUsername = extractUsername(token);
            return (extractedUsername.equals(username)) && !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }


    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }


    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }


    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }


    public String extractTokenType(String token) {
        return extractClaim(token, claims -> (String) claims.get("type"));
    }
}