package com.example.MedSafe.controller;

import com.example.MedSafe.dto.MessageResponse;
import com.example.MedSafe.model.User;
import com.example.MedSafe.model.dto.SendMessageRequest;
import com.example.MedSafe.service.MessageService;
import com.example.MedSafe.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;
    private final UserService userService;

    @GetMapping("/chats/{chatId}/messages")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MessageResponse>> getChatMessages(
            @PathVariable Long chatId,
            @RequestParam(defaultValue = "100") Integer limit,
            @RequestParam(defaultValue = "0") Integer offset) {

        List<MessageResponse> messages = messageService.getChatMessages(chatId, limit, offset);
        return ResponseEntity.ok(messages);
    }

    @PostMapping("/messages")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MessageResponse> sendMessage(
            @RequestParam("chatId") Long chatId,
            @RequestParam(value = "content", required = false) String content,
            @RequestParam(value = "replyToId", required = false) Long replyToId,
            @RequestParam(value = "attachments", required = false) List<MultipartFile> attachments) {

        Integer userId = getCurrentUserId();

        SendMessageRequest request = SendMessageRequest.builder()
                .chatId(chatId)
                .content(content)
                .replyToId(replyToId)
                .attachments(attachments)
                .build();

        MessageResponse message = messageService.sendMessage(userId, request);
        return ResponseEntity.ok(message);
    }

    @DeleteMapping("/messages/{messageId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteMessage(@PathVariable Long messageId) {
        Integer userId = getCurrentUserId();
        messageService.deleteMessage(messageId, userId);
        return ResponseEntity.ok().build();
    }

    private Integer getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userService.findByUsername(username);
        return user.getUserId();
    }
}