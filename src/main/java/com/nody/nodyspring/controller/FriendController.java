
package com.nody.nodyspring.controller;

import com.nody.nodyspring.config.JwtConfig;
import com.nody.nodyspring.dto.FriendDTO.FriendDTO.FriendRequest;
import com.nody.nodyspring.service.FriendService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/friend")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;
    private final JwtConfig jwtConfig;

    // 로그인 사용자 확인
    private String getUserid(String token) {

        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "로그인이 필요합니다."
            );
        }

        try {
            return jwtConfig.getUserIdFromToken(token);
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "유효하지 않은 토큰입니다."
            );
        }
    }

    // 친구 신청
    @PostMapping("/request")
    public ResponseEntity<?> requestFriend(
            @CookieValue(name = "token", required = false) String token,
            @RequestBody FriendRequest request
    ) {
        String userid = getUserid(token);

        return ResponseEntity.ok(
                friendService.requestFriend(
                        userid,
                        request.targetUserid()
                )
        );
    }

    // 받은 친구 신청
    @GetMapping("/requests/received")
    public ResponseEntity<?> getReceivedRequests(
            @CookieValue(name = "token", required = false) String token
    ) {
        String userid = getUserid(token);

        return ResponseEntity.ok(
                friendService.getReceivedRequests(userid)
        );
    }

    // 보낸 친구 신청
    @GetMapping("/requests/sent")
    public ResponseEntity<?> getSentRequests(
            @CookieValue(name = "token", required = false) String token
    ) {
        String userid = getUserid(token);

        return ResponseEntity.ok(
                friendService.getSentRequests(userid)
        );
    }

    // 친구 신청 수락
    @PostMapping("/accept/{idx}")
    public ResponseEntity<?> acceptRequest(
            @CookieValue(name = "token", required = false) String token,
            @PathVariable Long idx
    ) {
        String userid = getUserid(token);

        friendService.acceptRequest(userid, idx);

        return ResponseEntity.ok(
                Map.of("message", "친구 신청을 수락했습니다.")
        );
    }

    // 친구 신청 거절
    @DeleteMapping("/reject/{idx}")
    public ResponseEntity<?> rejectRequest(
            @CookieValue(name = "token", required = false) String token,
            @PathVariable Long idx
    ) {
        String userid = getUserid(token);

        friendService.rejectRequest(userid, idx);

        return ResponseEntity.ok(
                Map.of("message", "친구 신청을 거절했습니다.")
        );
    }

    // 친구 목록
    @GetMapping("/list")
    public ResponseEntity<?> getFriendList(
            @CookieValue(name = "token", required = false) String token
    ) {
        String userid = getUserid(token);

        return ResponseEntity.ok(
                friendService.getFriendList(userid)
        );
    }

    // 친구 삭제
    @DeleteMapping("/{friendUserid}")
    public ResponseEntity<?> deleteFriend(
            @CookieValue(name = "token", required = false) String token,
            @PathVariable String friendUserid
    ) {
        String userid = getUserid(token);

        friendService.deleteFriend(userid, friendUserid);

        return ResponseEntity.ok(
                Map.of("message", "친구를 삭제했습니다.")
        );
    }

    // 친구 채널 목록
    // GET /friend/{friendUserid}/channels
    @GetMapping("/{friendUserid}/channels")
    public ResponseEntity<?> getFriendChannels(
            @CookieValue(name = "token", required = false) String token,
            @PathVariable String friendUserid
    ) {
        String userid = getUserid(token);

        return ResponseEntity.ok(
                friendService.getFriendChannels(userid, friendUserid)
        );
    }

    // =====================================================
    // 친구 채널 상세 정보
    // GET /friend/{friendUserid}/channels/{channelIdx}
    // =====================================================
    @GetMapping("/{friendUserid}/channels/{channelIdx}")
    public ResponseEntity<?> getFriendChannelDetail(
            @CookieValue(name = "token", required = false) String token,
            @PathVariable String friendUserid,
            @PathVariable Integer channelIdx
    ) {
        String userid = getUserid(token);

        return ResponseEntity.ok(
                friendService.getFriendChannelDetail(
                        userid,
                        friendUserid,
                        channelIdx
                )
        );
    }

    // =====================================================
    // 친구 채널 운동 통계 및 기록
    // GET /friend/{friendUserid}/channels/{channelIdx}/activity/detail
    // =====================================================
    @GetMapping("/{friendUserid}/channels/{channelIdx}/activity/detail")
    public ResponseEntity<?> getFriendChannelActivityDetail(
            @CookieValue(name = "token", required = false) String token,
            @PathVariable String friendUserid,
            @PathVariable Integer channelIdx
    ) {
        String userid = getUserid(token);

        return ResponseEntity.ok(
                friendService.getFriendChannelActivityDetail(
                        userid,
                        friendUserid,
                        channelIdx
                )
        );
    }
}
