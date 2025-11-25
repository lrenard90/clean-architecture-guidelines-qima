package com.example.cleanarchitectureapplication.messaging.builders;

import com.example.cleanarchitectureapplication.messaging.domain.entity.Message;
import java.time.LocalDateTime;
import java.util.UUID;

public class MessageBuilder {
    private UUID id = UUID.fromString("de048ea1-0149-4a9f-bcdd-af8d402d33c7");
    private String author = "Alice";
    private String text = "Hello world!";
    private LocalDateTime publishedDate = LocalDateTime.of(2020, 2, 14, 17, 46, 51);

    public static MessageBuilder aliceMessageBuilder() {
        return new MessageBuilder()
                .withId(UUID.fromString("de048ea1-0149-4a9f-bcdd-af8d402d33c7"))
                .withAuthor("Alice")
                .withText("Hello world!")
                .withPublishedDate(LocalDateTime.of(2020, 2, 14, 17, 46, 51));
    }

    public MessageBuilder withId(UUID id) {
        this.id = id;
        return this;
    }

    public MessageBuilder withAuthor(String author) {
        this.author = author;
        return this;
    }

    public MessageBuilder withText(String text) {
        this.text = text;
        return this;
    }

    public MessageBuilder withPublishedDate(LocalDateTime publishedDate) {
        this.publishedDate = publishedDate;
        return this;
    }

    public Message build() {
        return new Message(id, author, text, publishedDate);
    }
}

