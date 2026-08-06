package com.fit.subscription.service;

import com.fit.subscription.dto.auth.AuthResponse;
import com.fit.subscription.dto.auth.LoginRequest;
import com.fit.subscription.entity.User;
import com.fit.subscription.repository.UserRepository;
import com.fit.subscription.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;

    private final JwtService jwtService;

    private final UserRepository userRepository;

    public AuthResponse login(LoginRequest request) {

        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                request.getEmail(),
                                request.getPassword()
                        )
                );

        String token =
                jwtService.generateToken(authentication.getName());

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return new AuthResponse(
                token,
                "Login Successful",
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole()
        );
    }
}
