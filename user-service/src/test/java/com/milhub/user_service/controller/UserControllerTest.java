package com.milhub.user_service.controller;

import com.milhub.user_service.dto.user.UserResponseDTO;
import com.milhub.user_service.repository.UserRepository;
import com.milhub.user_service.service.UserService;
import com.milhub.user_service.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtService jwtService; 

    @Test
    @WithMockUser(authorities = {"user:ban"})
    void getAllUsers_AsAdmin_ShouldReturn200() throws Exception {
        when(userRepository.findAll()).thenReturn(Collections.emptyList());
        mockMvc.perform(get("/api/v1/users"))
               .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = {"user:read"})
    void getAllUsers_AsRegularUser_ShouldReturn403() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
               .andExpect(status().isForbidden());
    }

    @Test
    void getAllUsers_AsAnonymous_ShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
               .andExpect(status().isUnauthorized());
    }
}
