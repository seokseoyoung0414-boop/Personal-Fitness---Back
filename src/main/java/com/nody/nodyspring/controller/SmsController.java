package com.nody.nodyspring.controller;


import com.nody.nodyspring.dto.SmsDTO.SendCodeRequestDto;
import com.nody.nodyspring.dto.SmsDTO.VerifyCodeRequestDto;
import com.nody.nodyspring.repository.UserRepository;
import com.nody.nodyspring.service.SmsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/sms")
@RequiredArgsConstructor
public class SmsController {
    private final SmsService verificationService;
    private final UserRepository userRepository;

    // 1️⃣ 회원가입 시 인증번호 전송 (등록 여부 확인 안함)
    @PostMapping("/send-code/signup")
    public ResponseEntity<?> sendCodeForSignup(@RequestBody @Valid SendCodeRequestDto dto) {
        String sessionId = verificationService.sendCode(dto.getPhone());
        return ResponseEntity.ok(Map.of("message", "회원가입 인증번호 전송 완료", "sessionId", sessionId));
    }

    // 2️⃣ 비밀번호 찾기 시 인증번호 전송 (등록된 번호인지 확인)
    @PostMapping("/send-code/reset")
    public ResponseEntity<?> sendCodeForPasswordReset(@RequestBody @Valid SendCodeRequestDto dto) {
        boolean exists = userRepository.existsByPhone(dto.getPhone());
        if (!exists) {
            return ResponseEntity.status(409).body(Map.of("message", "등록되지 않은 번호입니다."));
        }

        String sessionId = verificationService.sendCode(dto.getPhone());
        return ResponseEntity.ok(Map.of("message", "비밀번호 재설정 인증번호 전송 완료", "sessionId", sessionId));
    }

    // 3️⃣ 인증번호 확인
    @PostMapping("/verify-code")
    public ResponseEntity<?> verifyCode(@RequestBody @Valid VerifyCodeRequestDto dto) {
        if (verificationService.verifyCode(dto.getSessionId(), dto.getCode())) {
            return ResponseEntity.ok(Map.of("message", "인증 성공", "verified", true));
        } else {
            return ResponseEntity.status(400).body(Map.of("message", "인증 실패"));
        }
    }
}
