package com.LogicProjector.account;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class DemoUserSeeder implements ApplicationRunner {

    private final UserAccountRepository repository;

    public DemoUserSeeder(UserAccountRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(ApplicationArguments args) {
        repository.findByEmail("teacher@example.com")
                .orElseGet(() -> repository.save(new UserAccount(null, "teacher@example.com", 300, 0, "ACTIVE")));
    }
}
