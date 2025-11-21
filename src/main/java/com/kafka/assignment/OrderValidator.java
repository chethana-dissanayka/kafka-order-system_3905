package com.kafka.assignment;

public class OrderValidator {

    public enum ValidationResult {
        VALID,
        RETRY,
        DLQ,
        INVALID
    }


    public static ValidationResult validateOrderId(String orderId) {
        if (orderId == null || orderId.trim().isEmpty()) {
            return ValidationResult.INVALID;
        }

        String trimmed = orderId.trim();


        if (trimmed.contains(".")) {
            try {
                Double.parseDouble(trimmed);
                return ValidationResult.RETRY;
            } catch (NumberFormatException e) {
                return ValidationResult.INVALID;
            }
        }


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

