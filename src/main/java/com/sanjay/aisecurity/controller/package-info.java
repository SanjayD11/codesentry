/**
 * REST controllers package.
 *
 * <p>Contains all Spring MVC {@code @RestController} classes that expose
 * the platform's REST API endpoints. Every controller delegates business
 * logic to a corresponding service interface and returns {@code ApiResponse<T>}
 * wrappers with appropriate HTTP status codes.</p>
 *
 * <p>Naming Convention: {@code <Feature>Controller} (e.g., {@code AuthController},
 * {@code ProjectController}, {@code ScanController}).</p>
 */
package com.sanjay.aisecurity.controller;
