package com.nody.nodyspring.repository;


import com.nody.nodyspring.DB.MogoDB.ChatRoom;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends MongoRepository<ChatRoom, String> {
    List<ChatRoom> findByParticipantsContains(String userId);

    Optional<ChatRoom> findByChannelId(Integer channelId);

}
