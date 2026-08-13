package com.nody.nodyspring.dto.ConfirmDTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class RecaptchaResponseDto {
    private boolean success;

    @JsonProperty("error-codes")
    private List<String> errorCodes;

    public List<String> getErrorCodes() {
        return errorCodes;
    }

    public void setErrorCodes(List<String> errorCodes) {
        this.errorCodes = errorCodes;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}
