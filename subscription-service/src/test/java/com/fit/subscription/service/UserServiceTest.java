package com.fit.subscription.service;

import com.fit.subscription.entity.User;
import com.fit.subscription.exception.ResourceNotFoundException;
import com.fit.subscription.repository.UserRepository;
import com.fit.subscription.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User createSampleUser() {
        User user = new User();
        user.setId(1L);
        user.setName("Mahek");
        user.setEmail("mahek@gmail.com");
        user.setPassword("password123");
        return user;
    }

    @Test
    void createUser_ShouldSaveSuccessfully() {

        User user = createSampleUser();

        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.empty());

        when(userRepository.save(user))
                .thenReturn(user);

        User savedUser = userService.createUser(user);

        assertNotNull(savedUser);
        assertEquals("Mahek", savedUser.getName());
        assertEquals("mahek@gmail.com", savedUser.getEmail());

        verify(userRepository).findByEmail(user.getEmail());
        verify(userRepository).save(user);
    }

    @Test
    void createUser_ShouldThrowException_WhenEmailAlreadyExists() {

        User user = createSampleUser();

        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> userService.createUser(user)
                );

        assertEquals("Email already exists", exception.getMessage());

        verify(userRepository).findByEmail(user.getEmail());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void getUserById_ShouldReturnUser() {

        User user = createSampleUser();

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        User result = userService.getUserById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());

        verify(userRepository).findById(1L);
    }

    @Test
    void getUserById_ShouldThrowException_WhenUserNotFound() {

        when(userRepository.findById(1L))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () -> userService.getUserById(1L)
                );

        assertEquals("User not found", exception.getMessage());

        verify(userRepository).findById(1L);
    }

    @Test
    void getAllUsers_ShouldReturnAllUsers() {

        User user1 = createSampleUser();

        User user2 = new User();
        user2.setId(2L);
        user2.setName("Rahul");
        user2.setEmail("rahul@gmail.com");
        user2.setPassword("abc");

        List<User> users = Arrays.asList(user1, user2);

        when(userRepository.findAll())
                .thenReturn(users);

        List<User> result = userService.getAllUsers();

        assertEquals(2, result.size());

        verify(userRepository).findAll();
    }

    @Test
    void deleteUser_ShouldDeleteSuccessfully() {

        when(userRepository.existsById(1L))
                .thenReturn(true);

        userService.deleteUserById(1L);

        verify(userRepository).existsById(1L);
        verify(userRepository).deleteById(1L);
    }

    @Test
    void deleteUser_ShouldThrowException_WhenUserNotFound() {

        when(userRepository.existsById(1L))
                .thenReturn(false);

        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () -> userService.deleteUserById(1L)
                );

        assertEquals("User does not exist", exception.getMessage());

        verify(userRepository).existsById(1L);
        verify(userRepository, never()).deleteById(anyLong());
    }

    @Test
    void updateUser_ShouldUpdateSuccessfully() {

        User existingUser = createSampleUser();

        User updatedUser = new User();
        updatedUser.setName("Updated");
        updatedUser.setEmail("updated@gmail.com");
        updatedUser.setPassword("newPassword");

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(existingUser));

        when(userRepository.findByEmail(updatedUser.getEmail()))
                .thenReturn(Optional.empty());

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.updateUserById(1L, updatedUser);

        assertEquals("Updated", result.getName());
        assertEquals("updated@gmail.com", result.getEmail());
        assertEquals("newPassword", result.getPassword());

        verify(userRepository).findById(1L);
        verify(userRepository).findByEmail(updatedUser.getEmail());
        verify(userRepository).save(existingUser);
    }

    @Test
    void updateUser_ShouldThrowException_WhenEmailAlreadyExists() {

        User existingUser = createSampleUser();

        User anotherUser = new User();
        anotherUser.setEmail("updated@gmail.com");

        User updatedUser = new User();
        updatedUser.setEmail("updated@gmail.com");

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(existingUser));

        when(userRepository.findByEmail(updatedUser.getEmail()))
                .thenReturn(Optional.of(anotherUser));

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> userService.updateUserById(1L, updatedUser)
                );

        assertEquals("Email already exists", exception.getMessage());

        verify(userRepository).findById(1L);
        verify(userRepository).findByEmail(updatedUser.getEmail());
        verify(userRepository, never()).save(any(User.class));
    }
}
