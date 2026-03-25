package com.listen.portfolio.api.v1.auth.dto;

import lombok.Data;

@Data
public class AuthRequest {
    private String userName;
    private String password;
}

