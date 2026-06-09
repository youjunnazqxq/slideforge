package com.slideforge.api.workflow;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowRunRepository extends JpaRepository<WorkflowRun, UUID> {
}

