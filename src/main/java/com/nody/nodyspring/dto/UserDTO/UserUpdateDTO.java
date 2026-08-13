package com.nody.nodyspring.dto.UserDTO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserUpdateDTO {
    private String userid;
    private String nickname;
    private String email;
    private String phone;
    private String password;
}
