package com.irishpubfinder.api.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Serves the public Privacy Policy and Terms of Service as static HTML, so the app has a
 * publicly reachable URL on its own domain (required by AdMob and the app stores). The same
 * content is also hosted on Cloudflare Pages (see IrishPubFinder/site/*). These routes are
 * permitted without authentication in SecurityConfig.
 */
@RestController
public class LegalController {

    @GetMapping(value = "/privacy", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> privacy() throws IOException {
        return html("legal/privacy.html");
    }

    @GetMapping(value = "/terms", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> terms() throws IOException {
        return html("legal/terms.html");
    }

    private ResponseEntity<String> html(String resourcePath) throws IOException {
        try (var in = new ClassPathResource(resourcePath).getInputStream()) {
            String body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePublic())
                    .body(body);
        }
    }
}
