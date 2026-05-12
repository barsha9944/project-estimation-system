package com.projectestimation.backend.auth.service;

import com.projectestimation.backend.auth.dto.LoginRequest;
import com.projectestimation.backend.auth.dto.LoginResponse;
import com.projectestimation.backend.auth.dto.RegisterRequest;
import com.projectestimation.backend.auth.dto.RegisterResponse;
import com.projectestimation.backend.auth.model.User;
import com.projectestimation.backend.auth.repository.UserRepository;
import com.projectestimation.backend.common.exception.BadRequestException;
import com.projectestimation.backend.security.jwt.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Value("${app.security.jwt-expiration-seconds}")
    private long jwtExpirationSeconds;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    public RegisterResponse register(RegisterRequest request) {
        String normalizedEmail = request.email().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new BadRequestException("Email is already registered");
        }

        User user = new User();
        user.setFullName(request.fullName());
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(request.password()));
        User saved = userRepository.save(user);

        return new RegisterResponse(saved.getId(), saved.getFullName(), saved.getEmail(), saved.getCreatedAt());
    }

    public LoginResponse login(LoginRequest request) {
        String normalizedEmail = request.email().toLowerCase();
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizedEmail, request.password())
        );

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new BadRequestException("Invalid credentials"));

        String token = jwtService.generateToken(user);
        return new LoginResponse(
                token,
                "Bearer",
                jwtExpirationSeconds,
                user.getId(),
                user.getFullName(),
                user.getEmail()
        );
    }
}
