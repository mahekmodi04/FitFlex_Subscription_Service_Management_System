package com.fit.subscription.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fit.subscription.entity.User;
import com.fit.subscription.enums.UserRole;
import com.fit.subscription.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class UserIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Integration - Create User")
    void createUser_ShouldSaveUserInDatabase() throws Exception {

        userRepository.deleteAll();

        User user = new User();

        user.setName("Mahek");
        user.setEmail("mahek@gmail.com");
        user.setPassword("password123");
        user.setRole(UserRole.USER);

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Mahek","email":"mahek@gmail.com","password":"password123","role":"USER"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Mahek"))
                .andExpect(jsonPath("$.email").value("mahek@gmail.com"));

        User savedUser =
                userRepository.findByEmail("mahek@gmail.com").orElse(null);

        assertNotNull(savedUser);

        assertEquals("Mahek", savedUser.getName());

        assertEquals(UserRole.USER, savedUser.getRole());

    }
}
