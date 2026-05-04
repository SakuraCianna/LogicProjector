package com.LogicProjector.exporttask;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ExportTaskRepository extends JpaRepository<ExportTask, Long> {
    List<ExportTask> findTop8ByUser_IdOrderByUpdatedAtDesc(Long userId);

    Optional<ExportTask> findFirstByGenerationTask_IdAndUser_IdAndStatusInOrderByUpdatedAtDesc(
            Long generationTaskId,
            Long userId,
            List<ExportTaskStatus> statuses);
}
