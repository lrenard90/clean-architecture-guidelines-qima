# Project Roadmap & TODOs

## 1. Example Project
- [ ] Create an example project demonstrating Clean Architecture
- [ ] Add comprehensive comments to highlight each architectural aspect

## 2. Detailed Code Design Guidelines
- [ ] **Application Core**
  - Use Case Handlers
  - Domain Services (when needed)
  - Entities & Aggregates
  - Value Objects
  - Dependency Inversion Principle application
- [ ] **Driving Ports**
  - HTTP Controllers / API Layers
  - CLI / Event Listeners
- [ ] **Driven Ports**
  - Repository Adapters (Persistence)
  - External Services / Third-party integrations

## 3. Detailed Testing Strategy Guidelines
- [ ] **(Sociable) Unit Tests**
  - Use `InMemory` repositories
  - Target Use Cases directly
  - Cover all business rules and cardinalities
- [ ] **End-to-End (E2E) Tests**
  - Target Driving Ports (e.g., REST Controllers)
  - Verify wiring and configuration
  - *Goal: Check integration, not re-test all business logic*
- [ ] **Driven Ports Integration Tests**
  - Repository Adapter tests (e.g., Hibernate/JPA query verification)
  - External Adapter contract tests

