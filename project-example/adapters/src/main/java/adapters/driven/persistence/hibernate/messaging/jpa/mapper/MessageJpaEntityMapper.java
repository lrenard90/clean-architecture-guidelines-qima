package adapters.driven.persistence.hibernate.messaging.jpa.mapper;

import adapters.driven.persistence.hibernate.messaging.jpa.entity.MessageJpaEntity;
import com.example.cleanarchitectureapplication.messaging.domain.entity.MessageData;
import org.mapstruct.Mapper;
import org.springframework.stereotype.Component;

@Mapper(componentModel = "spring")
@Component
public interface MessageJpaEntityMapper {
    MessageJpaEntity toJpaEntity(MessageData messageData);
    MessageData toData(MessageJpaEntity messageJpaEntity);
}

