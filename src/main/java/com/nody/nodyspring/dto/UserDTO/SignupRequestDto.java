package com.nody.nodyspring.dto.UserDTO;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class SignupRequestDto {
    @NotBlank
    private String userid;
    @NotBlank
    private String passwd;
    @NotBlank
    private String nickname;
    @NotBlank
    private String email;
    @NotBlank
    private String phone;

    private String recaptchaToken;
}
