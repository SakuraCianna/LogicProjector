package com.LogicProjector.generation;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GenerationTaskRepository extends JpaRepository<GenerationTask, Long> {
    List<GenerationTask> findTop8ByUser_IdOrderByUpdatedAtDesc(Long userId);
}
