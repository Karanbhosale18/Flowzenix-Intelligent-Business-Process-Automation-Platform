package com.example.authapp.repository;

import com.example.authapp.entity.User;
import com.example.authapp.entity.WorkflowInstance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkflowInstanceRepository extends JpaRepository<WorkflowInstance, Long> {
    List<WorkflowInstance> findByCreatedByOrderByCreatedAtDesc(User createdBy);
    List<WorkflowInstance> findAllByOrderByCreatedAtDesc();
}
