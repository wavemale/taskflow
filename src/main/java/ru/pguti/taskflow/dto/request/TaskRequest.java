package ru.pguti.taskflow.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import ru.pguti.taskflow.entity.Importance;
import ru.pguti.taskflow.entity.TaskStatus;

import java.time.LocalDate;

@Data
public class TaskRequest {

    @NotBlank(message = "Название задачи не может быть пустым")
    @Size(max = 100, message = "Название задачи не должно превышать 100 символов")
    private String title;

    @Size(max = 500, message = "Описание не должно превышать 500 символов")
    private String description;

    @NotNull(message = "Дедлайн обязателен")
    private LocalDate deadline;

    @NotNull(message = "Важность задачи обязательна")
    private Importance importance;

    @NotNull(message = "Статус задачи обязателен")
    private TaskStatus status;
}