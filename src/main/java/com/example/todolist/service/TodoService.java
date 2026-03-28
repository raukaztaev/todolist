package com.example.todolist.service;

import com.example.todolist.dto.common.PageResponse;
import com.example.todolist.dto.todo.request.BulkTodoStatusUpdateRequest;
import com.example.todolist.dto.todo.request.CreateTodoRequest;
import com.example.todolist.dto.todo.request.UpdateTodoDueDateRequest;
import com.example.todolist.dto.todo.request.UpdateTodoPriorityRequest;
import com.example.todolist.dto.todo.request.UpdateTodoRequest;
import com.example.todolist.dto.todo.response.TodoResponse;
import com.example.todolist.entity.Todo;
import com.example.todolist.entity.User;
import com.example.todolist.enums.Priority;
import com.example.todolist.exception.TodoNotFoundException;
import com.example.todolist.mapper.TodoMapper;
import com.example.todolist.repository.TodoRepository;
import com.example.todolist.util.TodoSpecifications;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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

    @Transactional(readOnly = true)
    public List<TodoResponse> findCompleted(Long userId) {
        return findBySpecification(userId, TodoSpecifications.hasCompleted(true), Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Transactional(readOnly = true)
    public List<TodoResponse> findPending(Long userId) {
        return findBySpecification(userId, TodoSpecifications.hasCompleted(false), Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Transactional(readOnly = true)
    public List<TodoResponse> findOverdue(Long userId) {
        return findBySpecification(
                userId,
                Specification.allOf(
                        TodoSpecifications.hasCompleted(false),
                        TodoSpecifications.dueDateTo(LocalDateTime.now())
                ),
                Sort.by(Sort.Direction.ASC, "dueDate")
        );
    }

    @Transactional(readOnly = true)
    public List<TodoResponse> findDueToday(Long userId) {
        LocalDate today = LocalDate.now();
        return findBySpecification(
                userId,
                Specification.allOf(
                        TodoSpecifications.dueDateFrom(today.atStartOfDay()),
                        TodoSpecifications.dueDateTo(today.plusDays(1).atStartOfDay().minusNanos(1))
                ),
                Sort.by(Sort.Direction.ASC, "dueDate")
        );
    }

    @Transactional(readOnly = true)
    public List<TodoResponse> findUpcoming(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        return findBySpecification(
                userId,
                Specification.allOf(
                        TodoSpecifications.hasCompleted(false),
                        TodoSpecifications.dueDateFrom(now),
                        TodoSpecifications.dueDateTo(now.plusDays(7))
                ),
                Sort.by(Sort.Direction.ASC, "dueDate")
        );
    }

    @Transactional(readOnly = true)
    public List<TodoResponse> findByPriority(Long userId, Priority priority) {
        return findBySpecification(userId, TodoSpecifications.hasPriority(priority), Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Transactional
    public TodoResponse updatePriority(Long id, Long userId, UpdateTodoPriorityRequest request) {
        Todo todo = getOwnedTodo(id, userId);
        todo.setPriority(request.priority());
        return todoMapper.toResponse(todo);
    }

    @Transactional
    public TodoResponse updateDueDate(Long id, Long userId, UpdateTodoDueDateRequest request) {
        Todo todo = getOwnedTodo(id, userId);
        todo.setDueDate(request.dueDate());
        return todoMapper.toResponse(todo);
    }

    @Transactional
    public TodoResponse duplicate(Long id, Long userId) {
        Todo source = getOwnedTodo(id, userId);
        Todo copy = Todo.builder()
                .title(source.getTitle() + " (copy)")
                .description(source.getDescription())
                .completed(false)
                .priority(source.getPriority())
                .dueDate(source.getDueDate())
                .owner(source.getOwner())
                .build();
        return todoMapper.toResponse(todoRepository.save(copy));
    }

    @Transactional
    public List<TodoResponse> bulkUpdateStatus(Long userId, BulkTodoStatusUpdateRequest request) {
        List<Todo> todos = todoRepository.findAllByIdInAndOwnerId(request.todoIds(), userId);
        if (todos.size() != request.todoIds().size()) {
            throw new TodoNotFoundException("One or more todos were not found");
        }
        todos.forEach(todo -> todo.setCompleted(request.completed()));
        return todos.stream().map(todoMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<TodoResponse> findAllForAdmin(
            int page,
            int size,
            String sort,
            String direction,
            Long ownerId,
            Boolean completed,
            Priority priority
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(resolveDirection(direction), resolveSort(sort)));
        Specification<Todo> specification = Specification.allOf(
                ownerId == null ? null : TodoSpecifications.ownedBy(ownerId),
                TodoSpecifications.hasCompleted(completed),
                TodoSpecifications.hasPriority(priority)
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
    public TodoResponse findByIdForAdmin(Long id) {
        return todoMapper.toResponse(getTodo(id));
    }

    @Transactional
    public TodoResponse updateStatusForAdmin(Long id, boolean completed) {
        Todo todo = getTodo(id);
        todo.setCompleted(completed);
        return todoMapper.toResponse(todo);
    }

    @Transactional
    public void deleteForAdmin(Long id) {
        todoRepository.delete(getTodo(id));
    }

    private Todo getOwnedTodo(Long id, Long userId) {
        return todoRepository.findByIdAndOwnerId(id, userId)
                .orElseThrow(() -> new TodoNotFoundException("Todo not found"));
    }

    private Todo getTodo(Long id) {
        return todoRepository.findById(id)
                .orElseThrow(() -> new TodoNotFoundException("Todo not found"));
    }

    private List<TodoResponse> findBySpecification(Long userId, Specification<Todo> extraSpecification, Sort sort) {
        Specification<Todo> specification = Specification.allOf(
                TodoSpecifications.ownedBy(userId),
                extraSpecification
        );
        return todoRepository.findAll(specification, sort).stream()
                .map(todoMapper::toResponse)
                .toList();
    }

    private String resolveSort(String sort) {
        return ALLOWED_SORT_FIELDS.contains(sort) ? sort : "createdAt";
    }

    private Sort.Direction resolveDirection(String direction) {
        return "ASC".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
    }
}
