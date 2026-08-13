package com.nody.nodyspring.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

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
        String code = generateCode();
        String sessionId = UUID.randomUUID().toString();

        smsCodeMap.put(phone, new CodeInfo(code, System.currentTimeMillis() + 3 * 60 * 1000)); // 3분 유효
        sessionMap.put(sessionId, phone);

        try {
            sendSMS(phone, code);
        } catch (Exception e) {
            throw new RuntimeException("SMS 전송 실패: " + e.getMessage(), e);
        }

        return sessionId;
    }

    public boolean verifyCode(String sessionId, String code) {
        String phone = sessionMap.get(sessionId);
        if (phone == null) return false;

        CodeInfo info = smsCodeMap.get(phone);
        return info != null && info.expiresAt >= System.currentTimeMillis() && info.code.equals(code);
    }

    public void sendSMS(String to, String code) throws Exception {
        String date = Instant.now().toString();
        String salt = UUID.randomUUID().toString().replace("-", "");
        String signature = getSignature(apiSecret, date, salt);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization",
                String.format("HMAC-SHA256 apiKey=%s, date=%s, salt=%s, signature=%s",
                        apiKey, date, salt, signature));

        Map<String, Object> message = new HashMap<>();
        message.put("from", senderPhone);
        message.put("to", to);
        message.put("text", "[NODY] 인증번호는 " + code + " 입니다.");
        message.put("type", "SMS");

        Map<String, Object> payload = new HashMap<>();
        payload.put("message", message);

        // ObjectMapper를 사용한 JSON 출력 (Jackson 라이브러리가 프로젝트에 있어야 함)
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
        RestTemplate rest = new RestTemplate();
        String response = rest.postForObject("https://api.coolsms.co.kr/messages/v4/send", request, String.class);
        System.out.println("[CoolSMS Response] " + response);
    }

    private String getSignature(String apiSecret, String date, String salt) throws Exception {
        String message = date + salt;
        SecretKeySpec signingKey = new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(signingKey);
        byte[] rawHmac = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(rawHmac);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private String generateCode() {
        return String.format("%06d", new Random().nextInt(1_000_000));
    }

    public String getPhoneBySessionId(String sessionId) {
        return sessionMap.get(sessionId);
    }

    private static class CodeInfo {
        String code;
        long expiresAt;

        CodeInfo(String code, long expiresAt) {
            this.code = code;
            this.expiresAt = expiresAt;
        }
    }
}
