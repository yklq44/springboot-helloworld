package com.wuwei.helloworld.web;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The protected HelloWorld resource.
 */
@RestController
@RequestMapping("/api")
public class HelloController {

    /**
     * {@code GET /api/hello} — requires authentication, returns the plain text {@code Hello World}.
     */
    @GetMapping(value = "/hello", produces = MediaType.TEXT_PLAIN_VALUE)
    public String hello() {
        return "Hello World";
    }

    /**
     * Convenience endpoint echoing who is calling, handy when checking the token.
     */
    @GetMapping(value = "/me", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> me(Authentication authentication) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("username", authentication.getName());
        body.put("authorities", authentication.getAuthorities().stream()
                .map(Object::toString)
                .toList());
        return body;
    }

    /**
     * Public liveness probe, no authentication required.
     */
    @GetMapping(value = "/health", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "UP");
        return body;
    }
}
