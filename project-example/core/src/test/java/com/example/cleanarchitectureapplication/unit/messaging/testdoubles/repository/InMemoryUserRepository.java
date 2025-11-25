package com.example.cleanarchitectureapplication.unit.messaging.testdoubles.repository;

import com.example.cleanarchitectureapplication.messaging.application.ports.UserRepository;
import com.example.cleanarchitectureapplication.messaging.domain.entity.User;
import java.util.HashMap;
import java.util.Map;

public class InMemoryUserRepository implements UserRepository {
    private final Map<String, User> userMap = new HashMap<>();

    @Override
    public User save(User user) {
        userMap.put(user.getName(), user);
        return user;
    }

    @Override
    public User findByName(String name) {
        return userMap.get(name);
    }

    public User get(String name) {
        User user = userMap.get(name);
        if (user == null) {
            throw new IllegalArgumentException("User " + name + " not found");
        }
        return user;
    }

    public void feedWith(User user) {
        userMap.put(user.getName(), user);
    }
}

