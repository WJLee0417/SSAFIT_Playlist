package com.ssafy.ssafit.common.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.ssafy.ssafit.common.auth.SessionMemberResolver;
import com.ssafy.ssafit.common.exception.CustomException;
import com.ssafy.ssafit.common.exception.ErrorCode;

import jakarta.servlet.http.HttpSession;

@SpringBootTest
class ApiInfrastructureIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private SessionMemberResolver sessionMemberResolver;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void viteOriginCanSendCredentialedApiRequests() throws Exception {
        mockMvc.perform(options("/api/music")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        "Access-Control-Allow-Origin",
                        "http://localhost:5173"))
                .andExpect(header().string(
                        "Access-Control-Allow-Credentials",
                        "true"));
    }

    @Test
    void protectedApiReturnsDistinguishableAuthenticationError() throws Exception {
        when(sessionMemberResolver.requireMemberId(any(HttpSession.class)))
                .thenThrow(new CustomException(ErrorCode.AUTH_REQUIRED));

        mockMvc.perform(get("/api/spotify/me/connection"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_REQUIRED"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }
}
