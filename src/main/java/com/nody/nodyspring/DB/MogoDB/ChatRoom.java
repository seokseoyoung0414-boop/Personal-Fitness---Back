package com.nody.nodyspring.DB.MogoDB;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Getter@Setter
@Document(collection = "chat_rooms")
public class ChatRoom {

    @Id
    private String id;

    //채팅방이름(채널이름)
    private String name;

    // MariaDB의 userid 목록
    private List<String> participants;

    // 연결된 채널 ID
    private Integer channelId;

    // 생성 시간
    private long createdAt;
}
