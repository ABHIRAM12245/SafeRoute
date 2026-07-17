package com.vlabs.routeguard.utils;

/**
 * Centralized Firestore path constants exclusively for voting operations.
 */
public final class VoteFirestorePaths {

    private VoteFirestorePaths() {
        // Prevent instantiation
    }

    // REPORTS is already defined in FirestoreCollections.REPORTS
    public static final String VOTES = "votes";
}
