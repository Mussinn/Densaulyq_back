package com.example.MedSafe.service;

import com.example.MedSafe.dto.MessageResponse;
import com.example.MedSafe.model.*;
import com.example.MedSafe.model.dto.SendMessageRequest;
import com.example.MedSafe.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final ChatRepository chatRepository;
    private final UserService userService;
    private final FileStorageService fileStorageService;
    private final MessageAttachmentRepository attachmentRepository;

    @Transactional(readOnly = true)
    public List<MessageResponse> getChatMessages(Long chatId, Integer limit, Integer offset) {
        PageRequest pageRequest = PageRequest.of(offset / limit, limit);
        List<Message> messages = messageRepository.findByChatId(chatId, pageRequest);

        // Переворачиваем список, чтобы старые сообщения были вверху
        List<Message> reversedMessages = new ArrayList<>(messages);
        java.util.Collections.reverse(reversedMessages);

        return reversedMessages.stream()
                .map(this::buildMessageResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public MessageResponse sendMessage(Integer senderId, SendMessageRequest request) {
        // Проверяем существование чата
        Chat chat = chatRepository.findById(request.getChatId())
                .orElseThrow(() -> new RuntimeException("Chat not found"));

        // Получаем отправителя
        User sender = userService.findById(senderId);

        // Создаем сообщение
        Message message = Message.builder()
                .chat(chat)
                .sender(sender)
                .content(request.getContent())
                .isDeleted(false)
                .attachments(new ArrayList<>())
                .build();

        // Если это ответ на другое сообщение
        if (request.getReplyToId() != null) {
            Message replyTo = messageRepository.findById(request.getReplyToId())
                    .orElse(null);
            message.setReplyTo(replyTo);
        }

        message = messageRepository.save(message);

        // Обрабатываем вложения
        if (request.getAttachments() != null && !request.getAttachments().isEmpty()) {
            for (MultipartFile file : request.getAttachments()) {
                try {
                    String fileName = fileStorageService.saveFile(file);

                    MessageAttachment attachment = MessageAttachment.builder()
                            .message(message)
                            .fileName(file.getOriginalFilename())
                            .filePath(fileName)
                            .fileType(file.getContentType())
                            .fileSize(file.getSize())
                            .build();

                    attachmentRepository.save(attachment);
                    message.getAttachments().add(attachment);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to save file", e);
                }
            }
        }

        // Обновляем время последнего обновления чата
        chat.setUpdatedAt(message.getCreatedAt());
        chatRepository.save(chat);

        return buildMessageResponse(message);
    }

    @Transactional
    public void deleteMessage(Long messageId, Integer userId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        // Проверяем, что пользователь является отправителем
        if (!message.getSender().getUserId().equals(userId)) {
            throw new RuntimeException("You can only delete your own messages");
        }

        // Помечаем сообщение как удаленное (мягкое удаление)
        message.setIsDeleted(true);
        messageRepository.save(message);
    }

    private MessageResponse buildMessageResponse(Message message) {
        MessageResponse.ReplyToInfo replyToInfo = null;
        if (message.getReplyTo() != null && !message.getReplyTo().getIsDeleted()) {
            replyToInfo = MessageResponse.ReplyToInfo.builder()
                    .id(message.getReplyTo().getMessageId())
                    .senderId(message.getReplyTo().getSender().getUserId())
                    .content(message.getReplyTo().getContent())
                    .build();
        }

        List<MessageResponse.AttachmentInfo> attachmentInfos = message.getAttachments().stream()
                .map(attachment -> MessageResponse.AttachmentInfo.builder()
                        .id(attachment.getAttachmentId())
                        .name(attachment.getFileName())
                        .url(fileStorageService.getFileUrl(attachment.getFilePath()))
                        .type(attachment.getFileType())
                        .size(attachment.getFileSize())
                        .build())
                .collect(Collectors.toList());

        return MessageResponse.builder()
                .id(message.getMessageId())
                .senderId(message.getSender().getUserId())
                .content(message.getContent())
                .createdAt(message.getCreatedAt())
                .readAt(message.getReadAt())
                .replyTo(replyToInfo)
                .attachments(attachmentInfos)
                .build();
    }
}