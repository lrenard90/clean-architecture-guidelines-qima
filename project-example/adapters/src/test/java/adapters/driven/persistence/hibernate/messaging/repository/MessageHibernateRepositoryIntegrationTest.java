package adapters.driven.persistence.hibernate.messaging.repository;

import adapters.driven.persistence.hibernate.configuration.IntegrationTest;
import adapters.driven.persistence.hibernate.messaging.jpa.entity.MessageJpaEntity;
import adapters.driven.persistence.hibernate.messaging.jpa.repository.MessageHibernateRepository;
import adapters.driven.persistence.hibernate.messaging.jpa.repository.MessageJpaEntityHibernateRepository;
import com.example.cleanarchitectureapplication.messaging.builders.MessageBuilder;
import com.example.cleanarchitectureapplication.messaging.domain.entity.Message;
import com.example.cleanarchitectureapplication.messaging.domain.entity.MessageData;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

class MessageHibernateRepositoryIntegrationTest extends IntegrationTest {
    @Autowired
    private MessageHibernateRepository messageHibernateRepository;

    @Autowired
    private MessageJpaEntityHibernateRepository messageJpaEntityHibernateRepository;

    @Nested
    class SaveMessage {
        @Test
        void createAMessage() {
            Message messageToSave = new MessageBuilder()
                    .withId(UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"))
                    .withAuthor("Alice")
                    .withText("Hello world!")
                    .withPublishedDate(LocalDateTime.of(2020, 1, 1, 0, 0, 0))
                    .build();

            messageHibernateRepository.save(messageToSave);

            MessageJpaEntity messageJpaEntity = messageJpaEntityHibernateRepository.getReferenceById(messageToSave.data().id());
            MessageData messageData = messageToSave.data();
            Assertions.assertThat(messageJpaEntity.getId()).isEqualTo(messageData.id());
            Assertions.assertThat(messageJpaEntity.getAuthor()).isEqualTo(messageData.author());
            Assertions.assertThat(messageJpaEntity.getText()).isEqualTo(messageData.text());
            Assertions.assertThat(messageJpaEntity.getPublishedDate()).isEqualTo(messageData.publishedDate());
        }

        @Test
        void updateAMessageText() {
            messageJpaEntityHibernateRepository.saveAll(
                    List.of(
                            new MessageJpaEntity(
                                    UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"),
                                    "Alice",
                                    "Message A",
                                    LocalDateTime.of(2020, 1, 1, 0, 0, 0)
                            ),
                            new MessageJpaEntity(
                                    UUID.fromString("b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12"),
                                    "Alice",
                                    "Message B",
                                    LocalDateTime.of(2021, 2, 1, 0, 0, 0)
                            )
                    )
            );

            Message messageToSave = new MessageBuilder()
                    .withId(UUID.fromString("b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12"))
                    .withAuthor("Alice")
                    .withText("Hello world! updated")
                    .withPublishedDate(LocalDateTime.of(2020, 1, 1, 0, 0, 0))
                    .build();

            messageHibernateRepository.save(messageToSave);

            MessageJpaEntity messageJpaEntity = messageJpaEntityHibernateRepository.getReferenceById(messageToSave.data().id());
            MessageData messageData = messageToSave.data();
            Assertions.assertThat(messageJpaEntity.getId()).isEqualTo(messageData.id());
            Assertions.assertThat(messageJpaEntity.getAuthor()).isEqualTo(messageData.author());
            Assertions.assertThat(messageJpaEntity.getText()).isEqualTo(messageData.text());
            Assertions.assertThat(messageJpaEntity.getPublishedDate()).isEqualTo(messageData.publishedDate());
        }
    }

    @Test
    void findAllByAuthor() {
        messageJpaEntityHibernateRepository.saveAll(
                List.of(
                        new MessageJpaEntity(
                                UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"),
                                "Alice",
                                "Message A",
                                LocalDateTime.of(2020, 1, 1, 0, 0, 0)
                        ),
                        new MessageJpaEntity(
                                UUID.fromString("b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12"),
                                "Alice",
                                "Message B",
                                LocalDateTime.of(2021, 2, 1, 0, 0, 0)
                        ),
                        new MessageJpaEntity(
                                UUID.fromString("c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13"),
                                "Bob",
                                "Message C",
                                LocalDateTime.of(2022, 3, 1, 0, 0, 0)
                        )
                )
        );

        List<Message> messages = messageHibernateRepository.findAllByAuthor("Alice");

        Assertions.assertThat(messages.stream().map(Message::data).toList()).containsExactly(
                new MessageData(
                        UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"),
                        "Alice",
                        "Message A",
                        LocalDateTime.of(2020, 1, 1, 0, 0, 0)
                ),
                new MessageData(
                        UUID.fromString("b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12"),
                        "Alice",
                        "Message B",
                        LocalDateTime.of(2021, 2, 1, 0, 0, 0)
                )
        );
    }

    @Nested
    class ExistsById {
        @Test
        void testExistsById_Exists() {
            messageJpaEntityHibernateRepository.saveAll(
                    List.of(
                            new MessageJpaEntity(
                                    UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"),
                                    "Alice",
                                    "Message A",
                                    LocalDateTime.of(2020, 1, 1, 0, 0, 0)
                            )
                    )
            );

            boolean existsById = messageHibernateRepository.existsById(UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"));

            Assertions.assertThat(existsById).isTrue();
        }

        @Test
        void testExistsById_notExists() {
            messageJpaEntityHibernateRepository.saveAll(
                    List.of(
                            new MessageJpaEntity(
                                    UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"),
                                    "Alice",
                                    "Message A",
                                    LocalDateTime.of(2020, 1, 1, 0, 0, 0)
                            )
                    )
            );

            boolean existsById = messageHibernateRepository.existsById(UUID.fromString("b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"));

            Assertions.assertThat(existsById).isFalse();
        }
    }

    @Nested
    class FindById {
        @Test
        void testFindById_Exists() {
            messageJpaEntityHibernateRepository.saveAll(
                    List.of(
                            new MessageJpaEntity(
                                    UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"),
                                    "Alice",
                                    "Message A",
                                    LocalDateTime.of(2020, 1, 1, 0, 0, 0)
                            )
                    )
            );

            var message = messageHibernateRepository.findById(UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"));

            Assertions.assertThat(message.get().data()).isEqualTo(
                    new MessageData(
                            UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"),
                            "Alice",
                            "Message A",
                            LocalDateTime.of(2020, 1, 1, 0, 0, 0)
                    )
            );
        }

        @Test
        void testFindById_notExists() {
            messageJpaEntityHibernateRepository.saveAll(
                    List.of(
                            new MessageJpaEntity(
                                    UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"),
                                    "Alice",
                                    "Message A",
                                    LocalDateTime.of(2020, 1, 1, 0, 0, 0)
                            )
                    )
            );

            var message = messageHibernateRepository.findById(UUID.fromString("b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"));

            Assertions.assertThat(message).isEmpty();
        }
    }
}

