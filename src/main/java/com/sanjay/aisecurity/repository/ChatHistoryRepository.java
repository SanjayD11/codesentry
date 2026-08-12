package com.sanjay.aisecurity.repository;

import com.sanjay.aisecurity.entity.ChatHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for {@link ChatHistory} entity.
 *
 * <p>Manages database records of AI chatbot conversations, supporting distinct
 * thread grouping and bulk deletes of conversation threads.</p>
 *
 * @author Sanjay
 * @version 1.0.0
 */
@Repository
public interface ChatHistoryRepository extends JpaRepository<ChatHistory, Long> {

    /**
     * Finds a paginated log of user's exchanges.
     *
     * @param email    owner email
     * @param pageable pagination parameters
     * @return page of chat history
     */
    Page<ChatHistory> findByUserEmail(String email, Pageable pageable);

    /**
     * Retrieves all messages in a specific thread (conversationId) for the user.
     *
     * @param conversationId grouping ID of the thread
     * @param email          user email
     * @return list of chat history exchanges ordered from oldest to newest
     */
    List<ChatHistory> findByConversationIdAndUserEmailOrderByCreatedAtAsc(String conversationId, String email);

    /**
     * Deletes all messages in a specific thread for the user.
     *
     * @param conversationId thread grouping ID
     * @param email          user email
     */
    @Modifying
    @Query("DELETE FROM ChatHistory c WHERE c.conversationId = :conversationId AND c.user.email = :email")
    void deleteByConversationIdAndUserEmail(@Param("conversationId") String conversationId, @Param("email") String email);

    /**
     * Returns a distinct list of conversation IDs active for a user.
     *
     * @param email user email
     * @return list of active conversation ID strings
     */
    @Query("SELECT DISTINCT c.conversationId FROM ChatHistory c WHERE c.user.email = :email")
    List<String> findDistinctConversationIdsByUserEmail(@Param("email") String email);
}
