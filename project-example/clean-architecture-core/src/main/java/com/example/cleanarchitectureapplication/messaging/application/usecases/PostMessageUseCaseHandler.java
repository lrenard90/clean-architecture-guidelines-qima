package com.example.cleanarchitectureapplication.messaging.application.usecases;

import com.example.cleanarchitectureapplication.messaging.application.dto.PostMessageRequestDTO;
import com.example.cleanarchitectureapplication.messaging.application.ports.MessageRepository;
import com.example.cleanarchitectureapplication.messaging.domain.entity.Message;
import com.example.cleanarchitectureapplication.socle.dependencyinjection.annotation.UseCase;
import com.example.cleanarchitectureapplication.socle.time.DateProvider;

@UseCase
public class PostMessageUseCaseHandler {
    private final MessageRepository messageRepository;
    private final DateProvider dateProvider;

    public PostMessageUseCaseHandler(MessageRepository messageRepository, DateProvider dateProvider) {
        this.messageRepository = messageRepository;
        this.dateProvider = dateProvider;
    }

    public void handle(PostMessageRequestDTO postMessageRequestDTO) {
        if (messageRepository.existsById(postMessageRequestDTO.id())) {
            throw new RuntimeException("Message already exists");
        }

        Message messageToCreate = new Message(
                postMessageRequestDTO.id(),
                postMessageRequestDTO.author(),
                postMessageRequestDTO.text(),
                dateProvider.now()
        );

        messageRepository.save(messageToCreate);
    }
}

