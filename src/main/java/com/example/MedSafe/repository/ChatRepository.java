package com.example.MedSafe.repository;

import com.example.MedSafe.model.Chat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRepository extends JpaRepository<Chat, Long> {

    @Query("SELECT c FROM Chat c " +
            "JOIN c.participants cp " +
            "WHERE cp.user.userId = :userId " +
            "ORDER BY c.updatedAt DESC")
    List<Chat> findAllByUserId(@Param("userId") Integer userId);

    @Query("SELECT c FROM Chat c " +
            "JOIN c.participants cp1 " +
            "JOIN c.participants cp2 " +
            "WHERE cp1.user.userId = :userId1 " +
            "AND cp2.user.userId = :userId2 " +
            "AND (SELECT COUNT(cp3) FROM ChatParticipant cp3 WHERE cp3.chat = c) = 2")
    Optional<Chat> findPrivateChatBetweenUsers(@Param("userId1") Integer userId1,
                                               @Param("userId2") Integer userId2);
}