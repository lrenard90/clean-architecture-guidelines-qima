# clean-architecture-spring-boot-rest-api Module (Java)

Web adapter providing REST API endpoints.

## Directory Structure

### `src/main/java/{package}/configuration/`
**Spring Configuration** - Application setup and dependency injection.

- `CustomAnnotationScanConfiguration`: Scans core packages for `@UseCase` and `@Mapper` annotations
- Spring Boot application class with `@SpringBootApplication`
- Configuration classes with `@Configuration`

### `src/main/java/{package}/{feature}/web/controller/`
**REST Controllers** - HTTP endpoints.

- Annotated with `@RestController`
- Inject use case handlers (not repositories directly) via constructor
- Handle HTTP requests/responses
- Convert between HTTP DTOs and application DTOs
- Use `@RequestMapping` or `@GetMapping`, `@PostMapping`, etc.

### `src/test/java/{package}/integration/`
**Integration/E2E Tests** - Test API endpoints.

- Use `@SpringBootTest`
- Use TestContainers for database
- Use MockMvc or RestAssured for HTTP testing
- Validate complete request/response flow
- Use `@AutoConfigureMockMvc` for MockMvc

## Java-Specific Rules

- ✅ Depends on `clean-architecture-application`
- ✅ Depends on `clean-architecture-hibernate-adapter`
- ✅ Controllers inject use case handlers, not repositories
- ✅ Use custom configuration to scan core packages
- ✅ Never put business logic in controllers
- ✅ Use `@RestController` and `@RequestMapping` annotations
- ✅ Use constructor injection for dependencies
- ✅ Return ResponseEntity for proper HTTP status codes

## Java Code Examples

### REST Controller
```java
@RestController
@RequestMapping("/api/messages")
public class MessageController {
    private final PostMessageUseCaseHandler postMessageUseCaseHandler;
    private final ViewTimelineUseCaseHandler viewTimelineUseCaseHandler;
    
    public MessageController(
            PostMessageUseCaseHandler postMessageUseCaseHandler,
            ViewTimelineUseCaseHandler viewTimelineUseCaseHandler) {
        this.postMessageUseCaseHandler = postMessageUseCaseHandler;
        this.viewTimelineUseCaseHandler = viewTimelineUseCaseHandler;
    }
    
    @PostMapping
    public ResponseEntity<Void> postMessage(@RequestBody PostMessageRequestDTO request) {
        postMessageUseCaseHandler.handle(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
    
    @GetMapping("/timeline")
    public ResponseEntity<List<TimelineMessageDTO>> getTimeline(@RequestParam String userId) {
        GetTimelineRequestDTO request = new GetTimelineRequestDTO(userId);
        List<TimelineMessageDTO> timeline = viewTimelineUseCaseHandler.handle(request);
        return ResponseEntity.ok(timeline);
    }
}
```

### Configuration
```java
@Configuration
public class CustomAnnotationScanConfiguration {
    @Bean
    public CustomAnnotationBeanPostProcessor customAnnotationBeanPostProcessor() {
        return new CustomAnnotationBeanPostProcessor();
    }
}
```

### Integration Test
```java
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MessageControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void shouldPostMessage() throws Exception {
        PostMessageRequestDTO request = new PostMessageRequestDTO(UUID.randomUUID(), "author", "text");
        
        mockMvc.perform(post("/api/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }
}
```
