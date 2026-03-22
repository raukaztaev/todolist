package com.example.todolist.mapper;

import com.example.todolist.dto.todo.request.CreateTodoRequest;
import com.example.todolist.dto.todo.request.UpdateTodoRequest;
import com.example.todolist.dto.todo.response.TodoResponse;
import com.example.todolist.entity.Todo;
import com.example.todolist.entity.User;
import org.springframework.stereotype.Component;

@Component
public class TodoMapper {

    public Todo toEntity(CreateTodoRequest request, User owner) {
        return Todo.builder()
                .title(request.title())
                .description(request.description())
                .completed(false)
                .priority(request.priority())
                .dueDate(request.dueDate())
                .owner(owner)
                .build();
    }

    public void updateEntity(Todo todo, UpdateTodoRequest request) {
        todo.setTitle(request.title());
        todo.setDescription(request.description());
        todo.setCompleted(request.completed());
        todo.setPriority(request.priority());
        todo.setDueDate(request.dueDate());
    }

    public TodoResponse toResponse(Todo todo) {
        return new TodoResponse(
                todo.getId(),
                todo.getTitle(),
                todo.getDescription(),
                todo.isCompleted(),
                todo.getPriority(),
                todo.getDueDate(),
                todo.getCreatedAt(),
                todo.getUpdatedAt()
        );
    }
}
