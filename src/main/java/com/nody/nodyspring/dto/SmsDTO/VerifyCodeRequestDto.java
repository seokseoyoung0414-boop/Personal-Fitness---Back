package com.nody.nodyspring.dto.SmsDTO;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyCodeRequestDto {
    @NotBlank(message = "세션 ID는 필수입니다.")
    private String sessionId;

    @NotBlank(message = "인증번호는 필수입니다.")
    private String code;
}
