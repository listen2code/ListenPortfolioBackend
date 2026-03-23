package com.listen.portfolio.controller;

import com.listen.portfolio.jwt.JwtUtil;
import com.listen.portfolio.model.*;
import com.listen.portfolio.model.request.AuthRequest;
import com.listen.portfolio.model.request.ChangePasswordRequest;
import com.listen.portfolio.model.request.ForgotPasswordRequest;
import com.listen.portfolio.model.request.SignUpRequest;
import com.listen.portfolio.model.response.AuthResponse;
import com.listen.portfolio.service.UserService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserDetailsService userDetailsService;

    private final UserService userInfoService;

    public AuthController(UserService userInfoService) {
        this.userInfoService = userInfoService;
    }

    @PostMapping("/signUp")
    public ResponseEntity<ApiResponse<Void>> signUp(@RequestBody SignUpRequest signUpRequest) {
        boolean success = userInfoService.signUp(signUpRequest);
        if (success) {
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(null));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error("1001", "Username already exists"));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest authRequest) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(authRequest.getUserName(), authRequest.getPassword())
            );
        } catch (BadCredentialsException e) {
            logger.error("Invalid credentials for username {}", authRequest.getUserName());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("401", "Invalid credentials"));
        }

        final UserDetails userDetails = userDetailsService.loadUserByUsername(authRequest.getUserName());
        final String jwt = jwtUtil.generateToken(userDetails);
        final Long userId = userInfoService.getUserByName(authRequest.getUserName()).map(u -> u.getId()).orElse(null);
        logger.info("User logged in successfully: {}", authRequest.getUserName());

        return ResponseEntity.ok(ApiResponse.success(new AuthResponse(jwt, null, userId)));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout() {
        // For stateless JWT, logout is handled client-side by deleting the token.
        // This endpoint is conventional and confirms logout from the client's perspective.
        return ResponseEntity.ok(ApiResponse.success("Logout successful"));
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
