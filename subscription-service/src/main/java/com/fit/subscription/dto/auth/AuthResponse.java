package com.fit.subscription.dto.auth;

import com.fit.subscription.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {

    private String token;

    private String message;

    private Long userId;

    private String name;

    private String email;

    private UserRole role;
}
