package com.olehkobylianskyi.familypoints.exception;

public class PointExchangeRateAlreadyExistsException extends RuntimeException {
    public PointExchangeRateAlreadyExistsException(String message) {
        super(message);
    }
}
