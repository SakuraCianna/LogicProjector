package com.LogicProjector.exporttask;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ExportTaskRepository extends JpaRepository<ExportTask, Long> {
    List<ExportTask> findTop8ByUser_IdOrderByUpdatedAtDesc(Long userId);
}
