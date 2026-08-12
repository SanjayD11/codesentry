/**
 * JPA Entity classes package.
 *
 * <p>Contains all Hibernate-managed database entity classes annotated with
 * {@code @Entity}. All entities extend {@code BaseEntity} for consistent ID,
 * createdAt, and updatedAt auditing. Relationships are configured with explicit
 * cascade rules, fetch strategies, and join column constraints.</p>
 *
 * <p>Naming Convention: singular noun (e.g., {@code User}, {@code Project},
 * {@code ScanHistory}).</p>
 */
package com.sanjay.aisecurity.entity;
