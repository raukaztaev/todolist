package com.example.todolist.mapper;

import com.example.todolist.dto.auth.response.UserSummaryResponse;
import com.example.todolist.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserSummaryResponse toSummary(User user) {
        return new UserSummaryResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole()
        );
    }
}
