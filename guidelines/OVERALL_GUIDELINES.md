# Clean Architecture Development Guide (Kotlin/Hexagonal Style)

This document defines coding rules, module structure, and patterns to follow for developing and migrating code toward a Clean Architecture, based on the analysis of the reference project.

---

## Why Clean Architecture?

While Clean Architecture is often presented as a way to easily swap technologies (e.g., changing databases or frameworks), **the real and most valuable benefit is having a loosely coupled core that makes business logic easy to locate, understand, and test.**

### Key Benefits

1. **Business Logic Isolation:** All business rules live in the `core` module, completely independent of frameworks, databases, or delivery mechanisms. You know exactly where to find and modify business behavior.

2. **Testability:** Business logic can be tested in isolation with simple unit tests, without requiring Spring context, database containers, or HTTP servers. Tests run fast and focus purely on business behavior.

3. **Reduced Coupling:** The core depends on abstractions (ports/interfaces), not concrete implementations. This makes the codebase more maintainable and easier to reason about.

4. **Technology Changes as a Side Effect:** While not the primary goal, the loosely coupled architecture does make it easier to change infrastructure technologies when needed, because business logic remains untouched.

**Focus:** The architecture's value comes from organizing code around business capabilities and making that code easy to test and maintain, not from theoretical technology swapping scenarios.

---

## 1. Project Structure and Modules

The application uses a simplified structure with **2 Gradle modules** and **package-based organization** to enforce separation of responsibilities and dependency direction.

### core (The Core Module)

**Content:** Contains all business logic organized in packages:
- **`{package}/socle/`**: Shared kernel (annotations, date utilities, abstractions)
- **`{package}/{feature}/domain/`**: Domain entities and value objects
- **`{package}/{feature}/application/`**: Use cases, ports (interfaces), and DTOs

**Dependencies:** No dependencies on external frameworks (Spring, Hibernate, etc.). Only standard Java/Kotlin and test libraries (JUnit, AssertJ).
**Golden Rule:** This module must never depend on infrastructure modules.

---

### adapters (The Adapters Module)

**Content:** Contains all infrastructure adapters organized in packages:
- **`adapters/driven/persistence/hibernate/`**: Persistence implementation (JPA entities, repositories, mappers)
- **`adapters/driving/web/spring/`**: REST controllers, Spring Boot configuration, dependency injection

**Dependencies:** Depends on `core` module, Spring Boot, Spring Data JPA, and other infrastructure frameworks.

---

## 2. Domain Layer

Located in the `core` module.

### Entities (`src/main/java/{package}/{feature}/domain/entity`)

Entities are rich objects containing both data and business behavior.

#### Snapshotable Pattern

**Purpose:** Enable mapping to and from persistence adapters while maintaining encapsulation and ensuring robust, consistent domain objects during business logic execution.

Entities must not directly expose their mutable fields for persistence.
They must implement the `Snapshotable<DataType>` interface to provide an immutable view (Data Class) of their internal state.

**Key Benefits:**
- **Encapsulation:** Domain entities expose no setters; state changes occur only through business methods
- **Persistence Mapping:** Adapters map between the immutable snapshot (e.g., `MessageData`) and JPA entities, never accessing domain entity internals directly
- **Consistency:** Domain objects remain in a valid state throughout their lifecycle, as all mutations go through validated business methods
- **Separation:** The snapshot acts as a boundary between the domain layer and infrastructure concerns

#### Encapsulation

State modifications must go through business methods (e.g., `editText()`, `follows()`) and never through direct setters.

#### Value Objects

Use Value Objects for attributes requiring validation (e.g., `MessageText`, `UserEmail`).
Validation occurs in the `init` block.

---

### Example Entity Structure

```kotlin
class Message(id: UUID, author: String, text: String, publishedDate: LocalDateTime) :
    Snapshotable<MessageData> {

    // Properties
    val id: UUID = id
    lateinit var text: MessageText // Value Object

    // Business behavior
    fun editText(text: String) {
        this.text = MessageText(text)
    }

    // Snapshot pattern for exporting data
    override fun data(): MessageData {
        return MessageData(id, author, text.value, publishedDate)
    }

    // Static factory method for reconstruction
    companion object {
        fun fromData(data: MessageData) = Message(data.id, ...)
    }
}
```

---

## 3. Application Layer

Located in the `core` module under `{package}/{feature}/application/`.

### Ports (Interfaces)

Define the contracts for interacting with the outside world.

- **Repositories:** `MessageRepository`, `UserRepository`
- **Gateways:** `AuthenticationGateway`
- **Providers:** `DateProvider` (abstract class)

Repositories manipulate **Domain Entities**, not JPA entities.

---

### Use Cases

**Naming:** Suffix with `UseCaseHandler` (e.g., `PostMessageUseCaseHandler`).
**Annotation:** Must be annotated with `@UseCase` (custom annotation from the shared kernel, not Spring’s `@Service`).
**DTOs:** Simple data classes suffixed with `DTO` (e.g., `PostMessageRequestDTO`).
**Responsibility:** Orchestrate repository calls and execute business logic via domain entities.

---

### Example Handler

```kotlin
@UseCase
class PostMessageUseCaseHandler(
    private val messageRepository: MessageRepository,
    private val dateProvider: DateProvider
) {
    fun handle(request: PostMessageRequestDTO) {
        // Orchestration logic
        val message = Message(request.id, request.author, request.text, dateProvider.now())
        messageRepository.save(message)
    }
}
```

---

## 4. Infrastructure Layer (Adapters)

Located in the `adapters` module, organized by adapter type.

### Persistence Adapter (Driven/Hibernate)

Located in `adapters/driven/persistence/hibernate/{feature}/`.

- **JPA Entities:** Separate classes annotated with `@Entity` (e.g., `MessageJpaEntity`). Distinct from Domain Entities.
- **Mappers:** Use MapStruct or manual mappers to convert  
  **Domain Entity (via `data()`) ⇄ JPA Entity**
- **Repository Implementation:** Implements the port interface (e.g., `MessageRepository`).  
  Injects the JPA repository (`MessageJpaEntityHibernateRepository`) and the Mapper.

---

### Web Adapter (Driving/Spring Boot)

Located in `adapters/driving/web/spring/`.

- **Controllers:** Annotated with `@RestController`. Inject UseCaseHandlers.
- **Configuration:** Use a custom configuration class (`CustomAnnotationScanConfiguration`) to scan core module packages and detect `@UseCase` and `@Mapper` annotations.

---

## 5. Testing Strategy

### Unit Tests (Application Layer)

**Location:** `core/src/test`
**Technology:** JUnit 5, AssertJ.
**No Spring context.**

**Test Doubles:**  
Prefer in-memory repositories (e.g., `InMemoryMessageRepository` using a `HashMap`) over heavy mocks (Mockito) when possible.

**Defensive Copying in In-Memory Repositories:**  
When saving entities in in-memory repositories, always create a copy of the object before storing it. This prevents false positives in tests where the original reference might be modified in the code after saving, which wouldn't happen with a real database. Use the Snapshotable pattern (`data()` and `fromData()`) to create defensive copies that maintain proper encapsulation and validation logic.

**Example:**
```java
public class InMemoryMessageRepository implements MessageRepository {
    private Map<UUID, Message> messagesById = new HashMap<>();

    @Override
    public Message save(Message message) {
        Message copied = copy(message);
        messagesById.put(copied.data().id(), copied);
        return copied;
    }

    private Message copy(Message message) {
        // Use data() and fromData() to create a defensive copy
        // This ensures validation logic is preserved
        MessageData data = message.data();
        return Message.fromData(new MessageData(
            data.id(), 
            data.author(), 
            data.text(), 
            data.publishedDate()
        ));
    }
}
```

**Why In-Memory Test Doubles Over Heavy Mocking:**  
In-memory test doubles provide far more reliable and robust tests compared to heavy mocking frameworks (like Mockito) for several key reasons:

1. **Simple Contract Implementation:** You simply implement the interface contract in memory (e.g., using a `HashMap`). No need to mock specific method calls or configure complex mock behaviors.

2. **Focus on Business Logic:** Tests focus on verifying business behavior and outcomes rather than brittle implementation details like "was method X called with parameter Y?"

3. **Refactoring Resilience:** The most important advantage is that you can change the implementation without breaking tests, as long as the expected behavior is preserved. Tests verify what the system does, not how it does it.

4. **Clean Architecture Benefits:** Thanks to dependency inversion (ports/interfaces), you can easily inject test doubles without any mocking framework complexity.

**Example - Refactoring Resilience:**

Consider a use case that needs to check if a message exists before editing it:

```java
// Initial implementation
@UseCase
public class EditMessageUseCaseHandler {
    private final MessageRepository messageRepository;
    
    public void handle(EditMessageRequestDTO request) {
        Message message = messageRepository.findById(request.messageId())
            .orElseThrow(() -> new IllegalArgumentException("Message not found"));
        message.editText(request.newText());
        messageRepository.save(message);
    }
}
```

**Test with In-Memory Repository:**
```java
@Test
void should_edit_message_text() {
    // Given
    Message existingMessage = new Message(messageId, "author", "old text", now);
    inMemoryRepository.save(existingMessage);
    
    // When
    handler.handle(new EditMessageRequestDTO(messageId, "new text"));
    
    // Then
    Message edited = inMemoryRepository.get(messageId);
    assertThat(edited.data().text()).isEqualTo("new text");
}
```

**Now refactor the implementation** to use `existsById()` instead of `findById()`:

```java
// Refactored implementation - different approach
@UseCase
public class EditMessageUseCaseHandler {
    private final MessageRepository messageRepository;
    
    public void handle(EditMessageRequestDTO request) {
        if (!messageRepository.existsById(request.messageId())) {
            throw new IllegalArgumentException("Message not found");
        }
        Message message = messageRepository.findById(request.messageId()).get();
        message.editText(request.newText());
        messageRepository.save(message);
    }
}
```

**Result:** The test still passes! The in-memory repository implements both methods correctly, and the test verifies the behavior (message text is edited), not the implementation details (which repository methods are called).

**With Mockito, this refactoring would break the test** because you would have mocked specific method calls:
```java
// Brittle mock-based test (would break after refactoring)
when(mockRepository.findById(messageId)).thenReturn(Optional.of(message));
verify(mockRepository).save(any()); // Fragile verification
```

After refactoring to use `existsById()`, you'd need to update all mock configurations, even though the behavior didn't change.

**Note:** This is a simple example demonstrating refactoring capabilities with in-memory test doubles. The advantages become even more significant in complex use cases where you would require multiple mocks (e.g., coordinating behavior across multiple repositories, gateways, and providers). With in-memory test doubles, you simply implement each interface once and reuse them across all tests, while with mocks you'd need to configure and maintain numerous mock setups for each test scenario.

**Fixtures:**
Use fixture classes (e.g., `MessagingFixture`) to standardize Given–When–Then and improve readability (BDD).

---

### Integration / E2E Tests

**Location:** `adapters/src/test`
**Technology:** `@SpringBootTest`, TestContainers (PostgreSQL), RestAssured (API).
**Goal:** Validate Spring configuration and SQL/HTTP flows.

---

## 6. Specific Coding Rules

- **No Framework in the Core:** Never import `org.springframework.*` or `jakarta.persistence.*` in the `core` module.
- **Time Management:** Never call `LocalDateTime.now()` directly in business code; always use `DateProvider.now()`.
- **Immutability:** Prefer `val` over `var` (Kotlin) or `final` (Java) unless strictly necessary.
- **Exceptions:** Use standard or domain-specific exceptions (e.g., `IllegalArgumentException` for validation).
