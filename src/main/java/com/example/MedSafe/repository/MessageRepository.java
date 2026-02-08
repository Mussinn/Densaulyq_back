package com.example.MedSafe.repository;

import com.example.MedSafe.model.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("SELECT m FROM Message m " +
            "WHERE m.chat.chatId = :chatId " +
            "AND m.isDeleted = false " +
            "ORDER BY m.createdAt DESC")
    List<Message> findByChatId(@Param("chatId") Long chatId, Pageable pageable);

    @Query("SELECT m FROM Message m " +
            "WHERE m.chat.chatId = :chatId " +
            "AND m.isDeleted = false " +
            "ORDER BY m.createdAt DESC")
    List<Message> findLastMessageByChatId(@Param("chatId") Long chatId, Pageable pageable);

    @Query("SELECT COUNT(m) FROM Message m " +
            "WHERE m.chat.chatId = :chatId " +
            "AND m.sender.userId != :userId " +
            "AND m.createdAt > :lastReadAt " +
            "AND m.isDeleted = false")
    Integer countUnreadMessages(@Param("chatId") Long chatId,
                                @Param("userId") Integer userId,
                                @Param("lastReadAt") LocalDateTime lastReadAt);

    @Modifying
    @Query("UPDATE Message m SET m.readAt = :readAt " +
            "WHERE m.chat.chatId = :chatId " +
            "AND m.sender.userId != :userId " +
            "AND m.readAt IS NULL")
    void markMessagesAsRead(@Param("chatId") Long chatId,
                            @Param("userId") Integer userId,
                            @Param("readAt") LocalDateTime readAt);
}