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
public class RenewalScheduler {
    private final RenewalService renewalService;
    private final SubscriptionRepository subscriptionRepository;

    public RenewalScheduler(RenewalService renewalService,
                            SubscriptionRepository subscriptionRepository){
        this.renewalService = renewalService;
        this.subscriptionRepository = subscriptionRepository;
    }

    @Scheduled(cron = "0 0 0 * * *") //Everyday at 12AM spring will run this automatically
    public void processRenewals(){
        //fetch only eligible subscription rows which are active, auto-renew = true, end date = today
        //so we have to add these in subscriptionRepository and call it here


        List<Subscription> eligibleSubscriptions = subscriptionRepository.findByEndDateLessThanEqualAndAutoRenewAndStatus(LocalDate.now(),true, SubscriptionStatus.ACTIVE);

        for(Subscription subscription : eligibleSubscriptions){
            try{
                renewalService.renewSubscription(subscription.getId());
            }
            catch(Exception e){
                System.out.println(
                        "Renewal attempt failed for Id " +
                                subscription.getId()
                                + " Reason : "
                                + e.getMessage()
                );
            }
        }
    }
}
