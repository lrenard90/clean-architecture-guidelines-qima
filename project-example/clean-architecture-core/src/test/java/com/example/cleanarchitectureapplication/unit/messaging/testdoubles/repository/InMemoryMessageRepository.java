package com.example.cleanarchitectureapplication.unit.messaging.testdoubles.repository;

import com.example.cleanarchitectureapplication.messaging.application.ports.MessageRepository;
import com.example.cleanarchitectureapplication.messaging.domain.entity.Message;
import com.example.cleanarchitectureapplication.messaging.domain.entity.MessageData;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class InMemoryMessageRepository implements MessageRepository {
    private Map<UUID, Message> messagesById = new HashMap<>();

    @Override
    public Message save(Message message) {
        Message copied = copy(message);
        messagesById.put(copied.data().id(), copied);
        return copied;
    }

    @Override
    public List<Message> findAllByAuthor(String author) {
        return messagesById.values().stream()
                .filter(message -> message.data().author().equals(author))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Message> findById(UUID messageId) {
        return Optional.ofNullable(messagesById.get(messageId));
    }

    @Override
    public boolean existsById(UUID id) {
        return messagesById.containsKey(id);
    }

    public Collection<Message> messages() {
        return messagesById.values();
    }

    public void setMessages(List<Message> messages) {
        messagesById = new HashMap<>();
        for (Message message : messages) {
            messagesById.put(message.data().id(), message);
        }
    }

    private Message copy(Message message) {
        // We use data structure instance creation to avoid validation errors and be sure the validation logic is in the hexagon and not in this in memory test double
        MessageData data = message.data();
        return Message.fromData(new MessageData(data.id(), data.author(), data.text(), data.publishedDate()));
    }

    public Message get(UUID id) {
        return messagesById.values().stream()
                .filter(message -> message.data().id().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));
    }
}

