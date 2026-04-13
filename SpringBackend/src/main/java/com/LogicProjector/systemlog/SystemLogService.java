package com.LogicProjector.systemlog;

import java.time.Instant;

import org.springframework.stereotype.Service;

@Service
public class SystemLogService {

    private final SystemLogEntryRepository repository;

    public SystemLogService(SystemLogEntryRepository repository) {
        this.repository = repository;
    }

    public void info(Long userId, Long taskId, String module, String message) {
        repository.save(new SystemLogEntry(null, taskId, userId, "INFO", module, message, null, Instant.now()));
    }

    public void error(Long userId, Long taskId, String module, String message, String details) {
        repository.save(new SystemLogEntry(null, taskId, userId, "ERROR", module, message, details, Instant.now()));
    }
}
