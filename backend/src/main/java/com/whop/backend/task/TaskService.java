package com.whop.backend.task;

import com.whop.backend.auth.AuthUserPrincipal;
import com.whop.backend.auth.UserEntity;
import com.whop.backend.auth.UserRepository;
import com.whop.backend.task.TaskDtos.CreateTaskRequest;
import com.whop.backend.task.TaskDtos.TaskResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskService {
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public TaskService(TaskRepository taskRepository, UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> listOpenTasks() {
        return taskRepository.findByStatusOrderByCreatedAtDesc(TaskStatus.OPEN).stream()
                .map(TaskResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> listMyJobs(Authentication authentication) {
        AuthUserPrincipal principal = (AuthUserPrincipal) authentication.getPrincipal();
        return taskRepository.findJobsForUserByStatus(principal.getId(), TaskStatus.ASSIGNED).stream()
                .map(TaskResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public TaskResponse getTask(UUID id) {
        return taskRepository
                .findById(id)
                .map(TaskResponse::from)
                .orElseThrow(() -> new TaskNotFoundException(id));
    }

    @Transactional
    public TaskResponse createTask(CreateTaskRequest request, Authentication authentication) {
        AuthUserPrincipal principal = (AuthUserPrincipal) authentication.getPrincipal();
        UserEntity owner =
                userRepository
                        .findById(principal.getId())
                        .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));

        TaskEntity task = new TaskEntity();
        task.setOwner(owner);
        task.setTitle(request.title().trim());
        task.setDescription(request.description().trim());
        task.setBudgetAmount(request.budgetAmount());
        task.setBudgetCurrency(request.budgetCurrency());

        return TaskResponse.from(taskRepository.save(task));
    }
}
