package adapters.driven.persistence.hibernate.messaging.jpa.repository;

import adapters.driven.persistence.hibernate.messaging.jpa.entity.MessageJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface MessageJpaEntityHibernateRepository extends JpaRepository<MessageJpaEntity, UUID> {
    List<MessageJpaEntity> findAllByAuthor(String author);
}

