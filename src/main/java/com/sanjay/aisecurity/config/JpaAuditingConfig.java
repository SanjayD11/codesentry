package com.sanjay.aisecurity.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA Auditing Configuration.
 *
 * <p>Enables Spring Data JPA auditing which automatically populates
 * {@code @CreatedDate} and {@code @LastModifiedDate} fields on
 * {@link com.sanjay.aisecurity.entity.BaseEntity} whenever an entity
 * is persisted or updated.</p>
 *
 * <p>The {@code @EntityListeners(AuditingEntityListener.class)} annotation
 * on {@code BaseEntity} hooks into this configuration at the JPA lifecycle
 * level — no additional setup is required per entity.</p>
 *
 * @author Sanjay
 * @version 1.0.0
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
    // Intentionally empty — @EnableJpaAuditing activates the full auditing
    // infrastructure through the Spring Data JPA auto-configuration pipeline.
}
