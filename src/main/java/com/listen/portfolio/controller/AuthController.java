package com.listen.portfolio.controller;

import com.listen.portfolio.model.*;
import com.listen.portfolio.model.request.AuthRequest;
import com.listen.portfolio.model.request.ChangePasswordRequest;
import com.listen.portfolio.model.request.ForgotPasswordRequest;
import com.listen.portfolio.model.request.SignUpRequest;
import com.listen.portfolio.model.response.AuthResponse;
import com.listen.portfolio.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/auth")
public class AuthController {

    private final UserService userInfoService;

    public AuthController(UserService userInfoService) {
        this.userInfoService = userInfoService;
    }

    @PostMapping("/signUp")
    public ResponseEntity<ApiResponse<Void>> signUp(@RequestBody SignUpRequest signUpRequest) {
        userInfoService.signUp(signUpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(null));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@RequestBody AuthRequest authRequest) {
        return userInfoService.login(authRequest)
                .map(authResponse -> ResponseEntity.ok(ApiResponse.success(authResponse)))
                .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("401", "Invalid credentials")));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@RequestBody ChangePasswordRequest changePasswordRequest) {
        boolean success = userInfoService.changePassword(changePasswordRequest);
        if (success) {
            return ResponseEntity.ok(ApiResponse.success(null));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error("400", "Password change failed"));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@RequestBody ForgotPasswordRequest forgotPasswordRequest) {
        boolean success = userInfoService.forgotPassword(forgotPasswordRequest);
        if (success) {
            return ResponseEntity.ok(ApiResponse.success(null));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error("400", "Password change failed"));
        }
    }

    @DeleteMapping("/delete-account")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(@RequestParam Long userId) {
        boolean success = userInfoService.deleteAccount(userId);
        if (success) {
            return ResponseEntity.ok(ApiResponse.success(null));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("404", "User not found"));
        }
    }
}
