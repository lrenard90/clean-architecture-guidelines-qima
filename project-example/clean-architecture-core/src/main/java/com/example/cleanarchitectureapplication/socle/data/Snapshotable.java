package com.example.cleanarchitectureapplication.socle.data;

public interface Snapshotable<DOMAIN_DATA_TYPE> {
    DOMAIN_DATA_TYPE data();
}

