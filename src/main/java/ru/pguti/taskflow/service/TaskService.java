package ru.pguti.taskflow.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.pguti.taskflow.dto.request.TaskRequest;
import ru.pguti.taskflow.dto.response.StatisticsResponse;
import ru.pguti.taskflow.dto.response.TaskResponse;
import ru.pguti.taskflow.entity.Importance;
import ru.pguti.taskflow.entity.Task;
import ru.pguti.taskflow.entity.TaskStatus;
import ru.pguti.taskflow.entity.User;
import ru.pguti.taskflow.exception.ResourceNotFoundException;
import ru.pguti.taskflow.repository.TaskRepository;
import ru.pguti.taskflow.repository.UserRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final PriorityService priorityService;

    public TaskResponse create(TaskRequest request, String email) {

        User user = getUserByEmail(email);

        Task task = Task.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .deadline(request.getDeadline())
                .importance(request.getImportance())
                .status(request.getStatus())
                .priorityScore(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .user(user)
                .build();

        task.setPriorityScore(
                priorityService.calculatePriority(task)
        );

        return toResponse(taskRepository.save(task));
    }

    public List<TaskResponse> getAll(
            String email,
            String search,
            TaskStatus status,
            Importance importance,
            String sort
    ) {

        User user = getUserByEmail(email);

        List<Task> tasks = taskRepository.findAllByUser(user);

        if (search != null && !search.isBlank()) {

            String searchText = search.toLowerCase();

            tasks = tasks.stream()
                    .filter(task ->
                            task.getTitle() != null &&
                                    task.getTitle()
                                            .toLowerCase()
                                            .contains(searchText)
                    )
                    .toList();
        }

        if (status != null) {

            tasks = tasks.stream()
                    .filter(task ->
                            task.getStatus() == status
                    )
                    .toList();
        }

        if (importance != null) {

            tasks = tasks.stream()
                    .filter(task ->
                            task.getImportance() == importance
                    )
                    .toList();
        }

        if (sort != null) {

            switch (sort.toLowerCase()) {

                case "priority" -> tasks = tasks.stream()
                        .sorted(
                                Comparator.comparing(
                                        Task::getPriorityScore,
                                        Comparator.nullsLast(
                                                Comparator.reverseOrder()
                                        )
                                )
                        )
                        .toList();

                case "deadline" -> tasks = tasks.stream()
                        .sorted(
                                Comparator.comparing(
                                        Task::getDeadline,
                                        Comparator.nullsLast(
                                                Comparator.naturalOrder()
                                        )
                                )
                        )
                        .toList();

                case "created" -> tasks = tasks.stream()
                        .sorted(
                                Comparator.comparing(
                                        Task::getCreatedAt,
                                        Comparator.nullsLast(
                                                Comparator.reverseOrder()
                                        )
                                )
                        )
                        .toList();

                case "title" -> tasks = tasks.stream()
                        .sorted(
                                Comparator.comparing(
                                        Task::getTitle,
                                        Comparator.nullsLast(
                                                String.CASE_INSENSITIVE_ORDER
                                        )
                                )
                        )
                        .toList();
            }
        }

        return tasks.stream()
                .map(this::toResponse)
                .toList();
    }

    public StatisticsResponse getStatistics(String email) {

        User user = getUserByEmail(email);

        List<Task> tasks = taskRepository.findAllByUser(user);

        long totalTasks = tasks.size();

        long completedTasks = tasks.stream()
                .filter(task ->
                        task.getStatus() == TaskStatus.DONE
                )
                .count();

        long inProgressTasks = tasks.stream()
                .filter(task ->
                        task.getStatus() == TaskStatus.IN_PROGRESS
                )
                .count();

        long todoTasks = tasks.stream()
                .filter(task ->
                        task.getStatus() == TaskStatus.TODO
                )
                .count();

        long overdueTasks = tasks.stream()
                .filter(task ->
                        task.getDeadline() != null
                                && task.getDeadline().isBefore(LocalDate.now())
                                && task.getStatus() != TaskStatus.DONE
                )
                .count();

        double averagePriority = tasks.stream()
                .map(Task::getPriorityScore)
                .filter(score -> score != null)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);

        double completionPercentage = totalTasks == 0
                ? 0.0
                : (completedTasks * 100.0) / totalTasks;

        return StatisticsResponse.builder()
                .totalTasks(totalTasks)
                .completedTasks(completedTasks)
                .inProgressTasks(inProgressTasks)
                .todoTasks(todoTasks)
                .overdueTasks(overdueTasks)
                .averagePriority(
                        Math.round(averagePriority * 100.0) / 100.0
                )
                .completionPercentage(
                        Math.round(completionPercentage * 100.0) / 100.0
                )
                .build();
    }

    public TaskResponse getById(Long id, String email) {

        User user = getUserByEmail(email);

        Task task = taskRepository.findByIdAndUser(id, user)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Задача не найдена"
                        )
                );

        return toResponse(task);
    }

    public TaskResponse update(
            Long id,
            TaskRequest request,
            String email
    ) {

        User user = getUserByEmail(email);

        Task task = taskRepository.findByIdAndUser(id, user)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Задача не найдена"
                        )
                );

        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setDeadline(request.getDeadline());
        task.setImportance(request.getImportance());
        task.setStatus(request.getStatus());
        task.setUpdatedAt(LocalDateTime.now());

        task.setPriorityScore(
                priorityService.calculatePriority(task)
        );

        return toResponse(taskRepository.save(task));
    }

    public void delete(Long id, String email) {

        User user = getUserByEmail(email);

        Task task = taskRepository.findByIdAndUser(id, user)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Задача не найдена"
                        )
                );

        taskRepository.delete(task);
    }

    private User getUserByEmail(String email) {

        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Пользователь не найден"
                        )
                );
    }

    private TaskResponse toResponse(Task task) {

        return TaskResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .deadline(task.getDeadline())
                .importance(
                        task.getImportance() != null
                                ? task.getImportance().name()
                                : null
                )
                .status(
                        task.getStatus() != null
                                ? task.getStatus().name()
                                : null
                )
                .priorityScore(task.getPriorityScore())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }
}