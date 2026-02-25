# Incident Management API - Developer Guide

## Project Overview and Purpose

The Incident Management API is a comprehensive REST service for managing IT incidents with AI-powered analysis capabilities. It provides automated incident classification, severity assessment, solution suggestions, and similarity search to help operations teams efficiently handle and resolve incidents.

### Key Features
- **CRUD Operations**: Complete incident lifecycle management
- **AI-Powered Analysis**: Automatic severity, category, and solution classification using OpenAI
- **Similarity Search**: Find related incidents using keyword matching
- **Metrics & Analytics**: Comprehensive incident statistics and KPIs
- **Production Ready**: Full deployment configuration for Railway platform

## Tech Stack and Architecture

### Core Technologies
- **Java 21**: Latest LTS version for performance and modern language features
- **Spring Boot 3.4.2**: Framework for rapid application development
- **Spring Data JPA**: Data persistence layer with Hibernate
- **PostgreSQL**: Primary database for production
- **H2**: In-memory database for testing
- **Spring AI**: AI integration framework for LLM interactions
- **OpenAI GPT-4o-mini**: Cost-effective model for incident analysis

### Architecture Pattern
The project follows a **layered architecture** pattern:

```
┌─────────────────────────────────────┐
│           Controller Layer          │  ← REST endpoints, request/response handling
├─────────────────────────────────────┤
│            Service Layer            │  ← Business logic, AI integration
├─────────────────────────────────────┤
│          Repository Layer           │  ← Data access, JPA repositories
├─────────────────────────────────────┤
│             Model Layer             │  ← Entity definitions, DTOs
└─────────────────────────────────────┘
```

### Package Structure
```
src/main/java/com/victorlopez/incident_api/
├── controller/          # REST controllers
├── service/            # Business logic layer
├── repository/         # Data access layer
├── model/              # JPA entities and enums
├── dto/                # Data Transfer Objects
├── config/             # Configuration classes
└── exception/          # Custom exceptions and handlers
```

## Code Conventions and Patterns

### 1. Builder Pattern
All entities and DTOs use the **Builder pattern** via Lombok's `@Builder`:

```java
@Entity
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Incident {
    // Entity implementation
}

// Usage
Incident incident = Incident.builder()
    .title("Database timeout")
    .description("Connection timeout in production")
    .severity(Severity.HIGH)
    .build();
```

### 2. Repository Pattern
Data access follows Spring Data JPA conventions with custom queries when needed:

```java
@Repository
public interface IncidentRepository extends JpaRepository<Incident, UUID> {
    List<Incident> findByStatus(Status status);
    
    @Query("SELECT i FROM Incident i WHERE i.severity = :severity")
    List<Incident> findBySeverity(@Param("severity") Severity severity);
}
```

### 3. Service Layer Pattern
Business logic is encapsulated in service classes with clear responsibility separation:

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class IncidentService {
    private final IncidentRepository incidentRepository;
    private final AIAnalysisService aiAnalysisService;
    
    // Business methods with transaction management
}
```

### 4. DTO Mapping Pattern
All external communication uses DTOs to avoid exposing internal entity structure:

```java
// Always map entities to responses
private IncidentResponse mapToResponse(Incident incident) {
    return IncidentResponse.builder()
        .id(incident.getId())
        .title(incident.getTitle())
        // ... other fields
        .build();
}
```

### 5. Test-Driven Development (TDD)
The project follows TDD principles:
- **Repository tests**: Test data access layer
- **Service tests**: Test business logic with mocked dependencies
- **Controller tests**: Test REST endpoints with MockMvc

## Running the Project Locally

### Prerequisites
- Java 21
- Maven 3.8+
- PostgreSQL (optional, H2 used by default for development)
- OpenAI API Key

### Environment Setup
1. Clone the repository:
   ```bash
   git clone https://github.com/lopezviktor/incident-management-api.git
   cd incident-management-api
   ```

2. Set required environment variables:
   ```bash
   export OPENAI_API_KEY=your_api_key_here
   # Optional for local development
   export POSTGRES_DB=incident_db
   export POSTGRES_USER=postgres
   export POSTGRES_PASSWORD=password
   ```

3. Run the application:
   ```bash
   ./mvnw spring-boot:run
   ```

4. Access the application:
   - **API**: http://localhost:8080
   - **Swagger UI**: http://localhost:8080/swagger-ui.html
   - **Health Check**: http://localhost:8080/actuator/health

## Running Tests

### All Tests
```bash
./mvnw test
```

### Specific Test Categories
```bash
# Repository tests only
./mvnw test -Dtest="*RepositoryTest"

# Service tests only
./mvnw test -Dtest="*ServiceTest"

# Controller tests only
./mvnw test -Dtest="*ControllerTest"
```

### Test Configuration
- Tests use H2 in-memory database
- AI services are mocked in tests
- Seed data initialization is disabled in test profile

## Key Design Decisions

### Why Spring AI?
- **Unified abstraction**: Consistent interface across different LLM providers
- **Spring integration**: Seamless integration with Spring Boot ecosystem
- **Type safety**: Strongly typed responses and configurations
- **Future-proofing**: Easy to switch between AI providers

### Why GPT-4o-mini?
- **Cost-effective**: Significantly cheaper than GPT-4 for classification tasks
- **Performance**: Fast response times suitable for real-time incident processing
- **Accuracy**: Sufficient accuracy for incident categorization and severity assessment
- **Token efficiency**: Optimized for shorter, structured outputs

### Why UUID for Primary Keys?
- **Distributed systems**: UUIDs work well across multiple instances
- **Security**: Non-sequential IDs prevent enumeration attacks
- **Merging**: Safe merging of data from different sources

### Why PostgreSQL?
- **JSON support**: Native JSON columns for future extensibility
- **Full-text search**: Built-in search capabilities for incident descriptions
- **ACID compliance**: Strong consistency for critical incident data
- **Scalability**: Excellent performance characteristics

## Adding New Features

### Following Existing Patterns

When adding new features, follow these established patterns:

#### 1. New Entity Creation
```java
@Entity
@Table(name = "your_entities")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class YourEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    // Other fields...
}
```

#### 2. Repository Layer
```java
@Repository
public interface YourEntityRepository extends JpaRepository<YourEntity, UUID> {
    // Spring Data JPA method queries
    List<YourEntity> findByStatus(Status status);
    
    // Custom JPQL queries when needed
    @Query("SELECT e FROM YourEntity e WHERE e.customField = :value")
    List<YourEntity> findByCustomCriteria(@Param("value") String value);
}
```

#### 3. Service Layer
```java
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class YourEntityService {
    private final YourEntityRepository repository;
    
    public YourEntityResponse create(CreateYourEntityRequest request) {
        log.info("Creating new entity: {}", request.getName());
        
        YourEntity entity = YourEntity.builder()
            .name(request.getName())
            // Map other fields...
            .build();
            
        YourEntity saved = repository.save(entity);
        return mapToResponse(saved);
    }
    
    private YourEntityResponse mapToResponse(YourEntity entity) {
        return YourEntityResponse.builder()
            .id(entity.getId())
            .name(entity.getName())
            .createdAt(entity.getCreatedAt())
            .build();
    }
}
```

#### 4. Controller Layer
```java
@RestController
@RequestMapping("/api/your-entities")
@RequiredArgsConstructor
@Tag(name = "Your Entity Management", description = "APIs for managing your entities")
public class YourEntityController {
    private final YourEntityService service;
    
    @PostMapping
    @Operation(summary = "Create new entity", description = "Creates a new entity with the provided data")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Entity created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    public ResponseEntity<YourEntityResponse> create(
            @Valid @RequestBody CreateYourEntityRequest request) {
        YourEntityResponse response = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
```

#### 5. Testing Pattern
```java
@DataJpaTest
class YourEntityRepositoryTest {
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private YourEntityRepository repository;
    
    @Test
    @DisplayName("Should find entities by status")
    void shouldFindEntitiesByStatus() {
        // ARRANGE
        YourEntity entity = YourEntity.builder()
            .name("Test Entity")
            .status(Status.ACTIVE)
            .build();
        entityManager.persistAndFlush(entity);
        
        // ACT
        List<YourEntity> results = repository.findByStatus(Status.ACTIVE);
        
        // ASSERT
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("Test Entity");
    }
}
```

### Code Quality Guidelines

1. **Use Lombok annotations**: `@Data`, `@Builder`, `@RequiredArgsConstructor`
2. **Add logging**: Use `@Slf4j` and log important operations
3. **Validate inputs**: Use `@Valid` and Bean Validation annotations
4. **Document APIs**: Add OpenAPI annotations to all endpoints
5. **Write tests**: Follow TDD approach with comprehensive test coverage
6. **Handle exceptions**: Use global exception handler for consistent error responses
7. **Use transactions**: Add `@Transactional` to service methods that modify data

### Configuration Management

- **Environment variables**: All configuration should be externalized
- **Default values**: Provide sensible defaults for optional configurations
- **Profiles**: Use Spring profiles for different environments (dev, test, prod)
- **Documentation**: Update `application.properties` comments when adding new configs

### Database Changes

- **Migrations**: Use Hibernate's `ddl-auto=update` for development, proper migrations for production
- **Indexing**: Add database indexes for frequently queried fields
- **Constraints**: Define proper database constraints and validation rules

This guide should help maintain consistency and quality when extending the Incident Management API. Always refer to existing implementations as examples and follow the established patterns.