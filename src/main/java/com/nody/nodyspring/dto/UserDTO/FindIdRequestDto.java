package com.nody.nodyspring.dto.UserDTO;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class FindIdRequestDto {
    private String email;

    public FindIdRequestDto() {}

    public FindIdRequestDto(String email) {
        this.email = email;
    }

}