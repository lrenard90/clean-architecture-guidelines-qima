package com.example.cleanarchitectureapplication.messaging.application.usecases;

import com.example.cleanarchitectureapplication.messaging.application.dto.GetTimelineRequestDTO;
import com.example.cleanarchitectureapplication.messaging.application.dto.TimelineMessageDTO;
import com.example.cleanarchitectureapplication.messaging.application.ports.MessageRepository;
import com.example.cleanarchitectureapplication.messaging.domain.entity.Message;
import com.example.cleanarchitectureapplication.socle.dependencyinjection.annotation.UseCase;
import com.example.cleanarchitectureapplication.socle.time.DateProvider;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

@UseCase
public class ViewTimelineUseCaseHandler {
    private final MessageRepository messageRepository;
    private final DateProvider dateProvider;

    public ViewTimelineUseCaseHandler(MessageRepository messageRepository, DateProvider dateProvider) {
        this.messageRepository = messageRepository;
        this.dateProvider = dateProvider;
    }

    public List<TimelineMessageDTO> handle(GetTimelineRequestDTO getTimelineRequestDTO) {
        return messageRepository.findAllByAuthor(getTimelineRequestDTO.author())
                .stream()
                .sorted(Comparator.comparing((Message message) -> message.data().publishedDate()).reversed())
                .map(message -> new TimelineMessageDTO(
                        message.data().id(),
                        message.data().author(),
                        message.data().text(),
                        computePublishedDateString(message.data().publishedDate())
                ))
                .toList();
    }

    private String computePublishedDateString(LocalDateTime publishedDate) {
        LocalDateTime now = dateProvider.now();
        long minutesDifference = ChronoUnit.MINUTES.between(publishedDate, now);
        return minutesDifference + " minute" + (minutesDifference > 1 ? "s" : "") + " ago";
    }
}

