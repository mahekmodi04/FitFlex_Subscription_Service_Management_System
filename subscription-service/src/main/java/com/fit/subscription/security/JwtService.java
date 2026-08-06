package com.fit.subscription.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    // ------------------------
    // Secret Key
    // ------------------------

    private SecretKey getSigningKey() {

        return Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );
    }

    // ------------------------
    // Generate JWT
    // ------------------------

    public String generateToken(String email) {

        return Jwts.builder()

                .subject(email)

                .issuedAt(new Date())

                .expiration(new Date(System.currentTimeMillis() + expiration))

                .signWith(getSigningKey())

                .compact();
    }

    // ------------------------
    // Extract Username
    // ------------------------

    public String extractUsername(String token) {

        return extractClaim(token, Claims::getSubject);
    }

    // ------------------------
    // Extract Expiration
    // ------------------------

    public Date extractExpiration(String token) {

        return extractClaim(token, Claims::getExpiration);
    }

    // ------------------------
    // Generic Claim Extractor
    // ------------------------

    public <T> T extractClaim(String token,
                              Function<Claims, T> resolver) {

        Claims claims = extractAllClaims(token);

        return resolver.apply(claims);
    }

    // ------------------------
    // Parse Token
    // ------------------------

    private Claims extractAllClaims(String token) {

        return Jwts.parser()

                .verifyWith(getSigningKey())

                .build()

                .parseSignedClaims(token)

                .getPayload();
    }

    // ------------------------
    // Expiry Check
    // ------------------------

    private boolean isTokenExpired(String token) {

        return extractExpiration(token)

                .before(new Date());
    }

    // ------------------------
    // Validate JWT
    // ------------------------

    public boolean validateToken(String token,
                                 UserDetails userDetails) {

        try {

            String username = extractUsername(token);

            return username.equals(userDetails.getUsername())
                    && !isTokenExpired(token);

        }

        catch (JwtException | IllegalArgumentException ex) {

            return false;
        }
    }

}
