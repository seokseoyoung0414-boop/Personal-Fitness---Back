package com.nody.nodyspring.controller;

import com.nody.nodyspring.dto.ChatDTO.ChatMessageRequestDTO;
import com.nody.nodyspring.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

@Controller
public class ChatSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;

    @Autowired
    public ChatSocketController(SimpMessagingTemplate messagingTemplate, ChatService chatService) {
        this.messagingTemplate = messagingTemplate;
        this.chatService = chatService;
    }

    @MessageMapping("/chat/send/{roomId}")
    public void handleMessage(@DestinationVariable String roomId,
                              ChatMessageRequestDTO dto) {
        String now = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
        dto.setSentAt(now);

        // ✅ 메시지 저장
        chatService.saveMessage(roomId, dto.getSenderId(), dto.getMessage());

        // ✅ 클라이언트에 메시지 브로드캐스트
        messagingTemplate.convertAndSend("/topic/chatroom/" + roomId, dto);
    }
}