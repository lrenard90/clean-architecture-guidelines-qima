# Clean Architecture Development Guide (Kotlin/Hexagonal Style)

This document defines coding rules, module structure, and patterns to follow for developing and migrating code toward a Clean Architecture, based on the analysis of the reference project.

---

## 1. Project Structure and Modules

The application is divided into distinct Gradle modules to enforce separation of responsibilities and dependency direction.

### clean-architecture-application (The Core)

**Content:** Contains all business logic (Domain) and use cases (Application).
**Dependencies:** No dependencies on external frameworks (Spring, Hibernate, etc.). Only standard Kotlin and test libraries (JUnit, AssertJ).
**Golden Rule:** This module must never depend on infrastructure modules.

---

### clean-architecture-hibernate-adapter (Persistence Adapter)

**Content:** Implements the repository interfaces defined in the Core using JPA/Hibernate.
**Dependencies:** Depends on `clean-architecture-application` and Spring Data JPA.

---

### clean-architecture-spring-boot-rest-api (Web Adapter / API)

**Content:** REST controllers, application configuration, dependency injection.
**Dependencies:** Depends on `clean-architecture-application` and `clean-architecture-hibernate-adapter`.

---

### socle (Shared Kernel)

**Content:** Shared technical code (annotations, date utilities, abstractions). Can be integrated into the application or remain separate.

---

## 2. Domain Layer

Located in `clean-architecture-application`.

### Entities (`src/main/kotlin/.../domain/entity`)

Entities are rich objects containing both data and business behavior.

#### Snapshotable Pattern

Entities must not directly expose their mutable fields for persistence.
They must implement the `Snapshotable<DataType>` interface to provide an immutable view (Data Class) of their internal state.

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

Located in `clean-architecture-application`.

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

### Persistence Adapter (Hibernate)

Located in `clean-architecture-hibernate-adapter`.

- **JPA Entities:** Separate classes annotated with `@Entity` (e.g., `MessageJpaEntity`). Distinct from Domain Entities.
- **Mappers:** Use MapStruct or manual mappers to convert  
  **Domain Entity (via `data()`) ⇄ JPA Entity**
- **Repository Implementation:** Implements the port interface (e.g., `MessageRepository`).  
  Injects the JPA repository (`MessageJpaEntityHibernateRepository`) and the Mapper.

---

### Web Adapter (Spring Boot)

Located in `clean-architecture-spring-boot-rest-api`.

- **Controllers:** Annotated with `@RestController`. Inject UseCaseHandlers.
- **Configuration:** Use a custom configuration class (`CustomAnnotationScanConfiguration`) to scan Core packages and detect `@UseCase` and `@Mapper` annotations.

---

## 5. Testing Strategy

### Unit Tests (Application Layer)

**Location:** `clean-architecture-application/src/test`
**Technology:** JUnit 5, AssertJ.
**No Spring context.**

**Test Doubles:**  
Prefer in-memory repositories (e.g., `InMemoryMessageRepository` using a `HashMap`) over heavy mocks (Mockito) when possible.

**Fixtures:**  
Use fixture classes (e.g., `MessagingFixture`) to standardize Given–When–Then and improve readability (BDD).

---

### Integration / E2E Tests

**Location:** Adapter modules.
**Technology:** `@SpringBootTest`, TestContainers (PostgreSQL), RestAssured (API).
**Goal:** Validate Spring configuration and SQL/HTTP flows.

---

## 6. Specific Coding Rules

- **No Framework in the Core:** Never import `org.springframework.*` or `jakarta.persistence.*` in `clean-architecture-application`.
- **Time Management:** Never call `LocalDateTime.now()` directly in business code; always use `DateProvider.now()`.
- **Immutability:** Prefer `val` over `var` unless strictly necessary.
- **Exceptions:** Use standard or domain-specific exceptions (e.g., `IllegalArgumentException` for validation).
