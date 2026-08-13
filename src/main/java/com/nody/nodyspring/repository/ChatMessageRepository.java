package com.nody.nodyspring.repository;
import org.springframework.data.mongodb.repository.MongoRepository;
import com.nody.nodyspring.DB.MogoDB.ChatMessage;

import java.util.List;

public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {
    List<ChatMessage> findByRoomIdOrderBySentAt(String roomId);

    List<ChatMessage> findByRoomIdOrderBySentAtAsc(String roomId);
}
