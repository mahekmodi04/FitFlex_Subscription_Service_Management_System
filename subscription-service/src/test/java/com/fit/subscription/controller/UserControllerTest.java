package com.fit.subscription.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fit.subscription.entity.User;
import com.fit.subscription.enums.UserRole;
import com.fit.subscription.exception.ResourceNotFoundException;
import com.fit.subscription.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @Test
    @DisplayName("Should create a new user successfully")
    void createUser_ShouldReturnCreatedUser() throws Exception {

        User user = new User();
        user.setId(1L);
        user.setName("Mahek");
        user.setEmail("mahek@gmail.com");
        user.setPassword("password123");

        when(userService.createUser(any(User.class)))
                .thenReturn(user);

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Mahek","email":"mahek@gmail.com","password":"password123","role":"USER"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Mahek"))
                .andExpect(jsonPath("$.email").value("mahek@gmail.com"));

        verify(userService, times(1))
                .createUser(any(User.class));
    }
    @Test
    @DisplayName("Should return user by ID")
    void getUserById_ShouldReturnUser() throws Exception {

        User user = new User();
        user.setId(1L);
        user.setName("Mahek");
        user.setEmail("mahek@gmail.com");
        user.setPassword("password123");

        when(userService.getUserById(1L))
                .thenReturn(user);

        mockMvc.perform(get("/users/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Mahek"))
                .andExpect(jsonPath("$.email").value("mahek@gmail.com"));

        verify(userService, times(1))
                .getUserById(1L);
    }
    @Test
    @DisplayName("Should return all users")
    void getAllUsers_ShouldReturnListOfUsers() throws Exception {

        User user1 = new User();
        user1.setId(1L);
        user1.setName("Mahek");
        user1.setEmail("mahek@gmail.com");

        User user2 = new User();
        user2.setId(2L);
        user2.setName("Rahul");
        user2.setEmail("rahul@gmail.com");

        List<User> users = List.of(user1, user2);

        when(userService.getAllUsers())
                .thenReturn(users);

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Mahek"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].name").value("Rahul"));

        verify(userService, times(1))
                .getAllUsers();
    }
    @Test
    @DisplayName("Should update user successfully")
    void updateUser_ShouldReturnUpdatedUser() throws Exception {

        User updatedUser = new User();
        updatedUser.setId(1L);
        updatedUser.setName("Updated Mahek");
        updatedUser.setEmail("updated@gmail.com");
        updatedUser.setPassword("password123");
        updatedUser.setRole(UserRole.USER);

        when(userService.updateUserById(any(Long.class), any(User.class)))
                .thenReturn(updatedUser);

        mockMvc.perform(put("/users/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Updated Mahek","email":"updated@gmail.com","password":"password123","role":"USER"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Updated Mahek"))
                .andExpect(jsonPath("$.email").value("updated@gmail.com"));

        verify(userService, times(1))
                .updateUserById(any(Long.class), any(User.class));
    }
    @Test
    @DisplayName("Should delete user successfully")
    void deleteUser_ShouldReturnSuccessMessage() throws Exception {

        doNothing().when(userService).deleteUserById(1L);

        mockMvc.perform(delete("/users/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(content().string("User deleted successfully"));

        verify(userService, times(1))
                .deleteUserById(1L);
    }
    @Test
    @DisplayName("Should return 400 when user validation fails")
    void createUser_ShouldReturnBadRequest_WhenValidationFails() throws Exception {

        User invalidUser = new User();
        invalidUser.setName("");
        invalidUser.setEmail("");
        invalidUser.setPassword("");

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidUser)))
                .andExpect(status().isBadRequest());
    }
    @Test
    @DisplayName("Should return 404 when user does not exist")
    void getUserById_ShouldReturn404_WhenUserNotFound() throws Exception {

        when(userService.getUserById(100L))
                .thenThrow(new ResourceNotFoundException("User not found"));

        mockMvc.perform(get("/users/{id}", 100L))
                .andExpect(status().isNotFound());

        verify(userService, times(1))
                .getUserById(100L);
    }



}
