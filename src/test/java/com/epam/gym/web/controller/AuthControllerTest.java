package com.epam.gym.web.controller;

import com.epam.gym.dto.Credentials;
import com.epam.gym.exception.AuthenticationFailedException;
import com.epam.gym.service.AuthenticationService;
import com.epam.gym.web.dto.request.ChangePasswordRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static com.epam.gym.web.controller.ControllerTestSupport.basicAuth;
import static com.epam.gym.web.controller.ControllerTestSupport.json;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthenticationService authenticationService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = ControllerTestSupport.mockMvc(new AuthController(authenticationService));
    }

    @Test
    void login_validCredentials_returns200() throws Exception {
        // given
        String header = basicAuth("John.Smith", "password12");

        // when
        ResultActions result = mockMvc.perform(get("/api/auth/login").header(HttpHeaders.AUTHORIZATION, header));

        // then
        result.andExpect(status().isOk());
        verify(authenticationService).authenticateUser(new Credentials("John.Smith", "password12"));
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        // given
        Credentials wrong = new Credentials("John.Smith", "wrong");
        when(authenticationService.authenticateUser(wrong))
                .thenThrow(new AuthenticationFailedException("Invalid username or password"));

        // when
        ResultActions result = mockMvc.perform(get("/api/auth/login")
                .header(HttpHeaders.AUTHORIZATION, basicAuth("John.Smith", "wrong")));

        // then
        result.andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    void login_missingHeader_returns401() throws Exception {
        // given
        String url = "/api/auth/login";

        // when
        ResultActions result = mockMvc.perform(get(url));

        // then
        result.andExpect(status().isUnauthorized());
        verifyNoInteractions(authenticationService);
    }

    @Test
    void changePassword_usesOldPasswordAsCredentials() throws Exception {
        // given
        ChangePasswordRequest request = new ChangePasswordRequest("John.Smith", "password12", "newSecret1");

        // when
        ResultActions result = mockMvc.perform(put("/api/auth/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(request)));

        // then
        result.andExpect(status().isOk());
        verify(authenticationService).changePassword(new Credentials("John.Smith", "password12"), "newSecret1");
    }

    @Test
    void changePassword_missingNewPassword_returns400() throws Exception {
        // given
        String body = "{\"username\":\"John.Smith\",\"oldPassword\":\"password12\"}";

        // when
        ResultActions result = mockMvc.perform(put("/api/auth/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        // then
        result.andExpect(status().isBadRequest());
        verifyNoInteractions(authenticationService);
    }
}
