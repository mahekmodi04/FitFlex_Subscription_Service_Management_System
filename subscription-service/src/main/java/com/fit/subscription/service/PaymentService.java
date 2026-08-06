package com.fit.subscription.service;

import com.fit.subscription.dto.PaymentResponseDTO;
import com.fit.subscription.entity.Payment;
import com.fit.subscription.entity.Subscription;
import com.fit.subscription.entity.User;
import com.fit.subscription.enums.PaymentMethod;
import com.fit.subscription.enums.PaymentStatus;
import com.fit.subscription.enums.PaymentType;
import com.fit.subscription.exception.ResourceNotFoundException;
import com.fit.subscription.repository.PaymentRepository;
import com.fit.subscription.repository.SubscriptionRepository;
import com.fit.subscription.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PaymentService {

    // loyalty credit added to a user's wallet every time a SUBSCRIPTION or RENEWAL payment succeeds
    private static final BigDecimal WALLET_CREDIT_PER_SUBSCRIPTION = BigDecimal.valueOf(25);

    private final PaymentRepository paymentRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final PaymentGatewaySimulator paymentGatewaySimulator;

    public PaymentService(PaymentRepository paymentRepository,
                          SubscriptionRepository subscriptionRepository,
                          UserRepository userRepository,
                          PaymentGatewaySimulator paymentGatewaySimulator){
        this.paymentRepository = paymentRepository;
        this.subscriptionRepository= subscriptionRepository;
        this.userRepository = userRepository;
        this.paymentGatewaySimulator = paymentGatewaySimulator;
    }

    public PaymentResponseDTO processPayment(Long subscriptionId,
                                             BigDecimal amount,
                                             PaymentMethod paymentMethod,
                                             PaymentType paymentType){
        return processPayment(subscriptionId, amount, paymentMethod, paymentType, BigDecimal.ZERO);
    }

    // walletAmountUsed: how much of this charge the caller already committed to cover from the
    // user's wallet balance (amount is the remainder actually sent to the gateway). Only deducted
    // if the payment succeeds.
    @Transactional
    public PaymentResponseDTO processPayment(Long subscriptionId,
                                             BigDecimal amount,
                                             PaymentMethod paymentMethod,
                                             PaymentType paymentType,
                                             BigDecimal walletAmountUsed){
        Subscription subscription = subscriptionRepository.findById(subscriptionId).orElseThrow(()->new ResourceNotFoundException("Cannot find the subscription id"));
        if(amount.compareTo(BigDecimal.ZERO) < 0){
            throw new IllegalArgumentException("Invalid payment amount");
        }
        BigDecimal walletUsed = walletAmountUsed == null ? BigDecimal.ZERO : walletAmountUsed;

        Payment payment = new Payment();
        payment.setAmount(amount);
        payment.setSubscription(subscription);
        payment.setPaymentMethod(paymentMethod);
        payment.setPaymentDate(LocalDateTime.now());
        payment.setPaymentType(paymentType);
        //creating transc id unique for each payment
        String transactionId = "TXN-" + System.currentTimeMillis();
        payment.setTransactionId(transactionId);
        //payment status - fully wallet-covered charges (amount == 0) skip the gateway entirely
        boolean paymentSuccess = amount.compareTo(BigDecimal.ZERO) == 0
                || paymentGatewaySimulator.gatewayStatus(transactionId);
        if(paymentSuccess){
            payment.setPaymentStatus(PaymentStatus.SUCCESS);
        }
        else{
            payment.setPaymentStatus(PaymentStatus.FAILED);
        }

        //save to the repo now
        Payment savedPayment = paymentRepository.save(payment);

        if(paymentSuccess){
            User user = subscription.getUser();
            if(walletUsed.compareTo(BigDecimal.ZERO) > 0){
                user.setWalletBalance(user.getWalletBalance().subtract(walletUsed));
            }
            if(paymentType == PaymentType.SUBSCRIPTION || paymentType == PaymentType.RENEWAL){
                user.setWalletBalance(user.getWalletBalance().add(WALLET_CREDIT_PER_SUBSCRIPTION));
            }
            userRepository.save(user);
        }

        PaymentResponseDTO response = new PaymentResponseDTO();

        response.setSubscriptionId(savedPayment.getSubscription().getId());
        response.setPaymentId(savedPayment.getId());
        response.setPaymentStatus(savedPayment.getPaymentStatus());
        response.setPaymentMethod(savedPayment.getPaymentMethod());
        response.setAmount(savedPayment.getAmount());
        response.setPaymentDate(savedPayment.getPaymentDate());
        response.setTransactionId(savedPayment.getTransactionId());
        response.setPaymentType(savedPayment.getPaymentType());


        return response;
    }

    public PaymentResponseDTO getPaymentById(Long id){
        return toResponse(paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found")));
    }

    public List<PaymentResponseDTO> getAllPayments(){
        return paymentRepository.findAll().stream().map(this::toResponse).toList();
    }

    public List<PaymentResponseDTO> getPaymentsBySubscription(Long subscriptionId){
        return paymentRepository.findBySubscriptionId(subscriptionId).stream().map(this::toResponse).toList();
    }

    private PaymentResponseDTO toResponse(Payment payment) {
        PaymentResponseDTO response = new PaymentResponseDTO();
        response.setPaymentId(payment.getId());
        response.setSubscriptionId(payment.getSubscription().getId());
        response.setAmount(payment.getAmount());
        response.setPaymentMethod(payment.getPaymentMethod());
        response.setPaymentStatus(payment.getPaymentStatus());
        response.setTransactionId(payment.getTransactionId());
        response.setPaymentDate(payment.getPaymentDate());
        response.setPaymentType(payment.getPaymentType());
        return response;
    }

}
