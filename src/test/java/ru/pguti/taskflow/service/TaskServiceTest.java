package ru.pguti.taskflow.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PriorityService priorityService;

    @InjectMocks
    private TaskService taskService;

    private User user;
    private Task task;
    private TaskRequest request;

    @BeforeEach
    void setUp() {

        user = User.builder()
                .id(1L)
                .email("max@test.ru")
                .build();

        task = Task.builder()
                .id(1L)
                .title("Изучить Spring Boot")
                .description("Изучить сервисы и тестирование")
                .deadline(LocalDate.of(2026, 9, 10))
                .importance(Importance.HIGH)
                .status(TaskStatus.TODO)
                .priorityScore(80)
                .createdAt(LocalDateTime.of(2026, 9, 8, 12, 0))
                .updatedAt(LocalDateTime.of(2026, 9, 8, 12, 0))
                .user(user)
                .build();

        request = new TaskRequest();
        request.setTitle("Изучить Spring Boot");
        request.setDescription("Изучить сервисы и тестирование");
        request.setDeadline(LocalDate.of(2026, 9, 10));
        request.setImportance(Importance.HIGH);
        request.setStatus(TaskStatus.TODO);
    }

    @Test
    void shouldCreateTask() {

        when(userRepository.findByEmail("max@test.ru"))
                .thenReturn(Optional.of(user));

        when(priorityService.calculatePriority(any(Task.class)))
                .thenReturn(80);

        when(taskRepository.save(any(Task.class)))
                .thenReturn(task);

        TaskResponse response = taskService.create(
                request,
                "max@test.ru"
        );

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(
                "Изучить Spring Boot",
                response.getTitle()
        );
        assertEquals("HIGH", response.getImportance());
        assertEquals("TODO", response.getStatus());
        assertEquals(80, response.getPriorityScore());

        verify(userRepository).findByEmail("max@test.ru");
        verify(priorityService).calculatePriority(any(Task.class));
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    void shouldGetAllTasks() {

        when(userRepository.findByEmail("max@test.ru"))
                .thenReturn(Optional.of(user));

        when(taskRepository.findAllByUser(user))
                .thenReturn(List.of(task));

        List<TaskResponse> responses = taskService.getAll(
                "max@test.ru",
                null,
                null,
                null,
                "priority"
        );

        assertEquals(1, responses.size());
        assertEquals(
                "Изучить Spring Boot",
                responses.get(0).getTitle()
        );

        verify(taskRepository).findAllByUser(user);
    }

    @Test
    void shouldSearchTasksByTitle() {

        when(userRepository.findByEmail("max@test.ru"))
                .thenReturn(Optional.of(user));

        when(taskRepository.findAllByUser(user))
                .thenReturn(List.of(
                        task,
                        createAnotherTask("Купить молоко")
                ));

        List<TaskResponse> responses = taskService.getAll(
                "max@test.ru",
                "Spring",
                null,
                null,
                "priority"
        );

        assertEquals(1, responses.size());
        assertEquals(
                "Изучить Spring Boot",
                responses.get(0).getTitle()
        );
    }

    @Test
    void shouldFilterTasksByStatus() {

        Task doneTask = createAnotherTask("Готовая задача");
        doneTask.setStatus(TaskStatus.DONE);

        when(userRepository.findByEmail("max@test.ru"))
                .thenReturn(Optional.of(user));

        when(taskRepository.findAllByUser(user))
                .thenReturn(List.of(task, doneTask));

        List<TaskResponse> responses = taskService.getAll(
                "max@test.ru",
                null,
                TaskStatus.DONE,
                null,
                "priority"
        );

        assertEquals(1, responses.size());
        assertEquals(
                "Готовая задача",
                responses.get(0).getTitle()
        );
        assertEquals(
                "DONE",
                responses.get(0).getStatus()
        );
    }

    @Test
    void shouldFilterTasksByImportance() {

        Task lowTask = createAnotherTask("Простая задача");
        lowTask.setImportance(Importance.LOW);

        when(userRepository.findByEmail("max@test.ru"))
                .thenReturn(Optional.of(user));

        when(taskRepository.findAllByUser(user))
                .thenReturn(List.of(task, lowTask));

        List<TaskResponse> responses = taskService.getAll(
                "max@test.ru",
                null,
                null,
                Importance.LOW,
                "priority"
        );

        assertEquals(1, responses.size());
        assertEquals(
                "Простая задача",
                responses.get(0).getTitle()
        );
        assertEquals(
                "LOW",
                responses.get(0).getImportance()
        );
    }

    @Test
    void shouldSortTasksByPriority() {

        Task lowPriorityTask =
                createAnotherTask("Низкий приоритет");

        lowPriorityTask.setPriorityScore(20);

        task.setPriorityScore(90);

        when(userRepository.findByEmail("max@test.ru"))
                .thenReturn(Optional.of(user));

        when(taskRepository.findAllByUser(user))
                .thenReturn(List.of(
                        lowPriorityTask,
                        task
                ));

        List<TaskResponse> responses = taskService.getAll(
                "max@test.ru",
                null,
                null,
                null,
                "priority"
        );

        assertEquals(2, responses.size());
        assertEquals(
                90,
                responses.get(0).getPriorityScore()
        );
        assertEquals(
                20,
                responses.get(1).getPriorityScore()
        );
    }

    @Test
    void shouldSortTasksByDeadline() {

        Task laterTask =
                createAnotherTask("Поздняя задача");

        laterTask.setDeadline(
                LocalDate.of(2026, 9, 20)
        );

        task.setDeadline(
                LocalDate.of(2026, 9, 10)
        );

        when(userRepository.findByEmail("max@test.ru"))
                .thenReturn(Optional.of(user));

        when(taskRepository.findAllByUser(user))
                .thenReturn(List.of(
                        laterTask,
                        task
                ));

        List<TaskResponse> responses = taskService.getAll(
                "max@test.ru",
                null,
                null,
                null,
                "deadline"
        );

        assertEquals(2, responses.size());
        assertEquals(
                LocalDate.of(2026, 9, 10),
                responses.get(0).getDeadline()
        );
        assertEquals(
                LocalDate.of(2026, 9, 20),
                responses.get(1).getDeadline()
        );
    }

    @Test
    void shouldSortTasksByCreatedDate() {

        Task olderTask =
                createAnotherTask("Старая задача");

        olderTask.setCreatedAt(
                LocalDateTime.of(2026, 9, 1, 12, 0)
        );

        task.setCreatedAt(
                LocalDateTime.of(2026, 9, 8, 12, 0)
        );

        when(userRepository.findByEmail("max@test.ru"))
                .thenReturn(Optional.of(user));

        when(taskRepository.findAllByUser(user))
                .thenReturn(List.of(
                        olderTask,
                        task
                ));

        List<TaskResponse> responses = taskService.getAll(
                "max@test.ru",
                null,
                null,
                null,
                "created"
        );

        assertEquals(2, responses.size());
        assertEquals(
                "Изучить Spring Boot",
                responses.get(0).getTitle()
        );
        assertEquals(
                "Старая задача",
                responses.get(1).getTitle()
        );
    }

    @Test
    void shouldSortTasksByTitle() {

        Task taskA = createAnotherTask("Анализ проекта");
        Task taskB = createAnotherTask("Разработка проекта");

        when(userRepository.findByEmail("max@test.ru"))
                .thenReturn(Optional.of(user));

        when(taskRepository.findAllByUser(user))
                .thenReturn(List.of(
                        taskB,
                        taskA
                ));

        List<TaskResponse> responses = taskService.getAll(
                "max@test.ru",
                null,
                null,
                null,
                "title"
        );

        assertEquals(2, responses.size());
        assertEquals(
                "Анализ проекта",
                responses.get(0).getTitle()
        );
        assertEquals(
                "Разработка проекта",
                responses.get(1).getTitle()
        );
    }

    @Test
    void shouldGetTaskById() {

        when(userRepository.findByEmail("max@test.ru"))
                .thenReturn(Optional.of(user));

        when(taskRepository.findByIdAndUser(1L, user))
                .thenReturn(Optional.of(task));

        TaskResponse response = taskService.getById(
                1L,
                "max@test.ru"
        );

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(
                "Изучить Spring Boot",
                response.getTitle()
        );

        verify(taskRepository)
                .findByIdAndUser(1L, user);
    }

    @Test
    void shouldThrowExceptionWhenTaskNotFound() {

        when(userRepository.findByEmail("max@test.ru"))
                .thenReturn(Optional.of(user));

        when(taskRepository.findByIdAndUser(999L, user))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> taskService.getById(
                        999L,
                        "max@test.ru"
                )
        );
    }

    @Test
    void shouldUpdateTask() {

        when(userRepository.findByEmail("max@test.ru"))
                .thenReturn(Optional.of(user));

        when(taskRepository.findByIdAndUser(1L, user))
                .thenReturn(Optional.of(task));

        when(priorityService.calculatePriority(any(Task.class)))
                .thenReturn(95);

        when(taskRepository.save(any(Task.class)))
                .thenReturn(task);

        request.setTitle("Изучить Spring Security");
        request.setImportance(Importance.HIGH);
        request.setStatus(TaskStatus.IN_PROGRESS);

        TaskResponse response = taskService.update(
                1L,
                request,
                "max@test.ru"
        );

        assertNotNull(response);

        verify(taskRepository)
                .findByIdAndUser(1L, user);

        verify(priorityService)
                .calculatePriority(any(Task.class));

        verify(taskRepository)
                .save(task);
    }

    @Test
    void shouldDeleteTask() {

        when(userRepository.findByEmail("max@test.ru"))
                .thenReturn(Optional.of(user));

        when(taskRepository.findByIdAndUser(1L, user))
                .thenReturn(Optional.of(task));

        taskService.delete(
                1L,
                "max@test.ru"
        );

        verify(taskRepository)
                .findByIdAndUser(1L, user);

        verify(taskRepository)
                .delete(task);
    }

    @Test
    void shouldThrowExceptionWhenDeletingMissingTask() {

        when(userRepository.findByEmail("max@test.ru"))
                .thenReturn(Optional.of(user));

        when(taskRepository.findByIdAndUser(999L, user))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> taskService.delete(
                        999L,
                        "max@test.ru"
                )
        );

        verify(taskRepository, never())
                .delete(any(Task.class));
    }

    @Test
    void shouldCalculateStatistics() {

        Task completedTask =
                createAnotherTask("Завершённая задача");
        completedTask.setStatus(TaskStatus.DONE);
        completedTask.setPriorityScore(80);

        Task inProgressTask =
                createAnotherTask("Задача в работе");
        inProgressTask.setStatus(TaskStatus.IN_PROGRESS);
        inProgressTask.setPriorityScore(60);

        Task todoTask =
                createAnotherTask("Обычная задача");
        todoTask.setStatus(TaskStatus.TODO);
        todoTask.setPriorityScore(40);

        Task overdueTask =
                createAnotherTask("Просроченная задача");
        overdueTask.setStatus(TaskStatus.TODO);
        overdueTask.setDeadline(
                LocalDate.now().minusDays(2)
        );
        overdueTask.setPriorityScore(90);

        when(userRepository.findByEmail("max@test.ru"))
                .thenReturn(Optional.of(user));

        when(taskRepository.findAllByUser(user))
                .thenReturn(List.of(
                        completedTask,
                        inProgressTask,
                        todoTask,
                        overdueTask
                ));

        StatisticsResponse response =
                taskService.getStatistics("max@test.ru");

        assertNotNull(response);
        assertEquals(4, response.getTotalTasks());
        assertEquals(1, response.getCompletedTasks());
        assertEquals(1, response.getInProgressTasks());
        assertEquals(2, response.getTodoTasks());
        assertEquals(1, response.getOverdueTasks());
        assertEquals(67.5, response.getAveragePriority());
        assertEquals(25.0, response.getCompletionPercentage());

        verify(taskRepository)
                .findAllByUser(user);
    }

    @Test
    void shouldReturnEmptyStatisticsForUserWithoutTasks() {

        when(userRepository.findByEmail("max@test.ru"))
                .thenReturn(Optional.of(user));

        when(taskRepository.findAllByUser(user))
                .thenReturn(List.of());

        StatisticsResponse response =
                taskService.getStatistics("max@test.ru");

        assertNotNull(response);
        assertEquals(0, response.getTotalTasks());
        assertEquals(0, response.getCompletedTasks());
        assertEquals(0, response.getInProgressTasks());
        assertEquals(0, response.getTodoTasks());
        assertEquals(0, response.getOverdueTasks());
        assertEquals(0.0, response.getAveragePriority());
        assertEquals(0.0, response.getCompletionPercentage());
    }

    @Test
    void shouldThrowExceptionWhenUserNotFound() {

        when(userRepository.findByEmail("unknown@test.ru"))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> taskService.getAll(
                        "unknown@test.ru",
                        null,
                        null,
                        null,
                        "priority"
                )
        );

        verify(taskRepository, never())
                .findAllByUser(any(User.class));
    }

    private Task createAnotherTask(String title) {

        return Task.builder()
                .id(2L)
                .title(title)
                .description("Описание")
                .deadline(LocalDate.of(2026, 9, 15))
                .importance(Importance.MEDIUM)
                .status(TaskStatus.TODO)
                .priorityScore(50)
                .createdAt(
                        LocalDateTime.of(
                                2026,
                                9,
                                8,
                                13,
                                0
                        )
                )
                .updatedAt(
                        LocalDateTime.of(
                                2026,
                                9,
                                8,
                                13,
                                0
                        )
                )
                .user(user)
                .build();
    }
}