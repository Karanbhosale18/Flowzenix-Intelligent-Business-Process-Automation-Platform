package com.example.authapp.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseUrlParserTest {

    @Test
    void testNeonRawUriParsing() {
        String rawNeonUri = "postgresql://neondb_owner:npg_Y4Ep5eqOujRn@ep-fragrant-hill-b42na1cg-pooler.c-6.us-east-2.aws.neon.tech/neondb?sslmode=require&channel_binding=require";

        DatabaseUrlParser.ParsedDatabaseConfig parsed = DatabaseUrlParser.parse(rawNeonUri, null, null);

        assertEquals("jdbc:postgresql://ep-fragrant-hill-b42na1cg-pooler.c-6.us-east-2.aws.neon.tech:5432/neondb?sslmode=require&channel_binding=require", parsed.getJdbcUrl());
        assertEquals("neondb_owner", parsed.getUsername());
        assertEquals("npg_Y4Ep5eqOujRn", parsed.getPassword());
    }

    @Test
    void testStandardJdbcUrlPreserved() {
        String jdbcUrl = "jdbc:postgresql://localhost:5432/flowzenix_db?sslmode=disable";

        DatabaseUrlParser.ParsedDatabaseConfig parsed = DatabaseUrlParser.parse(jdbcUrl, "postgres", "secret");

        assertEquals(jdbcUrl, parsed.getJdbcUrl());
        assertEquals("postgres", parsed.getUsername());
        assertEquals("secret", parsed.getPassword());
    }

    @Test
    void testPostgresSchemeWithCustomPort() {
        String rawUri = "postgres://myuser:mypass@myhost.com:5433/mydb";

        DatabaseUrlParser.ParsedDatabaseConfig parsed = DatabaseUrlParser.parse(rawUri, null, null);

        assertEquals("jdbc:postgresql://myhost.com:5433/mydb", parsed.getJdbcUrl());
        assertEquals("myuser", parsed.getUsername());
        assertEquals("mypass", parsed.getPassword());
    }
}
