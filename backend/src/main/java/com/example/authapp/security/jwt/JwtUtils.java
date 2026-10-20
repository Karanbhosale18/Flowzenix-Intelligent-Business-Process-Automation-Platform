package com.example.authapp.security.jwt;

import com.example.authapp.security.services.UserDetailsImpl;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Date;

@Component
public class JwtUtils {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

    @Value("${app.jwt.secret:flowzenix-default-jwt-secret-key-must-be-at-least-32-bytes-long-for-hs256}")
    private String jwtSecret;

    @Value("${app.jwt.expirationMs:86400000}")
    private int jwtExpirationMs;

    private SecretKey key() {
        byte[] keyBytes;
        if (jwtSecret != null && jwtSecret.getBytes(StandardCharsets.UTF_8).length >= 32) {
            keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        } else {
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                keyBytes = md.digest((jwtSecret != null ? jwtSecret : "flowzenix-default-jwt-secret-key-hs256").getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                keyBytes = "flowzenix-default-jwt-secret-key-must-be-at-least-32-bytes-long-for-hs256".getBytes(StandardCharsets.UTF_8);
            }
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateJwtToken(Authentication authentication) {
        UserDetailsImpl userPrincipal = (UserDetailsImpl) authentication.getPrincipal();
        return generateTokenFromUsername(userPrincipal.getUsername());
    }

    public String generateTokenFromUsername(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key(), Jwts.SIG.HS256)
                .compact();
    }

    public String getUsernameFromJwtToken(String token) {
        return Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    /**
     * Validates signature, expiry and format. Any failure is logged safely without
     * leaking token contents, and returns false so the security filter chain rejects
     * invalid credentials cleanly.
     */
    public boolean validateJwtToken(String authToken) {
        try {
            Jwts.parser().verifyWith(key()).build().parseSignedClaims(authToken);
            return true;
        } catch (SecurityException e) {
            logger.warn("Invalid JWT signature");
        } catch (MalformedJwtException e) {
            logger.warn("Invalid JWT token format");
        } catch (ExpiredJwtException e) {
            logger.warn("JWT token is expired");
        } catch (UnsupportedJwtException e) {
            logger.warn("JWT token is unsupported");
        } catch (IllegalArgumentException e) {
            logger.warn("JWT claims string is empty");
        }
        return false;
    }
}
