package com.sanjay.aisecurity.enums;

/**
 * Vulnerability severity levels, ordered from least to most critical.
 *
 * <p>Used in the static analysis engine to classify detected vulnerabilities
 * and drive risk score calculations in {@code RiskCalculator}.</p>
 *
 * <ul>
 *   <li>{@code LOW} — Minor issues with limited direct impact.</li>
 *   <li>{@code MEDIUM} — Issues that could be exploited under specific conditions.</li>
 *   <li>{@code HIGH} — Significant vulnerabilities with real exploit potential.</li>
 *   <li>{@code CRITICAL} — Severe vulnerabilities requiring immediate remediation.</li>
 * </ul>
 *
 * @author Sanjay
 * @version 1.0.0
 */
public enum Severity {

    /** Informational finding — stub engine or non-vulnerability note. */
    INFORMATIONAL,

    /** Minor issue — informational or low-risk finding. */
    LOW,

    /** Moderate risk — context-dependent exploitability. */
    MEDIUM,

    /** Significant risk — high probability of exploitation. */
    HIGH,

    /** Severe risk — immediate remediation required. */
    CRITICAL
}
