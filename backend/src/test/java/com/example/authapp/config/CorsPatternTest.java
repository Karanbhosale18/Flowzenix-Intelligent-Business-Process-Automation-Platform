package com.example.authapp.config;

import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CorsPatternTest {

    @Test
    void testVercelWildcardOrigin() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of(
                "https://*.vercel.app",
                "https://*.onrender.com",
                "http://localhost:[*]"
        ));
        config.setAllowCredentials(true);

        String allowed = config.checkOrigin("https://flowzenix-frontend.vercel.app");
        assertEquals("https://flowzenix-frontend.vercel.app", allowed);

        String allowedLocal = config.checkOrigin("http://localhost:5173");
        assertEquals("http://localhost:5173", allowedLocal);

        String allowedRender = config.checkOrigin("https://flowzenix.onrender.com");
        assertEquals("https://flowzenix.onrender.com", allowedRender);
    }
}
