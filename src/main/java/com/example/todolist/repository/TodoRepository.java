package com.example.todolist.repository;

import com.example.todolist.entity.Todo;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TodoRepository extends JpaRepository<Todo, Long>, JpaSpecificationExecutor<Todo> {

    Optional<Todo> findByIdAndOwnerId(Long id, Long ownerId);

    List<Todo> findAllByIdInAndOwnerId(Collection<Long> ids, Long ownerId);
}
