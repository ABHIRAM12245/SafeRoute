package com.vlabs.routeguard.models;

/**
 * Generic data model for encapsulating the result of a voting operation.
 */
public class VoteResult {

    private boolean success;
    private String errorMessage;

    public VoteResult() {
        // Default constructor required for serialization
    }

    public VoteResult(boolean success, String errorMessage) {
        this.success = success;
        this.errorMessage = errorMessage;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
