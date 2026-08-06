package com.fit.subscription.controller;

import com.fit.subscription.dto.auth.LoginRequest;
import com.fit.subscription.dto.auth.AuthResponse;
import com.fit.subscription.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public AuthResponse login(
            @Valid @RequestBody LoginRequest request) {

        return authService.login(request);
    }
}
