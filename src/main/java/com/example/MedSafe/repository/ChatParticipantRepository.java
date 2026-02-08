package com.example.MedSafe.repository;

import com.example.MedSafe.model.ChatParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChatParticipantRepository extends JpaRepository<ChatParticipant, Long> {

    @Query("SELECT cp FROM ChatParticipant cp " +
            "WHERE cp.chat.chatId = :chatId " +
            "AND cp.user.userId = :userId")
    Optional<ChatParticipant> findByChatIdAndUserId(@Param("chatId") Long chatId,
                                                    @Param("userId") Integer userId);

    @Query("SELECT cp FROM ChatParticipant cp " +
            "WHERE cp.chat.chatId = :chatId " +
            "AND cp.user.userId != :userId")
    Optional<ChatParticipant> findOtherParticipant(@Param("chatId") Long chatId,
                                                   @Param("userId") Integer userId);
}