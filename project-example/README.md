# Clean Architecture Project Example Structure (Java)

This directory contains an example folder structure following Clean Architecture principles, based on the reference implementation in `clean_architecture_kotlin`, adapted for Java.

## Module Overview

The project is organized into three main modules:

### 1. clean-architecture-application (The Core)
**Purpose:** Contains all business logic (Domain) and use cases (Application).

**Key Characteristics:**
- No dependencies on external frameworks (Spring, Hibernate, etc.)
- Only standard Java and test libraries (JUnit, AssertJ)
- **Golden Rule:** This module must never depend on infrastructure modules

**Structure:**
```
clean-architecture-application/
├── src/
│   ├── main/java/
│   │   └── {package}/
│   │       ├── socle/                    # Shared kernel (annotations, utilities)
│   │       │   ├── data/                 # Snapshotable interface
│   │       │   ├── dependencyinjection/annotation/  # @UseCase, @Mapper
│   │       │   └── time/                 # DateProvider abstractions
│   │       └── messaging/                # Messaging feature module
│   │           ├── domain/               # Domain layer
│   │           │   ├── entity/           # Domain entities
│   │           │   └── valueobject/      # Value objects
│   │           └── application/          # Application layer
│   │               ├── ports/            # Repository and gateway interfaces
│   │               ├── usecases/         # Use case handlers
│   │               └── dto/              # Data transfer objects
│   ├── test/java/                        # Unit tests
│   │   └── {package}/
│   │       ├── unit/messaging/           # Messaging feature unit tests
│   │       │   ├── testdoubles/repository/  # In-memory repositories
│   │       │   └── fixtures/            # Test fixtures
│   │       └── shared/                   # Shared test utilities
│   └── testFixtures/java/               # Test fixtures shared across modules
```

### 2. clean-architecture-hibernate-adapter (Persistence Adapter)
**Purpose:** Implements repository interfaces using JPA/Hibernate.

**Dependencies:** 
- Depends on `clean-architecture-application`
- Spring Data JPA

**Structure:**
```
clean-architecture-hibernate-adapter/
├── src/
│   ├── main/java/
│   │   └── {package}/
│   │       └── messaging/
│   │           └── jpa/
│   │               ├── entity/           # JPA entities (separate from domain entities)
│   │               ├── mapper/           # MapStruct or manual mappers
│   │               └── repository/       # Repository implementations
│   └── test/java/                        # Integration tests
│       └── {package}/
│           └── integration/              # Integration/E2E tests
```

### 3. clean-architecture-spring-boot-rest-api (Web Adapter)
**Purpose:** REST controllers, application configuration, dependency injection.

**Dependencies:**
- Depends on `clean-architecture-application`
- Depends on `clean-architecture-hibernate-adapter`

**Structure:**
```
clean-architecture-spring-boot-rest-api/
├── src/
│   ├── main/java/
│   │   └── {package}/
│   │       ├── configuration/           # Spring configuration
│   │       └── messaging/
│   │           └── web/
│   │               └── controller/       # REST controllers
│   └── test/java/                        # Integration/E2E tests
│       └── {package}/
│           └── integration/              # API integration tests
```

## Package Naming Convention

**Java Package Naming:**
- Use lowercase letters only (no underscores or hyphens)
- Replace `{package}` with your actual package name (e.g., `com.example` or `com.qima.platform`)
- This example includes the **messaging** feature and **socle** (shared kernel) as reference implementations
- Package names follow Java conventions: lowercase, no underscores (e.g., `dependencyinjection` not `dependency_injection`)

## Key Principles

1. **Dependency Direction:** Dependencies flow inward - adapters depend on the core, never the reverse
2. **Domain Entities:** Rich objects with business behavior, not just data containers
3. **Snapshotable Pattern:** Entities expose immutable data via `data()` method
4. **Value Objects:** Use for validated attributes (e.g., `Email`, `MessageText`)
5. **Ports and Adapters:** Interfaces (ports) in core, implementations (adapters) in infrastructure
6. **Testing:** Unit tests in core (no Spring), integration tests in adapters

## Reference

See `../guidelines/OVERALL_GUIDELINES.md` for detailed coding rules and patterns.

