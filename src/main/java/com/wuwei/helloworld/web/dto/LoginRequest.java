package com.wuwei.helloworld.web.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Login payload: {@code {"username":"test","password":"123456"}}.
 */
public record LoginRequest(
        @NotBlank(message = "username must not be blank") String username,
        @NotBlank(message = "password must not be blank") String password) {
}
