package com.listen.portfolio.api.v1.auth.dto;

import lombok.Data;

@Data
public class ChangePasswordRequest {
    private String userId;
    private String oldPassword;
    private String newPassword;
}

