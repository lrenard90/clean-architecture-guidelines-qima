package com.example.cleanarchitectureapplication.messaging.domain.valueobject;

public record MessageText(String value) {
    public MessageText {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Message text must not be blank");
        }
        if (value.length() > 280) {
            throw new IllegalArgumentException("Message text must be less than 280 characters");
        }
    }
}

