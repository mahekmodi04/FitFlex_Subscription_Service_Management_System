package com.fit.subscription.service;

import com.fit.subscription.entity.User;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class NotificationService {

    public void sendDunningEmail(User user,
                                 int attempt,
                                 LocalDate nextRetry){
        System.out.println(
                "Dunning Email -> User: " + user.getEmail()
                + " | Attempt No: " + attempt
                + " | Next Retry Date: " + nextRetry
        );

    }
}
