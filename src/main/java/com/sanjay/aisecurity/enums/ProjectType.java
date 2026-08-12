package com.sanjay.aisecurity.enums;

/**
 * Supported programming language/project types for static code analysis.
 *
 * <p>Used on the {@code Project} entity and by the {@code AnalyzerFactory}
 * to route uploaded files to the appropriate language-specific analyzer.</p>
 *
 * @author Sanjay
 * @version 1.0.0
 */
public enum ProjectType {

    /** Standard Java application. */
    JAVA,

    /** Spring Boot Java application. */
    SPRING_BOOT,

    /** Python application or script. */
    PYTHON,

    /** Vanilla JavaScript application. */
    JAVASCRIPT,

    /** Node.js server-side JavaScript application. */
    NODE,

    /** React frontend JavaScript application. */
    REACT,

    /** Angular frontend TypeScript application. */
    ANGULAR,
    
    /** General web application (HTML/CSS/JS). */
    WEB_APPLICATION,

    /** Any other project type not listed above. */
    OTHER
}
