package com.irishpubfinder.api.model;

/**
 * Platform user types.
 * <ul>
 *   <li>{@link #USER} — default free user.</li>
 *   <li>{@link #PRO} — paid pro subscriber.</li>
 *   <li>{@link #FREE_PRO} — comped pro access (all pro features, no payment).</li>
 *   <li>{@link #ADMIN} — full access: every pro feature plus admin-only screens.</li>
 * </ul>
 */
public enum UserRole {
    USER,
    PRO,
    FREE_PRO,
    ADMIN;

    /** Roles that unlock pro features. */
    public boolean grantsPro() {
        return this == ADMIN || this == PRO || this == FREE_PRO;
    }
}
