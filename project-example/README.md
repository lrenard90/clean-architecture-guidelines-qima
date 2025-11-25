# Clean Architecture Project Example (Java)

This directory contains a complete Java example project following Clean Architecture principles, based on the Kotlin reference implementation.

## Project Structure

```
project-example/
├── core/                          # Core module (business logic)
│   ├── src/
│   │   ├── main/java/            # Domain and application layers
│   │   ├── test/java/             # Unit tests
│   │   └── testFixtures/java/     # Test fixtures (builders)
│   └── build.gradle
├── adapters/                      # Adapters module (infrastructure)
│   ├── src/
│   │   ├── main/java/            # Adapter implementations
│   │   │   ├── driven/          # Outbound adapters (persistence)
│   │   │   └── driving/          # Inbound adapters (web)
│   │   └── test/java/             # Integration and E2E tests
│   └── build.gradle
├── build.gradle                   # Root build configuration
├── settings.gradle                # Project settings
└── gradlew                        # Gradle wrapper script
```

## Modules

### Core Module
- **Purpose:** Contains all business logic (Domain) and use cases (Application)
- **Dependencies:** No Spring, Hibernate, or other framework dependencies
- **Features:**
  - Domain entities with business behavior
  - Use case handlers
  - Repository ports (interfaces)
  - Shared kernel (socle) with utilities

### Adapters Module
- **Purpose:** Infrastructure implementations
- **Features:**
  - **Driven (Outbound):** Hibernate persistence adapter
  - **Driving (Inbound):** Spring Boot REST API adapter
  - Integration and E2E tests

## Setup

### Prerequisites
- Java 21 or higher
- Gradle 8.5+ (or use the included wrapper)

### Initialize Gradle Wrapper

If the `gradle-wrapper.jar` is missing, initialize it:

```bash
# If you have Gradle installed
gradle wrapper --gradle-version 8.5

# Or download the wrapper jar manually from:
# https://raw.githubusercontent.com/gradle/gradle/v8.5.0/gradle/wrapper/gradle-wrapper.jar
# and place it in gradle/wrapper/gradle-wrapper.jar
```

**Note:** The `gradlew` and `gradlew.bat` scripts are included. You only need the `gradle-wrapper.jar` file to use the wrapper.

### Build the Project

```bash
./gradlew build
```

### Run Tests

```bash
# Run all tests
./gradlew test

# Run only core tests
./gradlew :core:test

# Run only adapter tests
./gradlew :adapters:test
```

### Run the Application

```bash
./gradlew :adapters:bootRun
```

## Key Features

- **Clean Architecture:** Strict separation between core and infrastructure
- **Hexagonal Architecture:** Driven (outbound) and driving (inbound) adapters
- **Test Strategy:**
  - Unit tests in core (no Spring context)
  - Integration tests in adapters (with TestContainers)
  - E2E tests for API endpoints

## Package Structure

- **Core:** `com.example.cleanarchitectureapplication.*`
- **Adapters:** `adapters.driven.*` and `adapters.driving.*`

## Reference

See `../guidelines/OVERALL_GUIDELINES.md` for detailed coding rules and patterns.

