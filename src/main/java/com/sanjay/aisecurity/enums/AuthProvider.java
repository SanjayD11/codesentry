package com.sanjay.aisecurity.enums;

/**
 * Identifies the authentication provider used to create a user account.
 *
 * <p>{@code LOCAL} users registered with email and password via the standard
 * registration form. {@code GOOGLE} and {@code GITHUB} users authenticated
 * through Firebase OAuth and were auto-provisioned on first login.</p>
 *
 * @author Sanjay
 * @version 1.1.0
 */
public enum AuthProvider {

    /** Traditional email + password registration. */
    LOCAL,

    /** Google Sign-In via Firebase Authentication. */
    GOOGLE,

    /** GitHub Sign-In via Firebase Authentication. */
    GITHUB
}
