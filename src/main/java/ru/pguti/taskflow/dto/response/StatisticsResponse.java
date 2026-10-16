package ru.pguti.taskflow.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class StatisticsResponse {

    private long totalTasks;

    private long completedTasks;

    private long inProgressTasks;

    private long todoTasks;

    private long overdueTasks;

    private double averagePriority;

    private double completionPercentage;
}