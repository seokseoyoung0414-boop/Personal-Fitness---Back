package com.nody.nodyspring.controller;


import com.nody.nodyspring.DB.Channel;
import com.nody.nodyspring.DB.MogoDB.ChatMessage;
import com.nody.nodyspring.DB.MogoDB.ChatRoom;
import com.nody.nodyspring.DB.User;
import com.nody.nodyspring.dto.ChatDTO.ChatMessageRequestDTO;
import com.nody.nodyspring.dto.ChatDTO.ChatRoomRequestDTO;
import com.nody.nodyspring.dto.ChatDTO.ChatRoomUserRequestDTO;
import com.nody.nodyspring.dto.ChatDTO.ChatRoomWithChannelDTO;
import com.nody.nodyspring.repository.ChannelRepository;
import com.nody.nodyspring.repository.ChatRoomRepository;
import com.nody.nodyspring.service.ChatService;
import com.nody.nodyspring.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/chat")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private ChatRoomRepository chatRoomRepository;
    @Autowired
    private ChannelRepository channelRepository;

    // 채팅방 생성
    @PostMapping("/make")
    public ResponseEntity<?> createChatRoom(@CookieValue(name = "token", required = false) String token, @RequestBody ChatRoomRequestDTO request) {
        try {
            User user = jwtUtil.validateToken(token);
            ChatRoom room = chatService.createRoom(request.getParticipants(), request.getName(), request.getChannelId());
            return ResponseEntity.ok(room);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 메시지 전송
    @PostMapping("/msgsend")
    public ResponseEntity<?> sendMessage(@CookieValue(name = "token", required = false) String token, @RequestBody ChatMessageRequestDTO request) {
        try {
            User user = jwtUtil.validateToken(token);
            ChatMessage message = chatService.saveMessage(
                    request.getRoomId(),
                    request.getSenderId(),
                    request.getMessage()
            );
            return ResponseEntity.ok(message);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    //채팅방 유저 추가
    @PostMapping("/adduser")
    public ResponseEntity<?> addUserToRoom(@CookieValue(name = "token", required = false) String token, @RequestBody ChatRoomUserRequestDTO request) {
        try {
            User user = jwtUtil.validateToken(token);
            ChatRoom updatedRoom = chatService.addUserToRoom(request.getRoomId(), request.getUserid());
            return ResponseEntity.ok(updatedRoom);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    //채팅방 유저 삭제
    @PostMapping("/deleteuser")
    public ResponseEntity<?> removeUserFromRoom(@CookieValue(name = "token", required = false) String token, @RequestBody ChatRoomUserRequestDTO request) {
        try {
            User user = jwtUtil.validateToken(token);
            ChatRoom updatedRoom = chatService.removeUserFromRoom(request.getRoomId(), request.getUserid());
            return ResponseEntity.ok(updatedRoom);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/load")
    public ResponseEntity<?> loadMessages(@CookieValue(name = "token", required = false) String token, @RequestBody ChatRoomUserRequestDTO request) {
        try {
            User user = jwtUtil.validateToken(token);
            List<ChatMessage> messages = chatService.loadMessages(request.getRoomId());
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/room-id")
    public ResponseEntity<?> getRoomIdByChannel(@RequestParam Integer channelId) {
        ChatRoom room = chatService.findRoomByChannelId(channelId);
        if (room != null) {
            return ResponseEntity.ok(Map.of("roomId", room.getId()));
        } else {
            return ResponseEntity.status(404).body("채팅방이 존재하지 않습니다.");
        }
    }
    @GetMapping("/room/{roomId}")
    public ResponseEntity<?> getRoomInfo(@PathVariable String roomId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        Channel channel = channelRepository.findById(room.getChannelId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        return ResponseEntity.ok(new ChatRoomWithChannelDTO(
                room.getId(),
                room.getParticipants(),
                room.getChannelId(),
                channel.getChannelTitle()
        ));
    }
}
