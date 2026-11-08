package com.nody.nodyspring.controller;

import com.nody.nodyspring.DB.User;
import com.nody.nodyspring.config.JwtConfig;
import com.nody.nodyspring.dto.UserDTO.*;
import com.nody.nodyspring.repository.UserRepository;
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
    private SmsService smsService;

    @Autowired
    private UserRepository userRepository;


    // 🔵 회원가입 전 중복 체크
    @PostMapping("/check")
    public ResponseEntity<?> checkDuplicate(@RequestBody @Valid SignupRequestDto dto) {
        try {
            userService.signupCheck(dto);
            return ResponseEntity.ok(Map.of("message", "중복 없음"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }


    // 🔴 reCAPTCHA 검증 제거
    // 🔵 회원가입
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody @Valid SignupRequestDto dto) {
        try {

            userService.signup(dto);

            return ResponseEntity.ok(
                    Map.of("message", "회원가입 완료")
            );

        } catch (IllegalArgumentException e) {

            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }


    // 🔴 reCAPTCHA 검증 제거
    // 🔵 로그인
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody @Valid LoginRequestDto dto,
            HttpServletResponse response
    ) {

        try {

            // 1) 아이디 / 비밀번호 확인
            User user = userService.login(dto);


            // 2) 제재 여부 체크
            LocalDateTime bannedUntil = user.getBannedUntil();

            if (bannedUntil != null &&
                    bannedUntil.isAfter(LocalDateTime.now())) {

                String msg = bannedUntil.getYear() == 9999
                        ? "무기한 제재된 계정입니다."
                        : "제재 해제 시각: "
                          + bannedUntil.toString().replace('T', ' ');

                return ResponseEntity
                        .status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", msg));
            }


            // 3) JWT 발급 & 쿠키 세팅
            String token =
                    jwtConfig.createToken(user.getUserid());

            response.setHeader(
                    "Set-Cookie",
                    "token=" + token +
                            "; Path=/;" +
                            " HttpOnly;" +
                            " Max-Age=86400;" +
                            " SameSite=None;" +
                            " Secure"
            );


            // 4) 로그인 성공 응답
            return ResponseEntity.ok(
                    Map.of(
                            "message", "로그인 성공",
                            "userid", user.getUserid(),
                            "nickname", user.getNickname()
                    )
            );

        } catch (IllegalArgumentException e) {

            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }


    // 🔵 비밀번호 재설정
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(
            @RequestBody ResetPasswdDTO request
    ) {

        try {

            userService.resetPassword(
                    request,
                    smsService
            );

            return ResponseEntity.ok(
                    Map.of("message", "비밀번호 재설정 완료")
            );

        } catch (IllegalArgumentException e) {

            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }


    // 🔵 아이디 찾기
    @PostMapping("/findID")
    public ResponseEntity<?> findUserIdByEmail(
            @RequestBody FindIdRequestDto request
    ) {

        String email = request.getEmail();

        if (email == null || email.isBlank()) {

            return ResponseEntity.badRequest()
                    .body(
                            Map.of(
                                    "message",
                                    "이메일을 입력해주세요."
                            )
                    );
        }

        try {

            String userId =
                    userService.findUserIdByEmail(email);

            if (userId != null) {

                return ResponseEntity.ok(
                        new FindIdResponseDto(userId)
                );

            } else {

                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(
                                Map.of(
                                        "message",
                                        "해당 이메일의 사용자가 없습니다."
                                )
                        );
            }

        } catch (Exception e) {

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                            Map.of(
                                    "message",
                                    "서버 오류가 발생했습니다."
                            )
                    );
        }
    }


    // 🔵 userid - nickname 매핑
    @GetMapping("/usermapping")
    public ResponseEntity<?> getUserNicknameMap() {

        Map<String, String> result =
                userService.getAllUserNicknameMap();

        return ResponseEntity.ok(result);
    }


    // 🔵 현재 로그인 사용자 조회
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(
            @CookieValue(
                    name = "token",
                    required = false
            )
            String token
    ) {

        System.out.println("🔥 /user/me 요청 들어옴");

        System.out.println(
                "▶ [토큰 체크] 받은 쿠키 토큰: "
                        + token
        );


        if (token == null || token.isEmpty()) {

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(
                            Map.of(
                                    "message",
                                    "토큰이 없습니다."
                            )
                    );
        }


        try {

            String userid =
                    jwtConfig.getUserIdFromToken(token);

            System.out.println(
                    "▶ [JWT 파싱 결과] userid = "
                            + userid
            );


            User user =
                    userService.findByUserid(userid);


            return ResponseEntity.ok(
                    Map.of(
                            "userid", user.getUserid(),
                            "nickname", user.getNickname(),
                            "email", user.getEmail(),
                            "phone", user.getPhone()
                    )
            );

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(
                            Map.of(
                                    "message",
                                    "유효하지 않은 토큰입니다."
                            )
                    );
        }
    }


    // 🔵 회원정보 수정
    @PostMapping("/update")
    public ResponseEntity<?> updateUser(
            @RequestBody UserUpdateDTO dto
    ) {

        try {

            boolean updated =
                    userService.updateUser(dto);


            if (updated) {

                return ResponseEntity.ok(
                        "회원정보가 수정되었습니다."
                );

            } else {

                return ResponseEntity
                        .badRequest()
                        .body(
                                "해당 유저를 찾을 수 없습니다."
                        );
            }


        } catch (DataIntegrityViolationException ex) {

            String message =
                    ex.getRootCause().getMessage();


            String duplicateField =
                    "중복된 값이 있습니다.";


            if (message.contains("nickname")) {

                duplicateField =
                        "이미 사용 중인 닉네임입니다.";

            } else if (message.contains("email")) {

                duplicateField =
                        "이미 사용 중인 이메일입니다.";

            } else if (message.contains("phone")) {

                duplicateField =
                        "이미 사용 중인 휴대폰번호입니다.";
            }


            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(duplicateField);


        } catch (Exception e) {

            return ResponseEntity
                    .status(
                            HttpStatus.INTERNAL_SERVER_ERROR
                    )
                    .body(
                            "회원정보 수정 중 오류가 발생했습니다."
                    );
        }
    }


    // 🔵 관리자 전체 회원 조회
    @GetMapping("/all")
    public List<User> getAllUsers(
            @CookieValue(
                    name = "token",
                    required = false
            )
            String token
    ) {

        if (token == null || token.isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "토큰 없음"
            );
        }


        String userid =
                jwtConfig.getUserIdFromToken(token);


        if (!userid.equals("root")) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "권한 없음"
            );
        }


        return userRepository.findAll();
    }
}