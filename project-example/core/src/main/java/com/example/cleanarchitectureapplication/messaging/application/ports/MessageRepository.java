package com.example.cleanarchitectureapplication.messaging.application.ports;

import com.example.cleanarchitectureapplication.messaging.domain.entity.Message;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MessageRepository {
    Message save(Message message);
    List<Message> findAllByAuthor(String author);
    Optional<Message> findById(UUID messageId);
    boolean existsById(UUID id);
}

