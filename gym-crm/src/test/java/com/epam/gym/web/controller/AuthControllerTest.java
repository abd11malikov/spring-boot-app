package com.epam.gym.web.controller;

import com.epam.gym.exception.AuthenticationFailedException;
import com.epam.gym.service.AuthenticationService;
import com.epam.gym.web.dto.request.ChangePasswordRequest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import static com.epam.gym.web.controller.ControllerTestSupport.JWT_SERVICE;
import static com.epam.gym.web.controller.ControllerTestSupport.asUser;
import static com.epam.gym.web.controller.ControllerTestSupport.json;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthenticationService authenticationService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = ControllerTestSupport.mockMvc(new AuthController(authenticationService, JWT_SERVICE));
    }

    @Test
    void login_authenticatedUser_returnsBearerTokenForThatUser() throws Exception {
        // when
        MvcResult result = mockMvc.perform(get("/api/auth/login").with(asUser("John.Smith")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andReturn();

        // then
        String token = JsonPath.read(result.getResponse().getContentAsString(), "$.token");
        assertEquals("John.Smith", JWT_SERVICE.parse(token).username());
    }

    @Test
    void changePassword_ownAccount_passesOldAndNewPassword() throws Exception {
        // given
        ChangePasswordRequest request = new ChangePasswordRequest("John.Smith", "password12", "newSecret1");

        // when
        ResultActions result = mockMvc.perform(put("/api/auth/password")
                .with(asUser("John.Smith"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(request)));

        // then
        result.andExpect(status().isOk());
        verify(authenticationService).changePassword("John.Smith", "password12", "newSecret1");
    }

    @Test
    void changePassword_otherUsersAccount_returns403() throws Exception {
        // given
        ChangePasswordRequest request = new ChangePasswordRequest("Jane.Doe", "password12", "newSecret1");

        // when
        ResultActions result = mockMvc.perform(put("/api/auth/password")
                .with(asUser("John.Smith"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(request)));

        // then
        result.andExpect(status().isForbidden());
        verifyNoInteractions(authenticationService);
    }

    @Test
    void changePassword_wrongOldPassword_returns401() throws Exception {
        // given
        ChangePasswordRequest request = new ChangePasswordRequest("John.Smith", "wrong", "newSecret1");
        doThrow(new AuthenticationFailedException("Invalid username or password"))
                .when(authenticationService).changePassword("John.Smith", "wrong", "newSecret1");

        // when
        ResultActions result = mockMvc.perform(put("/api/auth/password")
                .with(asUser("John.Smith"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(request)));

        // then
        result.andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer realm=\"gym-crm\""))
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    void changePassword_missingNewPassword_returns400() throws Exception {
        // given
        String body = "{\"username\":\"John.Smith\",\"oldPassword\":\"password12\"}";

        // when
        ResultActions result = mockMvc.perform(put("/api/auth/password")
                .with(asUser("John.Smith"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        // then
        result.andExpect(status().isBadRequest());
        verifyNoInteractions(authenticationService);
    }
}
