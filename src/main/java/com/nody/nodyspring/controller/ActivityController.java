package com.nody.nodyspring.controller;

import com.nody.nodyspring.DB.User;
import com.nody.nodyspring.dto.ActivityDTO.ActivityDTO.*;
import com.nody.nodyspring.service.ActivityService;
import com.nody.nodyspring.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/channel/activity")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;
    private final JwtUtil jwtUtil;

    // ==================================================
    // 로그인 사용자 확인
    // ==================================================

    private String getUserid(String token) {
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "로그인이 필요합니다."
            );
        }

        try {
            User user = jwtUtil.validateToken(token);

            if (user == null || user.getUserid() == null) {
                throw new IllegalArgumentException();
            }

            return user.getUserid();
        } catch (Exception exception) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "로그인이 만료되었습니다. 다시 로그인해주세요."
            );
        }
    }

    // ==================================================
    // 내 목표 목록
    // GET /channel/activity
    // ==================================================

    @GetMapping
    public List<GoalResponse> getGoals(
            @CookieValue(name = "token", required = false)
            String token
    ) {
        return activityService.getGoals(getUserid(token));
    }

    // ==================================================
    // 목표 생성
    // POST /channel/activity
    // ==================================================

    @PostMapping
    public ResponseEntity<GoalResponse> createGoal(
            @CookieValue(name = "token", required = false)
            String token,
            @RequestBody GoalRequest request
    ) {
        String userid = getUserid(token);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(activityService.createGoal(userid, request));
    }

    // ==================================================
    // 목표 하나 조회: 수정 화면 초기값
    // GET /channel/activity/{goalId}
    // ==================================================

    @GetMapping("/{goalId}")
    public GoalResponse getGoal(
            @CookieValue(name = "token", required = false)
            String token,
            @PathVariable("goalId") Long goalId
    ) {
        return activityService.getGoal(
                getUserid(token),
                goalId
        );
    }

    // ==================================================
    // 목표 수정
    // PUT /channel/activity/{goalId}
    // ==================================================

    @PutMapping("/{goalId}")
    public GoalResponse updateGoal(
            @CookieValue(name = "token", required = false)
            String token,
            @PathVariable("goalId") Long goalId,
            @RequestBody GoalRequest request
    ) {
        return activityService.updateGoal(
                getUserid(token),
                goalId,
                request
        );
    }

    // ==================================================
    // 목표 삭제
    // DELETE /channel/activity/{goalId}
    // ==================================================

    @DeleteMapping("/{goalId}")
    public ResponseEntity<Void> deleteGoal(
            @CookieValue(name = "token", required = false)
            String token,
            @PathVariable("goalId") Long goalId
    ) {
        activityService.deleteGoal(getUserid(token), goalId);

        return ResponseEntity.noContent().build();
    }

    // ==================================================
    // 상세 통계
    // GET /channel/activity/{goalId}/detail
    // ==================================================

    @GetMapping("/{goalId}/detail")
    public DetailResponse getDetail(
            @CookieValue(name = "token", required = false)
            String token,
            @PathVariable("goalId") Long goalId
    ) {
        return activityService.getDetail(
                getUserid(token),
                goalId
        );
    }

    // ==================================================
    // 운동 기록 저장
    // POST /channel/activity/{goalId}/records
    // ==================================================

    @PostMapping("/{goalId}/records")
    public ResponseEntity<DetailResponse> addRecord(
            @CookieValue(name = "token", required = false)
            String token,
            @PathVariable("goalId") Long goalId,
            @RequestBody RecordRequest request
    ) {
        String userid = getUserid(token);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(activityService.addRecord(
                        userid,
                        goalId,
                        request
                ));
    }

    // ==================================================
    // 운동 기록 삭제
    // DELETE /channel/activity/{goalId}/records/{recordId}
    // ==================================================

    @DeleteMapping("/{goalId}/records/{recordId}")
    public DetailResponse deleteRecord(
            @CookieValue(name = "token", required = false)
            String token,
            @PathVariable("goalId") Long goalId,
            @PathVariable("recordId") Long recordId
    ) {
        return activityService.deleteRecord(
                getUserid(token),
                goalId,
                recordId
        );
    }

    // ==================================================
    // 오류 메시지 응답
    // ==================================================

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleStatusException(
            ResponseStatusException exception
    ) {
        String message = exception.getReason() == null
                ? "요청을 처리하지 못했습니다."
                : exception.getReason();

        return ResponseEntity.status(exception.getStatusCode())
                .body(Map.of("message", message));
    }
}