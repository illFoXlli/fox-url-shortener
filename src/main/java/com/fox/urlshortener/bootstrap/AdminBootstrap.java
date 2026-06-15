package com.fox.urlshortener.bootstrap;

import com.fox.urlshortener.auth.User;
import com.fox.urlshortener.auth.UserRepository;
import com.fox.urlshortener.auth.UserRole;
import com.fox.urlshortener.config.AppProperties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties appProperties;

    public AdminBootstrap(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AppProperties appProperties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.appProperties = appProperties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String username = appProperties.admin().username();
        String password = appProperties.admin().password();
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new IllegalStateException("ADMIN_USERNAME and ADMIN_PASSWORD must be set");
        }
        if (userRepository.existsByUsername(username)) {
            LOGGER.info("Admin user '{}' already exists", username);
            return;
        }
        userRepository.save(new User(username, passwordEncoder.encode(password), UserRole.ADMIN));
        LOGGER.info("Created default admin user '{}'", username);
    }
}
