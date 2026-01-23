# QIMA Platform Migration Strategy to Clean Architecture

This document outlines a pragmatic, incremental migration strategy for the `qima-platform` codebase toward Clean Architecture (Hexagonal Style).

---

## 1. Current State Analysis

### Current Architecture

The `qima-platform` backend follows a **traditional layered architecture** with the following structure:

```
backend/api/src/main/java/com/qima/platform/
├── domain/           # JPA entities (tightly coupled to Hibernate)
├── repository/       # Spring Data JPA repositories
├── service/          # Business logic (mixed with infrastructure concerns)
├── web/rest/         # REST controllers
├── config/           # Spring configuration
├── exception/        # Exception classes
├── security/         # Security utilities
└── utils/            # Utility classes
```

### Key Observations

| Aspect | Current State | Target State |
|--------|---------------|--------------|
| **Domain Entities** | JPA entities with `@Entity`, `@Column`, Hibernate annotations | Pure domain objects, framework-agnostic |
| **Business Logic** | Mixed in `@Service` classes with Spring/JPA dependencies | Isolated in `core` module, no framework imports |
| **Repositories** | Spring Data JPA interfaces directly used by services | Ports (interfaces) in core, adapters implement them |
| **Controllers** | Thin, but sometimes contain business logic | Thin layer delegating to Use Case Handlers |
| **Testing** | Heavy reliance on `@SpringBootTest`, mocks | Fast unit tests with in-memory test doubles |
| **DTOs** | Located in `service/dto/` | Located in `core` module |

### Pain Points Identified

1. **Tight Coupling:** Domain entities are JPA entities, making business logic inseparable from persistence
2. **Large Service Classes:** Some services exceed 1000+ lines (e.g., `EntityService`, `PurchaseOrderService`, `UserService`)
3. **Mixed Concerns:** Services contain Spring annotations (`@PreAuthorize`, `@Transactional`, `@Cacheable`) mixed with business logic
4. **Slow Tests:** Testing requires Spring context and database containers
5. **Difficult Refactoring:** Changes to persistence affect business logic and vice versa

### Positive Aspects (Already Moving Toward Clean Architecture)

- **Use Case Handlers exist:** `CreateInspectionUseCaseHandler`, `UpdateInspectionUseCaseHandler` show early adoption
- **Mapper pattern:** MapStruct mappers separate DTO/Entity conversion
- **Modular structure:** Separate modules (`shared`, `batches`, `public-api`, `processes`) indicate modularity awareness

---

## 2. Migration Strategy: The Strangler Fig Pattern

We recommend the **Strangler Fig Pattern**: gradually replace legacy code with clean architecture components while keeping the system functional.

### Guiding Principles

1. **Never break production** — All changes must be backward compatible
2. **Feature-by-feature migration** — Migrate one bounded context at a time
3. **New features in clean architecture** — All new development follows clean architecture
4. **Test-driven migration** — Write tests before refactoring

---

## 3. Migration Phases

### Phase 0: Foundation Setup (1-2 weeks)

**Goal:** Establish the infrastructure for clean architecture without touching existing code.

#### Tasks

1. **Create the `core` module**
   ```
   backend/
   ├── api/           # Existing (becomes adapters over time)
   ├── core/          # NEW: Business logic module
   │   ├── src/main/java/com/qima/platform/core/
   │   │   ├── socle/           # Shared kernel (annotations, utilities)
   │   │   └── {feature}/       # Feature packages
   │   └── src/test/java/
   └── shared/        # Existing
   ```

2. **Add `core` module to Maven**
   ```xml
   <!-- backend/pom.xml -->
   <modules>
     <module>core</module>  <!-- NEW -->
     <module>api</module>
     ...
   </modules>
   ```

3. **Configure `core` module dependencies**
   ```xml
   <!-- backend/core/pom.xml -->
   <dependencies>
     <!-- NO Spring, NO Hibernate, NO JPA -->
     <dependency>
       <groupId>org.junit.jupiter</groupId>
       <artifactId>junit-jupiter</artifactId>
       <scope>test</scope>
     </dependency>
     <dependency>
       <groupId>org.assertj</groupId>
       <artifactId>assertj-core</artifactId>
       <scope>test</scope>
     </dependency>
   </dependencies>
   ```

4. **Create shared kernel (socle)**
   - `@UseCase` annotation
   - `DateProvider` interface
   - Common value objects
   - Domain exceptions

5. **Add ArchUnit rules** to enforce dependency direction
   ```java
   @ArchTest
   static final ArchRule core_should_not_depend_on_spring =
       noClasses().that().resideInAPackage("..core..")
           .should().dependOnClassesThat()
           .resideInAPackage("org.springframework..");
   ```

---

### Phase 1: Pilot Feature Migration (2-4 weeks)

**Goal:** Migrate one small, well-bounded feature to validate the approach.

#### Recommended Pilot: `InspectionType`

**Why this feature?**
- Small scope (1 entity, 1 service, 1 controller)
- Limited dependencies
- Clear business rules
- Already has a simple service (`InspectionTypeService` ~60 lines)

#### Migration Steps

1. **Create domain entity in `core`**
   ```java
   // core/src/main/java/com/qima/platform/core/inspectiontype/domain/entity/InspectionType.java
   public class InspectionType {
       private final InspectionTypeId id;
       private final BrandId brandId;
       private String description;
       private boolean active;
       
       // Business methods
       public void activate() { ... }
       public void deactivate() { ... }
   }
   ```

2. **Define port (repository interface) in `core`**
   ```java
   // core/src/main/java/com/qima/platform/core/inspectiontype/application/port/InspectionTypeRepository.java
   public interface InspectionTypeRepository {
       Optional<InspectionType> findById(InspectionTypeId id);
       List<InspectionType> findAllByBrandId(BrandId brandId);
       InspectionType save(InspectionType inspectionType);
   }
   ```

3. **Create Use Case Handler in `core`**
   ```java
   // core/src/main/java/com/qima/platform/core/inspectiontype/application/usecase/FindInspectionTypesUseCaseHandler.java
   @UseCase
   public class FindInspectionTypesUseCaseHandler {
       private final InspectionTypeRepository repository;
       
       public List<InspectionTypeDTO> handle(FindInspectionTypesRequest request) {
           return repository.findAllByBrandId(request.brandId())
               .stream()
               .map(this::toDTO)
               .toList();
       }
   }
   ```

4. **Write unit tests with in-memory repository**
   ```java
   // core/src/test/java/com/qima/platform/core/inspectiontype/
   class FindInspectionTypesUseCaseHandlerTest {
       private InMemoryInspectionTypeRepository repository = new InMemoryInspectionTypeRepository();
       private FindInspectionTypesUseCaseHandler handler = new FindInspectionTypesUseCaseHandler(repository);
       
       @Test
       void should_return_inspection_types_for_brand() {
           // Given
           repository.save(anInspectionType().withBrandId(brandId).build());
           
           // When
           var result = handler.handle(new FindInspectionTypesRequest(brandId));
           
           // Then
           assertThat(result).hasSize(1);
       }
   }
   ```

5. **Create adapter in `api` module**
   ```java
   // api/src/main/java/com/qima/platform/adapters/driven/persistence/inspectiontype/
   @Repository
   public class JpaInspectionTypeRepositoryAdapter implements InspectionTypeRepository {
       private final InspectionTypeJpaRepository jpaRepository;
       private final InspectionTypeMapper mapper;
       
       @Override
       public Optional<InspectionType> findById(InspectionTypeId id) {
           return jpaRepository.findById(id.value())
               .map(mapper::toDomain);
       }
   }
   ```

6. **Update controller to use Use Case Handler**
   ```java
   @RestController
   @RequestMapping("/api/inspection-types")
   public class InspectionTypeResource {
       private final FindInspectionTypesUseCaseHandler findHandler;
       
       @GetMapping
       public List<InspectionTypeDTO> findAll() {
           var brandId = SecurityUtils.getCurrentUserBrandId();
           return findHandler.handle(new FindInspectionTypesRequest(brandId));
       }
   }
   ```

7. **Keep legacy code working** — The old `InspectionTypeService` can coexist during transition

---

### Phase 2: Core Domain Migration (3-6 months)

**Goal:** Migrate the most critical business domains.

#### Priority Order (based on business value and coupling)

| Priority | Domain | Complexity | Dependencies |
|----------|--------|------------|--------------|
| 1 | **Inspection** | High | Entity, Product, PurchaseOrder, Workflow |
| 2 | **Entity (Factory/Supplier)** | High | Brand, Contact, Address |
| 3 | **Product** | Medium | Brand, Category |
| 4 | **PurchaseOrder** | High | Entity, Product, Inspection |
| 5 | **Workflow** | High | Template, Checklist |
| 6 | **User Management** | Medium | Role, Permission, Brand |

#### For Each Domain

1. **Identify bounded context boundaries**
2. **Extract domain entities** (remove JPA annotations)
3. **Define ports** (repository interfaces, external service interfaces)
4. **Create Use Case Handlers** for each business operation
5. **Write comprehensive unit tests** with in-memory test doubles
6. **Implement adapters** (JPA, external APIs)
7. **Update controllers** to delegate to Use Case Handlers
8. **Deprecate old services** (mark with `@Deprecated`)

---

### Phase 3: Large Service Decomposition (Ongoing)

**Goal:** Break down monolithic services into focused Use Case Handlers.

#### Example: `EntityService` (1271 lines) Decomposition

Current methods in `EntityService`:
- `findAllForCurrentUser()`
- `getOneByNameOrCustomIdAndBrandId()`
- `create()`
- `update()`
- `delete()`
- `findHierarchy()`
- ... (many more)

**Target structure:**
```
core/src/main/java/com/qima/platform/core/entity/application/usecase/
├── FindEntitiesUseCaseHandler.java
├── CreateEntityUseCaseHandler.java
├── UpdateEntityUseCaseHandler.java
├── DeleteEntityUseCaseHandler.java
├── FindEntityHierarchyUseCaseHandler.java
└── ...
```

Each Use Case Handler:
- Single responsibility
- 50-150 lines max
- Independently testable
- Clear input/output DTOs

---

### Phase 4: Testing Strategy Transformation (Parallel to Phases 1-3)

**Goal:** Shift from slow integration tests to fast unit tests.

#### Current Testing Pyramid (Inverted)

```
        /\
       /  \  E2E Tests (slow, many)
      /----\
     /      \ Integration Tests (slow, many)
    /--------\
   /          \ Unit Tests (few)
  /____________\
```

#### Target Testing Pyramid

```
        /\
       /  \  E2E Tests (few, critical paths)
      /----\
     /      \ Integration Tests (adapter verification)
    /--------\
   /          \ Unit Tests (many, fast, behavior-focused)
  /____________\
```

#### Actions

1. **Create in-memory test doubles** for each port
2. **Create test fixtures** (Given-When-Then helpers)
3. **Migrate existing tests** to use in-memory doubles where possible
4. **Keep integration tests** only for adapter verification
5. **Add ArchUnit tests** to prevent architectural drift

---

## 4. Package Structure Target

```
backend/
├── core/
│   └── src/main/java/com/qima/platform/core/
│       ├── socle/
│       │   ├── annotation/UseCase.java
│       │   ├── date/DateProvider.java
│       │   └── exception/DomainException.java
│       ├── inspection/
│       │   ├── domain/
│       │   │   ├── entity/Inspection.java
│       │   │   └── valueobject/InspectionId.java
│       │   └── application/
│       │       ├── port/InspectionRepository.java
│       │       ├── usecase/CreateInspectionUseCaseHandler.java
│       │       └── dto/CreateInspectionRequest.java
│       ├── entity/
│       │   ├── domain/
│       │   └── application/
│       └── product/
│           ├── domain/
│           └── application/
│
├── api/  (becomes adapters module)
│   └── src/main/java/com/qima/platform/
│       ├── adapters/
│       │   ├── driven/
│       │   │   ├── persistence/
│       │   │   │   └── inspection/
│       │   │   │       ├── InspectionJpaEntity.java
│       │   │   │       ├── InspectionJpaRepository.java
│       │   │   │       └── JpaInspectionRepositoryAdapter.java
│       │   │   └── external/
│       │   │       └── storage/CloudStorageAdapter.java
│       │   └── driving/
│       │       └── web/
│       │           └── inspection/InspectionResource.java
│       ├── config/  (Spring configuration)
│       └── security/ (Security adapters)
```

---

## 5. Migration Checklist per Feature

Use this checklist when migrating each feature:

- [ ] **Domain Analysis**
  - [ ] Identify entities and value objects
  - [ ] Define aggregate boundaries
  - [ ] Document business rules

- [ ] **Core Module**
  - [ ] Create domain entity (no framework annotations)
  - [ ] Create value objects
  - [ ] Define repository port (interface)
  - [ ] Define external service ports (if needed)
  - [ ] Create Use Case Handlers
  - [ ] Create request/response DTOs

- [ ] **Testing**
  - [ ] Create in-memory repository implementation
  - [ ] Write unit tests for all Use Case Handlers
  - [ ] Achieve >90% coverage on business logic

- [ ] **Adapters**
  - [ ] Create JPA entity (separate from domain entity)
  - [ ] Create JPA repository adapter
  - [ ] Create mapper (JPA entity ↔ Domain entity)
  - [ ] Update/create REST controller

- [ ] **Integration**
  - [ ] Wire dependencies in Spring configuration
  - [ ] Verify with integration test
  - [ ] Deprecate old service methods

- [ ] **Cleanup**
  - [ ] Remove deprecated code (after stabilization)
  - [ ] Update documentation

---

## 6. Risk Mitigation

| Risk | Mitigation |
|------|------------|
| **Breaking changes** | Feature flags, parallel implementations |
| **Team unfamiliarity** | Training sessions, pair programming, code reviews |
| **Scope creep** | Strict feature boundaries, time-boxed phases |
| **Performance regression** | Benchmark critical paths before/after |
| **Incomplete migration** | Track progress, celebrate milestones |

---

## 7. Success Metrics

| Metric | Current | Target |
|--------|---------|--------|
| Unit test execution time | Minutes | Seconds |
| Test coverage (core logic) | ~60% | >90% |
| Average service class size | 500+ lines | <200 lines |
| Framework imports in business logic | Many | Zero |
| Time to understand a feature | Hours | Minutes |

---

## 8. Getting Started

### Immediate Actions

1. **Create `core` module** with basic structure
2. **Migrate `InspectionType`** as pilot
3. **Document learnings** and adjust strategy
4. **Train team** on clean architecture principles
5. **Establish code review guidelines** for clean architecture

### First Sprint Goals

- [ ] `core` module created and building
- [ ] `@UseCase` annotation and `DateProvider` in socle
- [ ] ArchUnit rules enforcing dependency direction
- [ ] `InspectionType` migrated with full test coverage
- [ ] Team aligned on patterns and conventions

---

## References

- [OVERALL_GUIDELINES.md](./OVERALL_GUIDELINES.md) — Clean Architecture patterns and conventions
- [Backend Coding Guidelines](../../../qima-platform/backend/docs/codingGuidelines.md)
- [Clean Code Guide](../../../qima-platform/backend/docs/cleanCodeGuide.md)
