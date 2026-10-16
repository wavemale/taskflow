package ru.pguti.taskflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.pguti.taskflow.dto.request.TaskRequest;
import ru.pguti.taskflow.dto.response.StatisticsResponse;
import ru.pguti.taskflow.dto.response.TaskResponse;
import ru.pguti.taskflow.entity.Importance;
import ru.pguti.taskflow.entity.TaskStatus;
import ru.pguti.taskflow.security.JwtService;
import ru.pguti.taskflow.security.SecurityConfig;
import ru.pguti.taskflow.service.TaskService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaskController.class)
@Import(SecurityConfig.class)
@WithMockUser(username = "max@test.ru", roles = "USER")
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TaskService taskService;

    @MockitoBean
    private JwtService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private final String testEmail = "max@test.ru";

    @Test
    void shouldCreateTask() throws Exception {

        TaskRequest request = createTaskRequest();
        TaskResponse response = createTaskResponse();

        when(taskService.create(
                any(TaskRequest.class),
                eq(testEmail)
        )).thenReturn(response);

        mockMvc.perform(
                        post("/tasks")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(request)
                                )
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title")
                        .value("Изучить Spring Boot"))
                .andExpect(jsonPath("$.priorityScore").value(90));

        verify(taskService).create(
                any(TaskRequest.class),
                eq(testEmail)
        );
    }

    @Test
    void shouldGetAllTasks() throws Exception {

        TaskResponse response = createTaskResponse();

        when(taskService.getAll(
                eq(testEmail),
                eq(null),
                eq(null),
                eq(null),
                eq("priority")
        )).thenReturn(List.of(response));

        mockMvc.perform(
                        get("/tasks")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title")
                        .value("Изучить Spring Boot"));

        verify(taskService).getAll(
                eq(testEmail),
                eq(null),
                eq(null),
                eq(null),
                eq("priority")
        );
    }

    @Test
    void shouldSearchTasks() throws Exception {

        TaskResponse response = createTaskResponse();

        when(taskService.getAll(
                eq(testEmail),
                eq("Spring"),
                eq(null),
                eq(null),
                eq("priority")
        )).thenReturn(List.of(response));

        mockMvc.perform(
                        get("/tasks")
                                .param("search", "Spring")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title")
                        .value("Изучить Spring Boot"));

        verify(taskService).getAll(
                eq(testEmail),
                eq("Spring"),
                eq(null),
                eq(null),
                eq("priority")
        );
    }

    @Test
    void shouldFilterTasksByStatus() throws Exception {

        TaskResponse response = createTaskResponse();

        when(taskService.getAll(
                eq(testEmail),
                eq(null),
                eq(TaskStatus.TODO),
                eq(null),
                eq("priority")
        )).thenReturn(List.of(response));

        mockMvc.perform(
                        get("/tasks")
                                .param("status", "TODO")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status")
                        .value("TODO"));

        verify(taskService).getAll(
                eq(testEmail),
                eq(null),
                eq(TaskStatus.TODO),
                eq(null),
                eq("priority")
        );
    }

    @Test
    void shouldFilterTasksByImportance() throws Exception {

        TaskResponse response = createTaskResponse();

        when(taskService.getAll(
                eq(testEmail),
                eq(null),
                eq(null),
                eq(Importance.HIGH),
                eq("priority")
        )).thenReturn(List.of(response));

        mockMvc.perform(
                        get("/tasks")
                                .param("importance", "HIGH")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].importance")
                        .value("HIGH"));

        verify(taskService).getAll(
                eq(testEmail),
                eq(null),
                eq(null),
                eq(Importance.HIGH),
                eq("priority")
        );
    }

    @Test
    void shouldSortTasksByDeadline() throws Exception {

        TaskResponse response = createTaskResponse();

        when(taskService.getAll(
                eq(testEmail),
                eq(null),
                eq(null),
                eq(null),
                eq("deadline")
        )).thenReturn(List.of(response));

        mockMvc.perform(
                        get("/tasks")
                                .param("sort", "deadline")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].deadline")
                        .value("2026-09-09"));

        verify(taskService).getAll(
                eq(testEmail),
                eq(null),
                eq(null),
                eq(null),
                eq("deadline")
        );
    }

    @Test
    void shouldGetTaskById() throws Exception {

        TaskResponse response = createTaskResponse();

        when(taskService.getById(
                eq(1L),
                eq(testEmail)
        )).thenReturn(response);

        mockMvc.perform(
                        get("/tasks/1")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title")
                        .value("Изучить Spring Boot"));

        verify(taskService).getById(
                eq(1L),
                eq(testEmail)
        );
    }

    @Test
    void shouldUpdateTask() throws Exception {

        TaskRequest request = createTaskRequest();
        TaskResponse response = createTaskResponse();

        when(taskService.update(
                eq(1L),
                any(TaskRequest.class),
                eq(testEmail)
        )).thenReturn(response);

        mockMvc.perform(
                        put("/tasks/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(request)
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title")
                        .value("Изучить Spring Boot"));

        verify(taskService).update(
                eq(1L),
                any(TaskRequest.class),
                eq(testEmail)
        );
    }

    @Test
    void shouldDeleteTask() throws Exception {

        doNothing().when(taskService)
                .delete(eq(1L), eq(testEmail));

        mockMvc.perform(
                        delete("/tasks/1")
                )
                .andExpect(status().isNoContent());

        verify(taskService).delete(
                eq(1L),
                eq(testEmail)
        );
    }

    @Test
    void shouldGetStatistics() throws Exception {

        StatisticsResponse response = StatisticsResponse.builder()
                .totalTasks(10)
                .completedTasks(4)
                .inProgressTasks(3)
                .todoTasks(3)
                .overdueTasks(2)
                .averagePriority(62.5)
                .completionPercentage(40.0)
                .build();

        when(taskService.getStatistics(testEmail))
                .thenReturn(response);

        mockMvc.perform(
                        get("/tasks/statistics")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTasks").value(10))
                .andExpect(jsonPath("$.completedTasks").value(4))
                .andExpect(jsonPath("$.inProgressTasks").value(3))
                .andExpect(jsonPath("$.todoTasks").value(3))
                .andExpect(jsonPath("$.overdueTasks").value(2))
                .andExpect(jsonPath("$.averagePriority").value(62.5))
                .andExpect(jsonPath("$.completionPercentage")
                        .value(40.0));

        verify(taskService).getStatistics(testEmail);
    }

    @Test
    void shouldRejectTaskWithEmptyTitle() throws Exception {

        TaskRequest request = createTaskRequest();
        request.setTitle("");

        mockMvc.perform(
                        post("/tasks")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(request)
                                )
                )
                .andExpect(status().isBadRequest());

        verify(taskService, never()).create(
                any(TaskRequest.class),
                any(String.class)
        );
    }

    @Test
    void shouldRejectTaskWithoutDeadline() throws Exception {

        TaskRequest request = createTaskRequest();
        request.setDeadline(null);

        mockMvc.perform(
                        post("/tasks")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(request)
                                )
                )
                .andExpect(status().isBadRequest());

        verify(taskService, never()).create(
                any(TaskRequest.class),
                any(String.class)
        );
    }

    @Test
    void shouldRejectTaskWithoutImportance() throws Exception {

        TaskRequest request = createTaskRequest();
        request.setImportance(null);

        mockMvc.perform(
                        post("/tasks")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(request)
                                )
                )
                .andExpect(status().isBadRequest());

        verify(taskService, never()).create(
                any(TaskRequest.class),
                any(String.class)
        );
    }

    private TaskRequest createTaskRequest() {

        TaskRequest request = new TaskRequest();

        request.setTitle("Изучить Spring Boot");
        request.setDescription(
                "Изучить тестирование контроллеров"
        );
        request.setDeadline(
                LocalDate.of(2026, 9, 9)
        );
        request.setImportance(Importance.HIGH);
        request.setStatus(TaskStatus.TODO);

        return request;
    }

    private TaskResponse createTaskResponse() {

        return TaskResponse.builder()
                .id(1L)
                .title("Изучить Spring Boot")
                .description(
                        "Изучить тестирование контроллеров"
                )
                .deadline(
                        LocalDate.of(2026, 9, 9)
                )
                .importance("HIGH")
                .status("TODO")
                .priorityScore(90)
                .createdAt(
                        LocalDateTime.of(
                                2026, 9, 8, 12, 0
                        )
                )
                .updatedAt(
                        LocalDateTime.of(
                                2026, 9, 8, 12, 0
                        )
                )
                .build();
    }
}