# Hexagonal Architecture Thoughts

### Use Case Handlers Are Entry Points, NOT Inner Services

**Problem:** Use Case Handlers (or Use Case Ports) should act as **entry points to the hexagon**, not as internal classes that get called by services within the application.

**Understanding the Hexagon:**

```
                    ┌─────────────────────────────────────────────┐
                    │              DRIVING ADAPTERS               │
                    │    (REST Controllers, CLI, Event Listeners) │
                    └──────────────────────┬──────────────────────┘
                                           │
                                           ▼
                    ┌──────────────────────────────────────────────┐
                    │          ══ USE CASE HANDLERS ══             │
                    │     (Entry Points / Inbound Ports / API)     │
                    │                                              │
                    │   ┌──────────────────────────────────────┐   │
                    │   │          APPLICATION CORE            │   │
                    │   │                                      │   │
                    │   │   Domain Entities, Domain Services,  │   │
                    │   │   Business Rules, Invariants         │   │
                    │   │                                      │   │
                    │   └──────────────────────────────────────┘   │
                    │                                              │
                    │         ══ OUTBOUND PORTS ══                 │
                    │   (Repository interfaces, External APIs)     │
                    └──────────────────────┬───────────────────────┘
                                           │
                                           ▼
                    ┌──────────────────────────────────────────────┐
                    │              DRIVEN ADAPTERS                 │
                    │   (JPA Repositories, HTTP Clients, Queues)   │
                    └──────────────────────────────────────────────┘
```

**The Rule:** Use Case Handlers sit at the **boundary** of the hexagon. They are the **only entry points** for external actors to interact with the application core.

**Who should call Use Case Handlers:**
- ✅ REST Controllers (driving adapters)
- ✅ CLI Commands (driving adapters)
- ✅ Event/Message Listeners (driving adapters)
- ✅ Scheduled Jobs (driving adapters)
- ✅ GraphQL Resolvers (driving adapters)

**Who should NOT call Use Case Handlers:**
- ❌ Other Use Case Handlers (see section below)
- ❌ Domain Services
- ❌ Application Services
- ❌ Any internal component within the hexagon

**Why this matters:**

1. **Clear System Boundaries**
   - Use Cases define "what the system can do" from the outside world's perspective
   - Each Use Case is a discrete capability exposed to external actors
   - If internal services call Use Cases, the boundary becomes blurred

2. **Proper Dependency Direction**
   - Driving adapters depend on Use Cases (inward dependency)
   - Use Cases depend on domain and outbound ports (inward then outward)
   - Internal services calling Use Cases creates circular or inverted dependencies

3. **Transaction Ownership**
   - Use Cases typically own the transaction boundary
   - When a controller calls a Use Case, it's clear where the transaction starts/ends
   - Internal calls create nested or ambiguous transactions

4. **Testability and Isolation**
   - Use Cases can be tested in isolation with fake adapters
   - If internal services depend on Use Cases, you need to mock Use Cases to test services
   - This defeats the purpose of the hexagonal architecture

**Bad Example (Anti-Pattern):**

```java
// ❌ WRONG: Internal service calling a Use Case Handler
@Service
public class OrderProcessingService {
    private final CreateOrderUseCase createOrderUseCase;      // ❌ Wrong dependency!
    private final SendNotificationUseCase notificationUseCase; // ❌ Wrong dependency!
    
    public void processOrder(OrderData data) {
        // This service is INSIDE the hexagon, 
        // but it's calling Use Cases as if it were OUTSIDE
        createOrderUseCase.handle(data);           // ❌ Treating UseCase as inner class
        notificationUseCase.handle(data.email());  // ❌ Treating UseCase as inner class
    }
}
```

**Good Example:**

```java
// ✅ CORRECT: Controller (driving adapter) calls Use Case
@RestController
public class OrderController {
    private final CreateOrderUseCase createOrderUseCase;  // ✅ Adapter → UseCase
    
    @PostMapping("/orders")
    public ResponseEntity<OrderDto> createOrder(@RequestBody CreateOrderRequest request) {
        return ResponseEntity.ok(createOrderUseCase.handle(request));  // ✅ Entry point
    }
}

// ✅ CORRECT: Use Case orchestrates domain, not other Use Cases
@UseCase
public class CreateOrderUseCaseHandler implements CreateOrderUseCase {
    private final OrderRepository orderRepository;        // Outbound port
    private final NotificationPort notificationPort;      // Outbound port
    private final OrderDomainService orderDomainService;  // Domain service
    
    public OrderDto handle(CreateOrderRequest request) {
        Order order = orderDomainService.createOrder(request);  // ✅ Domain logic
        orderRepository.save(order);                             // ✅ Outbound port
        notificationPort.sendOrderConfirmation(order);           // ✅ Outbound port
        return OrderDto.from(order);
    }
}
```

**Key Insight:** If you find yourself wanting to call a Use Case from inside the hexagon, it's a sign that:
1. The logic should be in a **Domain Service** (business logic reuse)
2. The logic should be in a **Domain Entity** (entity behavior)
3. You need an **Outbound Port** (for side effects like notifications)
4. You're trying to compose operations that should be **separate API calls** from the client

---

### Screaming Architecture: Controllers Can Call Use Case Handlers Directly

**Note:** This section presents an **alternative architectural approach**, not a strict rule. Whether to use explicit Use Case Handlers vs. grouped Application Services depends on team preferences and project context.

**Observation:** In the current implementation, controllers call generic "Application Services" (like `ContactOriginCommandService`) instead of explicit Use Case Handlers. This can obscure what the system actually does.

**Current Approach (Grouped Services):**

```java
// Controller calls a generic "service" that groups multiple operations
@RestController
public class ContactOriginController {
    private final ContactOriginCommand contactOriginCommand;  // Generic service interface
    
    @PostMapping("/contact-origins")
    public ResponseEntity<ContactOriginDto> create(@RequestBody CreateContactOriginRequest request) {
        return ResponseEntity.ok(contactOriginCommand.create(webMapper.toCommand(request)));
    }
    
    @PutMapping("/contact-origins/{id}")
    public ResponseEntity<ContactOriginDto> update(@PathVariable Integer id, @RequestBody UpdateContactOriginRequest request) {
        return ResponseEntity.ok(contactOriginCommand.update(id, webMapper.toCommand(request)));
    }
}

// The "service" groups multiple use cases together
@Service
public class ContactOriginCommandService implements ContactOriginCommand {
    public ContactOriginDto create(CreateContactOriginCommand command) { ... }
    public ContactOriginDto update(Integer id, UpdateContactOriginCommand command) { ... }
    public void deactivate(Integer id) { ... }
}
```

**The Issue:** Looking at the folder structure, you can't immediately tell what the system does:

```
application/
├── service/
│   ├── command/
│   │   ├── ContactOriginCommandService.java   // What does this do? 🤔
│   │   └── EventTypeCommandService.java
│   └── query/
│       ├── ContactOriginQueryService.java
│       └── EventTypeQueryService.java
```

**Screaming Architecture Approach:**

With screaming architecture, the folder structure **screams** what the system can do:

```java
// ✅ Controller calls explicit Use Case Handlers
@RestController
public class ContactOriginController {
    private final CreateContactOriginUseCase createContactOrigin;
    private final UpdateContactOriginUseCase updateContactOrigin;
    private final DeactivateContactOriginUseCase deactivateContactOrigin;
    
    @PostMapping("/contact-origins")
    public ResponseEntity<ContactOriginDto> create(@RequestBody CreateContactOriginRequest request) {
        return ResponseEntity.ok(createContactOrigin.handle(webMapper.toCommand(request)));
    }
    
    @PutMapping("/contact-origins/{id}")
    public ResponseEntity<ContactOriginDto> update(@PathVariable Integer id, @RequestBody UpdateContactOriginRequest request) {
        return ResponseEntity.ok(updateContactOrigin.handle(id, webMapper.toCommand(request)));
    }
}
```

**Improved Folder Structure (Screaming Architecture):**

```
contact/
├── application/
│   ├── port/
│   │   ├── in/
│   │   │   ├── command/
│   │   │   │   ├── CreateContactOriginUseCase.java      // ✅ Clear: creates contact origins
│   │   │   │   ├── UpdateContactOriginUseCase.java      // ✅ Clear: updates contact origins
│   │   │   │   └── DeactivateContactOriginUseCase.java  // ✅ Clear: deactivates contact origins
│   │   │   └── query/
│   │   │       ├── GetContactOriginByIdQuery.java       // ✅ Clear: gets one by ID
│   │   │       ├── GetContactOriginsTreeQuery.java      // ✅ Clear: gets hierarchical tree
│   │   │       └── GetContactOriginParentOptionsQuery.java
│   │   └── out/
│   │       └── ContactOriginRepository.java
│   │
│   ├── usecase/                                         // Use Case implementations
│   │   ├── command/
│   │   │   ├── CreateContactOriginUseCaseHandler.java
│   │   │   ├── UpdateContactOriginUseCaseHandler.java
│   │   │   └── DeactivateContactOriginUseCaseHandler.java
│   │   └── query/
│   │       ├── GetContactOriginByIdQueryHandler.java
│   │       ├── GetContactOriginsTreeQueryHandler.java
│   │       └── GetContactOriginParentOptionsQueryHandler.java
│   │
│   └── dto/
│       ├── command/
│       │   ├── CreateContactOriginCommand.java
│       │   └── UpdateContactOriginCommand.java
│       └── query/
│           └── ContactOriginDto.java
│
├── domain/
│   └── model/
│       ├── ContactOrigin.java
│       └── ContactOriginId.java
│
└── adapter/
    ├── in/
    │   └── web/
    │       └── ContactOriginController.java
    └── out/
        └── persistence/
            └── JpaContactOriginRepository.java
```

**Why Screaming Architecture Matters:**

| Aspect | Generic Services | Screaming Architecture (Use Cases) |
|--------|-----------------|-----------------------------------|
| **Discoverability** | Must open file to see methods | File names reveal capabilities |
| **Single Responsibility** | One class handles many operations | One class = one operation |
| **Testability** | Test file tests many behaviors | One test file per use case |
| **Navigation** | Search for method names | Browse folders to understand system |
| **Onboarding** | "What can the system do?" → Read code | "What can the system do?" → Look at `usecase/` folder |
| **Modification** | Changes may affect unrelated operations | Changes are isolated to one use case |

**The "Scream" Test:**

> *"Can a new developer understand what the module does just by looking at the folder structure?"*

- ❌ `service/command/ContactOriginCommandService.java` → "It does... something with commands?"
- ✅ `usecase/command/CreateContactOriginUseCaseHandler.java` → "It creates contact origins!"

**Important Nuance: Commands vs. Queries**

The benefits of explicit Use Case Handlers are **more significant for Commands than for Queries**:

| Aspect | Commands (Writes) | Queries (Reads) |
|--------|------------------|-----------------|
| **Complexity** | Often involve business rules, validation, side effects | Usually simple data retrieval |
| **Orchestration** | May coordinate multiple domain objects and ports | Typically just fetch and map data |
| **Transaction boundaries** | Critical — each command is a transaction | Less critical — reads don't modify state |
| **Business logic** | Significant — invariants, workflows | Minimal — mostly projection/mapping |

**For Commands:** Explicit Use Case Handlers (`CreateContactOriginUseCase`, `UpdateContactOriginUseCase`) make sense because:
- Each command represents a distinct business operation with its own rules
- Transaction boundaries are clear
- Testing is focused on one behavior

**For Queries:** Grouped Query Services (`ContactOriginQueryService`) are often **more practical** because:
- Queries are typically simple read operations with little to no business logic
- Having one handler per query can lead to many small classes with trivial implementations
- A single Query Service grouping related reads reduces file count without losing clarity
- The "screaming" benefit is less valuable when the operation is just "get data"

**Recommended Hybrid Approach:**

```
contact/application/
├── port/in/
│   ├── command/
│   │   ├── CreateContactOriginUseCase.java      // ✅ Explicit: complex business operation
│   │   ├── UpdateContactOriginUseCase.java      // ✅ Explicit: complex business operation
│   │   └── DeactivateContactOriginUseCase.java  // ✅ Explicit: complex business operation
│   └── query/
│       └── ContactOriginQuery.java              // ✅ Grouped: simple read operations
├── usecase/
│   └── command/
│       ├── CreateContactOriginUseCaseHandler.java
│       ├── UpdateContactOriginUseCaseHandler.java
│       └── DeactivateContactOriginUseCaseHandler.java
└── service/
    └── query/
        └── ContactOriginQueryService.java       // ✅ One service for all queries
```

**Summary:**

- **For Commands:** Consider explicit Use Case Handlers for better discoverability and single responsibility
- **For Queries:** Grouped Query Services are often sufficient and more practical
- **File names should describe the operation** (CreateX, UpdateX, DeleteX, GetXById, etc.)
- **Folder structure should reveal the system's capabilities** at a glance
- This aligns with Robert C. Martin's "Screaming Architecture" principle: *"The architecture should scream the intent of the system"*

---

### Use Case Handlers Should NOT Call Other Use Case Handlers

**Problem:** Calling use case handlers from multiple locations (especially from other use case handlers) is a bad practice in Clean Architecture.

**Why this is problematic:**

1. **Violates Single Responsibility Principle**
   - Each Use Case represents a single business operation or user story
   - When Use Case A calls Use Case B, Use Case A now has two responsibilities: its own logic AND orchestrating Use Case B
   - This makes the code harder to understand and reason about

2. **Creates Tight Coupling**
   - Use Cases become interdependent, making changes ripple through multiple handlers
   - You cannot modify or replace Use Case B without considering all the Use Cases that call it
   - This defeats the purpose of having isolated, independent operations

3. **Breaks Transaction Boundaries**
   - Each Use Case typically represents an atomic operation with its own transaction
   - Nested Use Case calls create ambiguous transaction semantics
   - Should the inner Use Case commit? What happens on partial failure?

4. **Obscures the System's Capabilities**
   - Use Cases should be a clear catalog of "what the system can do"
   - When Use Cases call each other, this catalog becomes a tangled web
   - It's harder to understand the system's actual business capabilities

5. **Complicates Testing**
   - Testing Use Case A now requires considering all paths through Use Case B
   - You end up testing the same logic multiple times through different entry points
   - Mock setups become complex and brittle

**The Correct Approach:**

If you need to reuse logic across multiple Use Cases:

1. **Extract to Domain Entities** (preferred)
   - Business rules naturally belong in domain objects
   - Entities encapsulate their own invariants and behavior

   ```java
   // Good: Logic in domain entity
   public class Order {
       public void cancel(LocalDateTime cancelDate) {
           if (this.status == OrderStatus.SHIPPED) {
               throw new IllegalStateException("Cannot cancel shipped orders");
           }
           this.status = OrderStatus.CANCELLED;
           this.cancelledAt = cancelDate;
       }
   }
   ```

2. **Extract to Domain Services**
   - When logic spans multiple entities
   - When logic needs to be reused but doesn't belong to a single entity

   ```java
   // Good: Domain service for cross-entity logic
   public class OrderFulfillmentService {
       public void fulfill(Order order, Inventory inventory) {
           inventory.reserve(order.getItems());
           order.markAsFulfilling();
       }
   }
   ```

3. **Keep Use Cases as Thin Orchestrators**
   - Use Cases coordinate domain objects and infrastructure
   - They should NOT contain business logic themselves

   ```java
   // Good: Thin Use Case orchestrator
   @UseCase
   public class CancelOrderUseCaseHandler {
       private final OrderRepository orderRepository;
       private final DateProvider dateProvider;
       
       public void handle(CancelOrderRequest request) {
           Order order = orderRepository.findById(request.orderId())
               .orElseThrow(() -> new NotFoundException("Order not found"));
           
           order.cancel(dateProvider.now());  // Delegate to domain
           
           orderRepository.save(order);
       }
   }
   ```

**Bad Example (Anti-Pattern):**

```java
// BAD: Use Case calling another Use Case
@UseCase
public class CreateOrderUseCaseHandler {
    private final ValidateCustomerUseCaseHandler validateCustomer;  // ❌ Wrong!
    private final CheckInventoryUseCaseHandler checkInventory;      // ❌ Wrong!
    
    public void handle(CreateOrderRequest request) {
        validateCustomer.handle(request.customerId());  // ❌ Coupling!
        checkInventory.handle(request.items());         // ❌ Coupling!
        // ... create order
    }
}
```

**Good Example:**

```java
// GOOD: Use Case using domain services and entities
@UseCase
public class CreateOrderUseCaseHandler {
    private final CustomerRepository customerRepository;
    private final InventoryService inventoryService;  // Domain service
    private final OrderRepository orderRepository;
    
    public void handle(CreateOrderRequest request) {
        Customer customer = customerRepository.findById(request.customerId())
            .orElseThrow(() -> new NotFoundException("Customer not found"));
        
        customer.validateCanPlaceOrder();  // Domain logic in entity
        
        inventoryService.validateAvailability(request.items());  // Domain service
        
        Order order = Order.create(customer, request.items());
        orderRepository.save(order);
    }
}
```

---

### Validation Should Live in the Domain Layer, NOT in Use Cases or Application Services

**Problem:** Validation logic like this should NOT be placed in the UseCaseHandler or ApplicationService:

```java
// ❌ WRONG: Validation in the Application/Use Case layer
private void validateUpdateCommand(UpdateContactOriginCommand command) {
    if (command.getFrenchName() == null || command.getFrenchName().isBlank()) {
        throw new InvalidRequestException("French name is required");
    }
    if (command.getEnglishName() == null || command.getEnglishName().isBlank()) {
        throw new InvalidRequestException("English name is required");
    }
}
```

**Why this is problematic:**

1. **Business Rules Leak Out of the Domain**
   - Validation IS business logic ("a ContactOrigin must have a French name")
   - When validation lives in Use Cases, the domain becomes anemic
   - The same validation may need to be duplicated across multiple Use Cases

2. **Domain Invariants Become Scattered**
   - An entity's rules should be encapsulated within the entity itself
   - External code shouldn't need to "know" what makes a ContactOrigin valid
   - Scattered validation leads to inconsistencies and bugs

3. **Domain Entities Become Anemic**
   - Domain entities become "dumb data bags" that can be in invalid states
   - An invalid entity can exist in memory, leading to potential bugs

**Note on Testing:** While it might seem that "you can't test domain rules without going through the Use Case" is a problem, it's actually **not problematic** when using **behavioral unit testing**:

   - **Behavioral testing targets Use Cases** (the upper level), not domain entities directly
   - This approach **decouples tests from implementation details**
   - If a business rule moves (e.g., from an entity to a domain service due to refactoring or increased complexity), tests targeting the Use Case **remain valid** because they verify the behavior, not the implementation
   - Testing domain entities directly couples tests to their structure—if you refactor, tests break even though the business rule still holds
   - **Recommended approach:** Test business rules through Use Case handlers to ensure tests survive refactoring

**Where Validation Should Go:**

Depending on the context, validation belongs in:

1. **Domain Entity Constructor** (for entity invariants)
   
   ```java
   // ✅ CORRECT: Validation in domain entity constructor
   public record ContactOrigin(
           ContactOriginId id,
           String frenchName,
           String englishName,
           ContactOriginId parentId,
           boolean isActive
   ) {
       public ContactOrigin {
           // Invariants enforced at construction time
           if (frenchName == null || frenchName.isBlank()) {
               throw new DomainValidationException("French name is required");
           }
           if (englishName == null || englishName.isBlank()) {
               throw new DomainValidationException("English name is required");
           }
       }
   }
   ```

2. **Value Objects** (for complex/reusable validation like email, phone, etc.)

   ```java
   // ✅ CORRECT: Complex validation in Value Object
   public record EmailAddress(String value) {
       private static final Pattern EMAIL_PATTERN = 
           Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
       
       public EmailAddress {
           if (value == null || value.isBlank()) {
               throw new DomainValidationException("Email is required");
           }
           if (!EMAIL_PATTERN.matcher(value).matches()) {
               throw new DomainValidationException("Invalid email format: " + value);
           }
       }
   }
   
   // Usage in entity
   public record Contact(
           ContactId id,
           String name,
           EmailAddress email  // Value Object handles email validation
   ) { }
   ```

3. **Domain Services** (for complex validation involving multiple entities or external rules)

   ```java
   // ✅ CORRECT: Complex cross-entity validation in Domain Service
   public class ContactOriginDomainService {
       
       public void validateHierarchy(ContactOrigin origin, ContactOrigin parent) {
           if (parent != null && !parent.isActive()) {
               throw new DomainValidationException(
                   "Cannot assign inactive parent to ContactOrigin"
               );
           }
           if (wouldCreateCycle(origin, parent)) {
               throw new DomainValidationException(
                   "Cannot create circular hierarchy"
               );
           }
       }
   }
   ```

**The Result: Thin Use Cases That Orchestrate**

```java
// ✅ CORRECT: Use Case is a thin orchestrator, validation is in domain
@UseCase
public class UpdateContactOriginUseCaseHandler {
    private final ContactOriginRepository repository;
    
    public ContactOriginDto handle(UpdateContactOriginCommand command) {
        ContactOrigin existing = repository.findById(command.getId())
            .orElseThrow(() -> new NotFoundException("ContactOrigin not found"));
        
        // Validation happens inside the domain entity constructor/factory
        // If frenchName or englishName is invalid, the entity throws
        ContactOrigin updated = new ContactOrigin(
            existing.id(),
            command.getFrenchName(),    // Validated by entity constructor
            command.getEnglishName(),   // Validated by entity constructor
            existing.parentId(),
            existing.isActive()
        );
        
        return toDto(repository.save(updated));
    }
}
```

**Benefits of Domain-Layer Validation:**

| Aspect | Validation in Use Case | Validation in Domain |
|--------|----------------------|---------------------|
| **Encapsulation** | ❌ Rules exposed | ✅ Rules hidden |
| **Consistency** | ❌ May be duplicated | ✅ Single source of truth |
| **Testability** | ❌ Need Use Case to test | ✅ Unit test domain directly |
| **Invalid States** | ❌ Entity can be invalid | ✅ Entity always valid |
| **Reusability** | ❌ Tied to Use Case | ✅ Reusable across Use Cases |

**Rule of Thumb:**
- If validation is about **what makes an entity valid** → put it in the **entity constructor**
- If validation is about **a specific field format** → put it in a **Value Object**
- If validation requires **multiple entities or external data** → put it in a **Domain Service**
- Use Cases should **only validate that required inputs are present** (technical validation), not business rules

---

### Understanding Service Types: Use Case Handlers vs Application Services vs Domain Services

In Clean/Hexagonal Architecture, different "service" types have distinct roles and responsibilities. Confusing them leads to architectural violations.

| Service Type | Layer | Purpose | Framework Dependencies |
|--------------|-------|---------|----------------------|
| **Use Case Handler** | Application | Entry point to the hexagon, orchestrates a single user action | Should be minimal (custom `@UseCase` annotation preferred) |
| **Application Service** | Application | Same as Use Case Handler (often used interchangeably) | Should be minimal |
| **Domain Service** | Domain | Encapsulates business logic that spans multiple entities | **None** (pure Java/Kotlin) |

**Use Case Handler / Application Service:**
- Acts as the **entry point** for external actors (controllers, CLI, event listeners)
- **Orchestrates** the flow: loads entities, calls domain logic, persists changes
- Should be **thin** — delegates business logic to domain objects
- Owns the **transaction boundary**
- Located in `application/` layer

```java
// Application layer - orchestrates but doesn't contain business logic
@UseCase
public class TransferMoneyUseCaseHandler {
    private final AccountRepository accountRepository;
    private final TransferDomainService transferService;  // Domain Service
    
    public void handle(TransferCommand cmd) {
        Account from = accountRepository.findById(cmd.fromId());
        Account to = accountRepository.findById(cmd.toId());
        
        transferService.transfer(from, to, cmd.amount());  // Delegate to domain
        
        accountRepository.save(from);
        accountRepository.save(to);
    }
}
```

**Domain Service:**
- Contains **pure business logic** that doesn't naturally fit in a single entity
- **No framework dependencies** (no Spring, no JPA, no external I/O)
- Can be called by Use Case Handlers or other Domain Services
- Located in `domain/` layer

```java
// Domain layer - pure business logic, no framework dependencies
public class TransferDomainService {
    
    public void transfer(Account from, Account to, Money amount) {
        if (from.equals(to)) {
            throw new DomainException("Cannot transfer to same account");
        }
        from.withdraw(amount);  // Entity method
        to.deposit(amount);     // Entity method
    }
}
```

**Key Differences:**

| Aspect | Use Case Handler / App Service | Domain Service |
|--------|-------------------------------|----------------|
| **Location** | `application/` | `domain/` |
| **Dependencies** | Repositories, Domain Services, Ports | Only other domain objects |
| **I/O** | Yes (via ports) | **No** |
| **Transaction** | Owns it | Unaware of it |
| **Framework** | Minimal (ideally none) | **None** |
| **Called by** | Driving adapters only | Use Cases, other Domain Services |

**Common Mistake:** Putting business logic in Application Services instead of Domain Services, resulting in an **anemic domain model** where entities are just data holders.

---

### Repositories Should NOT Contain Validation or Business Logic

**Problem:** Having validation and custom business logic in Repositories (including InMemoryRepositories used for testing) is an anti-pattern that undermines the testability guarantees of Clean Architecture.

**Bad Example (Anti-Pattern):**

```java
// ❌ WRONG: InMemoryRepository with validation logic
public class InMemoryContactOriginRepository implements ContactOriginRepository {
    private final Map<Integer, ContactOrigin> store = new ConcurrentHashMap<>();
    
    @Override
    public synchronized ContactOrigin save(ContactOrigin contactOrigin, Integer modificationTrackId) {
        if (contactOrigin == null) {
            throw new IllegalArgumentException("Contact origin is required");
        }
        contactOrigin.validate();  // ❌ Business validation in repository!
        
        // ... storage logic
        store.put(resolvedId, stored);
        return stored;
    }
}
```

**Why this is problematic:**

1. **Tests Pass for the Wrong Reasons**
   - When testing business logic through the hexagon, validation might be triggered in the InMemoryRepository
   - Tests pass not because the business logic is correctly implemented in the core/hexagon, but because the InMemoryRepository (a test double!) enforces the rules
   - This gives **false confidence** that the production code is correct

2. **Cannot Verify Core Business Logic**
   - The purpose of InMemory repositories in tests is to **simulate storage**, not to enforce business rules
   - If validation happens in the test double, you cannot be sure the same validation exists in your domain layer
   - The real JPA repository adapter likely doesn't have this validation, so production behavior differs from test behavior

3. **Repositories Are Infrastructure, Not Business Logic**
   - Repositories belong to the **driven adapter** or **outbound port** layer
   - Their single responsibility is **persistence** (save, find, delete)
   - Business rules like "ContactOrigin must have a French name" are **domain concerns**
   - Mixing them violates the Single Responsibility Principle and the hexagonal boundary

4. **Test Doubles Should Mirror Production Behavior**
   - InMemory repositories should behave like the real repository **minus the database**
   - They should store and retrieve data, generate IDs, maybe simulate constraints (unique keys)
   - They should **not** add validation that doesn't exist in the real adapter

**The Correct Approach:**

Validation should happen in the **domain layer** before the entity reaches the repository:

```java
// ✅ CORRECT: Validation in the domain entity
public record ContactOrigin(
        ContactOriginId id,
        String frenchName,
        String englishName,
        ContactOriginId parentId,
        boolean isActive,
        Integer modificationTrackId
) {
    public ContactOrigin {
        // Domain invariants enforced at construction
        if (frenchName == null || frenchName.isBlank()) {
            throw new DomainValidationException("French name is required");
        }
        if (englishName == null || englishName.isBlank()) {
            throw new DomainValidationException("English name is required");
        }
    }
}

// ✅ CORRECT: InMemoryRepository is a simple storage stub
public class InMemoryContactOriginRepository implements ContactOriginRepository {
    private final Map<Integer, ContactOrigin> store = new ConcurrentHashMap<>();
    
    @Override
    public synchronized ContactOrigin save(ContactOrigin contactOrigin, Integer modificationTrackId) {
        // Only storage logic, NO business validation
        ContactOriginId id = contactOrigin.id();
        Integer resolvedId = id == null || id.isUndefined() ? nextId() : id.value();
        
        ContactOrigin stored = new ContactOrigin(
                new ContactOriginId(resolvedId),
                contactOrigin.frenchName(),
                contactOrigin.englishName(),
                contactOrigin.parentId(),
                contactOrigin.isActive(),
                modificationTrackId
        );
        store.put(resolvedId, stored);
        return stored;
    }
}
```

**What Repositories CAN Do:**

| Allowed | Not Allowed |
|---------|-------------|
| ✅ Store and retrieve data | ❌ Validate business rules |
| ✅ Generate IDs | ❌ Call `entity.validate()` |
| ✅ Enforce unique constraints (simulating DB) | ❌ Check required fields |
| ✅ Throw if entity is null (technical check) | ❌ Apply business transformations |
| ✅ Map between domain and persistence models | ❌ Enforce domain invariants |

**Special Case: External Services with Business Logic**

If an outbound port needs to have business logic (e.g., an external API that validates inputs), it should **not** be modeled as a repository:

- It should be an **External Service Port** with clear, documented API rules
- The business logic / API validation rules should be explicit in the port interface documentation
- Example: `PaymentGatewayPort`, `TaxCalculationServicePort` — these have external rules that are part of the integration contract, not your domain

```java
// ✅ External service with documented business rules (not a repository)
public interface PaymentGatewayPort {
    /**
     * Processes a payment through the external gateway.
     * 
     * External API rules (documented):
     * - Amount must be positive
     * - Currency must be supported (USD, EUR, GBP)
     * - Card must not be expired
     * 
     * @throws PaymentRejectedException if the gateway rejects the payment
     */
    PaymentResult processPayment(PaymentRequest request);
}
```

**Test Doubles for External Services:**

For external services with business logic, the test double can be either:

1. **Fake Implementation** (simple/stub) — A simplified implementation that mimics the external service's behavior:
   ```java
   // ✅ Fake implementation for testing
   public class FakePaymentGateway implements PaymentGatewayPort {
       private boolean shouldSucceed = true;
       
       public void willRejectPayments() { this.shouldSucceed = false; }
       public void willAcceptPayments() { this.shouldSucceed = true; }
       
       @Override
       public PaymentResult processPayment(PaymentRequest request) {
           // Simulate the external API rules
           if (request.amount().isNegativeOrZero()) {
               throw new PaymentRejectedException("Amount must be positive");
           }
           return shouldSucceed 
               ? PaymentResult.success(generateTransactionId())
               : PaymentResult.rejected("Insufficient funds");
       }
   }
   ```

2. **Mock** — Using a mocking framework (Mockito, etc.) when you need precise control over interactions:
   ```java
   // ✅ Mock for testing specific scenarios
   @Mock PaymentGatewayPort paymentGateway;
   
   @Test
   void shouldHandlePaymentRejection() {
       when(paymentGateway.processPayment(any()))
           .thenThrow(new PaymentRejectedException("Card expired"));
       
       // Test the Use Case behavior when payment fails
   }
   ```

**When to use which:**

| Approach | Best For |
|----------|----------|
| **Fake** | Testing business flows where you want realistic behavior simulation |
| **Mock** | Testing specific error scenarios, verifying interactions, or when fake is too complex |

**Key Point:** Whichever approach you choose, it must be **clear and explicit** in your test setup. The test double's purpose is to simulate the external service's contract, including its validation rules and error cases — not to add domain business logic that belongs in your hexagon.

**Summary:**
- **Repositories = Storage only** (no business logic)
- **Validation = Domain layer** (entity constructors, Value Objects, Domain Services)
- **InMemory test doubles = Mirror production adapters** (store data, don't add extra validation)
- **External services with rules = Separate ports** (not repositories)

---

## Code Organization: Separate Command and Query DTOs

**Observation:** The current codebase mixes Command objects (for write requests) and DTOs (for query responses) in the same `application/dto/` folder:

```
application/dto/
├── ContactOriginDto.java          # Query response
├── EventTypeDto.java              # Query response
├── CreateContactOriginCommand.java # Command (write)
├── UpdateContactOriginCommand.java # Command (write)
├── CreateEventTypeCommand.java     # Command (write)
└── UpdateEventTypeCommand.java     # Command (write)
```

**Recommendation:** Following CQRS-lite principles (as documented in ADR-004), separate these into distinct folders to make the intent clear:

```
application/
├── dto/
│   ├── command/                    # Objects for write operations (requests)
│   │   ├── CreateContactOriginCommand.java
│   │   ├── UpdateContactOriginCommand.java
│   │   ├── CreateEventTypeCommand.java
│   │   └── UpdateEventTypeCommand.java
│   └── query/                      # Objects for read operations (responses)
│       ├── ContactOriginDto.java
│       └── EventTypeDto.java
```

**Alternative structure** (aligning with port structure):

```
application/
├── port/
│   ├── in/
│   │   ├── command/               # Command ports (interfaces)
│   │   └── query/                 # Query ports (interfaces)
│   └── out/                       # Outbound ports
├── command/                       # Command DTOs (request objects)
│   ├── CreateContactOriginCommand.java
│   └── UpdateContactOriginCommand.java
└── query/                         # Query DTOs (response objects)
    ├── ContactOriginDto.java
    └── EventTypeDto.java
```

**Why This Matters:**

1. **Clear Intent** — Immediately obvious whether an object is for reading or writing
2. **CQRS Alignment** — Folder structure mirrors the Command/Query separation in ports
3. **Discoverability** — Developers know where to find/add request vs response objects
4. **Scalability** — As the module grows, commands and queries stay organized

**Naming Conventions:**

| Type | Naming Pattern | Example |
|------|---------------|---------|
| **Command** (write request) | `<Action><Aggregate>Command` | `CreateContactOriginCommand`, `UpdateEventTypeCommand` |
| **Query DTO** (read response) | `<Aggregate>Dto` or `<Aggregate>Response` | `ContactOriginDto`, `EventTypeResponse` |

**Note:** Some teams prefer `Request` suffix for commands (e.g., `CreateContactOriginRequest`) to distinguish from CQRS event-sourcing "commands". Either convention is fine as long as it's consistent.

---

## DTOs Should NOT Be Used for Business Logic

**Note:** Since we are not implementing strict CQRS, it is **acceptable for Use Case Handlers to return DTOs** for convenience. The key principle is that **DTOs should not be used in business logic** — they exist solely to cross boundaries (e.g., for API responses).

**The Problem:** DTOs are designed for data transfer, not for business operations. When DTOs leak into business logic, you mix concerns: contract/presentation needs get entangled with domain rules.

---

### The Core Rule: Don't Use DTOs in Business Logic

**DTOs Are for Data Transfer, Not Business Logic**

DTOs (Data Transfer Objects) are designed for one purpose: **transferring data across boundaries** (e.g., from backend to frontend). They are:
- Flat structures optimized for serialization
- Tied to a specific consumer's needs (REST API, frontend, etc.)
- Not suitable for representing business invariants

```java
// DTO: Flat, for transfer
public class ContactOriginDto {
    private Integer id;
    private String frenchName;
    private String englishName;
    private Integer parentId;  // Just an ID, not a rich object
    private Boolean active;
    // No business methods, no invariants
}

// Domain Entity: Rich, for business logic
public record ContactOrigin(
    ContactOriginId id,
    String frenchName,
    String englishName,
    ContactOriginId parentId,  // Value Object
    boolean isActive
) {
    public ContactOrigin {
        // Invariants enforced here
        if (frenchName == null || frenchName.isBlank()) {
            throw new DomainValidationException("French name required");
        }
    }
    
    public ContactOrigin deactivate() { /* business logic */ }
    public ContactOrigin activate() { /* business logic */ }
}
```

---

### Anti-Pattern: Fetching Query DTOs for Business Logic

A common mistake is to fetch DTOs from Query services (the read side of CQRS) and use them inside Command handlers for business logic. This leads to mixed concerns between business rules and API contracts.

**❌ BAD: Using Query DTOs in Command Handler**

```java
@Service
public class OrderCommandService implements OrderCommand {
    private final CustomerQuery customerQuery;  // ❌ Query service in command handler!
    private final OrderRepository orderRepository;
    
    public OrderDto createOrder(CreateOrderCommand command) {
        // ❌ WRONG: Fetching a DTO designed for API responses
        CustomerDto customerDto = customerQuery.findById(command.getCustomerId())
            .orElseThrow(() -> new NotFoundException("Customer not found"));
        
        // ❌ WRONG: Using DTO fields for business logic
        if (!customerDto.isActive()) {
            throw new InvalidRequestException("Cannot create order for inactive customer");
        }
        if (customerDto.getCreditLimit() < command.getTotalAmount()) {
            throw new InvalidRequestException("Order exceeds credit limit");
        }
        
        // Business logic is now coupled to the DTO structure (API contract)
        // If CustomerDto changes for frontend needs, this business logic breaks!
        Order order = new Order(command.getItems(), customerDto.getId());
        return mapper.toDto(orderRepository.save(order));
    }
}
```

**Why this is problematic:**

1. **Mixed Concerns** — The CustomerDto is shaped for API consumers (frontend), not for business rules. Its structure may include/exclude fields based on presentation needs, not domain needs.

2. **Fragile Business Logic** — If the DTO changes (e.g., `creditLimit` is removed because the frontend no longer needs it), the business logic breaks even though the domain hasn't changed.

3. **Wrong Dependency Direction** — Command handlers (application layer) should depend on domain objects and repositories, not on Query services that return presentation-oriented DTOs.

4. **Testing Complexity** — You now need to mock Query services in Command handler tests, and the mocks must return DTOs with the right shape for business logic to work.

**✅ GOOD: Using Domain Objects for Business Logic**

```java
@Service
public class OrderCommandService implements OrderCommand {
    private final CustomerRepository customerRepository;  // ✅ Repository returns domain entity
    private final OrderRepository orderRepository;
    
    public OrderDto createOrder(CreateOrderCommand command) {
        // ✅ CORRECT: Fetch domain entity from repository
        Customer customer = customerRepository.findById(new CustomerId(command.getCustomerId()))
            .orElseThrow(() -> new NotFoundException("Customer not found"));
        
        // ✅ CORRECT: Business logic uses domain entity
        customer.validateCanPlaceOrder();  // Encapsulated in domain
        customer.validateCreditLimit(command.getTotalAmount());  // Encapsulated in domain
        
        Order order = Order.create(customer, command.getItems());
        return mapper.toDto(orderRepository.save(order));
    }
}
```

---

### What's Acceptable vs. What's Not

| Practice | Acceptable? | Reason |
|----------|-------------|--------|
| **Use Case Handler returns a DTO** | ✅ Yes | Convenience for non-strict CQRS; DTO is just the output format |
| **Query service returns DTOs** | ✅ Yes | Query services are designed for read operations |
| **Fetching Query DTOs to use in Command logic** | ❌ No | Mixes business logic with API contracts |
| **Domain layer imports DTOs** | ❌ Never | Fundamental Clean Architecture violation |
| **Using DTO fields for business decisions** | ❌ No | DTOs are shaped for consumers, not for domain rules |

---

### The Key Insight

**DTOs are contracts with external consumers** (frontend, other services, etc.). They can change based on presentation needs, API versioning, or consumer requirements.

**Domain objects are contracts with business rules**. They should only change when business requirements change.

When you use DTOs for business logic, you create a hidden dependency: **your business rules now depend on your API contract**. This coupling is backwards — the API should depend on the business, not the other way around.

---

### Summary

| Layer | Can Return DTOs? | Can Use DTOs for Logic? |
|-------|------------------|------------------------|
| **Domain** | ❌ Never | ❌ Never |
| **Application (Commands)** | ✅ Acceptable | ❌ No — use domain objects |
| **Application (Queries)** | ✅ Yes | N/A (queries don't have business logic) |
| **Adapters** | ✅ Yes | N/A (adapters don't have business logic) |

**Key Rules:**
1. **Domain layer** must NEVER import or depend on DTOs
2. **Command handlers** CAN return DTOs, but must use **domain objects** for business logic
3. **Don't fetch Query DTOs** in Command handlers — use repositories to get domain entities
4. **Query services** return DTOs (they're designed for read operations)
5. **DTOs are for data transfer**, not for business decisions

---

## CQRS: Separate Command and Query Repositories

**Problem:** In the current codebase, some repositories mix methods that return **domain entities** (for business logic) with methods that return **DTOs/views** (for querying). This violates CQRS principles and creates several issues.

**Example of the Anti-Pattern:**

```java
// ❌ WRONG: Repository mixes domain entities and DTOs
public interface ModificationTrackRepository {
    // Command operations (return domain entities) ✅
    ModificationTrackId insertDetached(ModificationTrack track);
    int linkToPrevious(ModificationTrackId newTrackId, ModificationTrackId oldHeadId);
    Optional<ModificationTrack> findById(ModificationTrackId id);
    
    // Query operation (returns DTO) ❌ — doesn't belong here!
    List<TrackNodeDto> loadChain(ModificationTrackId headId, int maxDepth);
    
    // DTO defined inside the repository ❌
    record TrackNodeDto(long id, long previousId, String title, ...) {}
}
```

---

### Why This Is Problematic

**1. DTOs Can Leak into the Hexagon**

When a repository returns DTOs, those DTOs might be used in business logic:

```java
// ❌ Risk: Using query DTOs for business decisions
@Service
public class SomeCommandService {
    private final ModificationTrackRepository repository;
    
    public void processTrack(ModificationTrackId id) {
        // Developer might use loadChain() for business logic
        List<TrackNodeDto> chain = repository.loadChain(id, 10);
        
        // Now business logic depends on a DTO (API contract)
        if (chain.size() > 5) {
            // ...business decision based on DTO...
        }
    }
}
```

**2. The Repository Contract Is Unclear**

- Looking at `ModificationTrackRepository`, developers can't immediately tell:
  - Which methods are for business operations (commands)?
  - Which methods are for read-only views (queries)?
- The repository has **two different responsibilities**, violating Single Responsibility Principle
- The DTO defined inside the repository interface pollutes the port layer

**3. Different Consumers Have Different Needs**

- **Commands** need rich domain entities with business logic, invariants, and behavior
- **Queries** need flat, optimized DTOs shaped for specific consumers (UI, reports, APIs)
- Mixing them in one interface forces both concerns to evolve together

---

### The CQRS Solution: Separate Repositories

In CQRS (Command Query Responsibility Segregation), we separate:
- **Command Repository** (Write side): Works with domain aggregates/entities
- **Query Repository** (Read side): Returns DTOs/views optimized for consumers

**✅ CORRECT: Separated Repositories**

```java
// ✅ Command Repository — works with domain entities only
public interface ModificationTrackRepository {
    ModificationTrackId insertDetached(ModificationTrack track);
    int linkToPrevious(ModificationTrackId newTrackId, ModificationTrackId oldHeadId);
    Optional<ModificationTrack> findById(ModificationTrackId id);
    // No DTOs here!
}

// ✅ Query Repository — returns DTOs for read operations
public interface ModificationTrackQueryRepository {
    List<TrackNodeDto> loadChain(ModificationTrackId headId, int maxDepth);
    Optional<TrackSummaryDto> findSummaryById(ModificationTrackId id);
    // Only query methods returning DTOs
}

// ✅ DTOs defined in a separate package (application/dto/query/)
public record TrackNodeDto(
    long id,
    long previousId,
    String title,
    String reason,
    Integer createdBy,
    Instant createdAt
) {}
```

**Folder Structure:**

```
modificationtracking/application/
├── port/
│   ├── in/
│   │   ├── command/
│   │   │   └── CreateDetachedModificationTrackUseCase.java
│   │   └── query/
│   │       └── ModificationTrackQuery.java        // Query port interface
│   └── out/
│       ├── ModificationTrackRepository.java       // ✅ Command repository (domain entities)
│       └── ModificationTrackQueryRepository.java  // ✅ Query repository (DTOs)
├── dto/
│   └── query/
│       ├── TrackNodeDto.java
│       └── TrackSummaryDto.java
```

---

### Benefits of Separated Repositories

| Aspect | Mixed Repository | Separated Repositories |
|--------|-----------------|----------------------|
| **Contract clarity** | Unclear what's for business vs. views | Clear: Command repo = domain, Query repo = DTOs |
| **Dependency safety** | DTOs might leak into business logic | Command services only see domain entities |
| **Evolution** | Changes affect both commands and queries | Each side evolves independently |
| **Testing** | Must mock both concerns | Test command logic with domain entities only |
| **Single Responsibility** | ❌ Two responsibilities | ✅ One responsibility per repo |

---

### BONUS: Technology Flexibility

Separating repositories also allows using **different database technologies** for writes vs. reads:

**For Commands (Write Side):**
- **Hibernate/JPA** — works well with domain aggregates
- Handles complex relationships, lazy loading, change tracking
- Optimized for transactional consistency

**For Queries (Read Side):**
- **Pure SQL / JDBC** — simple, fast, no ORM overhead
- **jOOQ** — type-safe SQL with full control
- **Specialized read models** — materialized views, denormalized tables
- **Different databases** — read replicas, search engines (Elasticsearch)

```java
// ✅ Command adapter: Uses Hibernate for rich aggregate management
@Repository
public class JpaModificationTrackRepository implements ModificationTrackRepository {
    private final EntityManager entityManager;
    private final ModificationTrackEntityMapper mapper;
    
    @Override
    public ModificationTrackId insertDetached(ModificationTrack track) {
        // Use Hibernate for domain aggregate persistence
        ModificationTrackEntity entity = mapper.toEntity(track);
        entityManager.persist(entity);
        return new ModificationTrackId(entity.getId());
    }
}

// ✅ Query adapter: Uses pure SQL for optimized reads
@Repository
public class SqlModificationTrackQueryRepository implements ModificationTrackQueryRepository {
    private final JdbcTemplate jdbcTemplate;
    
    @Override
    public List<TrackNodeDto> loadChain(ModificationTrackId headId, int maxDepth) {
        // Use native SQL with recursive CTE for efficient chain loading
        String sql = """
            WITH RECURSIVE chain AS (
                SELECT id, previous_id, title, reason, created_by, created_at, 1 as depth
                FROM modification_track WHERE id = ?
                UNION ALL
                SELECT t.id, t.previous_id, t.title, t.reason, t.created_by, t.created_at, c.depth + 1
                FROM modification_track t
                JOIN chain c ON t.id = c.previous_id
                WHERE c.depth < ?
            )
            SELECT * FROM chain ORDER BY depth
            """;
        return jdbcTemplate.query(sql, this::mapToDto, headId.value(), maxDepth);
    }
}
```

**Why this matters:**

| Operation | Best Tool | Reason |
|-----------|----------|--------|
| **Create aggregate** | Hibernate | ORM handles relationships, cascades, change detection |
| **Update aggregate** | Hibernate | Dirty checking, optimistic locking, unit of work |
| **Load chain for display** | Pure SQL | Recursive CTE, no N+1, returns flat DTO |
| **Search/filter** | SQL / Elasticsearch | Full-text search, complex filters, pagination |
| **Reports** | SQL / Read replica | Aggregations, no impact on write database |

---

### Summary

- **Repositories should have a clear contract**: Either domain entities OR DTOs, not both
- **Command repositories** work with domain aggregates (for business logic)
- **Query repositories** return DTOs (for views/UI/reports)
- **DTOs should not be defined inside repository interfaces** — put them in `dto/query/`
- **Separation enables technology flexibility**: Hibernate for writes, SQL/jOOQ for reads
- **This aligns with CQRS**: Different models for reading and writing

---

## Notes & References

<!-- Links, articles, examples, or other resources -->

- [Clean Architecture by Robert C. Martin](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [Hexagonal Architecture by Alistair Cockburn](https://alistair.cockburn.us/hexagonal-architecture/)

---

*Last updated: 2026-01-30*
