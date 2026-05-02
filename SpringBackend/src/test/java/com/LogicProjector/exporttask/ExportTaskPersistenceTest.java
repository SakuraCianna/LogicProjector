package com.LogicProjector.exporttask;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.LogicProjector.account.UserAccount;
import com.LogicProjector.account.UserAccountRepository;
import com.LogicProjector.generation.GenerationTask;
import com.LogicProjector.generation.GenerationTaskRepository;
import com.LogicProjector.generation.GenerationTaskStatus;
import com.fasterxml.jackson.databind.ObjectMapper;

@Disabled("Requires an explicit MySQL test database; this project does not include an embedded JPA test database.")
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
        UserAccount user = userAccountRepository.save(new UserAccount(null, "teacher", "hash", 120, 0, "ACTIVE"));
        GenerationTask generationTask = generationTaskRepository.save(GenerationTask.pending(user, "class Demo {}", "java"));
        generationTask.complete("QUICK_SORT", 0.92, new ObjectMapper().createObjectNode(), "summary");

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
