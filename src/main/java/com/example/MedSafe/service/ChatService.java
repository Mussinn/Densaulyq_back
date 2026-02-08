package com.example.MedSafe.service;

import com.example.MedSafe.dto.ChatResponse;
import com.example.MedSafe.model.Chat;
import com.example.MedSafe.model.ChatParticipant;
import com.example.MedSafe.model.Message;
import com.example.MedSafe.model.User;
import com.example.MedSafe.model.dto.CreateChatRequest;
import com.example.MedSafe.repository.ChatParticipantRepository;
import com.example.MedSafe.repository.ChatRepository;
import com.example.MedSafe.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRepository chatRepository;
    private final ChatParticipantRepository chatParticipantRepository;
    private final MessageRepository messageRepository;
    private final UserService userService;

    @Transactional(readOnly = true)
    public List<ChatResponse> getUserChats(Integer userId) {
        List<Chat> chats = chatRepository.findAllByUserId(userId);

        return chats.stream()
                .map(chat -> buildChatResponse(chat, userId))
                .collect(Collectors.toList());
    }

    @Transactional
    public ChatResponse createChat(Integer currentUserId, CreateChatRequest request) {
        // Проверяем, не существует ли уже чат между этими пользователями
        var existingChat = chatRepository.findPrivateChatBetweenUsers(currentUserId, request.getParticipantId());

        if (existingChat.isPresent()) {
            return buildChatResponse(existingChat.get(), currentUserId);
        }

        // Создаем новый чат
        Chat chat = Chat.builder()
                .participants(new ArrayList<>())
                .messages(new ArrayList<>())
                .build();

        chat = chatRepository.save(chat);

        // Получаем пользователей
        User currentUser = userService.findById(currentUserId);
        User otherUser = userService.findById(request.getParticipantId());

        // Добавляем участников
        ChatParticipant participant1 = ChatParticipant.builder()
                .chat(chat)
                .user(currentUser)
                .build();

        ChatParticipant participant2 = ChatParticipant.builder()
                .chat(chat)
                .user(otherUser)
                .build();

        chatParticipantRepository.save(participant1);
        chatParticipantRepository.save(participant2);

        return buildChatResponse(chat, currentUserId);
    }

    @Transactional
    public void markAsRead(Long chatId, Integer userId) {
        ChatParticipant participant = chatParticipantRepository
                .findByChatIdAndUserId(chatId, userId)
                .orElseThrow(() -> new RuntimeException("Participant not found"));

        LocalDateTime now = LocalDateTime.now();
        participant.setLastReadAt(now);
        chatParticipantRepository.save(participant);

        // Отмечаем все сообщения как прочитанные
        messageRepository.markMessagesAsRead(chatId, userId, now);
    }

    private ChatResponse buildChatResponse(Chat chat, Integer currentUserId) {
        // Находим другого участника
        ChatParticipant otherParticipant = chatParticipantRepository
                .findOtherParticipant(chat.getChatId(), currentUserId)
                .orElse(null);

        // Получаем последнее сообщение
        List<Message> lastMessages = messageRepository.findLastMessageByChatId(
                chat.getChatId(),
                PageRequest.of(0, 1)
        );

        String lastMessage = null;
        LocalDateTime lastMessageAt = chat.getUpdatedAt();

        if (!lastMessages.isEmpty()) {
            Message msg = lastMessages.get(0);
            lastMessage = msg.getContent();
            lastMessageAt = msg.getCreatedAt();
        }

        // Подсчитываем непрочитанные сообщения
        ChatParticipant currentParticipant = chatParticipantRepository
                .findByChatIdAndUserId(chat.getChatId(), currentUserId)
                .orElse(null);

        Integer unreadCount = 0;
        if (currentParticipant != null) {
            LocalDateTime lastReadAt = currentParticipant.getLastReadAt() != null
                    ? currentParticipant.getLastReadAt()
                    : LocalDateTime.of(1970, 1, 1, 0, 0);
            unreadCount = messageRepository.countUnreadMessages(
                    chat.getChatId(),
                    currentUserId,
                    lastReadAt
            );
        }

        // Формируем информацию об участнике
        ChatResponse.ParticipantInfo participantInfo = null;
        if (otherParticipant != null) {
            User otherUser = otherParticipant.getUser();
            participantInfo = ChatResponse.ParticipantInfo.builder()
                    .id(otherUser.getUserId())
                    .name(otherUser.getFirstName() + " " + otherUser.getLastName())
                    .specialty(getDoctorSpecialty(otherUser))
                    .online(otherUser.getOnline() != null ? otherUser.getOnline() : false)
                    .email(otherUser.getEmail())
                    .build();
        }

        return ChatResponse.builder()
                .id(chat.getChatId())
                .participant(participantInfo)
                .lastMessage(lastMessage)
                .lastMessageAt(lastMessageAt)
                .createdAt(chat.getCreatedAt())
                .unreadCount(unreadCount)
                .build();
    }

    private String getDoctorSpecialty(User user) {
        // Здесь нужно получить специальность врача из таблицы doctor
        // Если у вас есть связь между User и Doctor, используйте её
        // Пока возвращаем заглушку
        return "—";
    }
}