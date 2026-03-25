package com.listen.portfolio.api.v1.auth.dto;

import lombok.Data;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Data
public class ForgotPasswordRequest {
    @NotBlank(message = "email must not be blank")
    @Email(message = "email must be valid")
    private String email;
}
