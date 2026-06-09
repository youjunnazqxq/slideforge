package com.slideforge.api.config;

import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DevDataInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    public DevDataInitializer(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        Integer userCount = jdbcTemplate.queryForObject("select count(*) from users", Integer.class);

        if (userCount != null && userCount > 0) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update(
                "insert into users (id, username, password_hash, status, created_at, updated_at) values (?, ?, ?, ?, ?, ?)",
                UUID.randomUUID(),
                "admin",
                passwordEncoder.encode("123456"),
                "active",
                now,
                now
        );
    }
}
