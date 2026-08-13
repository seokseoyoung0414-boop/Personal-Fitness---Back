package com.nody.nodyspring.controller;

import com.nody.nodyspring.DB.Channel;
import com.nody.nodyspring.DB.User;
import com.nody.nodyspring.config.JwtConfig;
import com.nody.nodyspring.dto.ChannelDTO.ChannelDTO;
import com.nody.nodyspring.dto.ChannelDTO.JoinRequestDTO;
import com.nody.nodyspring.repository.ChannelRepository;
import com.nody.nodyspring.repository.UserRepository;
import com.nody.nodyspring.service.ChannelService;
import com.nody.nodyspring.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/channel")
public class ChannelController {

    @Autowired
    private ChannelService channelService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ChannelRepository channelRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtConfig jwtConfig;

    @GetMapping("/list")
    public List<ChannelDTO> getChannelList() {
        return channelService.getAllChannels();
    }

    @PostMapping("/make")
    public ResponseEntity<?> makeChannel(@CookieValue(name = "token", required = false) String token,
                                         @RequestBody Map<String, Object> request) {
        try {
            User user = jwtUtil.validateToken(token); // 사용자 인증

            Channel newChannel = Channel.builder()
                    .channelTitle(request.get("channelTitle").toString())
                    .channelComment(request.get("channelComment").toString())
                    .sport(request.get("sport").toString())
                    .modes(request.get("modes").toString())
                    .startDate(LocalDate.parse(request.get("startDate").toString()))
                    .endDate(LocalDate.parse(request.get("endDate").toString()))
                    .goal(Integer.parseInt(request.get("goal").toString()))
                    .channelBuild(false)
                    .complete(false)
                    .countUser(0)
                    .build();

            Channel savedChannel = channelRepository.save(newChannel);

            // 참가자로 자동 등록
            channelService.joinChannel(savedChannel.getIdx(), user.getUserid());

            return ResponseEntity.ok("동아리 생성 완료");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("서버 오류: " + e.getMessage());
        }
    }


    @GetMapping("/mine")
    public ResponseEntity<?> getMyChannels(
            @CookieValue(name = "token", required = false) String token) {
        try {
            List<ChannelDTO> myChannels = channelService.getMyChannels(token);
            return ResponseEntity.ok(myChannels);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("채널 목록 조회 실패: " + e.getMessage());
        }
    }

    @GetMapping("/users")
    public ResponseEntity<?> getUserGoalStatus(@RequestParam("channelId") Integer channelId,
                                               @CookieValue(name = "token", required = false) String token) {
        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("토큰이 없습니다.");
        }

        try {
            jwtUtil.validateToken(token);
            List<Map<String, Object>> statusList = channelService.getUserGoalStatusByChannelId(channelId);
            return ResponseEntity.ok(statusList);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("유효하지 않은 토큰입니다.");
        }
    }

    @PostMapping("/join")
    public ResponseEntity<?> joinChannel(@RequestBody JoinRequestDTO joinRequest,
                                         @CookieValue(name = "token", required = false) String token) {
        try {
            User user = jwtUtil.validateToken(token);
            channelService.joinChannel(joinRequest.getChannelId(), user.getUserid());
            return ResponseEntity.ok("참가 성공");
        } catch (Exception e) {
            return ResponseEntity.status(401).body("유효하지 않은 토큰");
        }
    }

    @GetMapping("/view/{idx}")
    public ResponseEntity<?> getChannel(@PathVariable("idx") Integer idx) {
        try {
            ChannelDTO channel = channelService.getChannelById(idx);
            return ResponseEntity.ok(channel);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PostMapping("/auth")
    public ResponseEntity<?> authenticateFitness(@CookieValue(name = "token", required = false) String token,
                                                 @RequestBody Map<String, Object> request) {
        try {
            // 1. 파라미터 추출
            Integer channelId = Integer.parseInt(request.get("channelId").toString());
            Double achievedValue = Double.parseDouble(request.get("achievedValue").toString());

            // 2. 채널 존재 여부 확인
            Channel channel = channelRepository.findById(channelId)
                    .orElseThrow(() -> new RuntimeException("채널 없음"));

            int goal = channel.getGoal();
            boolean goalMet = achievedValue >= goal;

            String userid = jwtConfig.getUserIdFromToken(token);

            // 3. 인증 처리 (중복 인증은 내부에서 검사)
            channelService.authenticateFitness(token, channelId, goalMet, achievedValue);

            // 4. 포인트 지급: 목표 달성 시 +3P
            if (goalMet) {
                System.out.println("✅ 목표 달성, 포인트 추가 시도");
                userRepository.addPoint(userid, 3);
                System.out.println("✅ 포인트 추가 완료");
            }

            return ResponseEntity.ok(Map.of(
                    "message", "인증 성공",
                    "goalMet", goalMet
            ));

        } catch (IllegalStateException e) {
            return ResponseEntity.status(400).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("message", "서버 오류"));
        }
    }
}