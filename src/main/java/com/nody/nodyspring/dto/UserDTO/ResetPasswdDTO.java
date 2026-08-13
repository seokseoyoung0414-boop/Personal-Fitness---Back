package com.nody.nodyspring.dto.UserDTO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswdDTO {

    public ResetPasswdDTO() {}

    private String sessionId;
    private String newPassword;
}
