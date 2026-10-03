package com.olehkobylianskyi.familypoints.exception;

public class InsufficientPointsException extends RuntimeException {

    private final long required;
    private final long available;

    public InsufficientPointsException(
            long required,
            long available
    ) {
        super("Not enough points");

        this.required = required;
        this.available = available;
    }

    public long getRequired() {
        return required;
    }

    public long getAvailable() {
        return available;
    }
}