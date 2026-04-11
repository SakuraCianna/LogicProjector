package com.LogicProjector.systemlog;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemLogEntryRepository extends JpaRepository<SystemLogEntry, Long> {
}
