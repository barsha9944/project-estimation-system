package com.projectestimation.backend.auth.controller;

import com.projectestimation.backend.auth.dto.LoginRequest;
import com.projectestimation.backend.auth.dto.LoginResponse;
import com.projectestimation.backend.auth.dto.RegisterRequest;
import com.projectestimation.backend.auth.dto.RegisterResponse;
import com.projectestimation.backend.auth.service.AuthService;
import com.projectestimation.backend.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = authService.register(request);
        return ResponseEntity.ok(ApiResponse.success("User registered successfully", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }
}
