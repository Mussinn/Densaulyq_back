package com.example.MedSafe.controller;

import com.example.MedSafe.dto.ChatResponse;
import com.example.MedSafe.model.User;
import com.example.MedSafe.model.dto.CreateChatRequest;
import com.example.MedSafe.service.ChatService;
import com.example.MedSafe.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/chats")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final UserService userService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ChatResponse>> getUserChats() {
        Integer userId = getCurrentUserId();
        List<ChatResponse> chats = chatService.getUserChats(userId);
        return ResponseEntity.ok(chats);
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ChatResponse> createChat(@RequestBody CreateChatRequest request) {
        Integer userId = getCurrentUserId();
        ChatResponse chat = chatService.createChat(userId, request);
        return ResponseEntity.ok(chat);
    }

    @PostMapping("/{chatId}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> markAsRead(@PathVariable Long chatId) {
        Integer userId = getCurrentUserId();
        chatService.markAsRead(chatId, userId);
        return ResponseEntity.ok().build();
    }

    private Integer getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userService.findByUsername(username);
        return user.getUserId();
    }
}