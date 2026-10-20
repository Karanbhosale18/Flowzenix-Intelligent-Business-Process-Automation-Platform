package com.example.authapp.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * Parses standard PostgreSQL connection URIs (e.g., from Neon, Render, Heroku)
 * and normalizes them into standard JDBC URLs and credentials for HikariCP.
 */
public final class DatabaseUrlParser {

    private static final Logger log = LoggerFactory.getLogger(DatabaseUrlParser.class);

    private DatabaseUrlParser() {
    }

    public static class ParsedDatabaseConfig {
        private final String jdbcUrl;
        private final String username;
        private final String password;

        public ParsedDatabaseConfig(String jdbcUrl, String username, String password) {
            this.jdbcUrl = jdbcUrl;
            this.username = username;
            this.password = password;
        }

        public String getJdbcUrl() {
            return jdbcUrl;
        }

        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }
    }

    public static ParsedDatabaseConfig parse(String rawUrl, String defaultUsername, String defaultPassword) {
        if (!StringUtils.hasText(rawUrl)) {
            return new ParsedDatabaseConfig("jdbc:postgresql://localhost:5432/neondb", defaultUsername, defaultPassword);
        }

        String trimmed = rawUrl.trim();

        // Check if rawUrl starts with postgres:// or postgresql:// (standard URI format)
        if (trimmed.startsWith("postgres://") || trimmed.startsWith("postgresql://")) {
            try {
                // Convert postgresql:// to http:// dummy scheme to ensure java.net.URI parses authority cleanly
                String dummyUrl = trimmed.replaceFirst("^(postgres|postgresql)://", "http://");
                URI uri = URI.create(dummyUrl);

                String extractedUsername = defaultUsername;
                String extractedPassword = defaultPassword;

                String userInfo = uri.getUserInfo();
                if (StringUtils.hasText(userInfo)) {
                    String[] parts = userInfo.split(":", 2);
                    extractedUsername = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
                    if (parts.length > 1) {
                        extractedPassword = URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
                    }
                }

                String host = uri.getHost();
                int port = uri.getPort() > 0 ? uri.getPort() : 5432;
                String path = uri.getPath();
                if (!StringUtils.hasText(path) || "/".equals(path)) {
                    path = "/neondb";
                }
                String query = uri.getQuery();

                StringBuilder jdbcBuilder = new StringBuilder("jdbc:postgresql://")
                        .append(host)
                        .append(":")
                        .append(port)
                        .append(path);

                if (StringUtils.hasText(query)) {
                    jdbcBuilder.append("?").append(query);
                }

                log.info("Auto-normalized PostgreSQL connection URI for host: {}:{}", host, port);
                return new ParsedDatabaseConfig(jdbcBuilder.toString(), extractedUsername, extractedPassword);
            } catch (Exception e) {
                log.warn("Failed to parse URI format, falling back to prefixing jdbc: {}", e.getMessage());
                String fallback = trimmed.startsWith("jdbc:") ? trimmed : "jdbc:" + trimmed;
                return new ParsedDatabaseConfig(fallback, defaultUsername, defaultPassword);
            }
        }

        // Already a standard JDBC URL
        return new ParsedDatabaseConfig(trimmed, defaultUsername, defaultPassword);
    }
}
