package com.example.cleanarchitectureapplication.shared.time;

import com.example.cleanarchitectureapplication.socle.time.DateProvider;
import java.time.LocalDateTime;

public class FakeDateProvider extends DateProvider {
    private LocalDateTime now;

    public void setNow(LocalDateTime now) {
        this.now = now;
    }

    @Override
    public LocalDateTime now() {
        return now;
    }
}

