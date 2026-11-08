package com.nody.nodyspring.dto.FriendDTO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class FriendDTO {

    // 친구 신청 요청
    public record FriendRequest(
            String targetUserid
    ) {}

    // 친구 신청 결과
    public record FriendRequestResponse(
            Long idx,
            String userid,
            String nickname,
            LocalDateTime createdAt
    ) {}

    // 친구 목록
    public record FriendResponse(
            Long idx,
            String friendUserid,
            String nickname,
            LocalDateTime acceptedAt
    ) {}

    // 친구 채널 정보
    public record FriendChannelResponse(
            Integer idx,
            String channelTitle,
            String sport,
            String channelComment,
            LocalDate startDate,
            LocalDate endDate,
            Boolean complete,
            int goal
    ) {}

    // 친구 정보 및 채널 목록
    public record FriendChannelsResponse(
            String friendUserid,
            String nickname,
            List<FriendChannelResponse> channels
    ) {}
}
