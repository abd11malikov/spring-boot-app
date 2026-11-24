package com.epam.gym.security;

import com.epam.gym.dao.UserDao;
import com.epam.gym.model.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads trainees and trainers for the username/password login. A user blocked by the brute force
 * protection is returned as locked, so Spring Security rejects it before checking the password.
 */
@Service
public class GymUserDetailsService implements UserDetailsService {

    private final UserDao userDao;
    private final LoginAttemptService loginAttemptService;

    public GymUserDetailsService(UserDao userDao, LoginAttemptService loginAttemptService) {
        this.userDao = userDao;
        this.loginAttemptService = loginAttemptService;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        User user = userDao.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User " + username + " not found"));
        return org.springframework.security.core.userdetails.User.withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities("ROLE_USER")
                .accountLocked(loginAttemptService.isBlocked(username))
                .build();
    }
}
