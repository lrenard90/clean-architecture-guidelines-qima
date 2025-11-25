package com.example.cleanarchitectureapplication.unit.messaging.fixtures;

import com.example.cleanarchitectureapplication.messaging.application.dto.EditMessageRequestDTO;
import com.example.cleanarchitectureapplication.messaging.application.dto.GetTimelineRequestDTO;
import com.example.cleanarchitectureapplication.messaging.application.dto.PostMessageRequestDTO;
import com.example.cleanarchitectureapplication.messaging.application.dto.TimelineMessageDTO;
import com.example.cleanarchitectureapplication.messaging.domain.entity.Message;
import com.example.cleanarchitectureapplication.messaging.application.usecases.EditMessageUseCaseHandler;
import com.example.cleanarchitectureapplication.messaging.application.usecases.PostMessageUseCaseHandler;
import com.example.cleanarchitectureapplication.messaging.application.usecases.ViewTimelineUseCaseHandler;
import com.example.cleanarchitectureapplication.shared.time.FakeDateProvider;
import com.example.cleanarchitectureapplication.unit.messaging.testdoubles.repository.InMemoryMessageRepository;
import org.assertj.core.api.Assertions;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class MessagingFixture {
    private final InMemoryMessageRepository messageRepository = new InMemoryMessageRepository();
    private final FakeDateProvider dateProvider = new FakeDateProvider();
    private final PostMessageUseCaseHandler postMessageUseCaseHandler = new PostMessageUseCaseHandler(messageRepository, dateProvider);
    private final ViewTimelineUseCaseHandler viewTimelineUseCaseHandler = new ViewTimelineUseCaseHandler(messageRepository, dateProvider);
    private final EditMessageUseCaseHandler editMessageUseCaseHandler = new EditMessageUseCaseHandler(messageRepository);
    private String errorMessage;
    private List<TimelineMessageDTO> timelineMessages;

    public void givenNowIs(LocalDateTime now) {
        dateProvider.setNow(now);
    }

    public void whenUserPostsAMessage(PostMessageRequestDTO postMessageRequestDTO) {
        try {
            postMessageUseCaseHandler.handle(postMessageRequestDTO);
        } catch (Exception exception) {
            errorMessage = exception.getMessage();
        }
    }

    public void thenPostedMessageShouldBe(Message expectedMessage) {
        Assertions.assertThat(
                messageRepository.messages().stream()
                        .map(Message::data)
                        .toList()
        ).contains(expectedMessage.data());
    }

    public void thenErrorShouldBe(String error) {
        Assertions.assertThat(errorMessage).isEqualTo(error);
    }

    public void givenTheFollowingMessagesExists(List<Message> messages) {
        messageRepository.setMessages(messages);
    }

    public void whenUserSeesTimelineOf(String author) {
        timelineMessages = viewTimelineUseCaseHandler.handle(new GetTimelineRequestDTO(author));
    }

    public void thenUserGetsAnEmptyTimeline() {
        Assertions.assertThat(timelineMessages).isEmpty();
    }

    public void thenUserShouldSee(List<TimelineMessageDTO> timelineMessages) {
        Assertions.assertThat(this.timelineMessages).containsExactlyElementsOf(timelineMessages);
    }

    public void whenUserEditHisMessage(UUID messageId, String text) {
        try {
            editMessageUseCaseHandler.handle(new EditMessageRequestDTO(messageId, text));
        } catch (Exception exception) {
            errorMessage = exception.getMessage();
        }
    }

    public void thenMessageShouldBe(Message message) {
        Assertions.assertThat(messageRepository.get(message.data().id()).data()).isEqualTo(message.data());
    }
}

