package com.LogicProjector.generation;

import static org.assertj.core.api.Assertions.assertThat;

import com.LogicProjector.account.UserAccount;
import com.LogicProjector.account.UserAccountRepository;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@Disabled("Requires an explicit MySQL test database; this project does not include an embedded JPA test database.")
@DataJpaTest
class GenerationTaskPersistenceTest {

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private GenerationTaskRepository generationTaskRepository;

    @Test
    void shouldPersistTaskForTeacher() {
        UserAccount user = userAccountRepository.save(new UserAccount(null, "teacher", "hash", 200, 0, "ACTIVE"));

        GenerationTask task = generationTaskRepository.save(
                GenerationTask.pending(user, "public class Demo {}", "java")
        );

        assertThat(task.getId()).isNotNull();
        assertThat(task.getStatus()).isEqualTo(GenerationTaskStatus.PENDING);
        assertThat(task.getUser().getUsername()).isEqualTo("teacher");
    }
}
