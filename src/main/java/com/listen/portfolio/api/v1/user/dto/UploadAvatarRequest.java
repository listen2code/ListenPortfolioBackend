package com.listen.portfolio.api.v1.user.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class UploadAvatarRequest {
    @NotBlank(message = "avatar must not be blank")
    private String avatar;
}
