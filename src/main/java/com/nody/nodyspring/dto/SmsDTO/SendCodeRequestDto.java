package com.nody.nodyspring.dto.SmsDTO;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SendCodeRequestDto {
    @NotBlank(message = "전화번호는 필수입니다.")
    private String phone;
}
