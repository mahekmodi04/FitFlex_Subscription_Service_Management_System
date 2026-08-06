package com.fit.subscription.service;

import com.fit.subscription.dto.PaymentResponseDTO;
import com.fit.subscription.entity.Payment;
import com.fit.subscription.entity.Subscription;
import com.fit.subscription.entity.SubscriptionAddOn;
import com.fit.subscription.enums.PaymentStatus;
import com.fit.subscription.enums.PaymentType;
import com.fit.subscription.enums.SubscriptionStatus;
import com.fit.subscription.exception.ResourceNotFoundException;
import com.fit.subscription.repository.PaymentRepository;
import com.fit.subscription.repository.SubscriptionAddOnRepository;
import com.fit.subscription.repository.SubscriptionRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class RenewalService {

    private final PaymentService paymentService;
    private final SubscriptionRepository subscriptionRepository;
    private final PaymentRepository paymentRepository;
    private final DunningService dunningService;
    private final NotificationService notificationService;
    private final SubscriptionAddOnRepository subscriptionAddOnRepository;

    public RenewalService(PaymentService paymentService,
                          SubscriptionRepository subscriptionRepository,
                          PaymentRepository paymentRepository,
                          DunningService dunningService,
                          NotificationService notificationService,
                          SubscriptionAddOnRepository subscriptionAddOnRepository){
        this.paymentService = paymentService;
        this.subscriptionRepository = subscriptionRepository;
        this.paymentRepository = paymentRepository;
        this.dunningService  = dunningService;
        this.notificationService = notificationService;
        this.subscriptionAddOnRepository = subscriptionAddOnRepository;
    }
    @Transactional
    //this method only for the first time renewal attempt - later it shifts to retryPayment()
    public void renewSubscription(Long subscriptionId){
        Subscription subscription = subscriptionRepository.findById(subscriptionId).orElseThrow(()->new ResourceNotFoundException("Subscription id not found"));

        //validations
        if(!subscription.getAutoRenew()){
            throw new IllegalArgumentException("Auto renewal is not allowed");
        }
        if(subscription.getStatus() != SubscriptionStatus.ACTIVE){
            throw new IllegalArgumentException("Subscription is not Active to enable Renewal");
        }
        if(subscription.getEndDate().isAfter(LocalDate.now())){
            throw new IllegalArgumentException("Renew Date does not match");
        }
        //last payment fetch for paymentMethod
        Payment lastPayment = paymentRepository
                .findTopBySubscriptionOrderByPaymentDateDesc(subscription)
                .orElseThrow(() -> new ResourceNotFoundException("No previous payment found"));
        //to fetch all the addons to be included
        List<SubscriptionAddOn> subscriptionAddOns = subscriptionAddOnRepository.findBySubscription(subscription);

        //calculate the amount now
        BigDecimal addOnRenewalAmount = BigDecimal.ZERO;
        for (SubscriptionAddOn subscriptionAddOn : subscriptionAddOns) {
            BigDecimal addOnPrice = subscriptionAddOn.getAddOn().getUnitPrice().multiply(BigDecimal.valueOf(subscriptionAddOn.getUnitsIncluded()));
            addOnRenewalAmount = addOnRenewalAmount.add(addOnPrice);
        }
        //renewal amount - later add + addon amount
        BigDecimal renewalAmount = subscription.getPlan().getPrice().add(addOnRenewalAmount);
        //call the payment process
        PaymentResponseDTO paymentResponse = paymentService.processPayment(subscription.getId(),renewalAmount,lastPayment.getPaymentMethod(), PaymentType.RENEWAL);

        if(paymentResponse.getPaymentStatus() == PaymentStatus.SUCCESS){
            subscription.setStartDate(subscription.getEndDate().plusDays(1));
            subscription.setEndDate(subscription.getStartDate().plusDays(subscription.getPlan().getDurationDays()));
            subscription.setFinalPrice(renewalAmount);//renewalAmount or paymentResponse.getAmount()
            subscription.setStatus(SubscriptionStatus.ACTIVE);
            subscription.setGraceEndDate(null);
            subscription.setNextRetryDate(null);
            subscription.setRenewalAttempts(0);
            subscriptionRepository.save(subscription);

            //update the subscriptionAddOn table as well
            for (SubscriptionAddOn subscriptionAddOn : subscriptionAddOns) {
                subscriptionAddOn.setUnitsUsed(0);
                subscriptionAddOn.setBillingCycleStart(subscription.getStartDate());
                subscriptionAddOn.setBillingCycleEnd(subscription.getEndDate());

                subscriptionAddOnRepository.save(subscriptionAddOn);
            }
        }
        else{
            subscription.setStatus(SubscriptionStatus.GRACE);
            subscription.setRenewalAttempts(1);
            subscription.setNextRetryDate(LocalDate.now().plusDays(1));
            subscription.setGraceEndDate(subscription.getEndDate().plusDays(3));
            notificationService.sendDunningEmail(
                    subscription.getUser(),
                    1,
                    subscription.getNextRetryDate()
            );

        }

        subscriptionRepository.save(subscription);

        //put dunning after saving subscription
        //dunning log
        dunningService.saveLog(subscription, 1,paymentResponse.getPaymentStatus(),
                paymentResponse.getPaymentStatus() == PaymentStatus.SUCCESS ? null : "Renewal payment failed",
                subscription.getNextRetryDate());

    }

    // Admin-only test helpers so the renewal/dunning flow can be exercised on demand instead of
    // waiting for the real midnight cron and a real 30-day cycle. They reuse the exact same
    // renewSubscription/retryPayment methods the scheduler calls - only the date fast-forward
    // is test-only.
    @Transactional
    public void simulateCycleEndForTesting(Long subscriptionId){
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription id not found"));
        if(subscription.getEndDate().isAfter(LocalDate.now())){
            subscription.setEndDate(LocalDate.now());
            subscriptionRepository.save(subscription);
        }
        renewSubscription(subscriptionId);
    }

    @Transactional
    public void simulateRetryForTesting(Long subscriptionId){
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription id not found"));
        if(subscription.getStatus() != SubscriptionStatus.GRACE){
            throw new IllegalArgumentException("Subscription must be in GRACE status to test a retry");
        }
        if(subscription.getNextRetryDate() != null && subscription.getNextRetryDate().isAfter(LocalDate.now())){
            subscription.setNextRetryDate(LocalDate.now());
            subscriptionRepository.save(subscription);
        }
        retryPayment(subscriptionId);
    }

    @Transactional
    public void retryPayment(Long subscriptionId){
        Subscription subscription = subscriptionRepository.findById(subscriptionId).orElseThrow(()->new ResourceNotFoundException("Subscription ID not found for retrying payment"));

        //validate
        if(subscription.getStatus() != SubscriptionStatus.GRACE){
            throw new IllegalArgumentException("Cannot proceed with payment as it is not Grace");
        }
        if(subscription.getNextRetryDate().isAfter(LocalDate.now())){
            throw new IllegalArgumentException("Retry Payment date does not match");
        }
        // safety net: even if the attempt counter hasn't hit 3 yet (e.g. a retry got
        // delayed by downtime and the schedule drifted), the 3-day grace window is a
        // hard deadline - once it's passed, stop retrying and expire immediately
        // instead of attempting another charge.
        if(subscription.getGraceEndDate() != null && subscription.getGraceEndDate().isBefore(LocalDate.now())){
            subscription.setStatus(SubscriptionStatus.EXPIRED);
            subscription.setAutoRenew(false);
            subscription.setNextRetryDate(null);
            subscriptionRepository.save(subscription);

            dunningService.saveLog(subscription,
                    subscription.getRenewalAttempts(),
                    PaymentStatus.FAILED,
                    "Grace period expired before retry could be attempted",
                    null);
            return;
        }
        if(subscription.getRenewalAttempts() >= 3){
            throw new IllegalArgumentException("Maximum retry attempts reached");
        }
        //last payment fetch for paymentMethod
        Payment lastPayment = paymentRepository
                .findTopBySubscriptionOrderByPaymentDateDesc(subscription)
                .orElseThrow(() -> new ResourceNotFoundException("No previous payment found"));

        //to fetch all the addons to be included
        List<SubscriptionAddOn> subscriptionAddOns = subscriptionAddOnRepository.findBySubscription(subscription);

        //calculate the amount now
        BigDecimal addOnRenewalAmount = BigDecimal.ZERO;
        for (SubscriptionAddOn subscriptionAddOn : subscriptionAddOns) {
            BigDecimal addOnPrice = subscriptionAddOn.getAddOn().getUnitPrice().multiply(BigDecimal.valueOf(subscriptionAddOn.getUnitsIncluded()));
            addOnRenewalAmount = addOnRenewalAmount.add(addOnPrice);
        }
        //amount - later addon
        BigDecimal retryAmount = subscription.getPlan().getPrice().add(addOnRenewalAmount);

        PaymentResponseDTO retryPayResponse = paymentService.processPayment(subscription.getId(),retryAmount,lastPayment.getPaymentMethod(),PaymentType.RENEWAL);


        if(retryPayResponse.getPaymentStatus() == PaymentStatus.SUCCESS){

            subscription.setStartDate(subscription.getEndDate().plusDays(1));
            subscription.setEndDate(subscription.getStartDate().plusDays(subscription.getPlan().getDurationDays()));
            subscription.setFinalPrice(retryAmount);
            subscription.setStatus(SubscriptionStatus.ACTIVE);
            subscription.setGraceEndDate(null);
            subscription.setNextRetryDate(null);
            subscription.setRenewalAttempts(0);
            subscription.setAutoRenew(true);
            subscriptionRepository.save(subscription);

            for (SubscriptionAddOn subscriptionAddOn : subscriptionAddOns) {
                subscriptionAddOn.setUnitsUsed(0);
                subscriptionAddOn.setBillingCycleStart(subscription.getStartDate());
                subscriptionAddOn.setBillingCycleEnd(subscription.getEndDate());

                subscriptionAddOnRepository.save(subscriptionAddOn);
            }

        }
        else{
            subscription.setRenewalAttempts(subscription.getRenewalAttempts() + 1);
            if(subscription.getRenewalAttempts() < 3){
                subscription.setNextRetryDate(LocalDate.now().plusDays(1));
            }
            else{
                subscription.setStatus(SubscriptionStatus.EXPIRED);
                //subscription.setEndDate(LocalDate.now());
                subscription.setNextRetryDate(null);
                subscription.setGraceEndDate(null);
                subscription.setAutoRenew(false);
                subscription.setRenewalAttempts(3);

            }
            //call the notification only when payment fails - no real mail
            notificationService.sendDunningEmail(
                    subscription.getUser(),
                    subscription.getRenewalAttempts(),
                    subscription.getNextRetryDate()
            );
        }
        subscriptionRepository.save(subscription);

        //put dunning after saving subscription.
        //dunning log
        dunningService.saveLog(subscription ,
                              subscription.getRenewalAttempts(),
                              retryPayResponse.getPaymentStatus(),
                              retryPayResponse.getPaymentStatus() == PaymentStatus.SUCCESS?
                                      null : (subscription.getRenewalAttempts() >= 3 ?
                                      "Maximum retry attempts reached"
                                      : "Retry payment failed again"),
                                subscription.getNextRetryDate()
        );



    }
}
