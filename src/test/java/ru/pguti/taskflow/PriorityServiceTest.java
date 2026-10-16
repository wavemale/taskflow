package ru.pguti.taskflow.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.pguti.taskflow.entity.Importance;
import ru.pguti.taskflow.entity.Task;
import ru.pguti.taskflow.entity.TaskStatus;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PriorityServiceTest {

    private PriorityService priorityService;

    @BeforeEach
    void setUp() {
        priorityService = new PriorityService();
    }

    @Test
    void shouldCalculateHighPriorityForUrgentTask() {

        Task task = Task.builder()
                .deadline(LocalDate.now().plusDays(1))
                .importance(Importance.HIGH)
                .status(TaskStatus.TODO)
                .build();

        int priority = priorityService.calculatePriority(task);

        assertEquals(90, priority);
    }

    @Test
    void shouldCalculateMediumPriorityForTaskInProgress() {

        Task task = Task.builder()
                .deadline(LocalDate.now().plusDays(5))
                .importance(Importance.MEDIUM)
                .status(TaskStatus.IN_PROGRESS)
                .build();

        int priority = priorityService.calculatePriority(task);

        assertEquals(55, priority);
    }

    @Test
    void shouldGiveMaximumPriorityToOverdueImportantTask() {

        Task task = Task.builder()
                .deadline(LocalDate.now().minusDays(10))
                .importance(Importance.HIGH)
                .status(TaskStatus.TODO)
                .build();

        int priority = priorityService.calculatePriority(task);

        assertEquals(100, priority);
    }

    @Test
    void shouldNotAddStatusScoreForCompletedTask() {

        Task task = Task.builder()
                .deadline(LocalDate.now().plusDays(30))
                .importance(Importance.HIGH)
                .status(TaskStatus.DONE)
                .build();

        int priority = priorityService.calculatePriority(task);

        assertEquals(35, priority);
    }

    @Test
    void shouldCalculateLowPriorityForLowImportanceTask() {

        Task task = Task.builder()
                .deadline(LocalDate.now().plusDays(30))
                .importance(Importance.LOW)
                .status(TaskStatus.TODO)
                .build();

        int priority = priorityService.calculatePriority(task);

        assertEquals(35, priority);
    }

    @Test
    void shouldReturnZeroWhenAllParametersAreMissing() {

        Task task = Task.builder()
                .deadline(null)
                .importance(null)
                .status(null)
                .build();

        int priority = priorityService.calculatePriority(task);

        assertEquals(0, priority);
    }

    @Test
    void shouldNeverExceed100() {

        Task task = Task.builder()
                .deadline(LocalDate.now().minusDays(20))
                .importance(Importance.HIGH)
                .status(TaskStatus.TODO)
                .build();

        int priority = priorityService.calculatePriority(task);

        assertEquals(100, priority);
    }
}