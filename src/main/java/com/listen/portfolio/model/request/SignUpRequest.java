package com.listen.portfolio.model.request;

import lombok.Data;

@Data
public class SignUpRequest {
    private String userName;
    private String password;
    private String email;
}
