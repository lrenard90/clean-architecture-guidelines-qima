package com.example.cleanarchitectureapplication.messaging.domain.entity;

import com.example.cleanarchitectureapplication.messaging.domain.valueobject.MessageText;
import com.example.cleanarchitectureapplication.socle.data.Snapshotable;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class Message implements Snapshotable<MessageData> {
    private final UUID id;
    private final String author;
    private MessageText text;
    private final LocalDateTime publishedDate;

    public Message(UUID id, String author, String text, LocalDateTime publishedDate) {
        this.id = id;
        this.author = author;
        this.publishedDate = publishedDate;
        this.editText(text);
    }

    public void editText(String text) {
        this.text = new MessageText(text);
    }

    @Override
    public MessageData data() {
        return new MessageData(id, author, text.value(), publishedDate);
    }

    public static Message fromData(MessageData state) {
        return new Message(state.id(), state.author(), state.text(), state.publishedDate());
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        Message message = (Message) other;
        return Objects.equals(id, message.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Message(id=" + id + ", author='" + author + "', text='" + text + "', publishedDate=" + publishedDate + ")";
    }
}

