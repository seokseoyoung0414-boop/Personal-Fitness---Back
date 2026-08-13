package com.nody.nodyspring.dto.ChatDTO;

import java.util.List;

public record ChatRoomWithChannelDTO(
        String id,
        List<String> participants,
        Integer channelId,
        String channelTitle // 🔥 채널 제목 포함
) {}

