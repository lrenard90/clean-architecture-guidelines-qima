package com.example.cleanarchitectureapplication.messaging.application.usecases;

import com.example.cleanarchitectureapplication.messaging.application.dto.EditMessageRequestDTO;
import com.example.cleanarchitectureapplication.messaging.application.ports.MessageRepository;
import com.example.cleanarchitectureapplication.messaging.domain.entity.Message;
import com.example.cleanarchitectureapplication.socle.dependencyinjection.annotation.UseCase;

@UseCase
public class EditMessageUseCaseHandler {
    private final MessageRepository messageRepository;

    public EditMessageUseCaseHandler(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public void handle(EditMessageRequestDTO editMessageRequestDTO) {
        Message messageToEdit = messageRepository.findById(editMessageRequestDTO.messageId())
                .orElseThrow(() -> new RuntimeException("Message not found"));

        messageToEdit.editText(editMessageRequestDTO.text());

        messageRepository.save(messageToEdit);
    }
}

