package adapters.driven.persistence.hibernate.messaging.jpa.repository;

import adapters.driven.persistence.hibernate.messaging.jpa.entity.MessageJpaEntity;
import adapters.driven.persistence.hibernate.messaging.jpa.mapper.MessageJpaEntityMapper;
import com.example.cleanarchitectureapplication.messaging.application.ports.MessageRepository;
import com.example.cleanarchitectureapplication.messaging.domain.entity.Message;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class MessageHibernateRepository implements MessageRepository {
    private final MessageJpaEntityHibernateRepository messageJpaEntityHibernateRepository;
    private final MessageJpaEntityMapper messageJpaEntityMapper;

    public MessageHibernateRepository(
            MessageJpaEntityHibernateRepository messageJpaEntityHibernateRepository,
            MessageJpaEntityMapper messageJpaEntityMapper) {
        this.messageJpaEntityHibernateRepository = messageJpaEntityHibernateRepository;
        this.messageJpaEntityMapper = messageJpaEntityMapper;
    }

    @Override
    public Message save(Message message) {
        MessageJpaEntity messageJpaEntity = messageJpaEntityMapper.toJpaEntity(message.data());
        MessageJpaEntity savedJpaEntity = messageJpaEntityHibernateRepository.save(messageJpaEntity);
        return Message.fromData(messageJpaEntityMapper.toData(savedJpaEntity));
    }

    @Override
    public List<Message> findAllByAuthor(String author) {
        List<MessageJpaEntity> messageJpaEntities = messageJpaEntityHibernateRepository.findAllByAuthor(author);
        return messageJpaEntities.stream()
                .map(messageJpaEntity -> Message.fromData(messageJpaEntityMapper.toData(messageJpaEntity)))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Message> findById(UUID messageId) {
        return messageJpaEntityHibernateRepository.findById(messageId)
                .map(messageJpaEntity -> Message.fromData(messageJpaEntityMapper.toData(messageJpaEntity)));
    }

    @Override
    public boolean existsById(UUID id) {
        return messageJpaEntityHibernateRepository.existsById(id);
    }
}

