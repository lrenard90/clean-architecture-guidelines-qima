package com.example.cleanarchitectureapplication.messaging.application.ports;

import com.example.cleanarchitectureapplication.messaging.domain.entity.User;

public interface UserRepository {
    User save(User user);
    User findByName(String currentLoggedUserName);
}

