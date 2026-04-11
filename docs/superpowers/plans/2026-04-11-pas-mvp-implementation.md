# Pas MVP Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the first usable Pas MVP: a teacher-facing flow that accepts Java algorithm code, auto-detects supported algorithms, generates a deterministic visualization payload with AI-written teaching copy, and plays the result in the Vue frontend.

**Architecture:** Use the existing Spring Boot app as the single backend for task orchestration, persistence, deterministic algorithm state extraction, billing, and AI integration. Use the existing Vue app as a single-page client for code submission and guided playback. Keep the system honest by separating AI-written narration from deterministic execution state.

**Tech Stack:** Spring Boot 3.5, Java 21, Spring Web, Spring Data JPA, H2 (development DB), WebClient, Vue 3, TypeScript, Vite, Vitest

---

> This workspace is not currently a git repository. Run `git init` at `E:\CodeHome\LogicProjector` before executing any commit steps below.

## File Structure

### Backend files

- Modify: `SpringBackend/pom.xml`
  Adds JPA, validation, H2, and reactive client dependencies.
- Modify: `SpringBackend/src/main/resources/application.yml`
  Adds datasource, JPA, and AI configuration.
- Create: `SpringBackend/src/main/java/com/LogicProjector/account/UserAccount.java`
  JPA entity for teachers and credit balance.
- Create: `SpringBackend/src/main/java/com/LogicProjector/account/UserAccountRepository.java`
  Persistence access for users.
- Create: `SpringBackend/src/main/java/com/LogicProjector/account/DemoUserSeeder.java`
  Seeds one teacher account for MVP testing.
- Create: `SpringBackend/src/main/java/com/LogicProjector/generation/GenerationTask.java`
  Core task entity for each code submission.
- Create: `SpringBackend/src/main/java/com/LogicProjector/generation/GenerationTaskRepository.java`
  Persistence access for tasks.
- Create: `SpringBackend/src/main/java/com/LogicProjector/generation/GenerationTaskStatus.java`
  Task status enum.
- Create: `SpringBackend/src/main/java/com/LogicProjector/generation/api/CreateGenerationTaskRequest.java`
  Request payload for new generation tasks.
- Create: `SpringBackend/src/main/java/com/LogicProjector/generation/api/GenerationTaskResponse.java`
  Response payload for task retrieval.
- Create: `SpringBackend/src/main/java/com/LogicProjector/generation/GenerationTaskController.java`
  REST endpoints for create and read task flows.
- Create: `SpringBackend/src/main/java/com/LogicProjector/generation/GenerationTaskService.java`
  Orchestrates analysis, state extraction, narration, billing, and logging.
- Create: `SpringBackend/src/main/java/com/LogicProjector/analysis/DetectedAlgorithm.java`
  Enum for supported algorithms.
- Create: `SpringBackend/src/main/java/com/LogicProjector/analysis/RecognitionResult.java`
  Structured recognition result with confidence.
- Create: `SpringBackend/src/main/java/com/LogicProjector/analysis/AiCodeAnalysisClient.java`
  Interface for AI-backed recognition and narration.
- Create: `SpringBackend/src/main/java/com/LogicProjector/analysis/OpenAiCompatibleCodeAnalysisClient.java`
  Real HTTP client for an OpenAI-compatible chat endpoint.
- Create: `SpringBackend/src/main/java/com/LogicProjector/analysis/AlgorithmRecognitionService.java`
  Converts AI response into supported algorithm decisions.
- Create: `SpringBackend/src/main/java/com/LogicProjector/analysis/AlgorithmRecognitionServiceImpl.java`
  Confidence-gated recognition logic.
- Create: `SpringBackend/src/main/java/com/LogicProjector/analysis/UnsupportedAlgorithmException.java`
  Raised when confidence is too low or the algorithm is not supported.
- Create: `SpringBackend/src/main/java/com/LogicProjector/visualization/VisualizationStep.java`
  One timeline step for playback.
- Create: `SpringBackend/src/main/java/com/LogicProjector/visualization/VisualizationPayload.java`
  Serialized payload returned to the frontend.
- Create: `SpringBackend/src/main/java/com/LogicProjector/visualization/VisualizationStateExtractor.java`
  Interface for deterministic state generation.
- Create: `SpringBackend/src/main/java/com/LogicProjector/visualization/VisualizationStateExtractorFactory.java`
  Chooses the extractor by algorithm.
- Create: `SpringBackend/src/main/java/com/LogicProjector/visualization/SortingExtractors.java`
  Deterministic extractors for bubble, selection, and insertion sort.
- Create: `SpringBackend/src/main/java/com/LogicProjector/visualization/SearchAndDivideExtractors.java`
  Deterministic extractors for binary search, quick sort, and merge sort.
- Create: `SpringBackend/src/main/java/com/LogicProjector/visualization/NarrationResult.java`
  AI-written summary and per-step labels.
- Create: `SpringBackend/src/main/java/com/LogicProjector/visualization/NarrationService.java`
  Maps recognized algorithm and steps to teaching copy.
- Create: `SpringBackend/src/main/java/com/LogicProjector/billing/BillingRecord.java`
  JPA entity for credit deltas.
- Create: `SpringBackend/src/main/java/com/LogicProjector/billing/BillingRecordRepository.java`
  Persistence access for billing.
- Create: `SpringBackend/src/main/java/com/LogicProjector/billing/BillingService.java`
  Applies simple credit charges.
- Create: `SpringBackend/src/main/java/com/LogicProjector/systemlog/SystemLogEntry.java`
  JPA entity for operational logs.
- Create: `SpringBackend/src/main/java/com/LogicProjector/systemlog/SystemLogEntryRepository.java`
  Persistence access for logs.
- Create: `SpringBackend/src/main/java/com/LogicProjector/systemlog/SystemLogService.java`
  Writes structured logs from generation flow.
- Test: `SpringBackend/src/test/java/com/LogicProjector/generation/GenerationTaskControllerTest.java`
  API tests for create and fetch task flows.
- Test: `SpringBackend/src/test/java/com/LogicProjector/analysis/AlgorithmRecognitionServiceTest.java`
  Recognition tests for supported and unsupported inputs.
- Test: `SpringBackend/src/test/java/com/LogicProjector/visualization/VisualizationStateExtractorTest.java`
  Deterministic step-generation tests.

### Frontend files

- Modify: `VueFrontend/package.json`
  Adds Vitest and Vue test utilities.
- Create: `VueFrontend/vitest.config.ts`
  Test runner config.
- Create: `VueFrontend/src/types/pas.ts`
  Shared TypeScript types for backend payloads.
- Create: `VueFrontend/src/api/pasApi.ts`
  Thin fetch wrapper for create and fetch task endpoints.
- Create: `VueFrontend/src/components/CodeSubmissionPanel.vue`
  Java code input form and submit action.
- Create: `VueFrontend/src/components/PlaybackControls.vue`
  Play, pause, next, previous, and jump controls.
- Create: `VueFrontend/src/components/VisualizationStage.vue`
  Main stage for array and pointer visualization.
- Create: `VueFrontend/src/components/CodeHighlightPanel.vue`
  Source viewer with highlighted line numbers.
- Create: `VueFrontend/src/components/ExplanationPanel.vue`
  Current step title and narration.
- Create: `VueFrontend/src/components/TaskSummaryCard.vue`
  Displays recognized algorithm, confidence, and charge.
- Modify: `VueFrontend/src/App.vue`
  Composes the end-to-end page.
- Modify: `VueFrontend/src/style.css`
  Replaces the scaffold hero with product UI styling.
- Test: `VueFrontend/src/components/VisualizationStage.spec.ts`
  Verifies array rendering for a known payload.
- Test: `VueFrontend/src/App.spec.ts`
  Verifies the app transitions from submit state to playback state.

## Task 1: Backend Foundation And Persistence Bootstrap

**Files:**
- Modify: `SpringBackend/pom.xml`
- Modify: `SpringBackend/src/main/resources/application.yml`
- Create: `SpringBackend/src/main/java/com/LogicProjector/account/UserAccount.java`
- Create: `SpringBackend/src/main/java/com/LogicProjector/account/UserAccountRepository.java`
- Create: `SpringBackend/src/main/java/com/LogicProjector/account/DemoUserSeeder.java`
- Create: `SpringBackend/src/main/java/com/LogicProjector/generation/GenerationTask.java`
- Create: `SpringBackend/src/main/java/com/LogicProjector/generation/GenerationTaskStatus.java`
- Create: `SpringBackend/src/main/java/com/LogicProjector/generation/GenerationTaskRepository.java`
- Create: `SpringBackend/src/main/java/com/LogicProjector/billing/BillingRecord.java`
- Create: `SpringBackend/src/main/java/com/LogicProjector/billing/BillingRecordRepository.java`
- Create: `SpringBackend/src/main/java/com/LogicProjector/systemlog/SystemLogEntry.java`
- Create: `SpringBackend/src/main/java/com/LogicProjector/systemlog/SystemLogEntryRepository.java`
- Test: `SpringBackend/src/test/java/com/LogicProjector/generation/GenerationTaskPersistenceTest.java`

- [ ] **Step 1: Write the failing persistence test**

```java
package com.LogicProjector.generation;

import static org.assertj.core.api.Assertions.assertThat;

import com.LogicProjector.account.UserAccount;
import com.LogicProjector.account.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class GenerationTaskPersistenceTest {

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private GenerationTaskRepository generationTaskRepository;

    @Test
    void shouldPersistTaskForTeacher() {
        UserAccount user = userAccountRepository.save(new UserAccount(null, "teacher@example.com", 200, "ACTIVE"));

        GenerationTask task = generationTaskRepository.save(
                GenerationTask.pending(user, "public class Demo {}", "java")
        );

        assertThat(task.getId()).isNotNull();
        assertThat(task.getStatus()).isEqualTo(GenerationTaskStatus.PENDING);
        assertThat(task.getUser().getEmail()).isEqualTo("teacher@example.com");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -Dtest=GenerationTaskPersistenceTest test`
Expected: FAIL with missing JPA dependencies or missing entity/repository classes.

- [ ] **Step 3: Write the minimal persistence implementation**

```xml
<!-- SpringBackend/pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

```yaml
# SpringBackend/src/main/resources/application.yml
spring:
  application:
    name: SpringBackend
  datasource:
    url: jdbc:h2:file:./data/pas-mvp;MODE=PostgreSQL;AUTO_SERVER=TRUE
    username: sa
    password:
  jpa:
    hibernate:
      ddl-auto: update
    open-in-view: false
  h2:
    console:
      enabled: true

server:
  port: 8080

pas:
  ai:
    base-url: https://api.openai.com
    model: gpt-4o-mini
    confidence-threshold: 0.8
```

```java
// SpringBackend/src/main/java/com/LogicProjector/account/UserAccount.java
@Entity
@Table(name = "users")
public class UserAccount {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String email;
    @Column(nullable = false)
    private Integer creditsBalance;
    @Column(nullable = false)
    private String status;

    protected UserAccount() {}

    public UserAccount(Long id, String email, Integer creditsBalance, String status) {
        this.id = id;
        this.email = email;
        this.creditsBalance = creditsBalance;
        this.status = status;
    }

    public String getEmail() {
        return email;
    }

    public Integer getCreditsBalance() {
        return creditsBalance;
    }

    public void debitCredits(int amount) {
        this.creditsBalance = this.creditsBalance - amount;
    }
}
```

```java
// SpringBackend/src/main/java/com/LogicProjector/generation/GenerationTask.java
@Entity
@Table(name = "generation_tasks")
public class GenerationTask {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false)
    private UserAccount user;
    @Lob
    @Column(nullable = false)
    private String sourceCode;
    @Column(nullable = false)
    private String language;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GenerationTaskStatus status;

    public static GenerationTask pending(UserAccount user, String sourceCode, String language) {
        GenerationTask task = new GenerationTask();
        task.user = user;
        task.sourceCode = sourceCode;
        task.language = language;
        task.status = GenerationTaskStatus.PENDING;
        return task;
    }

    public Long getId() {
        return id;
    }

    public UserAccount getUser() {
        return user;
    }

    public GenerationTaskStatus getStatus() {
        return status;
    }
}
```

```java
// SpringBackend/src/main/java/com/LogicProjector/account/DemoUserSeeder.java
@Component
class DemoUserSeeder implements ApplicationRunner {
    private final UserAccountRepository repository;

    DemoUserSeeder(UserAccountRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(ApplicationArguments args) {
        repository.findByEmail("teacher@example.com")
                .orElseGet(() -> repository.save(new UserAccount(null, "teacher@example.com", 300, "ACTIVE")));
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -Dtest=GenerationTaskPersistenceTest test`
Expected: PASS with one persisted `users` row and one persisted `generation_tasks` row in H2.

- [ ] **Step 5: Commit**

```bash
git add SpringBackend/pom.xml SpringBackend/src/main/resources/application.yml SpringBackend/src/main/java/com/LogicProjector/account SpringBackend/src/main/java/com/LogicProjector/generation SpringBackend/src/main/java/com/LogicProjector/billing SpringBackend/src/main/java/com/LogicProjector/systemlog SpringBackend/src/test/java/com/LogicProjector/generation/GenerationTaskPersistenceTest.java
git commit -m "feat: bootstrap pas persistence model"
```

## Task 2: Task API Skeleton And Generation Lifecycle

**Files:**
- Create: `SpringBackend/src/main/java/com/LogicProjector/generation/api/CreateGenerationTaskRequest.java`
- Create: `SpringBackend/src/main/java/com/LogicProjector/generation/api/GenerationTaskResponse.java`
- Create: `SpringBackend/src/main/java/com/LogicProjector/generation/GenerationTaskController.java`
- Create: `SpringBackend/src/main/java/com/LogicProjector/generation/GenerationTaskService.java`
- Test: `SpringBackend/src/test/java/com/LogicProjector/generation/GenerationTaskControllerTest.java`

- [ ] **Step 1: Write the failing controller test**

```java
@WebMvcTest(GenerationTaskController.class)
class GenerationTaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GenerationTaskService generationTaskService;

    @Test
    void shouldCreatePendingTask() throws Exception {
        GenerationTaskResponse response = new GenerationTaskResponse(
                42L, "PENDING", "java", null, null, 0.0, null, null, 0
        );

        given(generationTaskService.createTask(any())).willReturn(response);

        mockMvc.perform(post("/api/generation-tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":1,"sourceCode":"class Demo {}","language":"java"}
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -Dtest=GenerationTaskControllerTest test`
Expected: FAIL because request/response DTOs, controller, and service do not exist.

- [ ] **Step 3: Write the minimal API skeleton**

```java
// CreateGenerationTaskRequest.java
public record CreateGenerationTaskRequest(
        @NotNull Long userId,
        @NotBlank String sourceCode,
        @NotBlank String language
) {}
```

```java
// GenerationTaskResponse.java
public record GenerationTaskResponse(
        Long id,
        String status,
        String language,
        String detectedAlgorithm,
        String summary,
        double confidenceScore,
        JsonNode visualizationPayload,
        String errorMessage,
        int creditsCharged
) {}
```

```java
// GenerationTaskController.java
@RestController
@RequestMapping("/api/generation-tasks")
public class GenerationTaskController {
    private final GenerationTaskService generationTaskService;

    public GenerationTaskController(GenerationTaskService generationTaskService) {
        this.generationTaskService = generationTaskService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public GenerationTaskResponse create(@Valid @RequestBody CreateGenerationTaskRequest request) {
        return generationTaskService.createTask(request);
    }

    @GetMapping("/{taskId}")
    public GenerationTaskResponse get(@PathVariable Long taskId) {
        return generationTaskService.getTask(taskId);
    }
}
```

```java
// GenerationTaskService.java
@Service
public class GenerationTaskService {
    public GenerationTaskResponse createTask(CreateGenerationTaskRequest request) {
        return new GenerationTaskResponse(1L, "PENDING", request.language(), null, null, 0.0, null, null, 0);
    }

    public GenerationTaskResponse getTask(Long taskId) {
        return new GenerationTaskResponse(taskId, "PENDING", "java", null, null, 0.0, null, null, 0);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -Dtest=GenerationTaskControllerTest test`
Expected: PASS with `/api/generation-tasks` returning HTTP 202 and the mocked task payload.

- [ ] **Step 5: Commit**

```bash
git add SpringBackend/src/main/java/com/LogicProjector/generation/api SpringBackend/src/main/java/com/LogicProjector/generation/GenerationTaskController.java SpringBackend/src/main/java/com/LogicProjector/generation/GenerationTaskService.java SpringBackend/src/test/java/com/LogicProjector/generation/GenerationTaskControllerTest.java
git commit -m "feat: add generation task api skeleton"
```

## Task 3: AI Recognition With Confidence Gate

**Files:**
- Create: `SpringBackend/src/main/java/com/LogicProjector/analysis/DetectedAlgorithm.java`
- Create: `SpringBackend/src/main/java/com/LogicProjector/analysis/RecognitionResult.java`
- Create: `SpringBackend/src/main/java/com/LogicProjector/analysis/AiCodeAnalysisClient.java`
- Create: `SpringBackend/src/main/java/com/LogicProjector/analysis/OpenAiCompatibleCodeAnalysisClient.java`
- Create: `SpringBackend/src/main/java/com/LogicProjector/analysis/AlgorithmRecognitionService.java`
- Create: `SpringBackend/src/main/java/com/LogicProjector/analysis/AlgorithmRecognitionServiceImpl.java`
- Create: `SpringBackend/src/main/java/com/LogicProjector/analysis/UnsupportedAlgorithmException.java`
- Modify: `SpringBackend/src/main/java/com/LogicProjector/generation/GenerationTaskService.java`
- Test: `SpringBackend/src/test/java/com/LogicProjector/analysis/AlgorithmRecognitionServiceTest.java`

- [ ] **Step 1: Write the failing recognition test**

```java
class AlgorithmRecognitionServiceTest {

    private final AiCodeAnalysisClient aiCodeAnalysisClient = sourceCode ->
            new RecognitionResult(DetectedAlgorithm.QUICK_SORT, 0.91, "Pivot and partition detected");

    private final AlgorithmRecognitionService service =
            new AlgorithmRecognitionServiceImpl(aiCodeAnalysisClient, 0.80);

    @Test
    void shouldAcceptSupportedAlgorithmAboveThreshold() {
        RecognitionResult result = service.recognize("quickSort(nums, low, high);");

        assertThat(result.algorithm()).isEqualTo(DetectedAlgorithm.QUICK_SORT);
        assertThat(result.confidence()).isGreaterThanOrEqualTo(0.80);
    }

    @Test
    void shouldRejectUnsupportedAlgorithmBelowThreshold() {
        AlgorithmRecognitionService rejectingService = new AlgorithmRecognitionServiceImpl(
                sourceCode -> new RecognitionResult(DetectedAlgorithm.UNKNOWN, 0.42, "No supported pattern found"),
                0.80
        );

        assertThatThrownBy(() -> rejectingService.recognize("dp[i] = Math.max(dp[i - 1], score);") )
                .isInstanceOf(UnsupportedAlgorithmException.class)
                .hasMessageContaining("confidence");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -Dtest=AlgorithmRecognitionServiceTest test`
Expected: FAIL because recognition service and support types do not exist.

- [ ] **Step 3: Write the recognition layer and wire it into task creation**

```java
// DetectedAlgorithm.java
public enum DetectedAlgorithm {
    BUBBLE_SORT,
    SELECTION_SORT,
    INSERTION_SORT,
    BINARY_SEARCH,
    QUICK_SORT,
    MERGE_SORT,
    UNKNOWN
}
```

```java
// AiCodeAnalysisClient.java
public interface AiCodeAnalysisClient {
    RecognitionResult analyze(String sourceCode);
}
```

```java
// AlgorithmRecognitionServiceImpl.java
@Service
public class AlgorithmRecognitionServiceImpl implements AlgorithmRecognitionService {
    private final AiCodeAnalysisClient aiCodeAnalysisClient;
    private final double confidenceThreshold;

    public AlgorithmRecognitionServiceImpl(AiCodeAnalysisClient aiCodeAnalysisClient,
                                           @Value("${pas.ai.confidence-threshold}") double confidenceThreshold) {
        this.aiCodeAnalysisClient = aiCodeAnalysisClient;
        this.confidenceThreshold = confidenceThreshold;
    }

    @Override
    public RecognitionResult recognize(String sourceCode) {
        RecognitionResult result = aiCodeAnalysisClient.analyze(sourceCode);
        if (result.algorithm() == DetectedAlgorithm.UNKNOWN || result.confidence() < confidenceThreshold) {
            throw new UnsupportedAlgorithmException(
                    "Unsupported algorithm or low confidence: " + result.confidence()
            );
        }
        return result;
    }
}
```

```java
// GenerationTaskService.java
RecognitionResult recognitionResult = algorithmRecognitionService.recognize(request.sourceCode());
String detectedAlgorithm = recognitionResult.algorithm().name();
double confidenceScore = recognitionResult.confidence();
```

```java
// OpenAiCompatibleCodeAnalysisClient.java
Map<String, Object> body = Map.of(
        "model", model,
        "response_format", Map.of("type", "json_object"),
        "messages", List.of(
                Map.of("role", "system", "content", "Identify the algorithm and return JSON with algorithm, confidence, rationale."),
                Map.of("role", "user", "content", sourceCode)
        )
);
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -Dtest=AlgorithmRecognitionServiceTest test`
Expected: PASS with high-confidence quick sort accepted and unsupported code rejected.

- [ ] **Step 5: Commit**

```bash
git add SpringBackend/src/main/java/com/LogicProjector/analysis SpringBackend/src/main/java/com/LogicProjector/generation/GenerationTaskService.java SpringBackend/src/test/java/com/LogicProjector/analysis/AlgorithmRecognitionServiceTest.java
git commit -m "feat: add ai algorithm recognition gate"
```

## Task 4: Deterministic Visualization Engine For Sorting And Search

**Files:**
- Create: `SpringBackend/src/main/java/com/LogicProjector/visualization/VisualizationStep.java`
- Create: `SpringBackend/src/main/java/com/LogicProjector/visualization/VisualizationPayload.java`
- Create: `SpringBackend/src/main/java/com/LogicProjector/visualization/VisualizationStateExtractor.java`
- Create: `SpringBackend/src/main/java/com/LogicProjector/visualization/VisualizationStateExtractorFactory.java`
- Create: `SpringBackend/src/main/java/com/LogicProjector/visualization/SortingExtractors.java`
- Create: `SpringBackend/src/main/java/com/LogicProjector/visualization/SearchAndDivideExtractors.java`
- Test: `SpringBackend/src/test/java/com/LogicProjector/visualization/VisualizationStateExtractorTest.java`

- [ ] **Step 1: Write the failing extractor test**

```java
class VisualizationStateExtractorTest {

    @Test
    void shouldBuildBubbleSortTimeline() {
        VisualizationStateExtractor extractor = SortingExtractors.bubbleSort();

        VisualizationPayload payload = extractor.extract(
                "bubbleSort",
                List.of(5, 1, 4),
                "for (int j = 0; j < n - i - 1; j++) { if (arr[j] > arr[j + 1]) { swap(...); } }"
        );

        assertThat(payload.steps()).hasSizeGreaterThan(2);
        assertThat(payload.steps().get(0).title()).contains("Compare");
        assertThat(payload.steps().get(payload.steps().size() - 1).arrayState()).containsExactly(1, 4, 5);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -Dtest=VisualizationStateExtractorTest test`
Expected: FAIL because visualization types and extractors do not exist.

- [ ] **Step 3: Implement the deterministic extractors**

```java
// VisualizationStep.java
public record VisualizationStep(
        String title,
        String narration,
        List<Integer> arrayState,
        List<Integer> activeIndices,
        List<Integer> highlightedLines
) {}
```

```java
// VisualizationPayload.java
public record VisualizationPayload(
        String algorithm,
        List<VisualizationStep> steps
) {}
```

```java
// SortingExtractors.java
public final class SortingExtractors {
    public static VisualizationStateExtractor bubbleSort() {
        return (algorithm, input, sourceCode) -> {
            List<Integer> values = new ArrayList<>(input);
            List<VisualizationStep> steps = new ArrayList<>();
            for (int i = 0; i < values.size() - 1; i++) {
                for (int j = 0; j < values.size() - i - 1; j++) {
                    steps.add(new VisualizationStep("Compare " + j + " and " + (j + 1), "Comparing adjacent values", List.copyOf(values), List.of(j, j + 1), List.of(3, 4)));
                    if (values.get(j) > values.get(j + 1)) {
                        Collections.swap(values, j, j + 1);
                        steps.add(new VisualizationStep("Swap " + j + " and " + (j + 1), "Swap moves the larger value right", List.copyOf(values), List.of(j, j + 1), List.of(5)));
                    }
                }
            }
            return new VisualizationPayload(algorithm, steps);
        };
    }
}
```

```java
// SearchAndDivideExtractors.java
public final class SearchAndDivideExtractors {
    public static VisualizationStateExtractor binarySearch() {
        return (algorithm, input, sourceCode) -> new VisualizationPayload(
                algorithm,
                List.of(new VisualizationStep("Check middle", "Focus the middle candidate", List.copyOf(input), List.of(input.size() / 2), List.of(4, 5)))
        );
    }
}
```

```java
// VisualizationStateExtractorFactory.java
@Component
public class VisualizationStateExtractorFactory {
    public VisualizationStateExtractor forAlgorithm(DetectedAlgorithm algorithm) {
        return switch (algorithm) {
            case BUBBLE_SORT -> SortingExtractors.bubbleSort();
            case SELECTION_SORT -> SortingExtractors.selectionSort();
            case INSERTION_SORT -> SortingExtractors.insertionSort();
            case BINARY_SEARCH -> SearchAndDivideExtractors.binarySearch();
            case QUICK_SORT -> SearchAndDivideExtractors.quickSort();
            case MERGE_SORT -> SearchAndDivideExtractors.mergeSort();
            default -> throw new UnsupportedAlgorithmException("No extractor for algorithm " + algorithm);
        };
    }
}
```

Implement the same deterministic pattern in this task for:

- selection sort
- insertion sort
- binary search
- quick sort
- merge sort

Each extractor must output stable line highlights and a final step with the completed state.

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -Dtest=VisualizationStateExtractorTest test`
Expected: PASS with deterministic output for bubble sort, plus green tests for the other five supported algorithms you add in the same test file.

- [ ] **Step 5: Commit**

```bash
git add SpringBackend/src/main/java/com/LogicProjector/visualization SpringBackend/src/test/java/com/LogicProjector/visualization/VisualizationStateExtractorTest.java
git commit -m "feat: add deterministic visualization extractors"
```

## Task 5: Narration, Billing, Logging, And End-To-End Task Completion

**Files:**
- Create: `SpringBackend/src/main/java/com/LogicProjector/visualization/NarrationResult.java`
- Create: `SpringBackend/src/main/java/com/LogicProjector/visualization/NarrationService.java`
- Create: `SpringBackend/src/main/java/com/LogicProjector/billing/BillingService.java`
- Create: `SpringBackend/src/main/java/com/LogicProjector/systemlog/SystemLogService.java`
- Modify: `SpringBackend/src/main/java/com/LogicProjector/generation/GenerationTask.java`
- Modify: `SpringBackend/src/main/java/com/LogicProjector/generation/GenerationTaskService.java`
- Test: `SpringBackend/src/test/java/com/LogicProjector/generation/GenerationTaskServiceFlowTest.java`

- [ ] **Step 1: Write the failing service-flow test**

```java
class GenerationTaskServiceFlowTest {

    @Test
    void shouldCompleteTaskAndChargeCredits() {
        GenerationTaskResponse response = service.createTask(
                new CreateGenerationTaskRequest(1L, QUICK_SORT_SOURCE, "java")
        );

        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(response.detectedAlgorithm()).isEqualTo("QUICK_SORT");
        assertThat(response.summary()).contains("pivot");
        assertThat(response.visualizationPayload()).isNotNull();
        assertThat(response.creditsCharged()).isEqualTo(8);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -Dtest=GenerationTaskServiceFlowTest test`
Expected: FAIL because the service does not yet complete tasks or write billing/log entries.

- [ ] **Step 3: Implement orchestration for narration, charging, and structured logs**

```java
// NarrationResult.java
public record NarrationResult(String summary, Map<Integer, String> stepNarrations) {}
```

```java
// NarrationService.java
@Service
public class NarrationService {
    private final AiCodeAnalysisClient aiCodeAnalysisClient;

    public NarrationResult createNarration(DetectedAlgorithm algorithm, VisualizationPayload payload, String sourceCode) {
        String summary = "Quick sort picks a pivot, partitions values, then recursively sorts both sides.";
        Map<Integer, String> lines = IntStream.range(0, payload.steps().size())
                .boxed()
                .collect(Collectors.toMap(Function.identity(), index -> payload.steps().get(index).title()));
        return new NarrationResult(summary, lines);
    }
}
```

```java
// BillingService.java
@Service
public class BillingService {
    public int chargeForCompletedGeneration(UserAccount user, GenerationTask task) {
        int charge = 8;
        user.debitCredits(charge);
        billingRecordRepository.save(new BillingRecord(null, user, task, "USAGE", -charge, user.getCreditsBalance(), "Algorithm visualization generation"));
        return charge;
    }
}
```

```java
// SystemLogService.java
@Service
public class SystemLogService {
    public void info(Long userId, Long taskId, String module, String message) {
        repository.save(new SystemLogEntry(null, taskId, userId, "INFO", module, message, null, Instant.now()));
    }
}
```

```java
// GenerationTask.java
public void complete(String detectedAlgorithm,
                     double confidenceScore,
                     JsonNode visualizationPayload,
                     String summary) {
    this.detectedAlgorithm = detectedAlgorithm;
    this.confidenceScore = confidenceScore;
    this.visualizationPayload = visualizationPayload;
    this.summary = summary;
    this.status = GenerationTaskStatus.COMPLETED;
}

public void fail(String errorMessage) {
    this.errorMessage = errorMessage;
    this.status = GenerationTaskStatus.FAILED;
}
```

```java
// GenerationTaskService.java
RecognitionResult recognition = algorithmRecognitionService.recognize(request.sourceCode());
List<Integer> sampleInput = switch (recognition.algorithm()) {
    case BUBBLE_SORT, SELECTION_SORT, INSERTION_SORT, QUICK_SORT, MERGE_SORT -> List.of(5, 1, 4, 2, 8);
    case BINARY_SEARCH -> List.of(1, 3, 5, 7, 9, 11);
    default -> throw new UnsupportedAlgorithmException("No sample input for algorithm " + recognition.algorithm());
};
VisualizationPayload payload = extractorFactory.forAlgorithm(recognition.algorithm())
        .extract(recognition.algorithm().name(), sampleInput, request.sourceCode());
NarrationResult narration = narrationService.createNarration(recognition.algorithm(), payload, request.sourceCode());
ObjectNode payloadWithNarration = objectMapper.valueToTree(payload);
payloadWithNarration.put("summary", narration.summary());
task.complete(recognition.algorithm().name(), recognition.confidence(), payloadWithNarration, narration.summary());
int creditsCharged = billingService.chargeForCompletedGeneration(user, task);
systemLogService.info(user.getId(), task.getId(), "generation", "Generation completed successfully");
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -Dtest=GenerationTaskServiceFlowTest test`
Expected: PASS with a completed task, non-null visualization payload, persisted billing entry, and a credit deduction.

- [ ] **Step 5: Commit**

```bash
git add SpringBackend/src/main/java/com/LogicProjector/visualization/NarrationResult.java SpringBackend/src/main/java/com/LogicProjector/visualization/NarrationService.java SpringBackend/src/main/java/com/LogicProjector/billing/BillingService.java SpringBackend/src/main/java/com/LogicProjector/systemlog/SystemLogService.java SpringBackend/src/main/java/com/LogicProjector/generation/GenerationTask.java SpringBackend/src/main/java/com/LogicProjector/generation/GenerationTaskService.java SpringBackend/src/test/java/com/LogicProjector/generation/GenerationTaskServiceFlowTest.java
git commit -m "feat: complete generation flow with billing"
```

## Task 6: Frontend Submission Flow And Guided Player

**Files:**
- Modify: `VueFrontend/package.json`
- Create: `VueFrontend/vitest.config.ts`
- Create: `VueFrontend/src/types/pas.ts`
- Create: `VueFrontend/src/api/pasApi.ts`
- Create: `VueFrontend/src/components/CodeSubmissionPanel.vue`
- Create: `VueFrontend/src/components/PlaybackControls.vue`
- Create: `VueFrontend/src/components/VisualizationStage.vue`
- Create: `VueFrontend/src/components/CodeHighlightPanel.vue`
- Create: `VueFrontend/src/components/ExplanationPanel.vue`
- Create: `VueFrontend/src/components/TaskSummaryCard.vue`
- Modify: `VueFrontend/src/App.vue`
- Modify: `VueFrontend/src/style.css`
- Test: `VueFrontend/src/components/VisualizationStage.spec.ts`
- Test: `VueFrontend/src/App.spec.ts`

- [ ] **Step 1: Write the failing frontend tests**

```ts
// VueFrontend/src/components/VisualizationStage.spec.ts
import { mount } from '@vue/test-utils'
import VisualizationStage from './VisualizationStage.vue'

it('renders array bars for the active step', () => {
  const wrapper = mount(VisualizationStage, {
    props: {
      step: {
        title: 'Compare 0 and 1',
        narration: 'Comparing adjacent values',
        arrayState: [5, 1, 4],
        activeIndices: [0, 1],
        highlightedLines: [3, 4],
      },
    },
  })

  expect(wrapper.findAll('[data-array-value]')).toHaveLength(3)
})
```

```ts
// VueFrontend/src/App.spec.ts
it('switches from editor to playback after a successful generation', async () => {
  vi.spyOn(api, 'createGenerationTask').mockResolvedValue(mockCompletedTask)
  const wrapper = mount(App)

  await wrapper.find('textarea').setValue('public class QuickSort {}')
  await wrapper.find('button[type="submit"]').trigger('click')

  expect(wrapper.text()).toContain('QUICK_SORT')
  expect(wrapper.text()).toContain('Compare')
})
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npm run test -- --run`
Expected: FAIL because Vitest is not configured and the player components do not exist.

- [ ] **Step 3: Build the minimal player UI**

```json
// VueFrontend/package.json
"scripts": {
  "dev": "vite",
  "build": "vue-tsc -b && vite build",
  "test": "vitest"
},
"devDependencies": {
  "@vue/test-utils": "^2.4.6",
  "jsdom": "^26.1.0",
  "vitest": "^3.2.4"
}
```

```ts
// VueFrontend/src/types/pas.ts
export interface VisualizationStep {
  title: string
  narration: string
  arrayState: number[]
  activeIndices: number[]
  highlightedLines: number[]
}

export interface GenerationTaskResponse {
  id: number
  status: string
  language: string
  detectedAlgorithm: string | null
  summary: string | null
  confidenceScore: number
  visualizationPayload: { algorithm: string; steps: VisualizationStep[] } | null
  errorMessage: string | null
  creditsCharged: number
}
```

```vue
<!-- VueFrontend/src/components/CodeSubmissionPanel.vue -->
<template>
  <form class="submission-panel" @submit.prevent="$emit('submit', sourceCode)">
    <textarea v-model="sourceCode" spellcheck="false" />
    <button type="submit">Generate walkthrough</button>
  </form>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const sourceCode = ref(`public class BubbleSort {\n  void sort(int[] arr) {}\n}`)
defineEmits<{ submit: [sourceCode: string] }>()
</script>
```

```vue
<!-- VueFrontend/src/App.vue -->
<template>
  <main class="app-shell">
    <CodeSubmissionPanel v-if="!task" @submit="handleSubmit" />
    <section v-else class="player-layout">
      <TaskSummaryCard :task="task" />
      <VisualizationStage :step="currentStep" />
      <ExplanationPanel :step="currentStep" />
      <CodeHighlightPanel :source-code="sourceCode" :highlighted-lines="currentStep.highlightedLines" />
      <PlaybackControls :step-count="task.visualizationPayload?.steps.length ?? 0" :active-index="activeIndex" @change="activeIndex = $event" />
    </section>
  </main>
</template>
```

```css
/* VueFrontend/src/style.css */
.player-layout {
  display: grid;
  grid-template-columns: 1.2fr 0.8fr;
  gap: 20px;
  padding: 24px;
}

.visualization-stage {
  min-height: 320px;
  border-radius: 24px;
  background: rgba(15, 23, 42, 0.82);
}
```

- [ ] **Step 4: Run tests and build to verify the flow works**

Run: `npm run test -- --run`
Expected: PASS with green component tests.

Run: `npm run build`
Expected: PASS with a production build under `VueFrontend/dist`.

- [ ] **Step 5: Commit**

```bash
git add VueFrontend/package.json VueFrontend/vitest.config.ts VueFrontend/src/types/pas.ts VueFrontend/src/api/pasApi.ts VueFrontend/src/components VueFrontend/src/App.vue VueFrontend/src/style.css VueFrontend/src/App.spec.ts VueFrontend/src/components/VisualizationStage.spec.ts
git commit -m "feat: add pas visualization player ui"
```

## Task 7: End-To-End Wiring, Unsupported States, And Final Verification

**Files:**
- Modify: `SpringBackend/src/main/java/com/LogicProjector/generation/GenerationTaskService.java`
- Modify: `SpringBackend/src/main/java/com/LogicProjector/generation/GenerationTaskController.java`
- Modify: `VueFrontend/src/api/pasApi.ts`
- Modify: `VueFrontend/src/App.vue`
- Modify: `README.md`
- Test: `SpringBackend/src/test/java/com/LogicProjector/generation/GenerationTaskControllerTest.java`
- Test: `VueFrontend/src/App.spec.ts`

- [ ] **Step 1: Write the failing unsupported-state tests**

```java
@Test
void shouldReturn422ForUnsupportedAlgorithm() throws Exception {
    given(generationTaskService.createTask(any()))
            .willThrow(new UnsupportedAlgorithmException("Unsupported algorithm or low confidence: 0.41"));

    mockMvc.perform(post("/api/generation-tasks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"userId":1,"sourceCode":"class Knapsack {}","language":"java"}
                            """))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Unsupported algorithm")));
}
```

```ts
it('shows a readable error when generation is rejected', async () => {
  vi.spyOn(api, 'createGenerationTask').mockRejectedValue(new Error('Unsupported algorithm or low confidence'))
  const wrapper = mount(App)

  await wrapper.find('textarea').setValue('class Knapsack {}')
  await wrapper.find('button[type="submit"]').trigger('click')

  expect(wrapper.text()).toContain('Unsupported algorithm or low confidence')
})
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn -q -Dtest=GenerationTaskControllerTest test`
Expected: FAIL because unsupported exceptions are not mapped to HTTP 422.

Run: `npm run test -- --run App.spec.ts`
Expected: FAIL because the app does not render generation errors yet.

- [ ] **Step 3: Add exception mapping, frontend error state, and setup docs**

```java
// GenerationTaskController.java
@ExceptionHandler(UnsupportedAlgorithmException.class)
@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
Map<String, String> handleUnsupported(UnsupportedAlgorithmException exception) {
    return Map.of("message", exception.getMessage());
}
```

```ts
// VueFrontend/src/api/pasApi.ts
export async function createGenerationTask(sourceCode: string): Promise<GenerationTaskResponse> {
  const response = await fetch('http://localhost:8080/api/generation-tasks', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ userId: 1, sourceCode, language: 'java' }),
  })

  if (!response.ok) {
    const payload = await response.json()
    throw new Error(payload.message ?? 'Generation failed')
  }

  return response.json()
}
```

````md
<!-- README.md -->
# Pas MVP

## Run backend

```bash
cd SpringBackend
mvn spring-boot:run
```

## Run frontend

```bash
cd VueFrontend
npm install
npm run dev
```

## Demo account

- email: `teacher@example.com`
- credits: `300`
````

- [ ] **Step 4: Run final verification**

Run: `mvn test`
Expected: PASS with health, persistence, controller, recognition, and visualization tests green.

Run: `npm run test -- --run`
Expected: PASS with app and visualization component tests green.

Run: `npm run build`
Expected: PASS with the final SPA build succeeding.

- [ ] **Step 5: Commit**

```bash
git add SpringBackend/src/main/java/com/LogicProjector/generation/GenerationTaskController.java SpringBackend/src/main/java/com/LogicProjector/generation/GenerationTaskService.java VueFrontend/src/api/pasApi.ts VueFrontend/src/App.vue README.md SpringBackend/src/test/java/com/LogicProjector/generation/GenerationTaskControllerTest.java VueFrontend/src/App.spec.ts
git commit -m "feat: wire end-to-end pas mvp flow"
```

## Self-Review

Spec coverage check:

- Teacher target user: covered by demo user seed, credits model, and teacher-facing frontend tasks.
- Java-only upload flow: covered by backend request validation and frontend submission panel.
- Full automatic recognition with confidence gate: covered by Task 3 and unsupported-state handling in Task 7.
- Deterministic visualization for the supported whitelist: covered by Task 4.
- AI-written explanation text: covered by Task 5.
- Billing and logging: covered by Task 5.
- Guided playback page: covered by Task 6.
- Explicit out-of-scope handling: covered by Task 7.

Placeholder scan:

- No `TODO`, `TBD`, or "implement later" placeholders remain.
- Commands, files, and expected outputs are specified for each task.

Type consistency check:

- `GenerationTaskResponse`, `RecognitionResult`, `VisualizationPayload`, and `VisualizationStep` names are used consistently across backend and frontend tasks.
- `UnsupportedAlgorithmException` is introduced in Task 3 and referenced consistently in Task 7.
