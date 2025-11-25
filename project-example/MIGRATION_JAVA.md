# Migration from Kotlin to Java

This document summarizes the key differences when migrating the Clean Architecture example from Kotlin to Java.

## Directory Structure Changes

### Source Directories
- `src/main/kotlin/` → `src/main/java/`
- `src/test/kotlin/` → `src/test/java/`
- `src/testFixtures/kotlin/` → `src/testFixtures/java/`

### Package Naming Conventions

**Kotlin (snake_case):**
- `dependency_injection` → **Java:** `dependencyinjection`
- `value_object` → **Java:** `valueobject`
- `test_doubles` → **Java:** `testdoubles`

**Java Package Rules:**
- All lowercase
- No underscores or hyphens
- Follow Java naming conventions

## Code Differences

### Annotations
- Kotlin: Custom `@UseCase` annotation
- Java: Same custom `@UseCase` annotation (not Spring's `@Service`)

### Classes and Interfaces
- Kotlin: `class Message : Snapshotable<MessageData>`
- Java: `class Message implements Snapshotable<MessageData>`

### Data Classes
- Kotlin: `data class MessageData(...)`
- Java: Use records (Java 14+): `record MessageData(...)` or immutable classes

### Immutability
- Kotlin: `val` vs `var`
- Java: `final` fields, immutable classes

### Constructors
- Kotlin: Primary constructor in class header
- Java: Constructor with parameter validation

### Factory Methods
- Kotlin: `companion object { fun fromData(...) }`
- Java: `public static Message fromData(...)`

### Dependency Injection
- Kotlin: Constructor injection (same)
- Java: Constructor injection (preferred over field injection)

## Testing

### Test Structure
- Same structure: unit tests in core, integration tests in adapters
- Java: Use JUnit 5, AssertJ, Mockito
- Java: Use `@ExtendWith(MockitoExtension.class)` for Mockito

### Test Doubles
- Kotlin: In-memory repositories with `HashMap`
- Java: Same approach, use `Map<UUID, Entity>` or similar

## Build System

### Kotlin
- Gradle with Kotlin DSL (`build.gradle.kts`)
- Kotlin compiler

### Java
- Maven (`pom.xml`) or Gradle (`build.gradle`)
- Java compiler (Java 17+ recommended)

## Key Principles (Unchanged)

1. ✅ **Dependency Direction:** Adapters depend on core, never reverse
2. ✅ **Domain Entities:** Rich objects with business behavior
3. ✅ **Snapshotable Pattern:** Entities expose immutable data via `data()` method
4. ✅ **Value Objects:** Use for validated attributes
5. ✅ **Ports and Adapters:** Interfaces in core, implementations in infrastructure
6. ✅ **No Framework in Core:** Never import Spring or JPA in core module

## Example Comparison

### Domain Entity

**Kotlin:**
```kotlin
class Message(id: UUID, author: String, text: String) : Snapshotable<MessageData> {
    val id: UUID = id
    lateinit var text: MessageText
    
    fun editText(text: String) {
        this.text = MessageText(text)
    }
    
    override fun data(): MessageData {
        return MessageData(id, text.value, publishedDate)
    }
    
    companion object {
        fun fromData(data: MessageData) = Message(data.id, data.author, data.text)
    }
}
```

**Java:**
```java
public class Message implements Snapshotable<MessageData> {
    private final UUID id;
    private MessageText text;
    
    public Message(UUID id, String author, String text, LocalDateTime publishedDate) {
        this.id = id;
        this.text = new MessageText(text);
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

**Kotlin:**
```kotlin
@UseCase
class PostMessageUseCaseHandler(
    private val messageRepository: MessageRepository,
    private val dateProvider: DateProvider
) {
    fun handle(request: PostMessageRequestDTO) {
        val message = Message(request.id, request.author, request.text, dateProvider.now())
        messageRepository.save(message)
    }
}
```

**Java:**
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

## Migration Checklist

- [x] Replace `kotlin/` directories with `java/`
- [x] Update package names (remove underscores)
- [x] Convert Kotlin classes to Java classes
- [x] Convert data classes to records or immutable classes
- [x] Update constructors to Java style
- [x] Convert companion objects to static methods
- [x] Update README files with Java examples
- [x] Ensure all dependencies are Java-compatible

