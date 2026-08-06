package com.fit.subscription.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    private final CustomUserDetailsService customUserDetailsService;

    @Override
    protected void doFilterInternal(

            HttpServletRequest request,

            HttpServletResponse response,

            FilterChain filterChain)

            throws ServletException, IOException {

        // Step 1 : Read Authorization Header

        String authHeader = request.getHeader("Authorization");

        // Step 2 : No JWT -> Skip

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {

            filterChain.doFilter(request, response);

            return;
        }

//        // Step 3 : Extract Token
//
//        String jwt = authHeader.substring(7);
//
//        // Step 4 : Extract Username
//
//        String username = jwtService.extractUsername(jwt);
        String jwt = authHeader.substring(7);

        String username;

        try {
            username = jwtService.extractUsername(jwt);
        } catch (Exception e) {
            filterChain.doFilter(request, response);
            return;
        }

        // Step 5 : Authenticate only if not already authenticated

        if (username != null &&
                SecurityContextHolder.getContext().getAuthentication() == null) {

            UserDetails userDetails =
                    customUserDetailsService.loadUserByUsername(username);

            // Step 6 : Validate JWT

            if (jwtService.validateToken(jwt, userDetails)) {

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(

                                userDetails,

                                null,

                                userDetails.getAuthorities()

                        );

                authentication.setDetails(

                        new WebAuthenticationDetailsSource()

                                .buildDetails(request)

                );

                // Step 7 : Store Authentication

                SecurityContextHolder.getContext()

                        .setAuthentication(authentication);

            }

        }

        // Step 8 : Continue Request

        filterChain.doFilter(request, response);

    }

}
