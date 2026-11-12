package com.example.labaa5.controller;

import com.example.labaa5.dto.LoginRequest;
import com.example.labaa5.dto.RefreshRequest;
import com.example.labaa5.dto.RegisterRequest;
import com.example.labaa5.dto.TokenPairResponse;
import com.example.labaa5.model.User;
import com.example.labaa5.service.AuthService;
import com.example.labaa5.service.TokenPairService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final TokenPairService tokenPairService;
    private final AuthService authService;
    private final PasswordEncoder passwordEncoder;


    @PostMapping("/register")
    public ResponseEntity<TokenPairResponse> register(@Valid @RequestBody RegisterRequest request) {
        try {

            User newUser = authService.register(request);

            TokenPairResponse tokens = tokenPairService.issueNewPair(newUser.getEmail(), request.getDeviceId());

            return ResponseEntity.status(HttpStatus.CREATED).body(tokens);
        } catch (RuntimeException e) {

            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }


    @PostMapping("/login")
    public ResponseEntity<TokenPairResponse> login(@Valid @RequestBody LoginRequest request) {
        try {

            User user = authService.findUserByEmail(request.getEmail());


            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            TokenPairResponse tokens = tokenPairService.issueNewPair(user.getEmail(), request.getDeviceId());

            return ResponseEntity.ok(tokens);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }


    @PostMapping("/refresh")
    public ResponseEntity<TokenPairResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        try {
            TokenPairResponse newTokens = tokenPairService.refreshTokens(request.getRefreshToken());
            return ResponseEntity.ok(newTokens);
        } catch (IllegalArgumentException | SecurityException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
}