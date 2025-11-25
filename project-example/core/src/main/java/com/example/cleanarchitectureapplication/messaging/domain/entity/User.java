package com.example.cleanarchitectureapplication.messaging.domain.entity;

import java.util.ArrayList;
import java.util.List;

public class User {
    private final String name;
    private final List<String> subscriptions;

    public User(String name) {
        this.name = name;
        this.subscriptions = new ArrayList<>();
    }

    public void follows(String userToFollow) {
        subscriptions.add(userToFollow);
    }

    public void unfollows(String userToUnfollow) {
        subscriptions.remove(userToUnfollow);
    }

    public String getName() {
        return name;
    }

    public List<String> getSubscriptions() {
        return new ArrayList<>(subscriptions);
    }
}

