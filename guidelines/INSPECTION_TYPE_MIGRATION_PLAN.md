# InspectionType Feature Migration Plan

This document provides a detailed, step-by-step plan to migrate the `InspectionType` feature from the current architecture to Clean Architecture.

---

## Current State Analysis

### Existing Components

| Component | Location | Lines | Description |
|-----------|----------|-------|-------------|
| **JPA Entity** | `api/.../domain/InspectionType.java` | 214 | JPA entity with business logic mixed in |
| **Service** | `api/.../service/InspectionTypeService.java` | 62 | Spring service with security annotations |
| **Repository** | `api/.../repository/InspectionTypeRepository.java` | 258 | Spring Data JPA repository with complex queries |

### Identified Business Rules

From analyzing the existing code:

1. **Soft Delete Rule:** QIMA default inspection types cannot be deleted (`softDelete()` method)
2. **Label Validation:** Label must be 1-5 characters, not blank
3. **Description Validation:** Description must be 1-255 characters, not blank
4. **Brand Ownership:** Inspection types belong to a brand (or are global if brand is null)
5. **Default Ratios:** Each inspection type has default produced/packed ratios

### Dependencies

- `Brand` — Many-to-one relationship
- `AqlSetting` — One-to-many relationship (settings per brand)
- `ExpectedRatio` — Embedded value object

### Existing Cucumber Tests

Identify Cucumber scenarios that test InspectionType behavior — these will serve as **safety belt** during migration.

---

## Migration Tasks

### Task 1: Set Up Core Module Structure (Day 1)

**Goal:** Create the package structure in the `core` module.

```
core/src/main/java/com/qima/platform/core/
├── socle/
│   ├── annotation/
│   │   └── UseCase.java
│   ├── Snapshotable.java
│   └── DateProvider.java
└── inspectiontype/
    ├── domain/
    │   ├── entity/
    │   │   └── InspectionType.java
    │   └── valueobject/
    │       ├── Label.java
    │       └── ExpectedRatio.java
    └── application/
        ├── port/
        │   └── InspectionTypeRepository.java
        ├── usecase/
        │   ├── FindInspectionTypesUseCaseHandler.java
        │   ├── FindInspectionTypeByIdUseCaseHandler.java
        │   └── DeleteInspectionTypeUseCaseHandler.java
        └── dto/
            ├── InspectionTypeDTO.java
            └── FindInspectionTypesRequestDTO.java
```

**Files to create:**
1. `@UseCase` annotation
2. `Snapshotable<T>` interface
3. `DateProvider` abstract class

---

### Task 2: Create Domain Entity (Day 1-2)

**Goal:** Create a pure domain entity with business logic, no JPA annotations.

```java
// core/src/main/java/com/qima/platform/core/inspectiontype/domain/entity/InspectionType.java
package com.qima.platform.core.inspectiontype.domain.entity;

import com.qima.platform.core.inspectiontype.domain.valueobject.Label;
import com.qima.platform.core.inspectiontype.domain.valueobject.ExpectedRatio;
import com.qima.platform.core.socle.Snapshotable;

public class InspectionType implements Snapshotable<InspectionTypeData> {
    
    private final Long id;
    private final Long brandId;  // null for QIMA default types
    private final Label label;
    private String description;
    private Integer order;
    private ExpectedRatio defaultRatio;
    private boolean deleted;

    // Constructor for new inspection types
    public InspectionType(Long brandId, Label label, String description, ExpectedRatio defaultRatio) {
        this.id = null;
        this.brandId = brandId;
        this.label = label;
        this.description = description;
        this.defaultRatio = defaultRatio;
        this.order = 1;
        this.deleted = false;
    }

    // Private constructor for reconstruction from data
    private InspectionType(Long id, Long brandId, Label label, String description, 
                           Integer order, ExpectedRatio defaultRatio, boolean deleted) {
        this.id = id;
        this.brandId = brandId;
        this.label = label;
        this.description = description;
        this.order = order;
        this.defaultRatio = defaultRatio;
        this.deleted = deleted;
    }

    // ========== Business Methods ==========

    /**
     * Soft delete this inspection type.
     * @throws IllegalStateException if this is a QIMA default inspection type
     */
    public void softDelete() {
        if (isQimaDefaultType()) {
            throw new IllegalStateException("Cannot delete QIMA default inspection type");
        }
        this.deleted = true;
    }

    public boolean isQimaDefaultType() {
        return brandId == null;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void updateDescription(String newDescription) {
        if (newDescription == null || newDescription.isBlank() || newDescription.length() > 255) {
            throw new IllegalArgumentException("Description must be 1-255 characters");
        }
        this.description = newDescription.trim();
    }

    public void updateDefaultRatio(ExpectedRatio newRatio) {
        this.defaultRatio = newRatio;
    }

    // ========== Snapshotable Implementation ==========

    @Override
    public InspectionTypeData data() {
        return new InspectionTypeData(
            id,
            brandId,
            label.value(),
            description,
            order,
            defaultRatio.produced(),
            defaultRatio.packed(),
            deleted
        );
    }

    public static InspectionType fromData(InspectionTypeData data) {
        return new InspectionType(
            data.id(),
            data.brandId(),
            new Label(data.label()),
            data.description(),
            data.order(),
            new ExpectedRatio(data.producedRatio(), data.packedRatio()),
            data.deleted()
        );
    }

    // ========== Getters ==========

    public Long getId() { return id; }
    public Long getBrandId() { return brandId; }
    public String getLabel() { return label.value(); }
    public String getDescription() { return description; }
    public Integer getOrder() { return order; }
    public ExpectedRatio getDefaultRatio() { return defaultRatio; }
}
```

---

### Task 3: Create Value Objects (Day 2)

**Goal:** Create value objects with validation.

```java
// core/src/main/java/com/qima/platform/core/inspectiontype/domain/valueobject/Label.java
package com.qima.platform.core.inspectiontype.domain.valueobject;

public record Label(String value) {
    public Label {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Label cannot be blank");
        }
        if (value.length() > 5) {
            throw new IllegalArgumentException("Label must be at most 5 characters");
        }
        value = value.trim();
    }
}
```

```java
// core/src/main/java/com/qima/platform/core/inspectiontype/domain/valueobject/ExpectedRatio.java
package com.qima.platform.core.inspectiontype.domain.valueobject;

public record ExpectedRatio(Short produced, Short packed) {
    public ExpectedRatio {
        if (produced != null && (produced < 0 || produced > 100)) {
            throw new IllegalArgumentException("Produced ratio must be between 0 and 100");
        }
        if (packed != null && (packed < 0 || packed > 100)) {
            throw new IllegalArgumentException("Packed ratio must be between 0 and 100");
        }
    }
}
```

---

### Task 4: Create Data Class for Snapshotable (Day 2)

```java
// core/src/main/java/com/qima/platform/core/inspectiontype/domain/entity/InspectionTypeData.java
package com.qima.platform.core.inspectiontype.domain.entity;

public record InspectionTypeData(
    Long id,
    Long brandId,
    String label,
    String description,
    Integer order,
    Short producedRatio,
    Short packedRatio,
    boolean deleted
) {}
```

---

### Task 5: Create Repository Port (Day 2)

**Goal:** Define the repository interface in the core module.

```java
// core/src/main/java/com/qima/platform/core/inspectiontype/application/port/InspectionTypeRepository.java
package com.qima.platform.core.inspectiontype.application.port;

import com.qima.platform.core.inspectiontype.domain.entity.InspectionType;
import java.util.List;
import java.util.Optional;

public interface InspectionTypeRepository {
    
    Optional<InspectionType> findById(Long id);
    
    Optional<InspectionType> findByIdAndBrandId(Long id, Long brandId);
    
    List<InspectionType> findAllByBrandId(Long brandId);
    
    InspectionType save(InspectionType inspectionType);
    
    boolean existsByLabelAndBrandId(String label, Long brandId);
    
    boolean existsByDescriptionAndBrandId(String description, Long brandId);
}
```

---

### Task 6: Create Unit Test Tooling (Day 2-3)

**Goal:** Create builders and in-memory test doubles.

#### 6a. Test Builder

```java
// core/src/test/java/com/qima/platform/core/inspectiontype/InspectionTypeBuilder.java
package com.qima.platform.core.inspectiontype;

import com.qima.platform.core.inspectiontype.domain.entity.InspectionType;
import com.qima.platform.core.inspectiontype.domain.entity.InspectionTypeData;
import com.qima.platform.core.inspectiontype.domain.valueobject.ExpectedRatio;
import com.qima.platform.core.inspectiontype.domain.valueobject.Label;

public class InspectionTypeBuilder {
    private Long id = 1L;
    private Long brandId = 100L;
    private String label = "PSI";
    private String description = "Pre-Shipment Inspection";
    private Integer order = 1;
    private Short producedRatio = (short) 100;
    private Short packedRatio = (short) 0;
    private boolean deleted = false;

    public static InspectionTypeBuilder anInspectionType() {
        return new InspectionTypeBuilder();
    }

    public static InspectionTypeBuilder aQimaDefaultInspectionType() {
        return new InspectionTypeBuilder().withBrandId(null);
    }

    public InspectionTypeBuilder withId(Long id) {
        this.id = id;
        return this;
    }

    public InspectionTypeBuilder withBrandId(Long brandId) {
        this.brandId = brandId;
        return this;
    }

    public InspectionTypeBuilder withLabel(String label) {
        this.label = label;
        return this;
    }

    public InspectionTypeBuilder withDescription(String description) {
        this.description = description;
        return this;
    }

    public InspectionTypeBuilder withOrder(Integer order) {
        this.order = order;
        return this;
    }

    public InspectionTypeBuilder withDefaultRatio(Short produced, Short packed) {
        this.producedRatio = produced;
        this.packedRatio = packed;
        return this;
    }

    public InspectionTypeBuilder deleted() {
        this.deleted = true;
        return this;
    }

    public InspectionType build() {
        return InspectionType.fromData(new InspectionTypeData(
            id, brandId, label, description, order, producedRatio, packedRatio, deleted
        ));
    }
}
```

#### 6b. In-Memory Repository

```java
// core/src/test/java/com/qima/platform/core/inspectiontype/InMemoryInspectionTypeRepository.java
package com.qima.platform.core.inspectiontype;

import com.qima.platform.core.inspectiontype.application.port.InspectionTypeRepository;
import com.qima.platform.core.inspectiontype.domain.entity.InspectionType;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryInspectionTypeRepository implements InspectionTypeRepository {
    
    private final Map<Long, InspectionType> store = new HashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public Optional<InspectionType> findById(Long id) {
        return Optional.ofNullable(store.get(id))
            .filter(it -> !it.isDeleted());
    }

    @Override
    public Optional<InspectionType> findByIdAndBrandId(Long id, Long brandId) {
        return findById(id)
            .filter(it -> Objects.equals(it.getBrandId(), brandId) || it.getBrandId() == null);
    }

    @Override
    public List<InspectionType> findAllByBrandId(Long brandId) {
        return store.values().stream()
            .filter(it -> !it.isDeleted())
            .filter(it -> Objects.equals(it.getBrandId(), brandId) || it.getBrandId() == null)
            .sorted(Comparator.comparing(InspectionType::getOrder)
                .thenComparing(InspectionType::getLabel))
            .toList();
    }

    @Override
    public InspectionType save(InspectionType inspectionType) {
        // Defensive copy
        InspectionType copy = InspectionType.fromData(inspectionType.data());
        
        Long id = copy.getId();
        if (id == null) {
            // Simulate ID generation for new entities
            id = idGenerator.getAndIncrement();
            copy = InspectionType.fromData(new com.qima.platform.core.inspectiontype.domain.entity.InspectionTypeData(
                id,
                copy.data().brandId(),
                copy.data().label(),
                copy.data().description(),
                copy.data().order(),
                copy.data().producedRatio(),
                copy.data().packedRatio(),
                copy.data().deleted()
            ));
        }
        store.put(id, copy);
        return copy;
    }

    @Override
    public boolean existsByLabelAndBrandId(String label, Long brandId) {
        return store.values().stream()
            .filter(it -> !it.isDeleted())
            .filter(it -> Objects.equals(it.getBrandId(), brandId) || it.getBrandId() == null)
            .anyMatch(it -> it.getLabel().equalsIgnoreCase(label));
    }

    @Override
    public boolean existsByDescriptionAndBrandId(String description, Long brandId) {
        return store.values().stream()
            .filter(it -> !it.isDeleted())
            .filter(it -> Objects.equals(it.getBrandId(), brandId) || it.getBrandId() == null)
            .anyMatch(it -> it.getDescription().equalsIgnoreCase(description.trim()));
    }

    // ========== Test Helpers ==========

    public void givenExists(InspectionType inspectionType) {
        store.put(inspectionType.getId(), InspectionType.fromData(inspectionType.data()));
    }

    public InspectionType get(Long id) {
        return store.get(id);
    }

    public void clear() {
        store.clear();
    }

    public int count() {
        return (int) store.values().stream().filter(it -> !it.isDeleted()).count();
    }
}
```

---

### Task 7: Create Use Case Handlers (Day 3-4)

#### 7a. Find All Inspection Types

```java
// core/src/main/java/com/qima/platform/core/inspectiontype/application/usecase/FindInspectionTypesUseCaseHandler.java
package com.qima.platform.core.inspectiontype.application.usecase;

import com.qima.platform.core.inspectiontype.application.dto.FindInspectionTypesRequestDTO;
import com.qima.platform.core.inspectiontype.application.dto.InspectionTypeDTO;
import com.qima.platform.core.inspectiontype.application.port.InspectionTypeRepository;
import com.qima.platform.core.socle.annotation.UseCase;
import java.util.List;

@UseCase
public class FindInspectionTypesUseCaseHandler {
    
    private final InspectionTypeRepository repository;

    public FindInspectionTypesUseCaseHandler(InspectionTypeRepository repository) {
        this.repository = repository;
    }

    public List<InspectionTypeDTO> handle(FindInspectionTypesRequestDTO request) {
        return repository.findAllByBrandId(request.brandId())
            .stream()
            .map(this::toDTO)
            .toList();
    }

    private InspectionTypeDTO toDTO(com.qima.platform.core.inspectiontype.domain.entity.InspectionType entity) {
        return new InspectionTypeDTO(
            entity.getId(),
            entity.getLabel(),
            entity.getDescription(),
            entity.getDefaultRatio().produced(),
            entity.getDefaultRatio().packed()
        );
    }
}
```

#### 7b. Delete Inspection Type

```java
// core/src/main/java/com/qima/platform/core/inspectiontype/application/usecase/DeleteInspectionTypeUseCaseHandler.java
package com.qima.platform.core.inspectiontype.application.usecase;

import com.qima.platform.core.inspectiontype.application.dto.DeleteInspectionTypeRequestDTO;
import com.qima.platform.core.inspectiontype.application.port.InspectionTypeRepository;
import com.qima.platform.core.inspectiontype.domain.entity.InspectionType;
import com.qima.platform.core.socle.annotation.UseCase;

@UseCase
public class DeleteInspectionTypeUseCaseHandler {
    
    private final InspectionTypeRepository repository;

    public DeleteInspectionTypeUseCaseHandler(InspectionTypeRepository repository) {
        this.repository = repository;
    }

    public void handle(DeleteInspectionTypeRequestDTO request) {
        InspectionType inspectionType = repository.findByIdAndBrandId(request.id(), request.brandId())
            .orElseThrow(() -> new IllegalArgumentException(
                "Inspection type not found with id: " + request.id()));

        // Business rule: cannot delete QIMA default types
        inspectionType.softDelete();

        repository.save(inspectionType);
    }
}
```

---

### Task 8: Write Behavior Unit Tests (Day 4-5)

**Goal:** Test business rules with fast unit tests (no Spring context). Tests should be organized using **nested classes** and **descriptive names** to clearly communicate business rules.

**Key Principles:**
- Use `@DisplayName` at class level to describe the feature
- Use `@Nested` classes to group tests by business rule
- Use `@DisplayName` on nested classes to describe rules in plain language
- Test method names should describe the scenario

```java
// core/src/test/java/com/qima/platform/core/inspectiontype/application/usecase/DeleteInspectionTypeUseCaseHandlerTest.java
package com.qima.platform.core.inspectiontype.application.usecase;

import com.qima.platform.core.inspectiontype.InMemoryInspectionTypeRepository;
import com.qima.platform.core.inspectiontype.InspectionTypeBuilder;
import com.qima.platform.core.inspectiontype.application.dto.DeleteInspectionTypeRequestDTO;
import com.qima.platform.core.inspectiontype.domain.entity.InspectionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static com.qima.platform.core.inspectiontype.InspectionTypeBuilder.*;

@DisplayName("Feature: Delete an inspection type")
class DeleteInspectionTypeUseCaseHandlerTest {

    private InMemoryInspectionTypeRepository repository;
    private DeleteInspectionTypeUseCaseHandler handler;

    @BeforeEach
    void setUp() {
        repository = new InMemoryInspectionTypeRepository();
        handler = new DeleteInspectionTypeUseCaseHandler(repository);
    }

    @Test
    void brandCanDeleteCustomInspectionType() {
        // Given
        Long brandId = 100L;
        InspectionType customType = anInspectionType()
            .withId(1L)
            .withBrandId(brandId)
            .withLabel("CUS")
            .build();
        repository.givenExists(customType);

        // When
        handler.handle(new DeleteInspectionTypeRequestDTO(1L, brandId));

        // Then
        assertThat(repository.get(1L).isDeleted()).isTrue();
    }

    @Nested
    class InspectionTypeNotFound {
        @Test
        void cannotDeleteInspectionTypeThatDoesNotExist() {
            // Given
            Long brandId = 100L;
            Long nonExistentId = 999L;

            // When / Then
            assertThatThrownBy(() -> 
                handler.handle(new DeleteInspectionTypeRequestDTO(nonExistentId, brandId)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
        }
    }

    @Nested
    @DisplayName("Rule: QIMA default inspection types cannot be deleted")
    class QimaDefaultTypesProtection {
        @Test
        void cannotDeleteQimaDefaultInspectionType() {
            // Given
            Long brandId = 100L;
            InspectionType qimaDefaultType = aQimaDefaultInspectionType()
                .withId(2L)
                .withLabel("PSI")
                .build();
            repository.givenExists(qimaDefaultType);

            // When / Then
            assertThatThrownBy(() -> 
                handler.handle(new DeleteInspectionTypeRequestDTO(2L, brandId)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot delete QIMA default inspection type");
        }

        @Test
        void cannotDeleteQimaDefaultInspectionTypeEvenWithDifferentLabel() {
            // Given - any inspection type without a brandId is a QIMA default
            Long brandId = 100L;
            InspectionType qimaDefaultType = aQimaDefaultInspectionType()
                .withId(3L)
                .withLabel("DPI")
                .build();
            repository.givenExists(qimaDefaultType);

            // When / Then
            assertThatThrownBy(() -> 
                handler.handle(new DeleteInspectionTypeRequestDTO(3L, brandId)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot delete QIMA default inspection type");
        }
    }
}
```

**Test Output Example:**

When running these tests, the output reads like a specification:

```
Feature: Delete an inspection type
  ✓ brandCanDeleteCustomInspectionType
  InspectionTypeNotFound
    ✓ cannotDeleteInspectionTypeThatDoesNotExist
  Rule: QIMA default inspection types cannot be deleted
    ✓ cannotDeleteQimaDefaultInspectionType
    ✓ cannotDeleteQimaDefaultInspectionTypeEvenWithDifferentLabel
```

**Benefits:**
- Tests serve as **living documentation** of business rules
- When a test fails, the hierarchical name immediately tells you which rule is broken
- Easy to navigate and find tests for specific business rules
- Mirrors the structure of Cucumber scenarios, making migration easier

---

### Task 9: Create JPA Adapter (Day 5-6)

**Goal:** Implement the repository port using the existing JPA infrastructure.

```java
// api/src/main/java/com/qima/platform/adapters/driven/persistence/inspectiontype/JpaInspectionTypeRepositoryAdapter.java
package com.qima.platform.adapters.driven.persistence.inspectiontype;

import com.qima.platform.core.inspectiontype.application.port.InspectionTypeRepository;
import com.qima.platform.core.inspectiontype.domain.entity.InspectionType;
import com.qima.platform.domain.InspectionType as InspectionTypeJpaEntity;
import com.qima.platform.repository.InspectionTypeRepository as InspectionTypeJpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public class JpaInspectionTypeRepositoryAdapter implements InspectionTypeRepository {
    
    private final InspectionTypeJpaRepository jpaRepository;
    private final InspectionTypeMapper mapper;

    public JpaInspectionTypeRepositoryAdapter(
            InspectionTypeJpaRepository jpaRepository,
            InspectionTypeMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<InspectionType> findById(Long id) {
        return jpaRepository.findById(id)
            .map(mapper::toDomain);
    }

    @Override
    public Optional<InspectionType> findByIdAndBrandId(Long id, Long brandId) {
        return jpaRepository.findByIdAndNullableBrandId(id, brandId)
            .map(mapper::toDomain);
    }

    @Override
    public List<InspectionType> findAllByBrandId(Long brandId) {
        return jpaRepository.findAllByBrandId(brandId)
            .stream()
            .map(mapper::toDomain)
            .toList();
    }

    @Override
    public InspectionType save(InspectionType inspectionType) {
        InspectionTypeJpaEntity jpaEntity = mapper.toJpaEntity(inspectionType.data());
        InspectionTypeJpaEntity saved = jpaRepository.save(jpaEntity);
        return mapper.toDomain(saved);
    }

    @Override
    public boolean existsByLabelAndBrandId(String label, Long brandId) {
        return !jpaRepository.isLabelNotExists(label, brandId);
    }

    @Override
    public boolean existsByDescriptionAndBrandId(String description, Long brandId) {
        return !jpaRepository.isDescriptionNotExists(description, brandId);
    }
}
```

---

### Task 10: Create Mapper (Day 6)

```java
// api/src/main/java/com/qima/platform/adapters/driven/persistence/inspectiontype/InspectionTypeMapper.java
package com.qima.platform.adapters.driven.persistence.inspectiontype;

import com.qima.platform.core.inspectiontype.domain.entity.InspectionType;
import com.qima.platform.core.inspectiontype.domain.entity.InspectionTypeData;
import com.qima.platform.domain.InspectionType as InspectionTypeJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class InspectionTypeMapper {

    public InspectionType toDomain(InspectionTypeJpaEntity jpaEntity) {
        return InspectionType.fromData(new InspectionTypeData(
            jpaEntity.getId(),
            jpaEntity.getBrand() != null ? jpaEntity.getBrand().getId() : null,
            jpaEntity.getLabel(),
            jpaEntity.getDescription(),
            jpaEntity.getOrder(),
            jpaEntity.getDefaultProducedRatio(),
            jpaEntity.getDefaultPackedRatio(),
            jpaEntity.isSoftDeleted()
        ));
    }

    public InspectionTypeJpaEntity toJpaEntity(InspectionTypeData data) {
        InspectionTypeJpaEntity entity = new InspectionTypeJpaEntity();
        entity.setId(data.id());
        // Note: Brand relationship needs to be set separately via BrandRepository
        entity.setLabel(data.label());
        entity.setDescription(data.description());
        entity.setDefaultProducedRatio(data.producedRatio());
        entity.setDefaultPackedRatio(data.packedRatio());
        return entity;
    }
}
```

---

### Task 11: Update Controller (Day 6-7)

**Goal:** Update the REST controller to use Use Case Handlers.

```java
// Keep existing controller but delegate to Use Case Handlers
@RestController
@RequestMapping("/api/inspection-types")
public class InspectionTypeResource {
    
    private final FindInspectionTypesUseCaseHandler findHandler;
    private final DeleteInspectionTypeUseCaseHandler deleteHandler;
    private final EntityService entityService; // For security context

    @GetMapping
    @PreAuthorize("hasAnyRole('BRAND', 'SUPERVISOR', 'FACTORY', 'INSPECTOR', 'SERVICE_PROVIDER_INSPECTOR', 'SERVICE_PROVIDER')")
    public List<InspectionTypeDTO> findAll() {
        Long brandId = SecurityUtils.getCurrentUserBrandIdWhenAuthorized(entityService);
        return findHandler.handle(new FindInspectionTypesRequestDTO(brandId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('BRAND', 'SUPERVISOR') and hasAnyAuthority('SETTINGS_EDIT')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        Long brandId = SecurityUtils.getCurrentUserBrandIdWhenAuthorized(entityService);
        deleteHandler.handle(new DeleteInspectionTypeRequestDTO(id, brandId));
    }
}
```

---

### Task 12: Run Cucumber Tests as Safety Belt (Day 7)

**Goal:** Verify no regression by running existing Cucumber tests.

1. Run all Cucumber scenarios related to InspectionType
2. All tests should pass without modification
3. Document any failures and fix them

---

### Task 13: Retire Redundant Cucumber Tests (Day 8+)

**Goal:** Once unit tests cover business rules, reduce E2E test scope.

1. Identify Cucumber scenarios that duplicate unit test coverage
2. Keep only integration verification scenarios (wiring, HTTP, DB)
3. Remove redundant scenarios

---

## Checklist

| # | Task | Status | Notes |
|---|------|--------|-------|
| 1 | Set up core module structure | ⬜ | |
| 2 | Create domain entity | ⬜ | |
| 3 | Create value objects (Label, ExpectedRatio) | ⬜ | |
| 4 | Create data class (InspectionTypeData) | ⬜ | |
| 5 | Create repository port | ⬜ | |
| 6 | Create test builder | ⬜ | |
| 7 | Create in-memory repository | ⬜ | |
| 8 | Create FindInspectionTypesUseCaseHandler | ⬜ | |
| 9 | Create DeleteInspectionTypeUseCaseHandler | ⬜ | |
| 10 | Write unit tests for use cases | ⬜ | |
| 11 | Create JPA adapter | ⬜ | |
| 12 | Create mapper | ⬜ | |
| 13 | Update controller | ⬜ | |
| 14 | Run Cucumber tests (safety belt) | ⬜ | |
| 15 | Retire redundant Cucumber tests | ⬜ | |

---

## Estimated Timeline

| Phase | Duration | Tasks |
|-------|----------|-------|
| **Setup & Domain** | Days 1-2 | Tasks 1-5 |
| **Test Tooling** | Days 2-3 | Tasks 6-7 |
| **Use Cases & Tests** | Days 3-5 | Tasks 8-10 |
| **Adapters & Integration** | Days 5-7 | Tasks 11-13 |
| **Validation & Cleanup** | Days 7-8 | Tasks 14-15 |

**Total: ~8 working days (2 weeks with buffer)**

---

## Success Criteria

1. ✅ All business rules tested with fast unit tests in `core/src/test`
2. ✅ No Spring/JPA dependencies in `core` module
3. ✅ All existing Cucumber tests pass (safety belt)
4. ✅ REST API behavior unchanged
5. ✅ Redundant Cucumber tests identified and retired
