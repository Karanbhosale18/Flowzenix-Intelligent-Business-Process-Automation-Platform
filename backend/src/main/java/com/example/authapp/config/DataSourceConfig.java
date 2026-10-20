package com.example.authapp.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;

/**
 * Custom DataSource configuration that transparently handles:
 * 1. Standard JDBC URLs (jdbc:postgresql://...)
 * 2. Raw URI connection strings from Neon, Render, Heroku (postgresql://user:pass@host/db...)
 * 3. Fallbacks to standard environment variables (DATABASE_URL, SPRING_DATASOURCE_URL)
 */
@Configuration
public class DataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(DataSourceConfig.class);

    @Value("${spring.datasource.url:#{null}}")
    private String springUrl;

    @Value("${DATABASE_URL:#{null}}")
    private String databaseUrl;

    @Value("${spring.datasource.username:#{null}}")
    private String username;

    @Value("${spring.datasource.password:#{null}}")
    private String password;

    @Value("${spring.datasource.driver-class-name:org.postgresql.Driver}")
    private String driverClassName;

    @Bean
    @Primary
    public DataSource dataSource() {
        String raw = StringUtils.hasText(springUrl) ? springUrl : databaseUrl;
        DatabaseUrlParser.ParsedDatabaseConfig parsed = DatabaseUrlParser.parse(raw, username, password);

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(parsed.getJdbcUrl());
        if (StringUtils.hasText(parsed.getUsername())) {
            config.setUsername(parsed.getUsername());
        }
        if (StringUtils.hasText(parsed.getPassword())) {
            config.setPassword(parsed.getPassword());
        }
        config.setDriverClassName(driverClassName);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setIdleTimeout(30000);
        config.setPoolName("FlowZenixHikariPool");

        log.info("Configured FlowZenix HikariDataSource for JDBC URL: {}", 
                parsed.getJdbcUrl().replaceAll(":[^:@]+@", ":***@").replaceAll("password=[^&]*", "password=***"));

        return new HikariDataSource(config);
    }
}
