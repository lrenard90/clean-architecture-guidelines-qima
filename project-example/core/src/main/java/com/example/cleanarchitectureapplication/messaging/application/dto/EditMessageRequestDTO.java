package com.example.cleanarchitectureapplication.messaging.application.dto;

import java.util.UUID;

public record EditMessageRequestDTO(UUID messageId, String text) {
}

