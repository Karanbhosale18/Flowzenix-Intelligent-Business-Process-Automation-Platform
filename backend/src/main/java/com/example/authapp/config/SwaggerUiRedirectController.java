package com.example.authapp.config;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/** Supports the common Swagger UI shorthand URL. */
@RestController
public class SwaggerUiRedirectController {

    @GetMapping("/swagger-ui")
    public ResponseEntity<Void> swaggerUi() {
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, URI.create("/swagger-ui/index.html").toString())
                .build();
    }
}
