package com.example.cleanarchitectureapplication.unit.messaging;

import com.example.cleanarchitectureapplication.messaging.application.dto.TimelineMessageDTO;
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

@DisplayName("Feature: View messages timeline")
class ViewTimelineUseCaseHandlerTest {
    private MessagingFixture messagingFixture;

    @BeforeEach
    void setUp() {
        messagingFixture = new MessagingFixture();
    }

    @Nested
    @DisplayName("Scenario: User view his empty timeline")
    class NoMessage {
        @Test
        void userShouldGetEmptyTimeline() {
            messagingFixture.givenTheFollowingMessagesExists(Collections.emptyList());
            messagingFixture.whenUserSeesTimelineOf("Alice");
            messagingFixture.thenUserGetsAnEmptyTimeline();
        }
    }

    @Nested
    @DisplayName("Rule: Messages in the timeline are displayed in reverse chronological order")
    class MessagesTimelineReversed {
        @Test
        void userShouldGetHisTimeline() {
            messagingFixture.givenNowIs(LocalDateTime.of(2020, 2, 14, 17, 46, 51));
            messagingFixture.givenTheFollowingMessagesExists(
                    List.of(
                            new MessageBuilder()
                                    .withId(UUID.fromString("e1fd6ad4-83d5-4f8d-a788-0132c9b33319"))
                                    .withAuthor("Alice")
                                    .withText("Hello world!")
                                    .withPublishedDate(LocalDateTime.of(2020, 2, 14, 17, 44, 51))
                                    .build(),
                            new MessageBuilder()
                                    .withId(UUID.fromString("cc865b1a-529a-4973-9d0b-58ca894f98a2"))
                                    .withAuthor("Alice")
                                    .withText("Hello world!")
                                    .withPublishedDate(LocalDateTime.of(2020, 2, 14, 17, 45, 51))
                                    .build()
                    )
            );

            messagingFixture.whenUserSeesTimelineOf("Alice");

            messagingFixture.thenUserShouldSee(
                    List.of(
                            new TimelineMessageDTO(
                                    UUID.fromString("cc865b1a-529a-4973-9d0b-58ca894f98a2"),
                                    "Alice",
                                    "Hello world!",
                                    "1 minute ago"
                            ),
                            new TimelineMessageDTO(
                                    UUID.fromString("e1fd6ad4-83d5-4f8d-a788-0132c9b33319"),
                                    "Alice",
                                    "Hello world!",
                                    "2 minutes ago"
                            )
                    )
            );
        }
    }

    @Nested
    @DisplayName("Rule: Timeline displays only messages of specified user")
    class FilterAuthorTimeline {
        @Test
        void userShouldGetUserOnlyTimeline() {
            messagingFixture.givenNowIs(LocalDateTime.of(2020, 2, 14, 17, 46, 51));
            messagingFixture.givenTheFollowingMessagesExists(
                    List.of(
                            new MessageBuilder()
                                    .withId(UUID.fromString("cc865b1a-529a-4973-9d0b-58ca894f98a2"))
                                    .withAuthor("Alice")
                                    .withText("Hello world!")
                                    .withPublishedDate(LocalDateTime.of(2020, 2, 14, 17, 45, 51))
                                    .build(),
                            new MessageBuilder()
                                    .withAuthor("Bob")
                                    .withText("Hello world!")
                                    .build()
                    )
            );

            messagingFixture.whenUserSeesTimelineOf("Alice");

            messagingFixture.thenUserShouldSee(
                    List.of(
                            new TimelineMessageDTO(
                                    UUID.fromString("cc865b1a-529a-4973-9d0b-58ca894f98a2"),
                                    "Alice",
                                    "Hello world!",
                                    "1 minute ago"
                            )
                    )
            );
        }
    }
}

