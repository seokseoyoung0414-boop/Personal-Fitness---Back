package com.nody.nodyspring.service;

import com.nody.nodyspring.DB.MogoDB.ChatMessage;
import com.nody.nodyspring.DB.MogoDB.ChatRoom;
import com.nody.nodyspring.DB.User;
import com.nody.nodyspring.repository.ChatMessageRepository;
import com.nody.nodyspring.repository.ChatRoomRepository;
import com.nody.nodyspring.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ChatService {

    @Autowired
    private ChatRoomRepository chatRoomRepo;

    @Autowired
    private ChatMessageRepository messageRepo;

    @Autowired
    private UserRepository userRepository;

    //채팅방
    public ChatRoom createRoom(List<String> participantIds, String name, Integer channelId) {
        // MariaDB에서 참가자 존재 여부 개별 확인
        for (String userid : participantIds) {
            if (!userRepository.existsById(userid)) {
                throw new RuntimeException("존재하지 않는 사용자: " + userid);
            }
        }

        ChatRoom room = new ChatRoom();
        room.setParticipants(participantIds);
        room.setName(name);
        room.setChannelId(channelId); // ✅ 채널 ID 저장
        room.setCreatedAt(System.currentTimeMillis());

        return chatRoomRepo.save(room);
    }


    //채팅 메세지
    public ChatMessage saveMessage(String roomId, String senderId, String message) {
        Optional<ChatRoom> optionalRoom = chatRoomRepo.findById(roomId);
        if (optionalRoom.isEmpty()) {
            try {
                throw new Exception("존재하지 않는 채팅방입니다.");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        ChatRoom room = optionalRoom.get();

        if (!room.getParticipants().contains(senderId)) {
            try {
                throw new Exception("채팅방에 참여하지 않은 유저입니다: " + senderId);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setRoomId(roomId);
        chatMessage.setSenderId(senderId);
        chatMessage.setMessage(message);
        chatMessage.setSentAt(System.currentTimeMillis());

        return messageRepo.save(chatMessage);
    }

    // 채팅방 유저 추가
    public ChatRoom addUserToRoom(String roomId, String userid) throws Exception {
        // MariaDB에서 userid 존재 확인
        Optional<User> userOpt = userRepository.findById(userid);
        if (userOpt.isEmpty()) {
            throw new Exception("해당 사용자가 존재하지 않습니다.");
        }

        ChatRoom room = chatRoomRepo.findById(roomId)
                .orElseThrow(() -> new Exception("채팅방을 찾을 수 없습니다."));

        if (!room.getParticipants().contains(userid)) {
            room.getParticipants().add(userid);
        }

        return chatRoomRepo.save(room);
    }

    // 채팅방 유저 삭제
    public ChatRoom removeUserFromRoom(String roomId, String userid) {
        Optional<ChatRoom> optionalRoom = chatRoomRepo.findById(roomId);
        if (optionalRoom.isEmpty()) {
            try {
                throw new Exception("존재하지 않는 채팅방입니다.");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        ChatRoom room = optionalRoom.get();

        if (!room.getParticipants().contains(userid)) {
            try {
                throw new Exception("해당 유저는 채팅방에 존재하지 않습니다: " + userid);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        room.getParticipants().remove(userid);
        return chatRoomRepo.save(room);
    }

    //채팅방 메세지 로딩
    public List<ChatMessage> loadMessages(String roomId) throws Exception {
        if (!chatRoomRepo.existsById(roomId)) {
            throw new Exception("존재하지 않는 채팅방입니다: " + roomId);
        }
        return messageRepo.findByRoomIdOrderBySentAtAsc(roomId);
    }

    public Map<String, String> getUserNicknamesForRoom(String roomId) throws Exception {
        Optional<ChatRoom> roomOpt = chatRoomRepo.findById(roomId);
        if (roomOpt.isEmpty()) {
            throw new Exception("채팅방이 존재하지 않습니다.");
        }

        List<String> userIds = roomOpt.get().getParticipants();
        Map<String, String> userMap = new HashMap<>();

        for (String userid : userIds) {
            Optional<User> userOpt = userRepository.findById(userid);
            if (userOpt.isPresent()) {
                userMap.put(userid, userOpt.get().getNickname());
            } else {
                userMap.put(userid, "알 수 없음");
            }
        }

        return userMap; // userid → nickname 매핑
    }

    public ChatRoom findRoomByChannelId(Integer channelId) {
        return chatRoomRepo.findByChannelId(channelId).orElse(null);
    }

    public ChatRoom saveRoom(ChatRoom room) {
        return chatRoomRepo.save(room);
    }
}
