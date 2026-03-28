package com.listen.portfolio.api.v1.user.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class DeleteAccountRequest {
    @NotBlank(message = "userId must not be blank")
    private String userId;
}
