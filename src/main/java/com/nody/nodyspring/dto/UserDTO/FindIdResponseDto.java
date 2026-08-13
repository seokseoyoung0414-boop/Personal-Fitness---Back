package com.nody.nodyspring.dto.UserDTO;

public class FindIdResponseDto {
    private String userid;

    public FindIdResponseDto() {}

    public FindIdResponseDto(String userid) {
        this.userid = userid;
    }

    public String getUserid() {
        return userid;
    }

    public void setUserid(String userid) {
        this.userid = userid;
    }
}
