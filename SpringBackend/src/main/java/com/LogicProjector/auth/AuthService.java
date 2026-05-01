package com.LogicProjector.auth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.LogicProjector.account.UserAccount;
import com.LogicProjector.account.UserAccountRepository;

@Service
public class AuthService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginAttemptTracker loginAttemptTracker;

    public AuthService(UserAccountRepository userAccountRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            LoginAttemptTracker loginAttemptTracker) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.loginAttemptTracker = loginAttemptTracker;
    }

    @Transactional
    public UserProfileResponse register(AuthRequest request) {
        userAccountRepository.findByUsername(request.username())
                .ifPresent(user -> {
                    throw new AuthException("USERNAME_ALREADY_EXISTS");
                });

        UserAccount user = userAccountRepository.save(new UserAccount(
                null,
                request.username(),
                passwordEncoder.encode(request.password()),
                300,
                0,
                "ACTIVE"
        ));

        return toProfile(user);
    }

    public AuthResponse login(AuthRequest request) {
        loginAttemptTracker.assertAllowed(request.username());

        UserAccount user = userAccountRepository.findByUsername(request.username())
                .orElseThrow(() -> {
                    loginAttemptTracker.recordFailure(request.username());
                    return new AuthException("INVALID_CREDENTIALS");
                });

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            loginAttemptTracker.recordFailure(request.username());
            throw new AuthException("INVALID_CREDENTIALS");
        }

        loginAttemptTracker.recordSuccess(request.username());
        return new AuthResponse(jwtService.issueToken(new AuthenticatedUser(user.getId(), user.getUsername())), toProfile(user));
    }

    public UserProfileResponse me(AuthenticatedUser authenticatedUser) {
        UserAccount user = userAccountRepository.findById(authenticatedUser.userId())
                .orElseThrow(() -> new AuthException("USER_NOT_FOUND"));
        return toProfile(user);
    }

    private UserProfileResponse toProfile(UserAccount user) {
        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getCreditsBalance(),
                user.getFrozenCreditsBalance(),
                user.getStatus()
        );
    }
}
