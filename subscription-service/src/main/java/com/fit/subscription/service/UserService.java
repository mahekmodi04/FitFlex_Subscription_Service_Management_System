package com.fit.subscription.service;

import com.fit.subscription.entity.User;
import com.fit.subscription.enums.UserRole;
import com.fit.subscription.exception.ResourceNotFoundException;
import com.fit.subscription.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


import java.math.BigDecimal;
import java.util.List;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,PasswordEncoder passwordEncoder){
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User createUser(User user){
        if(userRepository.findByEmail(user.getEmail()).isPresent()){
            throw new IllegalArgumentException("Email already exists"); //as email is unique
        }
        // public registration must never be allowed to set its own role or starting wallet balance
        user.setRole(UserRole.USER);
        user.setWalletBalance(BigDecimal.ZERO);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    public User getUserById(Long id){
        return userRepository.findById(id).
                orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public List<User> getAllUsers(){
        return userRepository.findAll();
    }

    public void deleteUserById(Long id){
        if(!userRepository.existsById(id)){
            throw new ResourceNotFoundException("User does not exist"); // if user does nto exist in first place
        }
        userRepository.deleteById(id);
    }

    //to update the few fields from frontend that came through updateUser and check first if it exists/throw exception
    //and then created an obj to fetch Username existingUser and then once found set the name/email/pw only and .save()
    public User updateUserById(Long id , User updateUser){
        User existingUser = userRepository.findById(id).orElseThrow(() ->new ResourceNotFoundException("User not found"));
        if(!existingUser.getEmail().equals(updateUser.getEmail())
                && userRepository.findByEmail(updateUser.getEmail()).isPresent()){
            throw new IllegalArgumentException("Email already exists");
        }//no duplicate email ids
        existingUser.setPassword(passwordEncoder.encode(updateUser.getPassword()));
        existingUser.setName(updateUser.getName());
        existingUser.setEmail(updateUser.getEmail());
        //existingUser.setPassword(updateUser.getPassword());
        // role is intentionally NOT updated here - a user must not be able to grant themselves ADMIN
        // via their own profile update. Role changes should go through a dedicated admin-only endpoint.

        return userRepository.save(existingUser);
    }
}
