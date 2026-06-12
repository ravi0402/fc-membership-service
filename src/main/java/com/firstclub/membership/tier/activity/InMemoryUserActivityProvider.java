package com.firstclub.membership.tier.activity;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Demo adapter for {@link UserActivityProvider}, backed by a thread-safe in-memory map.
 * Lets the rule engine be exercised end-to-end without a real Order service; the demo
 * seam ({@code PUT /users/{id}/activity}) writes here. Replace with a real adapter in prod.
 */
@Component
public class InMemoryUserActivityProvider implements UserActivityProvider {

    private final ConcurrentMap<String, UserActivity> activities = new ConcurrentHashMap<>();

    @Override
    public UserActivity getActivity(String userId) {
        return activities.getOrDefault(userId, UserActivity.empty(userId));
    }

    /** Upsert a user's activity snapshot (immutable value replaced atomically). */
    public UserActivity setActivity(UserActivity activity) {
        activities.put(activity.userId(), activity);
        return activity;
    }
}
