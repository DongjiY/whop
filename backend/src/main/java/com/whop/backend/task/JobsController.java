package com.whop.backend.task;

import com.whop.backend.task.TaskDtos.TaskResponse;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/jobs")
public class JobsController {
    private final TaskService taskService;

    public JobsController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public ResponseEntity<List<TaskResponse>> listMyJobs(Authentication authentication) {
        return ResponseEntity.ok(taskService.listMyJobs(authentication));
    }
}
