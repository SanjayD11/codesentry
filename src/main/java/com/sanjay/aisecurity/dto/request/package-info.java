/**
 * DTO request objects package.
 *
 * <p>Contains all inbound Data Transfer Objects received from API clients.
 * Every DTO includes Bean Validation annotations to enforce input constraints
 * before business logic is invoked. DTOs are never passed directly to the
 * data access layer — mapper classes convert them to entities.</p>
 *
 * <p>Naming Convention: {@code <Feature>Request} (e.g., {@code RegisterRequest},
 * {@code ProjectRequest}).</p>
 */
package com.sanjay.aisecurity.dto.request;
