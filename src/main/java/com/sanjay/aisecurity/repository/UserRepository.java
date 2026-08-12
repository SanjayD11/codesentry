package com.sanjay.aisecurity.repository;

import com.sanjay.aisecurity.entity.User;
import com.sanjay.aisecurity.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Repository interface for {@link User} entity.
 *
 * <p>Provides standard CRUD operations and custom query methods to look up
 * users by email, verify registration eligibility, and search users by security role.</p>
 *
 * @author Sanjay
 * @version 1.0.0
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Finds a user by their unique email address.
     *
     * @param email user email address
     * @return an {@link Optional} containing the user if found, or empty
     */
    Optional<User> findByEmail(String email);

    /**
     * Checks if a user already exists with the given email address.
     *
     * @param email user email address
     * @return {@code true} if a record is found, {@code false} otherwise
     */
    boolean existsByEmail(String email);

    /**
     * Counts users with a specific role.
     */
    long countByRole(Role role);

    /**
     * Retrieves a paginated page of users sharing a specific security role.
     *
     * @param role     the target {@link Role} filter
     * @param pageable pagination parameters
     * @return a page of matching users
     */
    Page<User> findAllByRole(Role role, Pageable pageable);

    /**
     * Searches for users by email, first name, or last name.
     */
    Page<User> findByEmailContainingIgnoreCaseOrFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
            String email, String firstName, String lastName, Pageable pageable);

    /** Counts users who are currently active. */
    long countByActiveTrue();

    /** Counts users who are currently disabled. */
    long countByActiveFalse();

    /** Counts users registered since a given timestamp. */
    long countByCreatedAtAfter(LocalDateTime since);

    /** Counts users with a specific role and active status. */
    long countByRoleAndActiveTrue(Role role);

    /** Finds users with optional role/active filter for admin listing. */
    @Query("SELECT u FROM User u WHERE " +
           "(:search IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) " +
           " OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           " OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:role IS NULL OR u.role = :role) " +
           "AND (:active IS NULL OR u.active = :active)")
    Page<User> findUsersWithFilters(
            @Param("search") String search,
            @Param("role") Role role,
            @Param("active") Boolean active,
            Pageable pageable);
}

