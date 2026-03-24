package com.snehil.project.lovable_clone.controller;

import com.snehil.project.lovable_clone.dto.auth.AuthResponse;
import com.snehil.project.lovable_clone.dto.auth.LoginRequest;
import com.snehil.project.lovable_clone.dto.auth.SignupRequest;
import com.snehil.project.lovable_clone.dto.auth.UserProfileResponse;
import com.snehil.project.lovable_clone.security.JwtUserPrincipal;
import com.snehil.project.lovable_clone.service.AuthService;
import com.snehil.project.lovable_clone.service.UserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@FieldDefaults(makeFinal = true,level = AccessLevel.PRIVATE)
public class AuthController {

    AuthService authService;
    UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@RequestBody SignupRequest request) {
        return ResponseEntity.ok(authService.signup(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    public  ResponseEntity<UserProfileResponse> getProfile(
            @AuthenticationPrincipal JwtUserPrincipal currentUser) {
        return ResponseEntity.ok(userService.getProfile(currentUser.userId()));
    }


}
