# Clean Architecture Development Guide (Hexagonal Style)

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

**IDs Without Validation:**  
Do not wrap IDs in Value Objects if there is no validation logic. Use primitive types (`Long`, `UUID`, `String`) directly instead. For example, use `Long` for `brandId` rather than creating a `BrandId` value object that adds no value. Value Objects should only be introduced when they encapsulate meaningful validation or behavior.

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

**Important Principles:**

1. **Use Cases should NOT call another Use Case:**
   - Each Use Case represents a single business operation/user story
   - If you need to reuse logic across multiple Use Cases, extract it to the domain layer (domain entities or domain services)
   - Use Case composition creates tight coupling and makes the system harder to understand and maintain

2. **Business rules and logic should be located in the Domain:**
   - **Most logic in Domain Objects:** Prefer placing business rules directly in domain entities and value objects where they naturally belong
   - **Domain Services for orchestration:** When business logic requires coordination across multiple entities or needs to be reused across multiple Use Cases, create a Domain Service
   - Use Cases should remain thin orchestrators that coordinate domain objects and infrastructure (repositories, gateways)

---

### Examples

#### Example 1: Business Logic in Domain Entity

**Good:** Business logic lives in the domain entity

```java
// Domain Entity with business logic
public class Message {
    private UUID id;
    private String author;
    private MessageText text;
    private MessageStatus status; // DRAFT, PUBLISHED, ARCHIVED
    private LocalDateTime publishedDate;
    
    // Business logic in domain entity
    public void publish(LocalDateTime publishDate) {
        if (this.status != MessageStatus.DRAFT) {
            throw new IllegalStateException("Only draft messages can be published");
        }
        if (this.text.isEmpty()) {
            throw new IllegalStateException("Cannot publish an empty message");
        }
        this.status = MessageStatus.PUBLISHED;
        this.publishedDate = publishDate;
    }
}

// Use Case - thin orchestrator
@UseCase
public class PublishMessageUseCaseHandler {
    private final MessageRepository messageRepository;
    private final DateProvider dateProvider;
    
    public void handle(PublishMessageRequestDTO request) {
        Message message = messageRepository.findById(request.messageId())
            .orElseThrow(() -> new IllegalArgumentException("Message not found"));
        
        // Business logic delegated to domain entity
        message.publish(dateProvider.now());
        
        messageRepository.save(message);
    }
}
```

**Bad:** Business logic in Use Case (avoid this)

```java
// Use Case with business logic (WRONG)
@UseCase
public class PublishMessageUseCaseHandler {
    private final MessageRepository messageRepository;
    private final DateProvider dateProvider;
    
    public void handle(PublishMessageRequestDTO request) {
        Message message = messageRepository.findById(request.messageId())
            .orElseThrow(() -> new IllegalArgumentException("Message not found"));
        
        // Business logic in Use Case (WRONG - should be in domain entity)
        if (message.getStatus() != MessageStatus.DRAFT) {
            throw new IllegalStateException("Only draft messages can be published");
        }
        if (message.getText().isEmpty()) {
            throw new IllegalStateException("Cannot publish an empty message");
        }
        message.setStatus(MessageStatus.PUBLISHED);
        message.setPublishedDate(dateProvider.now());
        
        messageRepository.save(message);
    }
}
```

---

#### Example 2: Domain Service for Cross-Entity Logic

When business logic requires coordination across multiple entities or needs to be reused across multiple Use Cases, create a Domain Service.

```java
// Domain Service (located in core/{feature}/domain/service/)
public class FollowService {
    
    // Business logic that coordinates multiple entities
    public void follow(User follower, User followee) {
        if (follower.equals(followee)) {
            throw new IllegalArgumentException("User cannot follow themselves");
        }
        
        if (follower.isFollowing(followee)) {
            throw new IllegalStateException("Already following this user");
        }
        
        follower.follow(followee);
        followee.addFollower(follower);
    }
    
    public void unfollow(User follower, User followee) {
        if (!follower.isFollowing(followee)) {
            throw new IllegalStateException("Not following this user");
        }
        
        follower.unfollow(followee);
        followee.removeFollower(follower);
    }
}

// Use Case 1 - uses Domain Service
@UseCase
public class FollowUserUseCaseHandler {
    private final UserRepository userRepository;
    private final FollowService followService; // Domain Service
    
    public void handle(FollowUserRequestDTO request) {
        User follower = userRepository.findById(request.followerId())
            .orElseThrow(() -> new IllegalArgumentException("Follower not found"));
        User followee = userRepository.findById(request.followeeId())
            .orElseThrow(() -> new IllegalArgumentException("Followee not found"));
        
        // Delegate business logic to Domain Service
        followService.follow(follower, followee);
        
        userRepository.save(follower);
        userRepository.save(followee);
    }
}

// Use Case 2 - reuses the same Domain Service
@UseCase
public class UnfollowUserUseCaseHandler {
    private final UserRepository userRepository;
    private final FollowService followService; // Same Domain Service reused
    
    public void handle(UnfollowUserRequestDTO request) {
        User follower = userRepository.findById(request.followerId())
            .orElseThrow(() -> new IllegalArgumentException("Follower not found"));
        User followee = userRepository.findById(request.followeeId())
            .orElseThrow(() -> new IllegalArgumentException("Followee not found"));
        
        // Reuse business logic from Domain Service
        followService.unfollow(follower, followee);
        
        userRepository.save(follower);
        userRepository.save(followee);
    }
}
```

**Key Points:**
- Domain Service contains reusable business logic that coordinates multiple entities
- Multiple Use Cases can reuse the same Domain Service
- Domain Services are part of the core module (no framework dependencies)
- Use Cases remain thin orchestrators focused on infrastructure coordination

---

#### Example 3: Why Use Cases Should NOT Call Other Use Cases

**Bad:** Use Case calling another Use Case (avoid this)

```java
// WRONG: Use Case calling another Use Case
@UseCase
public class PostMessageAndNotifyUseCaseHandler {
    private final PostMessageUseCaseHandler postMessageUseCaseHandler; // WRONG
    private final NotifyFollowersUseCaseHandler notifyFollowersUseCaseHandler; // WRONG
    
    public void handle(PostMessageAndNotifyRequestDTO request) {
        // Calling other Use Cases creates tight coupling (WRONG)
        postMessageUseCaseHandler.handle(new PostMessageRequestDTO(...));
        notifyFollowersUseCaseHandler.handle(new NotifyFollowersRequestDTO(...));
    }
}
```

**Good:** Extract shared logic to Domain Service

```java
// Solution: Extract to Domain Service if it's business logic
public class MessagePublicationService {
    public PublishedMessage publishMessage(User author, String text, LocalDateTime publishedDate) {
        // Business logic for publishing
        Message message = new Message(UUID.randomUUID(), author.getId(), text, publishedDate);
        return new PublishedMessage(message, author.getFollowers());
    }
}

@UseCase
public class PostMessageUseCaseHandler {
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final MessagePublicationService messagePublicationService;
    private final NotificationGateway notificationGateway;
    
    public void handle(PostMessageRequestDTO request) {
        User author = userRepository.findById(request.authorId())
            .orElseThrow(() -> new IllegalArgumentException("Author not found"));
        
        // Use Domain Service for business logic
        PublishedMessage published = messagePublicationService.publishMessage(
            author, request.text(), dateProvider.now()
        );
        
        messageRepository.save(published.getMessage());
        
        // Notify followers (infrastructure concern)
        notificationGateway.notifyFollowers(published.getFollowers(), published.getMessage());
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

### Example Persistence Adapter

```java
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
}
```

**Key Principles:**
- **Implements port interface:** The adapter implements the repository interface defined in the core module's application layer
- **Constructor injection:** Inject both the Spring Data JPA repository and the MapStruct mapper
- **Conversion pattern:** Always convert through the Snapshotable pattern:
  - **Save:** `Domain Entity` → `message.data()` → `Mapper.toJpaEntity()` → `JPA Entity` → save → `Mapper.toData()` → `Message.fromData()` → `Domain Entity`
  - **Load:** `JPA Entity` → `Mapper.toData()` → `Message.fromData()` → `Domain Entity`
- **Never expose JPA entities:** Domain entities (Message) go in and out, never JPA entities (MessageJpaEntity)
- **Delegate to Spring Data:** Use Spring Data JPA repository for actual database operations
- **Stateless:** The adapter has no state, only coordinates between JPA repository and mapper

---

### Spring Data JPA Hibernate Repository

The Spring Data JPA repository is a simple interface that extends `JpaRepository` and provides database operations for JPA entities.

**Location:** `adapters/driven/persistence/hibernate/{feature}/jpa/repository/`

```java
@Repository
public interface MessageJpaEntityHibernateRepository extends JpaRepository<MessageJpaEntity, UUID> {
    // Derived query method (Spring Data JPA naming convention)
    List<MessageJpaEntity> findAllByAuthor(String author);
    
    // Custom HQL query using @Query annotation
    @Query("SELECT m FROM MessageJpaEntity m WHERE m.author = :author AND m.publishedDate > :date ORDER BY m.publishedDate DESC")
    List<MessageJpaEntity> findRecentMessagesByAuthor(@Param("author") String author, @Param("date") LocalDateTime date);
}
```

**Key Principles:**
- **Interface only:** Spring Data JPA automatically generates the implementation at runtime
- **Extends JpaRepository:** Provides standard CRUD operations (save, findById, findAll, delete, etc.)
- **Generic types:** Specify the JPA entity type (MessageJpaEntity) and ID type (UUID)
- **Custom query methods:** Define custom finder methods using Spring Data JPA naming conventions (e.g., `findAllByAuthor`)
- **@Query annotation:** Use `@Query` with HQL (Hibernate Query Language) for complex queries that cannot be expressed with naming conventions. Use `@Param` to bind method parameters to query parameters
- **@Repository annotation:** Marks the interface as a Spring Data repository for component scanning
- **Works with JPA entities:** Only operates on JPA entities (MessageJpaEntity), never domain entities (Message)
- **No business logic:** Pure data access layer, no business rules or domain logic

**Query Method Naming Conventions:**
- `findBy{Property}`: Find entities by a specific property (e.g., `findByAuthor`)
- `findAllBy{Property}`: Find all entities matching a property (e.g., `findAllByAuthor`)
- `existsBy{Property}`: Check if entities exist with a property (e.g., `existsByEmail`)
- `countBy{Property}`: Count entities matching a property (e.g., `countByStatus`)
- `deleteBy{Property}`: Delete entities by a property (e.g., `deleteByAuthor`)

Spring Data JPA automatically translates these method names into SQL queries without requiring manual implementation.

---

### Web Adapter (Driving/Spring Boot)

Located in `adapters/driving/web/spring/`.

- **Controllers:** Annotated with `@RestController`. Inject UseCaseHandlers.
- **Naming Convention:** Use `*Resource` suffix (e.g., `MessageResource`, `UserResource`) to follow RESTful semantics.
- **Configuration:** Use a custom configuration class (`CustomAnnotationScanConfiguration`) to scan core module packages and detect `@UseCase` and `@Mapper` annotations.

---

### Example REST Resource

```java
@RestController
@RequestMapping("/api/messages")
public class MessageResource {
    private static final Logger logger = LoggerFactory.getLogger(MessageResource.class);
    
    private final PostMessageUseCaseHandler postMessageUseCaseHandler;
    private final EditMessageUseCaseHandler editMessageUseCaseHandler;

    public MessageResource(
            PostMessageUseCaseHandler postMessageUseCaseHandler,
            EditMessageUseCaseHandler editMessageUseCaseHandler) {
        this.postMessageUseCaseHandler = postMessageUseCaseHandler;
        this.editMessageUseCaseHandler = editMessageUseCaseHandler;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void postMessage(@RequestBody PostMessageRequestDTO request) {
        logger.debug("Post message: {}", request);
        postMessageUseCaseHandler.handle(request);
    }

    @PutMapping("/{messageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void editMessage(
            @PathVariable String messageId,
            @RequestBody EditMessageRequestDTO request) {
        logger.debug("Edit message {}: {}", messageId, request);
        editMessageUseCaseHandler.handle(request);
    }
}
```

**Key Principles:**
- **Thin layer:** Only handle HTTP concerns (routing, status codes, parameters)
- **Delegate:** All business logic goes to Use Case Handlers
- **Constructor injection:** Inject use case handlers (annotated with `@UseCase`)
- **DTOs:** Accept and return DTOs from the core module, never domain entities directly

---

## 5. Testing Strategy

### Unit Tests (Application Layer)

**Location:** `core/src/test`
**Technology:** JUnit 5, AssertJ.
**No Spring context.**

**Goal:** Test business logic in isolation from infrastructure concerns (databases, HTTP, frameworks). These tests should be fast, deterministic, and focused on verifying **behavior** rather than implementation details.

**Behavior-Driven Testing Approach:**  
Write tests that verify **what** the system does, not **how** it does it. This approach produces resilient tests that don't break when you refactor internal implementation details. A good unit test answers: "Given this initial state, when I perform this action, what observable outcome should I expect?"

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

### Behavior-Focused Test Structure

Unit tests should be organized to clearly communicate **business rules** and **features**, not implementation details. Use JUnit 5's `@Nested` classes and `@DisplayName` annotations to create a hierarchical structure that reads like a specification.

**Key Principles:**

1. **Feature-Level Test Class:** The outer class represents a feature or use case
2. **Rule-Level Nested Classes:** Use `@Nested` classes to group tests by business rule
3. **Descriptive Names:** Use `@DisplayName` to describe rules in plain language
4. **Scenario-Level Test Methods:** Each test method describes a specific scenario

**Example Structure:**

```java
@DisplayName("Feature: Edit a message")
class EditMessageUseCaseHandlerTest {
    private MessagingFixture fixture;

    @BeforeEach
    void setUp() {
        fixture = new MessagingFixture();
    }

    @Test
    void userCanEditHisMessage() {
        // Given
        fixture.givenTheFollowingMessagesExists(List.of(
            aMessage().withId(messageId).withText("Hello world!").build()
        ));

        // When
        fixture.whenUserEditHisMessage(messageId, "Hello world! I'm Alice");

        // Then
        fixture.thenMessageShouldBe(
            aMessage().withId(messageId).withText("Hello world! I'm Alice").build()
        );
    }

    @Nested
    class MessageNotFound {
        @Test
        void userCannotEditAMessageThatDoesNotExist() {
            fixture.givenTheFollowingMessagesExists(Collections.emptyList());

            fixture.whenUserEditHisMessage(messageId, "New text");

            fixture.thenErrorShouldBe("Message not found");
        }
    }

    @Nested
    @DisplayName("Rule: a message size is limited to 280 characters")
    class MessageSizeLimitation {
        @Test
        void userCannotEditAMessageWithMoreThan280Characters() {
            fixture.givenTheFollowingMessagesExists(List.of(
                aMessage().withId(messageId).withText("Hello").build()
            ));

            fixture.whenUserEditHisMessage(messageId, "a".repeat(281));

            fixture.thenErrorShouldBe("Message text must be less than 280 characters");
        }
    }

    @Nested
    @DisplayName("Rule: a message cannot be empty")
    class MessageNotEmpty {
        @Test
        void userCannotEditAMessageWithAnEmptyText() {
            fixture.givenTheFollowingMessagesExists(List.of(
                aMessage().withId(messageId).withText("Hello").build()
            ));

            fixture.whenUserEditHisMessage(messageId, "");

            fixture.thenErrorShouldBe("Message text must not be blank");
        }

        @Test
        void userCannotEditAMessageWithABlankText() {
            fixture.givenTheFollowingMessagesExists(List.of(
                aMessage().withId(messageId).withText("Hello").build()
            ));

            fixture.whenUserEditHisMessage(messageId, " ");

            fixture.thenErrorShouldBe("Message text must not be blank");
        }
    }
}
```

**Benefits of This Structure:**

- **Self-Documenting:** Test output reads like a specification:
  ```
  Feature: Edit a message
    ✓ userCanEditHisMessage
    MessageNotFound
      ✓ userCannotEditAMessageThatDoesNotExist
    Rule: a message size is limited to 280 characters
      ✓ userCannotEditAMessageWithMoreThan280Characters
    Rule: a message cannot be empty
      ✓ userCannotEditAMessageWithAnEmptyText
      ✓ userCannotEditAMessageWithABlankText
  ```
- **Easy Navigation:** Business rules are clearly grouped and easy to find
- **Living Documentation:** Tests serve as executable specifications of business behavior
- **Refactoring Confidence:** When a test fails, the hierarchical name immediately tells you which business rule is broken

---

### Integration / E2E Tests

**Location:** `adapters/src/test`
**Technology:** `@SpringBootTest`, TestContainers (PostgreSQL), RestAssured (API).
**Goal:** Validate Spring configuration and SQL/HTTP flows.

**Test Types and Scope:**

There are two main approaches for integration/E2E testing in Clean Architecture:

1. **E2E Tests (REST Endpoint Testing):**
   - Test the complete flow from HTTP request to database and back
   - Use RestAssured or similar tools to call actual REST endpoints
   - Verify HTTP status codes, response bodies, and end-to-end behavior
   - Example: POST to `/api/messages`, verify 201 status, then GET to verify persistence

2. **Integration Tests (UseCase + Real DB):**
   - Test UseCaseHandlers with real database adapters (no mocks)
   - Verify that the core module is properly wired with persistence adapters
   - Focus on data persistence, retrieval, and adapter conversions
   - Example: Call `PostMessageUseCaseHandler.handle()` directly, verify data in database

**Important Testing Principle:**

Integration and E2E tests should **verify that components are properly wired together**, not exhaustively test all business rule cardinalities. The goal is to ensure:
- Spring dependency injection works correctly
- Database mappings and queries function properly
- REST endpoints route to the correct use cases
- Data flows correctly through all layers

**Business logic cardinalities** (edge cases, validation rules, complex scenarios) should be thoroughly tested in **fast unit tests** at the core/UseCase level with in-memory test doubles. Integration/E2E tests should cover only a few representative scenarios to confirm the integration works, avoiding duplication of business logic testing.

**This approach avoids relying on heavy E2E/Integration tests that take a long time to run** to test the numerous business rules. By testing business logic exhaustively with fast unit tests in the core module, you maintain rapid feedback loops and keep your test suite efficient, while integration/E2E tests focus solely on verifying that all components are correctly wired together.

---

## 6. Migration Strategy

### Leveraging Existing Cucumber Tests

The existing codebase has extensive Cucumber tests covering most business rules. This is a significant asset for migration because:

1. **Business Rules Documentation:** Cucumber scenarios serve as executable specifications documenting expected behavior
2. **Safety Net:** They provide confidence that migrated code preserves existing functionality
3. **Migration Validation:** Run Cucumber tests after each migration step to verify behavior is unchanged

**Cucumber Tests as a Safety Belt During Migration:**

During the migration process, existing Cucumber tests serve as a **temporary safety belt**. Keep them running until the corresponding business rules are fully covered by behavior unit tests in the core module. This ensures:
- No regression is introduced during refactoring
- Business behavior is preserved even as code structure changes
- Confidence to proceed with migration knowing tests will catch any deviation

Once a business rule is properly tested with fast unit tests, the corresponding Cucumber scenario becomes redundant and can be retired.

### Migration Approach: From E2E to Behavior Unit Tests

A key benefit of Clean Architecture is **reducing reliance on heavy E2E tests** by moving business rule testing to fast, isolated unit tests.

**Current State (Before Migration):**
- Business rules tested primarily through Cucumber E2E tests
- Tests require full Spring context, database containers, HTTP servers
- Slow feedback loops, long CI pipelines
- Business logic scattered across services, making isolated testing difficult

**Target State (After Migration):**
- Business rules tested with **behavior unit tests** in the core module
- Tests run in milliseconds with in-memory test doubles
- E2E/Cucumber tests reduced to **integration verification only**
- Fast feedback loops, efficient CI pipelines

### Migration Steps

#### Step 1: Identify Business Rules in Cucumber Scenarios

For each Cucumber feature file:
1. Extract the business rules being tested (the Given/When/Then scenarios)
2. Document which rules belong to which domain concept
3. Identify rules that can be tested in isolation vs. those requiring integration

#### Step 2: Create Domain Entities with Business Logic

For each identified business rule:
1. Create or enhance domain entities in the core module
2. Implement business logic as methods on domain entities or domain services
3. Use the Snapshotable pattern for persistence mapping

#### Step 3: Write Behavior Unit Tests

For each business rule migrated to the domain:
1. Create corresponding unit tests in `core/src/test`
2. Use in-memory test doubles (not mocks)
3. Follow the Given/When/Then structure from Cucumber scenarios
4. Verify the same behavior, but without infrastructure dependencies

**Example - Migrating a Cucumber Scenario:**

```gherkin
# Original Cucumber scenario
Scenario: User cannot publish an empty message
  Given a user "alice" with a draft message with empty text
  When the user tries to publish the message
  Then the publication should fail with error "Cannot publish an empty message"
```

```java
// Migrated behavior unit test (fast, no Spring context)
@Test
void should_not_allow_publishing_empty_message() {
    // Given
    Message draftMessage = MessageFixture.aDraftMessage()
        .withAuthor("alice")
        .withEmptyText()
        .build();
    
    // When / Then
    assertThatThrownBy(() -> draftMessage.publish(now))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Cannot publish an empty message");
}
```

#### Step 4: Reduce Cucumber Tests to Integration Verification

Once business rules are covered by unit tests:
1. Keep a **minimal subset** of Cucumber scenarios to verify integration (wiring, HTTP routing, database persistence)
2. Remove redundant Cucumber scenarios that duplicate unit test coverage
3. Focus remaining E2E tests on happy-path flows and critical integration points

### Benefits of This Migration

| Aspect | Before (Cucumber E2E) | After (Behavior Unit Tests) |
|--------|----------------------|----------------------------|
| **Execution Time** | Seconds to minutes per test | Milliseconds per test |
| **Feedback Loop** | Slow (wait for full context) | Instant |
| **Debugging** | Complex (many layers involved) | Simple (isolated logic) |
| **Maintenance** | High (infrastructure changes break tests) | Low (tests focus on behavior) |
| **Coverage Confidence** | Good but expensive | Excellent and cheap |

### Gradual Migration Strategy

Migration should be **incremental**, not big-bang:

1. **Start with high-value domains:** Begin with features that have the most complex business rules or the slowest E2E tests
2. **Migrate one feature at a time:** Complete the full cycle (domain entity → unit tests → reduce E2E) for one feature before moving to the next
3. **Keep Cucumber tests running:** During migration, both test suites run in parallel to ensure no regression
4. **Retire Cucumber tests gradually:** Only remove E2E tests after confirming unit test coverage is complete

---

## 7. Specific Coding Rules

- **No Framework in the Core:** Never import `org.springframework.*` or `jakarta.persistence.*` in the `core` module.
- **Time Management:** Never call `LocalDateTime.now()` directly in business code; always use `DateProvider.now()`.
- **Immutability:** Prefer `val` over `var` (Kotlin) or `final` (Java) unless strictly necessary.
- **Exceptions:** Use standard or domain-specific exceptions (e.g., `IllegalArgumentException` for validation).
