package ru.pguti.taskflow.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ru.pguti.taskflow.dto.request.TaskRequest;
import ru.pguti.taskflow.dto.response.StatisticsResponse;
import ru.pguti.taskflow.dto.response.TaskResponse;
import ru.pguti.taskflow.entity.Importance;
import ru.pguti.taskflow.entity.TaskStatus;
import ru.pguti.taskflow.service.TaskService;

import java.util.List;

@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    public ResponseEntity<TaskResponse> create(
            @Valid @RequestBody TaskRequest request,
            Authentication authentication
    ) {
        TaskResponse task = taskService.create(
                request,
                authentication.getName()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(task);
    }

    @GetMapping
    public ResponseEntity<List<TaskResponse>> getAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) Importance importance,
            @RequestParam(
                    required = false,
                    defaultValue = "priority"
            ) String sort,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                taskService.getAll(
                        authentication.getName(),
                        search,
                        status,
                        importance,
                        sort
                )
        );
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> getStatistics(
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                taskService.getStatistics(
                        authentication.getName()
                )
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> getById(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                taskService.getById(
                        id,
                        authentication.getName()
                )
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody TaskRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                taskService.update(
                        id,
                        request,
                        authentication.getName()
                )
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            Authentication authentication
    ) {
        taskService.delete(
                id,
                authentication.getName()
        );

        return ResponseEntity.noContent().build();
    }
}