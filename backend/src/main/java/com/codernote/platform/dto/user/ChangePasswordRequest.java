package com.codernote.platform.dto.user;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Data
public class ChangePasswordRequest {

    @NotBlank(message = "Old password is required")
    private String oldPassword;

    @NotBlank(message = "New password is required")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{6,16}$", message = "New password must be 6-16 with letters and numbers")
    private String newPassword;

    @NotBlank(message = "Confirm new password is required")
    private String confirmPassword;
}
