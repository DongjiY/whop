package com.whop.backend.auth;

import com.whop.backend.auth.AuthDtos.LoginRequest;
import com.whop.backend.auth.AuthDtos.MeResponse;
import com.whop.backend.auth.AuthDtos.SignupRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
    }

    @Transactional
    public MeResponse signup(
            SignupRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String normalizedUsername = request.username().trim().toLowerCase();
        if (userRepository.existsByUsernameIgnoreCase(normalizedUsername)) {
            throw new DuplicateUsernameException();
        }

        UserEntity user = new UserEntity();
        user.setUsername(normalizedUsername);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        UserEntity saved = userRepository.save(user);

        authenticateAndSaveSession(normalizedUsername, request.password(), httpRequest, httpResponse);
        return new MeResponse(saved.getId(), saved.getUsername(), saved.getCreatedAt());
    }

    public MeResponse login(
            LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        Authentication authentication =
                authenticateAndSaveSession(
                        request.username().trim().toLowerCase(),
                        request.password(),
                        httpRequest,
                        httpResponse);
        AuthUserPrincipal principal = (AuthUserPrincipal) authentication.getPrincipal();
        UserEntity user =
                userRepository
                        .findById(principal.getId())
                        .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
        return new MeResponse(user.getId(), user.getUsername(), user.getCreatedAt());
    }

    public void logout(HttpServletRequest request, HttpServletResponse response) {
        SecurityContextHolder.clearContext();
        securityContextRepository.saveContext(new SecurityContextImpl(), request, response);
        if (request.getSession(false) != null) {
            request.getSession(false).invalidate();
        }
    }

    public MeResponse me(Authentication authentication) {
        AuthUserPrincipal principal = (AuthUserPrincipal) authentication.getPrincipal();
        return userRepository
                .findById(principal.getId())
                .map(user -> new MeResponse(user.getId(), user.getUsername(), user.getCreatedAt()))
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
    }

    private Authentication authenticateAndSaveSession(
            String username,
            String password,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        Authentication authentication =
                authenticationManager.authenticate(
                        UsernamePasswordAuthenticationToken.unauthenticated(username, password));
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
        securityContextRepository.saveContext(securityContext, httpRequest, httpResponse);
        return authentication;
    }
}
