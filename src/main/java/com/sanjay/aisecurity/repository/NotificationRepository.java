package com.sanjay.aisecurity.repository;

import com.sanjay.aisecurity.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for {@link Notification} entity.
 *
 * <p>Provides access to user notification logs, unread notification counts,
 * and scoped status updates.</p>
 *
 * @author Sanjay
 * @version 1.0.0
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * Retrieves all notifications for a user, paginated.
     *
     * @param email    user email
     * @param pageable pagination parameters
     * @return page of notifications
     */
    Page<Notification> findByUserEmail(String email, Pageable pageable);

    /**
     * Retrieves a notification by its ID, verified by owner's email.
     *
     * @param id    notification ID
     * @param email owner email
     * @return an {@link Optional} containing the notification if found and owned, or empty
     */
    Optional<Notification> findByIdAndUserEmail(Long id, String email);

    /**
     * Counts the number of unread notifications for a user.
     *
     * @param email user email
     * @return count of unread notifications
     */
    long countByUserEmailAndIsReadFalse(String email);
}
