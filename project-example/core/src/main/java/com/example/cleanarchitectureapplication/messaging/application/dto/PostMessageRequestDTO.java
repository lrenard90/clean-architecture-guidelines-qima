package com.example.cleanarchitectureapplication.messaging.application.dto;

import java.util.UUID;

public record PostMessageRequestDTO(UUID id, String author, String text) {
}

