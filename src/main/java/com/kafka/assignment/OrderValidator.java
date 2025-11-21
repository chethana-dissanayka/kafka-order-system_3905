package com.kafka.assignment;

public class OrderValidator {

    public enum ValidationResult {
        VALID,      // Non-negative integer -> process normally
        RETRY,      // Contains decimal point (float) -> retry topic
        DLQ,        // Negative value -> DLQ topic
        INVALID     // Not a valid number format
    }

    /**
     * Validates the orderId to determine routing:
     * - Negative integer -> DLQ
     * - Float (contains decimal point) -> RETRY
     * - Non-negative integer -> VALID
     * - Invalid format -> INVALID
     */
    public static ValidationResult validateOrderId(String orderId) {
        if (orderId == null || orderId.trim().isEmpty()) {
            return ValidationResult.INVALID;
        }

        String trimmed = orderId.trim();

        // Check if it contains a decimal point -> treat as float -> RETRY
        if (trimmed.contains(".")) {
            try {
                Double.parseDouble(trimmed); // Verify it's a valid number
                return ValidationResult.RETRY;
            } catch (NumberFormatException e) {
                return ValidationResult.INVALID;
            }
        }

        // Otherwise, treat as integer
        try {
            long value = Long.parseLong(trimmed);
            if (value < 0) {
                return ValidationResult.DLQ;
            } else {
                return ValidationResult.VALID;
            }
        } catch (NumberFormatException e) {
            return ValidationResult.INVALID;
        }
    }
}

