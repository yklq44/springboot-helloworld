package com.wuwei.helloworld;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * End-to-end cover of the two required behaviours:
 * a username/password login API and a HelloWorld API protected by Spring Security.
 */
@SpringBootTest
@AutoConfigureMockMvc
class HelloWorldApiTest {

    private static final String LOGIN_URL = "/api/auth/login";
    private static final String HELLO_URL = "/api/hello";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /api/hello without credentials is rejected with 401")
    void helloWithoutCredentialsIsUnauthorized() throws Exception {
        mockMvc.perform(get(HELLO_URL))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("GET /api/hello with a valid JWT returns 'Hello World'")
    void helloWithJwtReturnsHelloWorld() throws Exception {
        String token = login("test", "123456");

        mockMvc.perform(get(HELLO_URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string("Hello World"));
    }

    @Test
    @DisplayName("POST /api/auth/login with the demo account issues a bearer token")
    void loginIssuesToken() throws Exception {
        MvcResult result = mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"test\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.username").value("test"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.get("accessToken").asText()).isNotBlank();
        assertThat(body.get("expiresIn").asLong()).isPositive();
    }

    @Test
    @DisplayName("POST /api/auth/login with a wrong password is rejected with 401")
    void loginWithWrongPasswordIsUnauthorized() throws Exception {
        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"test\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    @DisplayName("A tampered JWT is rejected with 401")
    void tamperedTokenIsRejected() throws Exception {
        String token = login("test", "123456");
        String tampered = token.substring(0, token.length() - 2) + "xx";

        mockMvc.perform(get(HELLO_URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + tampered))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("HTTP Basic with the demo account also satisfies Spring Security")
    void helloWithHttpBasicWorks() throws Exception {
        mockMvc.perform(get(HELLO_URL).with(httpBasic("test", "123456")))
                .andExpect(status().isOk())
                .andExpect(content().string("Hello World"));
    }

    @Test
    @DisplayName("GET /api/health stays public")
    void healthIsPublic() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    private String login(String username, String password) throws Exception {
        String payload = objectMapper.writeValueAsString(
                java.util.Map.of("username", username, "password", password));

        MvcResult result = mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("accessToken").asText();
    }
}
