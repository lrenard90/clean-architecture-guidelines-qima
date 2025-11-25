# clean-architecture-application Module (Java)

The core module containing all business logic and use cases.

## Directory Structure

### `src/main/java/{package}/socle/`
**Shared Kernel** - Technical utilities and abstractions used across the application.

- **`data/`**: Contains the `Snapshotable<T>` interface for entities
- **`dependencyinjection/annotation/`**: Custom annotations like `@UseCase` and `@Mapper`
- **`time/`**: Time abstractions like `DateProvider` (never use `LocalDateTime.now()` directly)

### `src/main/java/{package}/messaging/domain/`
**Domain Layer** - Business entities and value objects for messaging feature.

- **`entity/`**: Domain entities (e.g., `Message`, `User`)
  - Must implement `Snapshotable<DataType>`
  - Must have business methods, not setters
  - Must have static factory method `fromData()`
- **`valueobject/`**: Value objects for validated attributes (e.g., `MessageText`)
  - Validation in constructor or builder
  - Immutable classes (final fields, no setters)

### `src/main/java/{package}/messaging/application/`
**Application Layer** - Use cases and ports.

- **`ports/`**: Interfaces for repositories and gateways
  - `{Entity}Repository` interfaces
  - `{Service}Gateway` interfaces
  - `Provider` abstract classes or interfaces
- **`usecases/`**: Use case handlers
  - Naming: `{Action}{Entity}UseCaseHandler`
  - Must be annotated with `@UseCase`
  - Inject ports (repositories, gateways) via constructor
- **`dto/`**: Data transfer objects
  - Request DTOs: `{Action}{Entity}RequestDTO`
  - Response DTOs: `{Entity}DTO`
  - Use records (Java 14+) or immutable classes

### `src/test/java/{package}/unit/messaging/`
**Unit Tests** - Test messaging use cases without Spring context.

- **`testdoubles/repository/`**: In-memory repository implementations
  - Prefer in-memory over mocks when possible
  - Example: `InMemoryMessageRepository`
- **`fixtures/`**: Test fixtures for BDD-style tests
  - Example: `MessagingFixture`

### `src/test/java/{package}/shared/`
**Shared Test Utilities** - Common test helpers.

- Example: `FakeDateProvider` for time-dependent tests

### `src/testFixtures/java/{package}/`
**Test Fixtures** - Shared test fixtures across modules.

## Java-Specific Rules

- ❌ **Never** import `org.springframework.*` or `jakarta.persistence.*`
- ❌ **Never** call `LocalDateTime.now()` directly - use `DateProvider.now()`
- ✅ Use `final` fields and immutable classes when possible
- ✅ Use records (Java 14+) for simple DTOs
- ✅ Entities must be rich objects with business behavior
- ✅ Use Value Objects for validated attributes
- ✅ Prefer constructor injection over field injection
- ✅ Use `@UseCase` annotation (custom, not Spring's `@Service`)

## Java Code Examples

### Domain Entity
```java
public class Message implements Snapshotable<MessageData> {
    private final UUID id;
    private MessageText text;
    
    public Message(UUID id, String author, String text, LocalDateTime publishedDate) {
        this.id = id;
        this.text = new MessageText(text);
        // ...
    }
    
    public void editText(String text) {
        this.text = new MessageText(text);
    }
    
    @Override
    public MessageData data() {
        return new MessageData(id, text.value(), publishedDate);
    }
    
    public static Message fromData(MessageData data) {
        return new Message(data.id(), data.author(), data.text(), data.publishedDate());
    }
}
```

### Use Case Handler
```java
@UseCase
public class PostMessageUseCaseHandler {
    private final MessageRepository messageRepository;
    private final DateProvider dateProvider;
    
    public PostMessageUseCaseHandler(MessageRepository messageRepository, DateProvider dateProvider) {
        this.messageRepository = messageRepository;
        this.dateProvider = dateProvider;
    }
    
    public void handle(PostMessageRequestDTO request) {
        Message message = new Message(request.id(), request.author(), request.text(), dateProvider.now());
        messageRepository.save(message);
    }
}
```
