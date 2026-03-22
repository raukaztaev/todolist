package com.example.todolist.util;

import com.example.todolist.entity.Todo;
import com.example.todolist.enums.Priority;
import java.time.LocalDateTime;
import org.springframework.data.jpa.domain.Specification;

public final class TodoSpecifications {

    private TodoSpecifications() {
    }

    public static Specification<Todo> ownedBy(Long ownerId) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("owner").get("id"), ownerId);
    }

    public static Specification<Todo> hasCompleted(Boolean completed) {
        return completed == null
                ? null
                : (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("completed"), completed);
    }

    public static Specification<Todo> hasPriority(Priority priority) {
        return priority == null
                ? null
                : (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("priority"), priority);
    }

    public static Specification<Todo> dueDateFrom(LocalDateTime dueDateFrom) {
        return dueDateFrom == null
                ? null
                : (root, query, criteriaBuilder) -> criteriaBuilder.greaterThanOrEqualTo(root.get("dueDate"), dueDateFrom);
    }

    public static Specification<Todo> dueDateTo(LocalDateTime dueDateTo) {
        return dueDateTo == null
                ? null
                : (root, query, criteriaBuilder) -> criteriaBuilder.lessThanOrEqualTo(root.get("dueDate"), dueDateTo);
    }
}
