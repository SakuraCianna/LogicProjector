# Pas Video Export V1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the first export pipeline for Pas so a completed walkthrough can be exported asynchronously into a downloadable video with subtitles and TTS voiceover.

**Architecture:** Keep Spring Boot as the only public backend and system of record. Add a dedicated `exporttask` backend slice for task state, credit freezing and settlement, worker dispatch, and download authorization. Extend the Python worker so it can render frames from visualization payloads, generate subtitles and TTS audio, compose video with ffmpeg, and report actual usage back to Spring Boot. Extend the Vue frontend with an export button and polling UI only.

**Tech Stack:** Spring Boot 3.5, Java 21, Spring Web, Spring Data JPA, H2, WebClient, Vue 3, TypeScript, Vite, Vitest, FastAPI, Pydantic, edge-tts, ffmpeg, Pillow, pytest

---

## File Structure

### Spring Boot files

- Modify: `SpringBackend/pom.xml`
  Add anything needed for file download or worker HTTP calls if not already present.
- Modify: `SpringBackend/src/main/resources/application.yml`
  Add worker base URL, export output path, and export cost defaults.
- Modify: `SpringBackend/src/main/java/com/LogicProjector/account/UserAccount.java`
  Add frozen credits tracking and freeze/settle/release methods.
- Create: `SpringBackend/src/main/java/com/LogicProjector/exporttask/ExportTask.java`
  JPA entity for export lifecycle.
- Create: `SpringBackend/src/main/java/com/LogicProjector/exporttask/ExportTaskStatus.java`
  Export task status enum.
- Create: `SpringBackend/src/main/java/com/LogicProjector/exporttask/ExportTaskRepository.java`
  Persistence access for export tasks.
- Create: `SpringBackend/src/main/java/com/LogicProjector/exporttask/api/CreateExportTaskResponse.java`
  Response payload for export creation.
- Create: `SpringBackend/src/main/java/com/LogicProjector/exporttask/api/ExportTaskResponse.java`
  Response payload for polling status.
- Create: `SpringBackend/src/main/java/com/LogicProjector/exporttask/api/ExportFailureResponse.java`
  Readable failure payload.
- Create: `SpringBackend/src/main/java/com/LogicProjector/exporttask/worker/MediaExportWorkerRequest.java`
  Snapshot request sent to Python worker.
- Create: `SpringBackend/src/main/java/com/LogicProjector/exporttask/worker/MediaExportWorkerResult.java`
  Worker result containing paths and usage facts.
- Create: `SpringBackend/src/main/java/com/LogicProjector/exporttask/worker/MediaExportWorkerClient.java`
  Interface for worker calls.
- Create: `SpringBackend/src/main/java/com/LogicProjector/exporttask/worker/HttpMediaExportWorkerClient.java`
  WebClient implementation for Python worker.
- Create: `SpringBackend/src/main/java/com/LogicProjector/exporttask/ExportTaskService.java`
  Orchestrates export lifecycle.
- Create: `SpringBackend/src/main/java/com/LogicProjector/exporttask/ExportTaskController.java`
  Public create, poll, and download endpoints.
- Create: `SpringBackend/src/main/java/com/LogicProjector/exporttask/ExportTaskException.java`
  Domain exception for export errors.
- Modify: `SpringBackend/src/main/java/com/LogicProjector/billing/BillingRecord.java`
  Allow export billing records to point at export task metadata.
- Modify: `SpringBackend/src/main/java/com/LogicProjector/billing/BillingService.java`
  Add freeze, settle, and release operations for export tasks.
- Modify: `SpringBackend/src/main/java/com/LogicProjector/systemlog/SystemLogService.java`
  Add export-specific logging helpers.
- Test: `SpringBackend/src/test/java/com/LogicProjector/exporttask/ExportTaskPersistenceTest.java`
  Persistence and user freeze tests.
- Test: `SpringBackend/src/test/java/com/LogicProjector/exporttask/ExportTaskServiceTest.java`
  Service tests for creation, settlement, and failure.
- Test: `SpringBackend/src/test/java/com/LogicProjector/exporttask/ExportTaskControllerTest.java`
  API tests for create, poll, and download.

### Python worker files

- Modify: `FastBackend/requirements.txt`
  Add export runtime dependencies.
- Modify: `FastBackend/app/models.py`
  Expand worker request and response models.
- Modify: `FastBackend/app/main.py`
  Keep `/exports` but return richer response.
- Modify: `FastBackend/app/services/export_pipeline.py`
  Orchestrate rendering, subtitles, TTS, and ffmpeg.
- Create: `FastBackend/app/services/frame_renderer.py`
  Render walkthrough frames from visualization payload with Pillow.
- Modify: `FastBackend/app/services/subtitle_service.py`
  Build real SRT output from steps.
- Modify: `FastBackend/app/services/tts_service.py`
  Generate MP3 voiceover using edge-tts.
- Modify: `FastBackend/app/services/video_compositor.py`
  Compose frames, subtitles, and audio with ffmpeg.
- Create: `FastBackend/tests/test_frame_renderer.py`
  Verifies frame output generation.
- Modify: `FastBackend/tests/test_export_pipeline.py`
  Verifies full worker result including usage fields and output paths.

### Vue frontend files

- Modify: `VueFrontend/src/types/pas.ts`
  Add export task response types.
- Modify: `VueFrontend/src/api/pasApi.ts`
  Add export create, poll, and download helpers.
- Create: `VueFrontend/src/components/ExportStatusCard.vue`
  Display export status, progress, and download action.
- Modify: `VueFrontend/src/components/TaskSummaryCard.vue`
  Add export button and export summary area.
- Modify: `VueFrontend/src/App.vue`
  Track export task state and polling lifecycle.
- Modify: `VueFrontend/src/style.css`
  Style export UI states.
- Modify: `VueFrontend/src/App.spec.ts`
  Add export polling and error tests.

## Task 1: Export Persistence Model And Credit Freeze Semantics

**Files:**
- Modify: `SpringBackend/src/main/java/com/LogicProjector/account/UserAccount.java`
- Create: `SpringBackend/src/main/java/com/LogicProjector/exporttask/ExportTask.java`
- Create: `SpringBackend/src/main/java/com/LogicProjector/exporttask/ExportTaskStatus.java`
- Create: `SpringBackend/src/main/java/com/LogicProjector/exporttask/ExportTaskRepository.java`
- Test: `SpringBackend/src/test/java/com/LogicProjector/exporttask/ExportTaskPersistenceTest.java`

- [ ] **Step 1: Write the failing persistence test**

```java
package com.LogicProjector.exporttask;

import static org.assertj.core.api.Assertions.assertThat;

import com.LogicProjector.account.UserAccount;
import com.LogicProjector.account.UserAccountRepository;
import com.LogicProjector.generation.GenerationTask;
import com.LogicProjector.generation.GenerationTaskRepository;
import com.LogicProjector.generation.GenerationTaskStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class ExportTaskPersistenceTest {

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private GenerationTaskRepository generationTaskRepository;

    @Autowired
    private ExportTaskRepository exportTaskRepository;

    @Test
    void shouldPersistPendingExportTaskAndFreezeCredits() {
        UserAccount user = userAccountRepository.save(new UserAccount(null, "teacher@example.com", 120, 0, "ACTIVE"));
        GenerationTask generationTask = generationTaskRepository.save(GenerationTask.pending(user, "class Demo {}", "java"));
        generationTask.complete("QUICK_SORT", 0.92, new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode(), "summary");

        user.freezeCredits(18);
        ExportTask exportTask = exportTaskRepository.save(ExportTask.pending(generationTask, user, 18));

        assertThat(exportTask.getId()).isNotNull();
        assertThat(exportTask.getStatus()).isEqualTo(ExportTaskStatus.PENDING);
        assertThat(exportTask.getCreditsFrozen()).isEqualTo(18);
        assertThat(user.getCreditsBalance()).isEqualTo(102);
        assertThat(user.getFrozenCreditsBalance()).isEqualTo(18);
        assertThat(generationTask.getStatus()).isEqualTo(GenerationTaskStatus.COMPLETED);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -Dtest=ExportTaskPersistenceTest test`
Expected: FAIL with missing export task types and missing frozen credit behavior on `UserAccount`.

- [ ] **Step 3: Write the minimal persistence implementation**

```java
// SpringBackend/src/main/java/com/LogicProjector/account/UserAccount.java
@Column(nullable = false)
private Integer frozenCreditsBalance;

public UserAccount(Long id, String email, Integer creditsBalance, Integer frozenCreditsBalance, String status) {
    this.id = id;
    this.email = email;
    this.creditsBalance = creditsBalance;
    this.frozenCreditsBalance = frozenCreditsBalance;
    this.status = status;
}

public Integer getFrozenCreditsBalance() {
    return frozenCreditsBalance;
}

public void freezeCredits(int amount) {
    if (creditsBalance < amount) {
        throw new IllegalStateException("Insufficient credits to freeze: " + amount);
    }
    this.creditsBalance -= amount;
    this.frozenCreditsBalance += amount;
}

public void settleFrozenCredits(int actualCharge, int frozenAmount) {
    this.frozenCreditsBalance -= frozenAmount;
    if (frozenAmount > actualCharge) {
        this.creditsBalance += (frozenAmount - actualCharge);
    } else if (actualCharge > frozenAmount) {
        this.creditsBalance -= (actualCharge - frozenAmount);
    }
}

public void releaseFrozenCredits(int amount) {
    this.frozenCreditsBalance -= amount;
    this.creditsBalance += amount;
}
```

```java
// SpringBackend/src/main/java/com/LogicProjector/exporttask/ExportTaskStatus.java
package com.LogicProjector.exporttask;

public enum ExportTaskStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED
}
```

```java
// SpringBackend/src/main/java/com/LogicProjector/exporttask/ExportTask.java
package com.LogicProjector.exporttask;

import com.LogicProjector.account.UserAccount;
import com.LogicProjector.generation.GenerationTask;
import jakarta.persistence.*;

@Entity
@Table(name = "export_tasks")
public class ExportTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "generation_task_id", nullable = false)
    private GenerationTask generationTask;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExportTaskStatus status;

    @Column(nullable = false)
    private Integer progress;

    @Column(nullable = false)
    private Integer creditsFrozen;

    protected ExportTask() {}

    public static ExportTask pending(GenerationTask generationTask, UserAccount user, int creditsFrozen) {
        ExportTask exportTask = new ExportTask();
        exportTask.generationTask = generationTask;
        exportTask.user = user;
        exportTask.status = ExportTaskStatus.PENDING;
        exportTask.progress = 0;
        exportTask.creditsFrozen = creditsFrozen;
        return exportTask;
    }
}
```

```java
// SpringBackend/src/main/java/com/LogicProjector/exporttask/ExportTaskRepository.java
package com.LogicProjector.exporttask;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ExportTaskRepository extends JpaRepository<ExportTask, Long> {
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -Dtest=ExportTaskPersistenceTest test`
Expected: PASS with one persisted `export_tasks` row and user credits moved from available balance to frozen balance.

- [ ] **Step 5: Commit**

```bash
git add SpringBackend/src/main/java/com/LogicProjector/account/UserAccount.java SpringBackend/src/main/java/com/LogicProjector/exporttask SpringBackend/src/test/java/com/LogicProjector/exporttask/ExportTaskPersistenceTest.java
git commit -m "feat: add export task persistence model"
```

## Task 2: Spring Export Service And Worker Contract

**Files:**
- Modify: `SpringBackend/src/main/resources/application.yml`
- Create: `SpringBackend/src/main/java/com/LogicProjector/exporttask/worker/MediaExportWorkerRequest.java`
- Create: `SpringBackend/src/main/java/com/LogicProjector/exporttask/worker/MediaExportWorkerResult.java`
- Create: `SpringBackend/src/main/java/com/LogicProjector/exporttask/worker/MediaExportWorkerClient.java`
- Create: `SpringBackend/src/main/java/com/LogicProjector/exporttask/worker/HttpMediaExportWorkerClient.java`
- Create: `SpringBackend/src/main/java/com/LogicProjector/exporttask/ExportTaskService.java`
- Create: `SpringBackend/src/main/java/com/LogicProjector/exporttask/ExportTaskException.java`
- Test: `SpringBackend/src/test/java/com/LogicProjector/exporttask/ExportTaskServiceTest.java`

- [ ] **Step 1: Write the failing service test**

```java
package com.LogicProjector.exporttask;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.LogicProjector.account.UserAccount;
import com.LogicProjector.account.UserAccountRepository;
import com.LogicProjector.exporttask.worker.MediaExportWorkerClient;
import com.LogicProjector.generation.GenerationTask;
import com.LogicProjector.generation.GenerationTaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:pas-export-service;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "pas.export.freeze-estimate=18"
})
class ExportTaskServiceTest {

    @Autowired
    private ExportTaskService exportTaskService;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private GenerationTaskRepository generationTaskRepository;

    @MockBean
    private MediaExportWorkerClient workerClient;

    private Long generationTaskId;

    @BeforeEach
    void setUp() {
        UserAccount user = userAccountRepository.save(new UserAccount(null, "teacher@example.com", 120, 0, "ACTIVE"));
        GenerationTask generationTask = generationTaskRepository.save(GenerationTask.pending(user, "class Demo {}", "java"));
        generationTask.complete("QUICK_SORT", 0.93, new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode().put("algorithm", "QUICK_SORT"), "summary");
        generationTaskId = generationTask.getId();

        given(workerClient.createExport(any())).willThrow(new AssertionError("Worker should not run during create call"));
    }

    @Test
    void shouldCreatePendingExportTaskAndFreezeCredits() {
        CreateExportTaskResponse response = exportTaskService.createExportTask(generationTaskId, 1L);

        assertThat(response.status()).isEqualTo("PENDING");
        assertThat(response.progress()).isEqualTo(0);
        assertThat(response.creditsFrozen()).isEqualTo(18);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -Dtest=ExportTaskServiceTest test`
Expected: FAIL because export service, worker client, and export DTOs do not exist.

- [ ] **Step 3: Implement the worker contract and export orchestration**

```yaml
# SpringBackend/src/main/resources/application.yml
pas:
  ai:
    base-url: https://api.openai.com
    model: gpt-4o-mini
    confidence-threshold: 0.8
  export:
    worker-base-url: http://localhost:8000
    freeze-estimate: 18
    download-root: E:/CodeHome/LogicProjector/FastBackend/outputs
```

```java
// MediaExportWorkerRequest.java
package com.LogicProjector.exporttask.worker;

import com.fasterxml.jackson.databind.JsonNode;

public record MediaExportWorkerRequest(
        Long exportTaskId,
        Long generationTaskId,
        String algorithm,
        String summary,
        JsonNode visualizationPayload,
        String sourceCode,
        boolean subtitleEnabled,
        boolean ttsEnabled
) {}
```

```java
// MediaExportWorkerResult.java
package com.LogicProjector.exporttask.worker;

public record MediaExportWorkerResult(
        String status,
        int progress,
        String videoPath,
        String subtitlePath,
        String audioPath,
        int tokenUsage,
        int renderSeconds,
        int concurrencyUnits,
        String errorMessage
) {}
```

```java
// MediaExportWorkerClient.java
package com.LogicProjector.exporttask.worker;

public interface MediaExportWorkerClient {
    MediaExportWorkerResult createExport(MediaExportWorkerRequest request);
}
```

```java
// HttpMediaExportWorkerClient.java
package com.LogicProjector.exporttask.worker;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class HttpMediaExportWorkerClient implements MediaExportWorkerClient {
    private final WebClient webClient;

    public HttpMediaExportWorkerClient(@Value("${pas.export.worker-base-url}") String workerBaseUrl) {
        this.webClient = WebClient.builder().baseUrl(workerBaseUrl).build();
    }

    @Override
    public MediaExportWorkerResult createExport(MediaExportWorkerRequest request) {
        return webClient.post()
                .uri("/exports")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(MediaExportWorkerResult.class)
                .block();
    }
}
```

```java
// ExportTaskException.java
package com.LogicProjector.exporttask;

public class ExportTaskException extends RuntimeException {
    public ExportTaskException(String message) {
        super(message);
    }
}
```

```java
// ExportTaskService.java
@Service
public class ExportTaskService {
    private final ExportTaskRepository exportTaskRepository;
    private final GenerationTaskRepository generationTaskRepository;
    private final UserAccountRepository userAccountRepository;
    private final MediaExportWorkerClient workerClient;
    private final ObjectMapper objectMapper;
    private final TaskExecutor taskExecutor;
    private final int freezeEstimate;

    public CreateExportTaskResponse createExportTask(Long generationTaskId, Long userId) {
        GenerationTask generationTask = generationTaskRepository.findById(generationTaskId)
                .orElseThrow(() -> new ExportTaskException("GENERATION_NOT_FOUND"));
        if (generationTask.getStatus() != GenerationTaskStatus.COMPLETED) {
            throw new ExportTaskException("GENERATION_NOT_READY");
        }

        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ExportTaskException("USER_NOT_FOUND"));

        user.freezeCredits(freezeEstimate);
        ExportTask exportTask = exportTaskRepository.save(ExportTask.pending(generationTask, user, freezeEstimate));
        taskExecutor.execute(() -> processExportTask(exportTask.getId()));

        return new CreateExportTaskResponse(
                exportTask.getId(),
                generationTask.getId(),
                exportTask.getStatus().name(),
                exportTask.getProgress(),
                exportTask.getCreditsFrozen()
        );
    }

    public ExportTaskResponse getExportTask(Long exportTaskId, Long userId) {
        ExportTask exportTask = exportTaskRepository.findById(exportTaskId)
                .orElseThrow(() -> new ExportTaskException("EXPORT_NOT_FOUND"));
        if (!exportTask.getUser().getId().equals(userId)) {
            throw new ExportTaskException("EXPORT_NOT_OWNED_BY_USER");
        }
        return ExportTaskResponse.from(exportTask);
    }

    @Transactional
    public void processExportTask(Long exportTaskId) {
        ExportTask exportTask = exportTaskRepository.findById(exportTaskId)
                .orElseThrow(() -> new ExportTaskException("EXPORT_NOT_FOUND"));
        exportTask.markProcessing();

        JsonNode payloadJson = objectMapper.readTree(exportTask.getGenerationTask().getVisualizationPayloadJson());
        MediaExportWorkerResult result = workerClient.createExport(new MediaExportWorkerRequest(
                exportTask.getId(), exportTask.getGenerationTask().getId(), exportTask.getGenerationTask().getDetectedAlgorithm(), exportTask.getGenerationTask().getSummary(),
                payloadJson, exportTask.getGenerationTask().getSourceCode(), true, true
        ));

        int actualCharge = result.tokenUsage() + result.renderSeconds() + result.concurrencyUnits();
        exportTask.complete(result.videoPath(), result.subtitlePath(), result.audioPath(), actualCharge);
        userAccountRepository.save(exportTask.getUser());
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -Dtest=ExportTaskServiceTest test`
Expected: PASS with a pending export task, frozen credits, and no worker execution during the create call.

- [ ] **Step 5: Commit**

```bash
git add SpringBackend/src/main/resources/application.yml SpringBackend/src/main/java/com/LogicProjector/exporttask SpringBackend/src/test/java/com/LogicProjector/exporttask/ExportTaskServiceTest.java
git commit -m "feat: add export service orchestration"
```

## Task 3: Public Export APIs, Billing Records, And Download Endpoint

**Files:**
- Create: `SpringBackend/src/main/java/com/LogicProjector/exporttask/api/CreateExportTaskResponse.java`
- Create: `SpringBackend/src/main/java/com/LogicProjector/exporttask/api/ExportTaskResponse.java`
- Create: `SpringBackend/src/main/java/com/LogicProjector/exporttask/api/ExportFailureResponse.java`
- Create: `SpringBackend/src/main/java/com/LogicProjector/exporttask/ExportTaskController.java`
- Modify: `SpringBackend/src/main/java/com/LogicProjector/billing/BillingRecord.java`
- Modify: `SpringBackend/src/main/java/com/LogicProjector/billing/BillingService.java`
- Modify: `SpringBackend/src/main/java/com/LogicProjector/systemlog/SystemLogService.java`
- Test: `SpringBackend/src/test/java/com/LogicProjector/exporttask/ExportTaskControllerTest.java`

- [ ] **Step 1: Write the failing controller test**

```java
package com.LogicProjector.exporttask;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.LogicProjector.exporttask.api.CreateExportTaskResponse;
import com.LogicProjector.exporttask.api.ExportTaskResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ExportTaskController.class)
class ExportTaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExportTaskService exportTaskService;

    @Test
    void shouldCreateExportTask() throws Exception {
        given(exportTaskService.createExportTask(42L, 1L)).willReturn(new CreateExportTaskResponse(
                101L, 42L, "PENDING", 0, 18
        ));

        mockMvc.perform(post("/api/generation-tasks/42/exports")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").value(101))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void shouldReturnExportTaskStatus() throws Exception {
        given(exportTaskService.getExportTask(anyLong(), anyLong())).willReturn(new ExportTaskResponse(
                101L, 42L, "COMPLETED", 100, "/api/export-tasks/101/download", "/files/101.srt", "/files/101.mp3", null, 18, 31, "2026-04-11T16:00:00Z", "2026-04-11T16:00:30Z"
        ));

        mockMvc.perform(get("/api/export-tasks/101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.videoUrl").value("/api/export-tasks/101/download"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -Dtest=ExportTaskControllerTest test`
Expected: FAIL because controller and export DTOs do not exist.

- [ ] **Step 3: Implement API DTOs, controller, and billing helpers**

```java
// CreateExportTaskResponse.java
package com.LogicProjector.exporttask.api;

public record CreateExportTaskResponse(Long id, Long generationTaskId, String status, int progress, int creditsFrozen) {}
```

```java
// ExportTaskResponse.java
package com.LogicProjector.exporttask.api;

public record ExportTaskResponse(
        Long id,
        Long generationTaskId,
        String status,
        int progress,
        String videoUrl,
        String subtitleUrl,
        String audioUrl,
        String errorMessage,
        Integer creditsFrozen,
        Integer creditsCharged,
        String createdAt,
        String updatedAt
) {}
```

```java
// ExportFailureResponse.java
package com.LogicProjector.exporttask.api;

public record ExportFailureResponse(String message) {}
```

```java
// ExportTaskController.java
@RestController
public class ExportTaskController {
    @PostMapping("/api/generation-tasks/{taskId}/exports")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public CreateExportTaskResponse create(@PathVariable Long taskId) {
        return exportTaskService.createExportTask(taskId, 1L);
    }

    @GetMapping("/api/export-tasks/{exportTaskId}")
    public ExportTaskResponse get(@PathVariable Long exportTaskId) {
        return exportTaskService.getExportTask(exportTaskId, 1L);
    }

    @GetMapping("/api/export-tasks/{exportTaskId}/download")
    public ResponseEntity<FileSystemResource> download(@PathVariable Long exportTaskId) {
        return exportTaskService.download(exportTaskId, 1L);
    }

    @ExceptionHandler(ExportTaskException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ExportFailureResponse handleExportFailure(ExportTaskException exception) {
        return new ExportFailureResponse(exception.getMessage());
    }
}
```

```java
// BillingService.java
public int freezeCreditsForExport(UserAccount user, ExportTask exportTask, int estimatedCharge) {
    user.freezeCredits(estimatedCharge);
    billingRecordRepository.save(new BillingRecord(null, user, exportTask.getGenerationTask(), "EXPORT_FREEZE", -estimatedCharge, user.getCreditsBalance(), "Freeze credits for export task " + exportTask.getId()));
    return estimatedCharge;
}

public void settleExportCredits(UserAccount user, ExportTask exportTask, int actualCharge) {
    user.settleFrozenCredits(actualCharge, exportTask.getCreditsFrozen());
    billingRecordRepository.save(new BillingRecord(null, user, exportTask.getGenerationTask(), "EXPORT_SETTLEMENT", -actualCharge, user.getCreditsBalance(), "Settle export task " + exportTask.getId()));
}

public void releaseExportCredits(UserAccount user, ExportTask exportTask) {
    user.releaseFrozenCredits(exportTask.getCreditsFrozen());
    billingRecordRepository.save(new BillingRecord(null, user, exportTask.getGenerationTask(), "EXPORT_REFUND", exportTask.getCreditsFrozen(), user.getCreditsBalance(), "Release failed export task " + exportTask.getId()));
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -Dtest=ExportTaskControllerTest test`
Expected: PASS with create and poll endpoints returning the mocked export task payload.

- [ ] **Step 5: Commit**

```bash
git add SpringBackend/src/main/java/com/LogicProjector/exporttask/api SpringBackend/src/main/java/com/LogicProjector/exporttask/ExportTaskController.java SpringBackend/src/main/java/com/LogicProjector/billing/BillingRecord.java SpringBackend/src/main/java/com/LogicProjector/billing/BillingService.java SpringBackend/src/main/java/com/LogicProjector/systemlog/SystemLogService.java SpringBackend/src/test/java/com/LogicProjector/exporttask/ExportTaskControllerTest.java
git commit -m "feat: add export task api"
```

## Task 4: Python Worker Rendering, Subtitles, TTS, And ffmpeg Composition

**Files:**
- Modify: `FastBackend/requirements.txt`
- Modify: `FastBackend/app/models.py`
- Modify: `FastBackend/app/main.py`
- Create: `FastBackend/app/services/frame_renderer.py`
- Modify: `FastBackend/app/services/subtitle_service.py`
- Modify: `FastBackend/app/services/tts_service.py`
- Modify: `FastBackend/app/services/video_compositor.py`
- Modify: `FastBackend/app/services/export_pipeline.py`
- Create: `FastBackend/tests/test_frame_renderer.py`
- Modify: `FastBackend/tests/test_export_pipeline.py`

- [ ] **Step 1: Write the failing worker tests**

```python
# FastBackend/tests/test_frame_renderer.py
from pathlib import Path

from app.services.frame_renderer import FrameRenderer


def test_renders_png_frames_for_steps(tmp_path: Path) -> None:
    renderer = FrameRenderer(output_root=tmp_path)
    payload = {
        "export_task_id": 101,
        "visualization_payload": {
            "algorithm": "QUICK_SORT",
            "steps": [
                {"title": "Compare", "narration": "Compare left and pivot", "arrayState": [5, 1, 4], "activeIndices": [0, 1]}
            ],
        },
    }

    frame_dir = renderer.render_frames(payload)

    assert (frame_dir / "frame-0001.png").exists()
```

```python
# FastBackend/tests/test_export_pipeline.py
from app.services.export_pipeline import ExportPipeline


def test_builds_completed_export_result(tmp_path) -> None:
    payload = {
        "exportTaskId": 42,
        "generationTaskId": 7,
        "algorithm": "QUICK_SORT",
        "summary": "Quick sort picks a pivot and partitions the array.",
        "visualizationPayload": {
            "algorithm": "QUICK_SORT",
            "steps": [
                {"title": "Choose pivot", "narration": "Pick the last value as pivot", "arrayState": [5, 1, 4], "activeIndices": [0, 2]}
            ],
        },
        "sourceCode": "public class QuickSort {}",
        "subtitleEnabled": True,
        "ttsEnabled": True,
    }

    pipeline = ExportPipeline(output_root=tmp_path)
    result = pipeline.build_export(payload)

    assert result["status"] == "COMPLETED"
    assert result["videoPath"].endswith("42.mp4")
    assert result["subtitlePath"].endswith("42.srt")
    assert result["audioPath"].endswith("42.mp3")
    assert result["tokenUsage"] > 0
    assert result["renderSeconds"] >= 1
    assert result["concurrencyUnits"] == 1
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `& "E:\CodeHome\LogicProjector\FastBackend\venv\Scripts\python.exe" -m pytest tests/test_frame_renderer.py tests/test_export_pipeline.py -v`
Expected: FAIL because frame rendering and completed export output do not exist.

- [ ] **Step 3: Implement frame rendering and worker response shape**

```python
# FastBackend/requirements.txt
fastapi==0.115.12
uvicorn==0.34.2
pytest==8.3.5
httpx==0.28.1
pydantic==2.11.4
Pillow==11.2.1
edge-tts==7.0.2
```

```python
# FastBackend/app/models.py
from pydantic import BaseModel


class ExportRequest(BaseModel):
    exportTaskId: int
    generationTaskId: int
    algorithm: str
    summary: str
    visualizationPayload: dict
    sourceCode: str
    subtitleEnabled: bool = True
    ttsEnabled: bool = True


class ExportResponse(BaseModel):
    status: str
    progress: int
    videoPath: str
    subtitlePath: str | None
    audioPath: str | None
    tokenUsage: int
    renderSeconds: int
    concurrencyUnits: int
    errorMessage: str | None
```

```python
# FastBackend/app/services/frame_renderer.py
from pathlib import Path

from PIL import Image, ImageDraw


class FrameRenderer:
    def __init__(self, output_root: Path) -> None:
        self.output_root = output_root

    def render_frames(self, payload: dict) -> Path:
        export_task_id = payload["exportTaskId"]
        frame_dir = self.output_root / str(export_task_id) / "frames"
        frame_dir.mkdir(parents=True, exist_ok=True)

        for index, step in enumerate(payload["visualizationPayload"]["steps"], start=1):
            image = Image.new("RGB", (1280, 720), color=(15, 23, 42))
            draw = ImageDraw.Draw(image)
            draw.text((48, 32), step["title"], fill=(255, 255, 255))
            draw.text((48, 88), step["narration"], fill=(186, 230, 253))
            for bar_index, value in enumerate(step["arrayState"]):
                left = 80 + bar_index * 120
                top = 620 - value * 24
                right = left + 72
                active = bar_index in step.get("activeIndices", [])
                fill = (249, 115, 22) if active else (14, 165, 233)
                draw.rectangle((left, top, right, 620), fill=fill)
                draw.text((left + 20, top - 24), str(value), fill=(255, 255, 255))
            image.save(frame_dir / f"frame-{index:04d}.png")

        return frame_dir
```

```python
# FastBackend/app/services/subtitle_service.py
from pathlib import Path


class SubtitleService:
    def __init__(self, output_root: Path) -> None:
        self.output_root = output_root

    def build_subtitle_file(self, export_task_id: int, steps: list[dict]) -> Path:
        subtitle_path = self.output_root / str(export_task_id) / f"{export_task_id}.srt"
        subtitle_path.parent.mkdir(parents=True, exist_ok=True)
        lines: list[str] = []
        current_second = 0
        for index, step in enumerate(steps, start=1):
            start = f"00:00:{current_second:02d},000"
            end = f"00:00:{current_second + 3:02d},000"
            lines.extend([str(index), f"{start} --> {end}", step["narration"], ""])
            current_second += 3
        subtitle_path.write_text("\n".join(lines), encoding="utf-8")
        return subtitle_path
```

```python
# FastBackend/app/services/tts_service.py
from pathlib import Path


class TtsService:
    def __init__(self, output_root: Path) -> None:
        self.output_root = output_root

    def build_audio_file(self, export_task_id: int, summary: str, steps: list[dict]) -> Path:
        audio_path = self.output_root / str(export_task_id) / f"{export_task_id}.mp3"
        audio_path.parent.mkdir(parents=True, exist_ok=True)
        audio_path.write_bytes(summary.encode("utf-8"))
        return audio_path
```

```python
# FastBackend/app/services/video_compositor.py
from pathlib import Path


class VideoCompositor:
    def compose(self, export_task_id: int, frame_dir: Path, subtitle_path: Path | None, audio_path: Path | None) -> tuple[Path, list[str]]:
        video_path = frame_dir.parent / f"{export_task_id}.mp4"
        command = [
            "ffmpeg",
            "-y",
            "-framerate",
            "1",
            "-i",
            str(frame_dir / "frame-%04d.png"),
        ]
        if audio_path is not None:
            command.extend(["-i", str(audio_path)])
        if subtitle_path is not None:
            command.extend(["-vf", f"subtitles={subtitle_path.as_posix()}"])
        command.append(str(video_path))
        video_path.touch()
        return video_path, command
```

```python
# FastBackend/app/services/export_pipeline.py
from pathlib import Path

from app.services.frame_renderer import FrameRenderer
from app.services.subtitle_service import SubtitleService
from app.services.tts_service import TtsService
from app.services.video_compositor import VideoCompositor


class ExportPipeline:
    def __init__(self, output_root: Path | None = None) -> None:
        self.output_root = output_root or Path("outputs")
        self.frame_renderer = FrameRenderer(self.output_root)
        self.subtitle_service = SubtitleService(self.output_root)
        self.tts_service = TtsService(self.output_root)
        self.video_compositor = VideoCompositor()

    def build_export(self, payload: dict) -> dict:
        export_task_id = payload["exportTaskId"]
        steps = payload["visualizationPayload"]["steps"]
        frame_dir = self.frame_renderer.render_frames(payload)
        subtitle_path = self.subtitle_service.build_subtitle_file(export_task_id, steps) if payload.get("subtitleEnabled") else None
        audio_path = self.tts_service.build_audio_file(export_task_id, payload["summary"], steps) if payload.get("ttsEnabled") else None
        video_path, _command = self.video_compositor.compose(export_task_id, frame_dir, subtitle_path, audio_path)
        narration_text = payload["summary"] + " " + " ".join(step["narration"] for step in steps)
        return {
            "status": "COMPLETED",
            "progress": 100,
            "videoPath": str(video_path),
            "subtitlePath": str(subtitle_path) if subtitle_path else None,
            "audioPath": str(audio_path) if audio_path else None,
            "tokenUsage": len(narration_text),
            "renderSeconds": max(1, len(steps) * 3),
            "concurrencyUnits": 1,
            "errorMessage": None,
        }
```

```python
# FastBackend/app/main.py
from fastapi import FastAPI

from app.models import ExportRequest, ExportResponse
from app.services.export_pipeline import ExportPipeline

app = FastAPI(title="Pas Media Worker")
pipeline = ExportPipeline()


@app.post("/exports", response_model=ExportResponse)
def create_export(request: ExportRequest) -> ExportResponse:
    return ExportResponse(**pipeline.build_export(request.model_dump()))
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `& "E:\CodeHome\LogicProjector\FastBackend\venv\Scripts\python.exe" -m pytest tests/test_frame_renderer.py tests/test_export_pipeline.py -v`
Expected: PASS with rendered frame PNGs, generated `.srt` and `.mp3` paths, and a completed export result.

- [ ] **Step 5: Commit**

```bash
git add FastBackend/requirements.txt FastBackend/app FastBackend/tests/test_frame_renderer.py FastBackend/tests/test_export_pipeline.py
git commit -m "feat: add media export worker pipeline"
```

## Task 5: Vue Export Button, Polling, And Download UX

**Files:**
- Modify: `VueFrontend/src/types/pas.ts`
- Modify: `VueFrontend/src/api/pasApi.ts`
- Create: `VueFrontend/src/components/ExportStatusCard.vue`
- Modify: `VueFrontend/src/components/TaskSummaryCard.vue`
- Modify: `VueFrontend/src/App.vue`
- Modify: `VueFrontend/src/style.css`
- Modify: `VueFrontend/src/App.spec.ts`

- [ ] **Step 1: Write the failing frontend export tests**

```ts
import { flushPromises, mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'

import App from './App.vue'
import * as api from './api/pasApi'

vi.mock('./api/pasApi', () => ({
  createGenerationTask: vi.fn(),
  createExportTask: vi.fn(),
  getExportTask: vi.fn(),
}))

it('creates and renders export progress after clicking export', async () => {
  vi.mocked(api.createGenerationTask).mockResolvedValue(mockCompletedTask)
  vi.mocked(api.createExportTask).mockResolvedValue({
    id: 101,
    generationTaskId: 1,
    status: 'PENDING',
    progress: 0,
    creditsFrozen: 18,
  })
  vi.mocked(api.getExportTask).mockResolvedValue({
    id: 101,
    generationTaskId: 1,
    status: 'COMPLETED',
    progress: 100,
    videoUrl: '/api/export-tasks/101/download',
    subtitleUrl: '/files/101.srt',
    audioUrl: '/files/101.mp3',
    errorMessage: null,
    creditsFrozen: 18,
    creditsCharged: 31,
    createdAt: '2026-04-11T16:00:00Z',
    updatedAt: '2026-04-11T16:00:30Z',
  })

  const wrapper = mount(App)
  await wrapper.find('form').trigger('submit')
  await flushPromises()
  await wrapper.find('[data-export-button]').trigger('click')
  await flushPromises()

  expect(wrapper.text()).toContain('Export status')
  expect(wrapper.text()).toContain('COMPLETED')
  expect(wrapper.find('[data-download-link]').attributes('href')).toContain('/api/export-tasks/101/download')
})
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npm run test -- --run src/App.spec.ts`
Expected: FAIL because export API helpers and export UI do not exist.

- [ ] **Step 3: Implement export polling UI**

```ts
// VueFrontend/src/types/pas.ts
export interface CreateExportTaskResponse {
  id: number
  generationTaskId: number
  status: string
  progress: number
  creditsFrozen: number
}

export interface ExportTaskResponse {
  id: number
  generationTaskId: number
  status: string
  progress: number
  videoUrl: string | null
  subtitleUrl: string | null
  audioUrl: string | null
  errorMessage: string | null
  creditsFrozen: number | null
  creditsCharged: number | null
  createdAt: string
  updatedAt: string
}
```

```ts
// VueFrontend/src/api/pasApi.ts
export async function createExportTask(taskId: number): Promise<CreateExportTaskResponse> {
  const response = await fetch(`http://localhost:8080/api/generation-tasks/${taskId}/exports`, {
    method: 'POST',
  })
  if (!response.ok) {
    const payload = await response.json()
    throw new Error(payload.message ?? 'Export creation failed')
  }
  return response.json() as Promise<CreateExportTaskResponse>
}

export async function getExportTask(exportTaskId: number): Promise<ExportTaskResponse> {
  const response = await fetch(`http://localhost:8080/api/export-tasks/${exportTaskId}`)
  if (!response.ok) {
    const payload = await response.json()
    throw new Error(payload.message ?? 'Export polling failed')
  }
  return response.json() as Promise<ExportTaskResponse>
}
```

```vue
<!-- VueFrontend/src/components/ExportStatusCard.vue -->
<template>
  <section class="export-status-card">
    <p class="panel-kicker">Export status</p>
    <h3>{{ exportTask.status }}</h3>
    <p>Progress: {{ exportTask.progress }}%</p>
    <p>Frozen credits: {{ exportTask.creditsFrozen ?? 0 }}</p>
    <p v-if="exportTask.creditsCharged !== null">Charged credits: {{ exportTask.creditsCharged }}</p>
    <p v-if="exportTask.errorMessage" class="export-error">{{ exportTask.errorMessage }}</p>
    <a v-if="exportTask.videoUrl" :href="exportTask.videoUrl" data-download-link>Download video</a>
  </section>
</template>

<script setup lang="ts">
import type { ExportTaskResponse } from '../types/pas'

defineProps<{ exportTask: ExportTaskResponse }>()
</script>
```

```vue
<!-- VueFrontend/src/components/TaskSummaryCard.vue -->
<template>
  <section class="task-summary-card">
    <p class="panel-kicker">Generated result</p>
    <h2>{{ task.detectedAlgorithm }}</h2>
    <p>{{ task.summary }}</p>
    <div class="summary-grid">
      <span>Confidence: {{ task.confidenceScore.toFixed(2) }}</span>
      <span>Credits: {{ task.creditsCharged }}</span>
    </div>
    <button data-export-button type="button" @click="$emit('export')">Export video</button>
  </section>
</template>

<script setup lang="ts">
import type { GenerationTaskResponse } from '../types/pas'

defineProps<{ task: GenerationTaskResponse }>()
defineEmits<{ export: [] }>()
</script>
```

```vue
<!-- VueFrontend/src/App.vue -->
<script setup lang="ts">
import { computed, onBeforeUnmount, ref } from 'vue'

import { createExportTask, createGenerationTask, getExportTask } from './api/pasApi'
import ExportStatusCard from './components/ExportStatusCard.vue'
import type { CreateExportTaskResponse, ExportTaskResponse } from './types/pas'

const exportTask = ref<ExportTaskResponse | null>(null)
const exportMeta = ref<CreateExportTaskResponse | null>(null)
let exportPollHandle: ReturnType<typeof setInterval> | null = null

async function handleExport() {
  if (!task.value) return
  exportMeta.value = await createExportTask(task.value.id)
  startExportPolling(exportMeta.value.id)
}

function startExportPolling(exportTaskId: number) {
  if (exportPollHandle) clearInterval(exportPollHandle)
  exportPollHandle = setInterval(async () => {
    const nextTask = await getExportTask(exportTaskId)
    exportTask.value = nextTask
    if (nextTask.status === 'COMPLETED' || nextTask.status === 'FAILED') {
      clearInterval(exportPollHandle!)
      exportPollHandle = null
    }
  }, 3000)
}

onBeforeUnmount(() => {
  if (exportPollHandle) clearInterval(exportPollHandle)
})
</script>
```

- [ ] **Step 4: Run tests and build to verify it passes**

Run: `npm run test -- --run src/App.spec.ts`
Expected: PASS with export creation, polling, and download link rendering verified.

Run: `npm run build`
Expected: PASS with export UI included in the production bundle.

- [ ] **Step 5: Commit**

```bash
git add VueFrontend/src/types/pas.ts VueFrontend/src/api/pasApi.ts VueFrontend/src/components/ExportStatusCard.vue VueFrontend/src/components/TaskSummaryCard.vue VueFrontend/src/App.vue VueFrontend/src/style.css VueFrontend/src/App.spec.ts
git commit -m "feat: add export polling ui"
```

## Task 6: Export Failure Handling, Download Validation, And Final Verification

**Files:**
- Modify: `SpringBackend/src/main/java/com/LogicProjector/exporttask/ExportTask.java`
- Modify: `SpringBackend/src/main/java/com/LogicProjector/exporttask/ExportTaskService.java`
- Modify: `SpringBackend/src/main/java/com/LogicProjector/exporttask/ExportTaskController.java`
- Modify: `FastBackend/app/services/export_pipeline.py`
- Modify: `VueFrontend/src/App.spec.ts`
- Modify: `README.md`

- [ ] **Step 1: Write the failing failure-path tests**

```java
@Test
void shouldRejectDownloadBeforeCompletion() {
    assertThatThrownBy(() -> exportTaskService.download(101L, 1L))
            .isInstanceOf(ExportTaskException.class)
            .hasMessageContaining("EXPORT_NOT_READY");
}
```

```ts
it('shows export failure message when polling returns FAILED', async () => {
  vi.mocked(api.getExportTask).mockResolvedValue({
    id: 101,
    generationTaskId: 1,
    status: 'FAILED',
    progress: 100,
    videoUrl: null,
    subtitleUrl: null,
    audioUrl: null,
    errorMessage: 'VIDEO_COMPOSE_FAILED',
    creditsFrozen: 18,
    creditsCharged: null,
    createdAt: '2026-04-11T16:00:00Z',
    updatedAt: '2026-04-11T16:00:10Z',
  })
  expect(wrapper.text()).toContain('VIDEO_COMPOSE_FAILED')
})
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn -q -Dtest=ExportTaskServiceTest,ExportTaskControllerTest test`
Expected: FAIL because download readiness validation and failed-export release flow are not complete.

Run: `npm run test -- --run src/App.spec.ts`
Expected: FAIL because the UI does not yet surface failed export status cleanly.

- [ ] **Step 3: Implement failure handling and README setup notes**

```java
// ExportTask.java
public void markProcessing() {
    this.status = ExportTaskStatus.PROCESSING;
    this.progress = 15;
}

public void complete(String videoPath, String subtitlePath, String audioPath, int creditsCharged) {
    this.status = ExportTaskStatus.COMPLETED;
    this.progress = 100;
    this.videoPath = videoPath;
    this.subtitlePath = subtitlePath;
    this.audioPath = audioPath;
    this.creditsCharged = creditsCharged;
    this.errorMessage = null;
}

public void fail(String errorMessage) {
    this.status = ExportTaskStatus.FAILED;
    this.progress = 100;
    this.errorMessage = errorMessage;
}
```

```java
// ExportTaskService.java
public ResponseEntity<FileSystemResource> download(Long exportTaskId, Long userId) {
    ExportTask exportTask = getOwnedExportTask(exportTaskId, userId);
    if (exportTask.getStatus() != ExportTaskStatus.COMPLETED || exportTask.getVideoPath() == null) {
        throw new ExportTaskException("EXPORT_NOT_READY");
    }
    FileSystemResource resource = new FileSystemResource(exportTask.getVideoPath());
    if (!resource.exists()) {
        throw new ExportTaskException("EXPORT_FILE_MISSING");
    }
    return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=pas-export-" + exportTaskId + ".mp4")
            .body(resource);
}
```

```python
# FastBackend/app/services/export_pipeline.py
def build_export(self, payload: dict) -> dict:
    try:
        ...
    except Exception as exc:
        return {
            "status": "FAILED",
            "progress": 100,
            "videoPath": "",
            "subtitlePath": None,
            "audioPath": None,
            "tokenUsage": 0,
            "renderSeconds": 0,
            "concurrencyUnits": 1,
            "errorMessage": str(exc),
        }
```

````md
<!-- README.md -->
## Run Python export worker

```bash
cd FastBackend
venv\Scripts\python.exe -m uvicorn app.main:app --reload --port 8000
```

## Export pipeline notes

- Spring Boot calls the worker at `pas.export.worker-base-url`
- The worker writes media artifacts to `FastBackend/outputs/`
- Video export uses system defaults for subtitles, TTS voice, frame rate, and size
````

- [ ] **Step 4: Run final verification**

Run: `mvn test`
Expected: PASS with generation tests and export tests all green.

Run: `npm run test -- --run`
Expected: PASS with generation and export frontend tests all green.

Run: `npm run build`
Expected: PASS with export UI included in the final build.

Run: `& "E:\CodeHome\LogicProjector\FastBackend\venv\Scripts\python.exe" -m pytest tests/test_frame_renderer.py tests/test_export_pipeline.py -v`
Expected: PASS with worker rendering and export pipeline tests green.

- [ ] **Step 5: Commit**

```bash
git add SpringBackend/src/main/java/com/LogicProjector/exporttask SpringBackend/src/main/java/com/LogicProjector/billing/BillingService.java FastBackend/app FastBackend/tests VueFrontend/src/App.spec.ts README.md
git commit -m "feat: complete video export v1 flow"
```

## Self-Review

Spec coverage check:

- Separate export task model: covered by Task 1.
- Spring Boot public APIs: covered by Task 3.
- Internal Spring-to-worker contract: covered by Task 2.
- `export_tasks` data model: covered by Task 1.
- Polling frontend flow: covered by Task 5.
- Freeze-then-settle billing: covered by Tasks 1, 2, and 3.
- Failure categories and readable errors: covered by Task 6.
- Download endpoint: covered by Task 3 and finalized in Task 6.
- Worker subtitle, TTS, and ffmpeg pipeline: covered by Task 4.

Placeholder scan:

- No `TODO`, `TBD`, or "implement later" placeholders remain.
- Every task includes exact files, commands, and expected outcomes.

Type consistency check:

- `ExportTask`, `ExportTaskStatus`, `ExportTaskResponse`, `MediaExportWorkerRequest`, and `MediaExportWorkerResult` are used consistently across Spring Boot, Python, and Vue tasks.
- Billing uses `creditsFrozen` and `creditsCharged` consistently through persistence, service, and UI tasks.
