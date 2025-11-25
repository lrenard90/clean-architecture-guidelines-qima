package com.example.cleanarchitectureapplication.messaging.domain.entity;

import java.time.LocalDateTime;
import java.util.UUID;

public record MessageData(UUID id, String author, String text, LocalDateTime publishedDate) {
}

