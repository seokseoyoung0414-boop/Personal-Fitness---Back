package com.nody.nodyspring.service;

import com.nody.nodyspring.dto.ConfirmDTO.RecaptchaResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class RecaptchaService {

    @Value("${recaptcha.secret}")
    private String secretKey;

    public boolean verifyToken(String token) {
        String url = "https://www.google.com/recaptcha/api/siteverify"
                + "?secret=" + secretKey + "&response=" + token;

        RestTemplate restTemplate = new RestTemplate();
        RecaptchaResponseDto response = restTemplate.postForObject(url, null, RecaptchaResponseDto.class);
        return response != null && response.isSuccess();
    }
}
