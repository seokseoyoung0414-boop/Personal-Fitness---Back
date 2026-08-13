package com.nody.nodyspring.controller;

import com.nody.nodyspring.DB.User;
import com.nody.nodyspring.config.JwtConfig;
import com.nody.nodyspring.dto.UserDTO.*;
import com.nody.nodyspring.repository.UserRepository;
import com.nody.nodyspring.service.RecaptchaService;
import com.nody.nodyspring.service.SmsService;
import com.nody.nodyspring.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtConfig jwtConfig;

    @Autowired
    private RecaptchaService recaptchaService;

    @Autowired
    private SmsService smsService;
    @Autowired
    private UserRepository userRepository;

    @PostMapping("/check")
    public ResponseEntity<?> checkDuplicate(@RequestBody @Valid SignupRequestDto dto) {
        try {
            userService.signupCheck(dto);
            return ResponseEntity.ok(Map.of("message", "중복 없음"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody @Valid SignupRequestDto dto) {
        try {
            boolean validCaptcha = recaptchaService.verifyToken(dto.getRecaptchaToken());
            if (!validCaptcha) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "reCAPTCHA 인증 실패"));
            }

            userService.signup(dto);
            return ResponseEntity.ok(Map.of("message", "회원가입 완료"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid LoginRequestDto dto,
                                   HttpServletResponse response) {
        try {
            // 1) 캡차 검증
            if (!recaptchaService.verifyToken(dto.getRecaptchaToken())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "reCAPTCHA 인증 실패"));
            }

            // 2) 비번·아이디 확인
            User user = userService.login(dto);

            /* 🔥 3) 제재 여부 체크 (추가) */
            LocalDateTime bannedUntil = user.getBannedUntil();
            if (bannedUntil != null && bannedUntil.isAfter(LocalDateTime.now())) {
                String msg = bannedUntil.getYear() == 9999
                        ? "무기한 제재된 계정입니다."
                        : "제재 해제 시각: " + bannedUntil.toString().replace('T', ' ');
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", msg));
            }

            // 4) JWT 발급 & 쿠키 세팅
            String token = jwtConfig.createToken(user.getUserid());
            response.setHeader("Set-Cookie",
                    "token=" + token +
                            "; Path=/; HttpOnly; Max-Age=86400; SameSite=None; Secure");

            // 5) 성공 응답
            return ResponseEntity.ok(Map.of(
                    "message", "로그인 성공",
                    "userid", user.getUserid(),
                    "nickname", user.getNickname()
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswdDTO request) {
        try {
            userService.resetPassword(request, smsService);
            return ResponseEntity.ok(Map.of("message", "비밀번호 재설정 완료"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/findID")
    public ResponseEntity<?> findUserIdByEmail(@RequestBody FindIdRequestDto request) {
        String email = request.getEmail();

        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "이메일을 입력해주세요."));
        }

        try {
            String userId = userService.findUserIdByEmail(email);
            if (userId != null) {
                return ResponseEntity.ok(new FindIdResponseDto(userId));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "해당 이메일의 사용자가 없습니다."));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "서버 오류가 발생했습니다."));
        }
    }

    @GetMapping("/usermapping")
    public ResponseEntity<?> getUserNicknameMap() {
        Map<String, String> result = userService.getAllUserNicknameMap();
        return ResponseEntity.ok(result);  // { "user1": "닉네임1", ... }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@CookieValue(name = "token", required = false) String token) {
        System.out.println("🔥 /user/me 요청 들어옴");
        System.out.println("▶ [토큰 체크] 받은 쿠키 토큰: " + token); // 로그 추가

        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "토큰이 없습니다."));
        }

        try {
            String userid = jwtConfig.getUserIdFromToken(token);
            System.out.println("▶ [JWT 파싱 결과] userid = " + userid); // 로그 추가

            User user = userService.findByUserid(userid);

            return ResponseEntity.ok(Map.of(
                    "userid", user.getUserid(),
                    "nickname", user.getNickname(),
                    "email", user.getEmail(),
                    "phone", user.getPhone(),
                    "point", user.getPoint()
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "유효하지 않은 토큰입니다."));
        }
    }
    @PostMapping("/update")
    public ResponseEntity<?> updateUser(@RequestBody UserUpdateDTO dto) {
        try {
            boolean updated = userService.updateUser(dto);

            if (updated) {
                return ResponseEntity.ok("회원정보가 수정되었습니다.");
            } else {
                return ResponseEntity.badRequest().body("해당 유저를 찾을 수 없습니다.");
            }

        } catch (DataIntegrityViolationException ex) {
            String message = ex.getRootCause().getMessage(); // DB 예외 메시지 추출

            // 예외 메시지에서 어떤 필드가 중복인지 감지
            String duplicateField = "중복된 값이 있습니다.";
            if (message.contains("nickname")) {
                duplicateField = "이미 사용 중인 닉네임입니다.";
            } else if (message.contains("email")) {
                duplicateField = "이미 사용 중인 이메일입니다.";
            } else if (message.contains("phone")) {
                duplicateField = "이미 사용 중인 휴대폰번호입니다.";
            }

            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(duplicateField); // 프론트로 에러 메시지 전송
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("회원정보 수정 중 오류가 발생했습니다.");
        }
    }
    @GetMapping("/all")
    public List<User> getAllUsers(@CookieValue(name = "token", required = false) String token) {
        if (token == null || token.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "토큰 없음");
        }

        String userid = jwtConfig.getUserIdFromToken(token);
        if (!userid.equals("root")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "권한 없음");
        }

        return userRepository.findAll();
    }
}
