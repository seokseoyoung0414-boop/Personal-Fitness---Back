package com.nody.nodyspring.controller;

import com.nody.nodyspring.DB.Channel;
import com.nody.nodyspring.DB.User;
import com.nody.nodyspring.dto.ChannelDTO.ChannelDTO;
import com.nody.nodyspring.repository.ChannelRepository;
import com.nody.nodyspring.service.ChannelService;
import com.nody.nodyspring.util.JwtUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.transaction.annotation.Transactional;
import java.time.format.DateTimeParseException;

@RestController
@RequestMapping("/channel")
public class ChannelController {

    @Autowired
    private ChannelService channelService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ChannelRepository channelRepository;


    // 내가 생성한 채널 목록 조회
    @GetMapping("/list")
    public ResponseEntity<?> getChannelList(
            @CookieValue(name = "token", required = false) String token) {

        try {
            if (token == null || token.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("로그인이 필요합니다.");
            }

            List<ChannelDTO> channels = channelService.getMyChannels(token);

            return ResponseEntity.ok(channels);

        } catch (Exception e) {
            e.printStackTrace();

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("채널 목록 조회 실패: " + e.getMessage());
        }
    }


    // 개인 운동 채널 생성
    @PostMapping("/make")
    public ResponseEntity<?> makeChannel(
            @CookieValue(name = "token", required = false) String token,
            @RequestBody Map<String, Object> request) {

        try {
            if (token == null || token.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("로그인이 필요합니다.");
            }

            User user = jwtUtil.validateToken(token);

            // 필수값 확인
            if (request.get("channelTitle") == null ||
                    request.get("channelComment") == null ||
                    request.get("sport") == null ||
                    request.get("startDate") == null ||
                    request.get("endDate") == null ||
                    request.get("goal") == null) {

                return ResponseEntity.badRequest()
                        .body("필수 입력값이 누락되었습니다.");
            }

            LocalDate startDate = LocalDate.parse(
                    request.get("startDate").toString()
            );

            LocalDate endDate = LocalDate.parse(
                    request.get("endDate").toString()
            );

            int goal = Integer.parseInt(
                    request.get("goal").toString()
            );

            if (endDate.isBefore(startDate)) {
                return ResponseEntity.badRequest()
                        .body("종료일은 시작일보다 빠를 수 없습니다.");
            }

            if (goal < 0) {
                return ResponseEntity.badRequest()
                        .body("운동 목표는 0 이상이어야 합니다.");
            }

            Channel newChannel = Channel.builder()
                    .creator(user)
                    .channelTitle(request.get("channelTitle").toString())
                    .channelComment(request.get("channelComment").toString())
                    .sport(request.get("sport").toString())
                    .startDate(startDate)
                    .endDate(endDate)
                    .goal(goal)
                    .complete(false)
                    .build();

            channelRepository.save(newChannel);

            return ResponseEntity.ok(
                    Map.of(
                            "message", "개인 운동 채널 생성 완료",
                            "channelId", newChannel.getIdx()
                    )
            );

        } catch (Exception e) {
            e.printStackTrace();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("채널 생성 실패: " + e.getMessage());
        }
    }


    // 내가 생성한 채널 조회
    @GetMapping("/mine")
    public ResponseEntity<?> getMyChannels(
            @CookieValue(name = "token", required = false) String token) {

        try {
            if (token == null || token.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("로그인이 필요합니다.");
            }

            List<ChannelDTO> myChannels = channelService.getMyChannels(token);

            return ResponseEntity.ok(myChannels);

        } catch (Exception e) {
            e.printStackTrace();

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("채널 목록 조회 실패: " + e.getMessage());
        }
    }


    // 채널 상세 조회
    @GetMapping("/view/{idx}")
    public ResponseEntity<?> getChannel(
            @PathVariable("idx") Integer idx,
            @CookieValue(name = "token", required = false) String token) {

        try {
            if (token == null || token.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("로그인이 필요합니다.");
            }

            User user = jwtUtil.validateToken(token);

            Channel channel = channelRepository.findById(idx)
                    .orElseThrow(() ->
                            new RuntimeException("채널이 존재하지 않습니다.")
                    );

            // 본인이 생성한 채널인지 확인
            if (channel.getCreator() == null ||
                    !channel.getCreator().getUserid().equals(user.getUserid())) {

                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("본인이 생성한 채널만 조회할 수 있습니다.");
            }

            ChannelDTO channelDTO = channelService.getChannelById(idx);

            return ResponseEntity.ok(channelDTO);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
        }
    }


    // 개인 운동 목표 인증
    @PostMapping("/auth")
    public ResponseEntity<?> authenticateFitness(
            @CookieValue(name = "token", required = false) String token,
            @RequestBody Map<String, Object> request) {

        try {
            if (token == null || token.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "로그인이 필요합니다."));
            }

            if (request.get("channelId") == null ||
                    request.get("achievedValue") == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "필수 입력값이 누락되었습니다."));
            }

            Integer channelId = Integer.parseInt(
                    request.get("channelId").toString()
            );

            Double achievedValue = Double.parseDouble(
                    request.get("achievedValue").toString()
            );

            if (achievedValue < 0) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "측정값은 0 이상이어야 합니다."));
            }

            Channel channel = channelRepository.findById(channelId)
                    .orElseThrow(() ->
                            new RuntimeException("채널이 존재하지 않습니다.")
                    );

            User user = jwtUtil.validateToken(token);

            // 본인이 생성한 채널인지 확인
            if (channel.getCreator() == null ||
                    !channel.getCreator().getUserid().equals(user.getUserid())) {

                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of(
                                "message", "본인이 생성한 채널만 인증할 수 있습니다."
                        ));
            }

            int goal = channel.getGoal();
            boolean goalMet = achievedValue >= goal;

            // 운동 인증 기록 저장
            channelService.authenticateFitness(
                    token,
                    channelId,
                    goalMet,
                    achievedValue
            );

            return ResponseEntity.ok(
                    Map.of(
                            "message", "운동 인증 성공",
                            "goalMet", goalMet,
                            "achievedValue", achievedValue
                    )
            );

        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));

        } catch (Exception e) {
            e.printStackTrace();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "서버 오류: " + e.getMessage()));
        }
    }
    // ==========================================
    // 내가 생성한 개인 운동 채널 삭제
    // ==========================================
    @DeleteMapping("/delete/{idx}")
    public ResponseEntity<?> deleteChannel(
            @PathVariable("idx") Integer idx,
            @CookieValue(name = "token", required = false) String token) {

        if (token == null || token.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("로그인이 필요합니다.");
        }

        try {
            channelService.deleteChannel(token, idx);

            return ResponseEntity.ok(
                    Map.of("message", "운동 채널과 운동 기록이 삭제되었습니다.")
            );

        } catch (Exception e) {
            e.printStackTrace();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("채널 삭제 중 오류가 발생했습니다.");
        }
    }
    // ==========================================
    // 내가 생성한 개인 운동 채널 수정
    // ==========================================
    @PutMapping("/update/{idx}")
    @Transactional
    public ResponseEntity<?> updateChannel(
            @PathVariable("idx") Integer idx,
            @CookieValue(name = "token", required = false) String token,
            @RequestBody Map<String, Object> request) {

        // 1. 로그인 여부 확인
        if (token == null || token.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "로그인이 필요합니다."));
        }

        // 2. JWT 인증
        User user;

        try {
            user = jwtUtil.validateToken(token);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "로그인 인증에 실패했습니다."));
        }

        // 3. 채널 존재 여부 확인
        Channel channel = channelRepository.findById(idx)
                .orElse(null);

        if (channel == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "수정할 채널을 찾을 수 없습니다."));
        }

        // 4. 채널 소유자 확인
        if (channel.getCreator() == null ||
                !channel.getCreator().getUserid().equals(user.getUserid())) {

            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "본인이 생성한 채널만 수정할 수 있습니다."));
        }

        try {
            // 5. 필수 입력값 확인
            if (request == null ||
                    request.get("channelTitle") == null ||
                    request.get("channelComment") == null ||
                    request.get("sport") == null ||
                    request.get("goal") == null ||
                    request.get("startDate") == null ||
                    request.get("endDate") == null) {

                return ResponseEntity.badRequest()
                        .body(Map.of("message", "필수 입력값이 누락되었습니다."));
            }

            String channelTitle =
                    request.get("channelTitle").toString().trim();

            String channelComment =
                    request.get("channelComment").toString().trim();

            String sport =
                    request.get("sport").toString().trim();

            // 6. 문자열 유효성 검사
            if (channelTitle.isEmpty() ||
                    channelComment.isEmpty() ||
                    sport.isEmpty()) {

                return ResponseEntity.badRequest()
                        .body(Map.of("message", "입력값을 확인해주세요."));
            }

            if (channelTitle.length() > 100) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "운동 이름은 100자 이내로 입력해주세요."));
            }

            if (sport.length() > 50) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "운동 종목을 확인해주세요."));
            }

            // 7. 날짜 변환
            LocalDate startDate = LocalDate.parse(
                    request.get("startDate").toString()
            );

            LocalDate endDate = LocalDate.parse(
                    request.get("endDate").toString()
            );

            // 8. 목표량 변환
            int goal = Integer.parseInt(
                    request.get("goal").toString()
            );

            if (goal < 1) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "운동 목표는 1 이상이어야 합니다."));
            }

            // 9. 날짜 유효성 검사
            if (endDate.isBefore(startDate.plusDays(7))) {
                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "message",
                                "종료 날짜는 시작 날짜로부터 최소 7일 이후여야 합니다."
                        ));
            }

            if (startDate.isAfter(LocalDate.of(2099, 12, 24)) ||
                    endDate.isAfter(LocalDate.of(2099, 12, 31))) {

                return ResponseEntity.badRequest()
                        .body(Map.of("message", "선택 가능한 날짜 범위를 확인해주세요."));
            }

            // 10. 기존 채널 정보 수정
            channel.setChannelTitle(channelTitle);
            channel.setChannelComment(channelComment);
            channel.setSport(sport);
            channel.setGoal(goal);
            channel.setStartDate(startDate);
            channel.setEndDate(endDate);

            channelRepository.save(channel);

            return ResponseEntity.ok(
                    Map.of(
                            "message", "운동 채널이 수정되었습니다.",
                            "channelId", channel.getIdx()
                    )
            );

        } catch (DateTimeParseException | NumberFormatException e) {

            return ResponseEntity.badRequest()
                    .body(Map.of("message", "날짜 또는 목표량 형식이 올바르지 않습니다."));
        }
    }
}