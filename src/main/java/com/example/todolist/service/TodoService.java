package com.example.todolist.service;

import com.example.todolist.dto.common.PageResponse;
import com.example.todolist.dto.todo.request.CreateTodoRequest;
import com.example.todolist.dto.todo.request.UpdateTodoRequest;
import com.example.todolist.dto.todo.response.TodoResponse;
import com.example.todolist.entity.Todo;
import com.example.todolist.entity.User;
import com.example.todolist.enums.Priority;
import com.example.todolist.exception.TodoNotFoundException;
import com.example.todolist.mapper.TodoMapper;
import com.example.todolist.repository.TodoRepository;
import com.example.todolist.util.TodoSpecifications;
import java.time.LocalDateTime;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TodoService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt", "dueDate");

    private final TodoRepository todoRepository;
    private final TodoMapper todoMapper;

    @Transactional
    public TodoResponse create(User user, CreateTodoRequest request) {
        Todo todo = todoMapper.toEntity(request, user);
        return todoMapper.toResponse(todoRepository.save(todo));
    }

    @Transactional(readOnly = true)
    public PageResponse<TodoResponse> findAll(
            Long userId,
            int page,
            int size,
            String sort,
            String direction,
            Boolean completed,
            Priority priority,
            LocalDateTime dueDateFrom,
            LocalDateTime dueDateTo
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(resolveDirection(direction), resolveSort(sort)));
        Specification<Todo> specification = Specification.allOf(
                TodoSpecifications.ownedBy(userId),
                TodoSpecifications.hasCompleted(completed),
                TodoSpecifications.hasPriority(priority),
                TodoSpecifications.dueDateFrom(dueDateFrom),
                TodoSpecifications.dueDateTo(dueDateTo)
        );

        Page<TodoResponse> todoPage = todoRepository.findAll(specification, pageable)
                .map(todoMapper::toResponse);

        return new PageResponse<>(
                todoPage.getContent(),
                todoPage.getNumber(),
                todoPage.getSize(),
                todoPage.getTotalElements(),
                todoPage.getTotalPages(),
                todoPage.isFirst(),
                todoPage.isLast(),
                resolveSort(sort),
                resolveDirection(direction).name()
        );
    }

    @Transactional(readOnly = true)
    public TodoResponse findById(Long id, Long userId) {
        Todo todo = getOwnedTodo(id, userId);
        return todoMapper.toResponse(todo);
    }

    @Transactional
    public TodoResponse update(Long id, Long userId, UpdateTodoRequest request) {
        Todo todo = getOwnedTodo(id, userId);
        todoMapper.updateEntity(todo, request);
        return todoMapper.toResponse(todo);
    }

    @Transactional
    public TodoResponse updateStatus(Long id, Long userId, boolean completed) {
        Todo todo = getOwnedTodo(id, userId);
        todo.setCompleted(completed);
        return todoMapper.toResponse(todo);
    }

    @Transactional
    public void delete(Long id, Long userId) {
        Todo todo = getOwnedTodo(id, userId);
        todoRepository.delete(todo);
    }

    private Todo getOwnedTodo(Long id, Long userId) {
        return todoRepository.findByIdAndOwnerId(id, userId)
                .orElseThrow(() -> new TodoNotFoundException("Todo not found"));
    }

    private String resolveSort(String sort) {
        return ALLOWED_SORT_FIELDS.contains(sort) ? sort : "createdAt";
    }

    private Sort.Direction resolveDirection(String direction) {
        return "ASC".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
    }
}
