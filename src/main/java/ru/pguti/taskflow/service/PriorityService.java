package ru.pguti.taskflow.service;

import org.springframework.stereotype.Service;
import ru.pguti.taskflow.entity.Importance;
import ru.pguti.taskflow.entity.Task;
import ru.pguti.taskflow.entity.TaskStatus;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
public class PriorityService {

    private static final int MAX_PRIORITY = 100;

    public int calculatePriority(Task task) {

        int deadlineScore = calculateDeadlineScore(
                task.getDeadline()
        );

        int importanceScore = calculateImportanceScore(
                task.getImportance()
        );

        int statusScore = calculateStatusScore(
                task.getStatus()
        );

        int totalScore =
                deadlineScore
                        + importanceScore
                        + statusScore;

        return Math.min(totalScore, MAX_PRIORITY);
    }

    private int calculateDeadlineScore(LocalDate deadline) {

        if (deadline == null) {
            return 0;
        }

        long daysUntilDeadline =
                ChronoUnit.DAYS.between(
                        LocalDate.now(),
                        deadline
                );

        if (daysUntilDeadline < -7) {
            return 50;
        }

        if (daysUntilDeadline < 0) {
            return 48;
        }

        if (daysUntilDeadline == 0) {
            return 45;
        }

        if (daysUntilDeadline == 1) {
            return 40;
        }

        if (daysUntilDeadline <= 3) {
            return 35;
        }

        if (daysUntilDeadline <= 7) {
            return 25;
        }

        if (daysUntilDeadline <= 14) {
            return 15;
        }

        return 5;
    }

    private int calculateImportanceScore(
            Importance importance
    ) {

        if (importance == null) {
            return 0;
        }

        return switch (importance) {
            case LOW -> 10;
            case MEDIUM -> 20;
            case HIGH -> 30;
        };
    }

    private int calculateStatusScore(
            TaskStatus status
    ) {

        if (status == null) {
            return 0;
        }

        return switch (status) {
            case TODO -> 20;
            case IN_PROGRESS -> 10;
            case DONE -> 0;
        };
    }
}