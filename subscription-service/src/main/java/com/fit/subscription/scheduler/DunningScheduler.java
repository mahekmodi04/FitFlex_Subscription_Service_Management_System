package com.fit.subscription.scheduler;

import com.fit.subscription.entity.Subscription;
import com.fit.subscription.enums.SubscriptionStatus;
import com.fit.subscription.repository.SubscriptionRepository;
import com.fit.subscription.service.RenewalService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class DunningScheduler {
    private final RenewalService renewalService;
    private final SubscriptionRepository subscriptionRepository;

    public DunningScheduler(RenewalService renewalService,
                            SubscriptionRepository subscriptionRepository){
        this.renewalService = renewalService;
        this.subscriptionRepository = subscriptionRepository;
    }

    @Scheduled(cron = "0 5 0 * * *")
    public void retryRenewals(){
            //fetch the eligible subscriptions based grace status and nextretry date as today
        List<Subscription> eligibleSubscriptions = subscriptionRepository.findByNextRetryDateLessThanEqualAndStatus(LocalDate.now() , SubscriptionStatus.GRACE);

        for(Subscription subscription : eligibleSubscriptions){
            try {
                renewalService.retryPayment(subscription.getId());
            }
            catch(Exception e){
                System.out.println(
                        "Retry payment failed for Subscription ID: "
                                + subscription.getId()
                                + " Reason: "
                                + e.getMessage()
                );
            }
        }
    }
}
