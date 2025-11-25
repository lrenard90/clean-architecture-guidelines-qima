package com.example.cleanarchitectureapplication.socle.time;

import java.time.LocalDateTime;

public class CurrentDateProvider extends DateProvider {
    @Override
    public LocalDateTime now() {
        return LocalDateTime.now();
    }
}

