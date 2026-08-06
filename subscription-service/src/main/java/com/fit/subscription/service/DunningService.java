package com.fit.subscription.service;

import com.fit.subscription.entity.DunningLog;
import com.fit.subscription.entity.Subscription;
import com.fit.subscription.enums.PaymentStatus;
import com.fit.subscription.repository.DunningLogRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class DunningService {
    private final DunningLogRepository dunningLogRepository;

    public DunningService(DunningLogRepository dunningLogRepository){
        this.dunningLogRepository = dunningLogRepository;
    }
    public void saveLog(Subscription subscription,
                        int attemptNumber,
                        PaymentStatus paymentStatus,
                        String failureReason,
                        LocalDate nextRetryDate){
        DunningLog dunningLog = new DunningLog();

        dunningLog.setSubscription(subscription);
        dunningLog.setAttemptedAt(LocalDateTime.now());
        dunningLog.setAttemptNumber(attemptNumber);
        dunningLog.setStatus(paymentStatus);
        dunningLog.setFailureReason(failureReason);
        dunningLog.setNextRetryDate(nextRetryDate);

        dunningLogRepository.save(dunningLog);

    }
}
