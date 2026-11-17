package com.wuwei.helloworld.web.dto;

import java.util.List;

/**
 * Login response.
 *
 * @param accessToken    the signed JWT
 * @param tokenType      always {@code Bearer}
 * @param expiresIn      token lifetime in seconds
 * @param username       authenticated principal
 * @param roles          granted authorities
 */
public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        String username,
        List<String> roles) {
}
