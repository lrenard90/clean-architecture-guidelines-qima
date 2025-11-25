package com.example.cleanarchitectureapplication.unit.messaging;

import com.example.cleanarchitectureapplication.messaging.application.dto.PostMessageRequestDTO;
import com.example.cleanarchitectureapplication.messaging.builders.MessageBuilder;
import com.example.cleanarchitectureapplication.unit.messaging.fixtures.MessagingFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.UUID;

@DisplayName("Feature: Post a message")
class PostMessageUseCaseHandlerTest {
    private MessagingFixture messagingFixture;

    @BeforeEach
    void setup() {
        messagingFixture = new MessagingFixture();
    }

    @Test
    @DisplayName("Scenario: User can post a valid message")
    void postValidMessage() {
        messagingFixture.givenNowIs(LocalDateTime.of(2020, 2, 14, 17, 46, 51));

        messagingFixture.whenUserPostsAMessage(
                new PostMessageRequestDTO(
                        UUID.fromString("e1fd6ad4-83d5-4f8d-a788-0132c9b33318"),
                        "Alice",
                        "Hello world!"
                )
        );

        messagingFixture.thenPostedMessageShouldBe(
                new MessageBuilder()
                        .withId(UUID.fromString("e1fd6ad4-83d5-4f8d-a788-0132c9b33318"))
                        .withAuthor("Alice")
                        .withText("Hello world!")
                        .withPublishedDate(LocalDateTime.of(2020, 2, 14, 17, 46, 51))
                        .build()
        );
    }

    @Nested
    @DisplayName("Rule: a message size is limited to 280 characters")
    class MessageSizeLimitation {
        @Test
        void userCannotPostAMessageWithMoreThan280Characters() {
            messagingFixture.givenNowIs(LocalDateTime.of(2020, 2, 14, 17, 46, 51));

            messagingFixture.whenUserPostsAMessage(
                    new PostMessageRequestDTO(
                            UUID.randomUUID(),
                            "Alice",
                            "a".repeat(281)
                    )
            );

            messagingFixture.thenErrorShouldBe("Message text must be less than 280 characters");
        }
    }

    @Nested
    @DisplayName("Rule: a message cannot be empty")
    class MessageNotEmpty {
        @Test
        void userCannotPostAnEmptyMessage() {
            messagingFixture.givenNowIs(LocalDateTime.of(2020, 2, 14, 17, 46, 51));

            messagingFixture.whenUserPostsAMessage(
                    new PostMessageRequestDTO(
                            UUID.randomUUID(),
                            "Alice",
                            ""
                    )
            );

            messagingFixture.thenErrorShouldBe("Message text must not be blank");
        }

        @Test
        void userCannotPostABlankMessage() {
            messagingFixture.givenNowIs(LocalDateTime.of(2020, 2, 14, 17, 46, 51));

            messagingFixture.whenUserPostsAMessage(
                    new PostMessageRequestDTO(
                            UUID.randomUUID(),
                            "Alice",
                            " "
                    )
            );

            messagingFixture.thenErrorShouldBe("Message text must not be blank");
        }
    }

    @Nested
    @DisplayName("Scenario: id already exists")
    class IdAlreadyExists {
        @Test
        void userCannotPostAMessageWithAnIdThatAlreadyExists() {
            UUID existingMessageId = UUID.fromString("e1fd6ad4-83d5-4f8d-a788-0132c9b33318");
            messagingFixture.givenTheFollowingMessagesExists(
                    java.util.List.of(
                            new MessageBuilder()
                                    .withId(existingMessageId)
                                    .build()
                    )
            );

            messagingFixture.whenUserPostsAMessage(
                    new PostMessageRequestDTO(
                            existingMessageId,
                            "Alice",
                            "Hello world!"
                    )
            );

            messagingFixture.thenErrorShouldBe("Message already exists");
        }
    }
}

