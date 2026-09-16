package com.nody.nodyspring.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@Service
public class SmsService {

    private final Map<String, CodeInfo> smsCodeMap = new HashMap<>();
    private final Map<String, String> sessionMap = new HashMap<>();

    @Value("${coolsms.api_key}")
    private String apiKey;

    @Value("${coolsms.api_secret}")
    private String apiSecret;

    @Value("${coolsms.sender_phone}")
    private String senderPhone;

    public String sendCode(String phone) {

        // 1. 인증번호 생성
        String code = generateCode();

        // 2. 세션 ID 생성
        String sessionId = UUID.randomUUID().toString();

        // 3. 인증번호 먼저 저장
        smsCodeMap.put(
                phone,
                new CodeInfo(
                        code,
                        System.currentTimeMillis() + 3 * 60 * 1000
                )
        );

        sessionMap.put(sessionId, phone);

        // 개발 테스트용
        System.out.println("========================================");
        System.out.println("[SMS 인증번호 생성]");
        System.out.println("phone      : " + phone);
        System.out.println("sessionId  : " + sessionId);
        System.out.println("code       : " + code);
        System.out.println("========================================");

        // 4. SMS 발송
        try {

            sendSMS(phone, code);

            System.out.println("========================================");
            System.out.println("[SMS 전송 성공]");
            System.out.println("phone : " + phone);
            System.out.println("========================================");

        } catch (HttpStatusCodeException e) {

            // SMS 발송 실패해도 인증번호와 sessionId는 유지
            System.out.println("========================================");
            System.out.println("[SMS 전송 실패 - 인증번호는 유지]");
            System.out.println("phone  : " + phone);
            System.out.println("status : " + e.getStatusCode());
            System.out.println("error  : " + e.getResponseBodyAsString());
            System.out.println("code   : " + code);
            System.out.println("========================================");

        } catch (Exception e) {

            // 기타 오류가 발생해도 인증번호는 유지
            System.out.println("========================================");
            System.out.println("[SMS 전송 실패 - 인증번호는 유지]");
            System.out.println("phone : " + phone);
            System.out.println("error : " + e.getMessage());
            System.out.println("code  : " + code);
            System.out.println("========================================");
        }

        // 5. SMS 성공/실패와 관계없이 sessionId 반환
        return sessionId;
    }

    public boolean verifyCode(String sessionId, String code) {

        // sessionId로 전화번호 찾기
        String phone = sessionMap.get(sessionId);

        if (phone == null) {
            return false;
        }

        // 전화번호로 저장된 인증번호 찾기
        CodeInfo info = smsCodeMap.get(phone);

        if (info == null) {
            return false;
        }

        // 만료 여부 확인
        if (info.expiresAt < System.currentTimeMillis()) {
            return false;
        }

        // 인증번호 비교
        return info.code.equals(code);
    }

    public void sendSMS(String to, String code) throws Exception {

        String date = Instant.now().toString();
        String salt = UUID.randomUUID()
                .toString()
                .replace("-", "");

        String signature = getSignature(
                apiSecret,
                date,
                salt
        );

        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_JSON);

        headers.set(
                "Authorization",
                String.format(
                        "HMAC-SHA256 apiKey=%s, date=%s, salt=%s, signature=%s",
                        apiKey,
                        date,
                        salt,
                        signature
                )
        );

        Map<String, Object> message = new HashMap<>();

        message.put("from", senderPhone);
        message.put("to", to);
        message.put(
                "text",
                "[Fit] 인증번호는 " + code + " 입니다."
        );
        message.put("type", "SMS");

        Map<String, Object> payload = new HashMap<>();

        payload.put("message", message);

        HttpEntity<Map<String, Object>> request =
                new HttpEntity<>(payload, headers);

        RestTemplate rest = new RestTemplate();

        try {

            String response = rest.postForObject(
                    "https://api.coolsms.co.kr/messages/v4/send",
                    request,
                    String.class
            );

            System.out.println("[CoolSMS Response] " + response);

        } catch (HttpStatusCodeException e) {

            System.out.println("======================================");
            System.out.println("[CoolSMS API ERROR]");
            System.out.println("HTTP STATUS : " + e.getStatusCode());
            System.out.println(
                    "RESPONSE    : " + e.getResponseBodyAsString()
            );
            System.out.println("======================================");

            throw e;
        }
    }

    private String getSignature(
            String apiSecret,
            String date,
            String salt
    ) throws Exception {

        String message = date + salt;

        SecretKeySpec signingKey =
                new SecretKeySpec(
                        apiSecret.getBytes(StandardCharsets.UTF_8),
                        "HmacSHA256"
                );

        Mac mac = Mac.getInstance("HmacSHA256");

        mac.init(signingKey);

        byte[] rawHmac =
                mac.doFinal(
                        message.getBytes(StandardCharsets.UTF_8)
                );

        return bytesToHex(rawHmac);
    }

    private String bytesToHex(byte[] bytes) {

        StringBuilder sb = new StringBuilder();

        for (byte b : bytes) {
            sb.append(
                    String.format("%02x", b)
            );
        }

        return sb.toString();
    }

    private String generateCode() {

        return String.format(
                "%06d",
                new Random().nextInt(1_000_000)
        );
    }

    public String getPhoneBySessionId(String sessionId) {

        return sessionMap.get(sessionId);
    }

    private static class CodeInfo {

        String code;
        long expiresAt;

        CodeInfo(
                String code,
                long expiresAt
        ) {
            this.code = code;
            this.expiresAt = expiresAt;
        }
    }
}