package com.example.cleanarchitectureapplication.unit.messaging;

import com.example.cleanarchitectureapplication.messaging.builders.MessageBuilder;
import com.example.cleanarchitectureapplication.unit.messaging.fixtures.MessagingFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@DisplayName("Feature: Edit a message")
class EditMessageUseCaseHandlerTest {
    private MessagingFixture messagingFixture;

    @BeforeEach
    void setUp() {
        messagingFixture = new MessagingFixture();
    }

    @Test
    void userCanEditHisMessage() {
        MessageBuilder aliceMessageBuilder = MessageBuilder.aliceMessageBuilder();
        messagingFixture.givenTheFollowingMessagesExists(
                List.of(
                        aliceMessageBuilder
                                .withId(UUID.fromString("cc865b1a-529a-4973-9d0b-58ca894f98a2"))
                                .withText("Hello world!")
                                .build(),
                        aliceMessageBuilder
                                .withId(UUID.fromString("e1fd6ad4-83d5-4f8d-a788-0132c9b33319"))
                                .withText("Hello!")
                                .build()
                )
        );

        messagingFixture.whenUserEditHisMessage(
                UUID.fromString("cc865b1a-529a-4973-9d0b-58ca894f98a2"),
                "Hello world! I'm Alice"
        );

        messagingFixture.thenMessageShouldBe(
                aliceMessageBuilder
                        .withId(UUID.fromString("cc865b1a-529a-4973-9d0b-58ca894f98a2"))
                        .withText("Hello world! I'm Alice")
                        .build()
        );
    }

    @Nested
    class MessageNotFound {
        @Test
        void userCannotEditAMessageThatDoesNotExist() {
            messagingFixture.givenTheFollowingMessagesExists(Collections.emptyList());

            messagingFixture.whenUserEditHisMessage(
                    UUID.fromString("cc865b1a-529a-4973-9d0b-58ca894f98a2"),
                    "Hello world! I'm Alice"
            );

            messagingFixture.thenErrorShouldBe("Message not found");
        }
    }

    @Nested
    @DisplayName("Rule: a message size is limited to 280 characters")
    class MessageSizeLimitation {
        @Test
        void userCannotEditAMessageWithMoreThan280Characters() {
            messagingFixture.givenTheFollowingMessagesExists(
                    List.of(
                            new MessageBuilder()
                                    .withId(UUID.fromString("cc865b1a-529a-4973-9d0b-58ca894f98a2"))
                                    .withAuthor("Alice")
                                    .withText("Hello world!")
                                    .withPublishedDate(LocalDateTime.of(2020, 2, 14, 17, 46, 51))
                                    .build()
                    )
            );

            messagingFixture.whenUserEditHisMessage(
                    UUID.fromString("cc865b1a-529a-4973-9d0b-58ca894f98a2"),
                    "a".repeat(281)
            );

            messagingFixture.thenErrorShouldBe("Message text must be less than 280 characters");
        }
    }

    @Nested
    @DisplayName("Rule: a message cannot be empty")
    class MessageNotEmpty {
        @Test
        void userCannotEditAMessageWithAnEmptyText() {
            messagingFixture.givenTheFollowingMessagesExists(
                    List.of(
                            new MessageBuilder()
                                    .withId(UUID.fromString("cc865b1a-529a-4973-9d0b-58ca894f98a2"))
                                    .withAuthor("Alice")
                                    .withText("Hello world!")
                                    .withPublishedDate(LocalDateTime.of(2020, 2, 14, 17, 46, 51))
                                    .build()
                    )
            );

            messagingFixture.whenUserEditHisMessage(
                    UUID.fromString("cc865b1a-529a-4973-9d0b-58ca894f98a2"),
                    ""
            );

            messagingFixture.thenErrorShouldBe("Message text must not be blank");
        }

        @Test
        void userCannotEditAMessageWithABlankText() {
            messagingFixture.givenTheFollowingMessagesExists(
                    List.of(
                            new MessageBuilder()
                                    .withId(UUID.fromString("cc865b1a-529a-4973-9d0b-58ca894f98a2"))
                                    .withAuthor("Alice")
                                    .withText("Hello world!")
                                    .withPublishedDate(LocalDateTime.of(2020, 2, 14, 17, 46, 51))
                                    .build()
                    )
            );

            messagingFixture.whenUserEditHisMessage(
                    UUID.fromString("cc865b1a-529a-4973-9d0b-58ca894f98a2"),
                    " "
            );

            messagingFixture.thenErrorShouldBe("Message text must not be blank");
        }
    }
}

