package com.listen.portfolio.api.v1.auth.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
public class AuthRequest {
    @NotBlank(message = "username must not be blank")
    @Size(min = 3, max = 50, message = "username length must be between 3 and 50")
    private String userName;
    @NotBlank(message = "password must not be blank")
    @Size(min = 6, message = "password length must be at least 6")
    private String password;
}
