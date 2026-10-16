package ru.pguti.taskflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.pguti.taskflow.entity.Task;
import ru.pguti.taskflow.entity.User;

import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findAllByUser(User user);

    List<Task> findAllByUserOrderByPriorityScoreDesc(User user);

    List<Task> findAllByUserOrderByDeadlineAsc(User user);

    List<Task> findAllByUserOrderByCreatedAtDesc(User user);

    List<Task> findAllByUserAndTitleContainingIgnoreCase(
            User user,
            String title
    );

    List<Task> findAllByUserAndStatus(
            User user,
            ru.pguti.taskflow.entity.TaskStatus status
    );

    List<Task> findAllByUserAndImportance(
            User user,
            ru.pguti.taskflow.entity.Importance importance
    );

    Optional<Task> findByIdAndUser(Long id, User user);
}