
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
    // 개인 진행 중인 채널별 운동시간 랭킹 조회
    // GET /channel/activity/ranking
    // ==================================================

    @GetMapping("/ranking")
    public List<ChannelActivityRankingResponse> getMyChannelActivityRanking(

            @CookieValue(name = "token", required = false)
            String token

    ) {

        String userid = getUserid(token);

        return activityService.getMyChannelActivityRanking(userid);
    }


    // ==================================================
    // 채널별 운동 통계 조회
    // GET /channel/activity/{channelIdx}/detail
    // ==================================================

    @GetMapping("/{channelIdx}/detail")
    public DetailResponse getDetail(

            @CookieValue(name = "token", required = false)
            String token,

            @PathVariable("channelIdx")
            Integer channelIdx

    ) {

        return activityService.getDetail(
                getUserid(token),
                channelIdx
        );
    }


    // ==================================================
    // 운동 기록 저장
    // POST /channel/activity/{channelIdx}/records
    // ==================================================

    @PostMapping("/{channelIdx}/records")
    public ResponseEntity<DetailResponse> addRecord(

            @CookieValue(name = "token", required = false)
            String token,

            @PathVariable("channelIdx")
            Integer channelIdx,

            @RequestBody RecordRequest request

    ) {

        String userid = getUserid(token);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(activityService.addRecord(
                        userid,
                        channelIdx,
                        request
                ));
    }


    // ==================================================
    // 운동 기록 삭제
    // DELETE /channel/activity/{channelIdx}/records/{recordId}
    // ==================================================

    @DeleteMapping("/{channelIdx}/records/{recordId}")
    public DetailResponse deleteRecord(

            @CookieValue(name = "token", required = false)
            String token,

            @PathVariable("channelIdx")
            Integer channelIdx,

            @PathVariable("recordId")
            Long recordId

    ) {

        return activityService.deleteRecord(
                getUserid(token),
                channelIdx,
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
