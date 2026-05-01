package com.LogicProjector.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
class LoginAttemptTracker {

    private final int maxFailedAttempts;
    private final Duration lockDuration;
    private final Clock clock;
    private final ConcurrentMap<String, AttemptState> attempts = new ConcurrentHashMap<>();

    @Autowired
    LoginAttemptTracker(
            @Value("${pas.auth.max-failed-login-attempts:5}") int maxFailedAttempts,
            @Value("${pas.auth.login-lock-seconds:300}") long lockSeconds) {
        this(maxFailedAttempts, Duration.ofSeconds(lockSeconds), Clock.systemUTC());
    }

    LoginAttemptTracker(int maxFailedAttempts, Duration lockDuration, Clock clock) {
        this.maxFailedAttempts = maxFailedAttempts;
        this.lockDuration = lockDuration;
        this.clock = clock;
    }

    void assertAllowed(String username) {
        AttemptState state = attempts.get(key(username));
        if (state == null) {
            return;
        }

        Instant now = clock.instant();
        if (state.lockedUntil() != null && state.lockedUntil().isAfter(now)) {
            throw new AuthException("TOO_MANY_LOGIN_ATTEMPTS");
        }
        if (state.lockedUntil() != null) {
            attempts.remove(key(username), state);
        }
    }

    void recordFailure(String username) {
        Instant now = clock.instant();
        attempts.compute(key(username), (ignored, state) -> {
            if (state != null && state.lockedUntil() != null && state.lockedUntil().isAfter(now)) {
                return state;
            }

            int failures = state == null || state.lockedUntil() != null ? 1 : state.failures() + 1;
            Instant lockedUntil = failures >= maxFailedAttempts ? now.plus(lockDuration) : null;
            return new AttemptState(failures, lockedUntil);
        });
    }

    void recordSuccess(String username) {
        attempts.remove(key(username));
    }

    private String key(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }

    private record AttemptState(int failures, Instant lockedUntil) {
    }
}
