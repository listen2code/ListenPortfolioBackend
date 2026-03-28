package com.listen.portfolio.api.v1.user.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
public class ChangePasswordRequest {
    @NotBlank(message = "userId must not be blank")
    private String userId;
    @NotBlank(message = "oldPassword must not be blank")
    private String oldPassword;
    @NotBlank(message = "newPassword must not be blank")
    @Size(min = 6, message = "newPassword length must be at least 6")
    private String newPassword;
}

