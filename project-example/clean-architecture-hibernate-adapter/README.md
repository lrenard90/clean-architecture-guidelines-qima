# clean-architecture-hibernate-adapter Module (Java)

Persistence adapter implementing repository interfaces using JPA/Hibernate.

## Directory Structure

### `src/main/java/{package}/{feature}/jpa/`
**JPA Implementation** - Hibernate/JPA specific code.

- **`entity/`**: JPA entities (e.g., `MessageJpaEntity`)
  - Separate from domain entities
  - Annotated with `@Entity`
  - Represents database schema
  - Use Lombok `@Getter`, `@Setter` if needed
- **`mapper/`**: Mappers for converting between domain and JPA entities
  - Use MapStruct or manual mappers
  - Convert: Domain Entity (via `data()`) ⇄ JPA Entity
  - MapStruct example: `@Mapper(componentModel = "spring")`
- **`repository/`**: Repository implementations
  - `{Entity}JpaEntityHibernateRepository`: Spring Data JPA repository interface
  - `{Entity}HibernateRepository`: Implements port interface from core
  - Injects JPA repository and mapper via constructor

### `src/test/java/{package}/integration/`
**Integration Tests** - Test database interactions.

- Use `@SpringBootTest`
- Use TestContainers for PostgreSQL
- Validate SQL queries and entity mappings
- Use `@Transactional` for test cleanup

## Java-Specific Rules

- ✅ Depends on `clean-architecture-application`
- ✅ Implements ports defined in the core
- ✅ Uses MapStruct or manual mapping
- ✅ JPA entities are separate from domain entities
- ✅ Never expose JPA entities outside this module
- ✅ Use `@Repository` annotation for Spring Data repositories
- ✅ Use constructor injection for dependencies

## Java Code Examples

### JPA Entity
```java
@Entity
@Table(name = "messages")
public class MessageJpaEntity {
    @Id
    private UUID id;
    
    @Column(nullable = false)
    private String text;
    
    // Getters and setters
}
```

### MapStruct Mapper
```java
@Mapper(componentModel = "spring")
public interface MessageJpaEntityMapper {
    MessageJpaEntity toJpaEntity(MessageData messageData);
    MessageData toDomainData(MessageJpaEntity jpaEntity);
}
```

### Repository Implementation
```java
@Repository
public class MessageHibernateRepository implements MessageRepository {
    private final MessageJpaEntityHibernateRepository jpaRepository;
    private final MessageJpaEntityMapper mapper;
    
    public MessageHibernateRepository(
            MessageJpaEntityHibernateRepository jpaRepository,
            MessageJpaEntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }
    
    @Override
    public void save(Message message) {
        MessageJpaEntity jpaEntity = mapper.toJpaEntity(message.data());
        jpaRepository.save(jpaEntity);
    }
}
```
