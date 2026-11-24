package com.epam.gym.service.impl;

import com.epam.gym.dao.UserDao;
import com.epam.gym.exception.AuthenticationFailedException;
import com.epam.gym.model.User;
import com.epam.gym.service.AuthenticationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.epam.gym.util.ValidationUtils.requireNotBlank;

@Service
@Transactional
public class AuthenticationServiceImpl implements AuthenticationService {
    private static final Logger log = LoggerFactory.getLogger(AuthenticationServiceImpl.class);
    private static final String INVALID_CREDENTIALS = "Invalid username or password";

    private final UserDao userDao;
    private final PasswordEncoder passwordEncoder;

    public AuthenticationServiceImpl(UserDao userDao, PasswordEncoder passwordEncoder) {
        this.userDao = userDao;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void changePassword(String username, String oldPassword, String newPassword) {
        requireNotBlank(newPassword, "New password");
        if (username == null || oldPassword == null) {
            throw new AuthenticationFailedException(INVALID_CREDENTIALS);
        }
        User user = userDao.findByUsername(username)
                .filter(found -> passwordEncoder.matches(oldPassword, found.getPassword()))
                .orElseThrow(() -> {
                    log.warn("Password change rejected for username {}: wrong old password", username);
                    return new AuthenticationFailedException(INVALID_CREDENTIALS);
                });
        user.setPassword(passwordEncoder.encode(newPassword));
        log.info("Changed password for user {}", username);
    }
}
