package com.example.cleanarchitectureapplication.messaging.application.dto;

import java.util.UUID;

public record TimelineMessageDTO(UUID id, String author, String text, String publicationTime) {
}

