package com.olehkobylianskyi.familypoints.exception;

public class InvalidPointExchangeException extends RuntimeException {

    public InvalidPointExchangeException(String message) {
        super(message);
    }
}