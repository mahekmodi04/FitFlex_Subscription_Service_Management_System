package com.fit.subscription.service;

import com.fit.subscription.dto.*;
import com.fit.subscription.entity.*;
import com.fit.subscription.enums.*;
import com.fit.subscription.exception.ResourceNotFoundException;
import com.fit.subscription.repository.CouponRepository;
import com.fit.subscription.repository.CouponUsageRepository;
import com.fit.subscription.repository.SubscriptionAddOnRepository;
import com.fit.subscription.repository.SubscriptionRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final UserService userService;
    private final PlanService planService;
    private final CouponService couponService;
    private final CouponUsageRepository couponUsageRepository;
    private final PaymentService paymentService;
    private final CouponRepository couponRepository;
    private final AddOnService addOnService;
    private final SubscriptionAddOnRepository subscriptionAddOnRepository;

    //Constructor Injection

    public SubscriptionService(SubscriptionRepository subscriptionRepository,
                               UserService userService,
                               PlanService planService,
                               CouponService couponService,
                               CouponUsageRepository couponUsageRepository,
                               PaymentService paymentService,
                               CouponRepository couponRepository,
                               AddOnService addOnService,
                               SubscriptionAddOnRepository subscriptionAddOnRepository){
        this.subscriptionRepository = subscriptionRepository;
        this.userService = userService;
        this.couponService = couponService;
        this.planService = planService;
        this.couponUsageRepository = couponUsageRepository;
        this.paymentService = paymentService;
        this.couponRepository = couponRepository;
        this.addOnService = addOnService;
        this.subscriptionAddOnRepository = subscriptionAddOnRepository;
    }

    @Transactional
    public SubscriptionResponseDTO createSubscription(CreateSubscriptionRequest request){
        User user = userService.getUserById(request.getUserId());
        Plan plan = planService.getPlanById(request.getPlanId());
        Coupon coupon = null;
        if(request.getCouponCode() != null && (!request.getCouponCode().isBlank())) { // for coupon to not be null
            coupon = couponService.getCouponByCode(request.getCouponCode());
        }
        //validate the coupon first
        if(coupon != null){
            validateCoupon(coupon, user);
        }
        //calculate price for this
        BigDecimal finalPrice = calculateFinalPrice(plan , coupon);
        // these 2 lines for addon during subscription creation
        BigDecimal addOnPrice = addOnService.calculateAddOnPrice(request.getAddOns());
        finalPrice = finalPrice.add(addOnPrice);
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(plan.getDurationDays());

        //set values for DB and response
        Subscription subscription = new Subscription(); // DB store

        subscription.setUser(user);
        subscription.setPlan(plan);
        subscription.setCoupon(coupon);
        subscription.setFinalPrice(finalPrice);
        subscription.setAutoRenew(request.getAutoRenew() != null ? request.getAutoRenew() : true);
//        subscription.setAutoRenew(request.getAutoRenew());
        subscription.setStartDate(startDate);
        subscription.setEndDate(endDate);
        subscription.setStatus(SubscriptionStatus.PENDING);
        subscription.setRenewalAttempts(0);

        Subscription savedSubscription = subscriptionRepository.save(subscription);

        // wallet redemption is opt-in and never covers more than what's actually owed
        BigDecimal walletToUse = BigDecimal.ZERO;
        if(Boolean.TRUE.equals(request.getUseWalletBalance())){
            walletToUse = user.getWalletBalance().min(finalPrice);
        }
        BigDecimal amountToCharge = finalPrice.subtract(walletToUse);

        PaymentResponseDTO paymentResponse = paymentService.processPayment(savedSubscription.getId(),
                                                                    amountToCharge,
                                                                    request.getPaymentMethod(),
                                                                    PaymentType.SUBSCRIPTION,
                                                                    walletToUse);
        if(paymentResponse.getPaymentStatus() == PaymentStatus.SUCCESS){
            savedSubscription.setStatus(SubscriptionStatus.ACTIVE);

            //for saving addOn rows in SubscriptionAddOn
            addOnService.attachAddOnsDuringSubscriptionCreation(savedSubscription, request.getAddOns());

            //couponusage handling which is happening together with subscription creation ->transactional
            if(coupon != null){
                //global coupon count
                coupon.setUsedCount(
                        coupon.getUsedCount() + 1
                );
                couponRepository.save(coupon);

                CouponUsage couponUsage = couponUsageRepository.findByUserAndCoupon(user, coupon).orElse(null);
                if(couponUsage == null){ // matlab first time user is using that coupon
                    couponUsage = new CouponUsage();
                    couponUsage.setUser(user);
                    couponUsage.setCoupon(coupon);
                    couponUsage.setUsageCount(1);

                }
                else{// else just increment the count
                    couponUsage.setUsageCount(couponUsage.getUsageCount() + 1);
                }

                couponUsageRepository.save(couponUsage);

            }
        }
        else{
            subscription.setStatus(SubscriptionStatus.PENDING);
        }
        subscriptionRepository.save(savedSubscription);



        SubscriptionResponseDTO response = new SubscriptionResponseDTO();  //for DTO frontend response

        response.setId(savedSubscription.getId());
        response.setUserName(savedSubscription.getUser().getName());
        response.setPlanName(savedSubscription.getPlan().getName());
        if(savedSubscription.getCoupon() != null){
            response.setCouponCode(savedSubscription.getCoupon().getCode());
        }
        response.setStartDate(savedSubscription.getStartDate());
        response.setEndDate(savedSubscription.getEndDate());
        response.setStatus(savedSubscription.getStatus());
        response.setFinalPrice(savedSubscription.getFinalPrice());
        response.setAutoRenew(savedSubscription.getAutoRenew());
        response.setPaymentStatus(paymentResponse.getPaymentStatus());
        response.setNextRetryDate(savedSubscription.getNextRetryDate());
        response.setGraceEndDate(savedSubscription.getGraceEndDate());


        return response;
    }

    //cancel subscription method
    @Transactional
    public SubscriptionResponseDTO cancelSubscription(Long subscriptionId){
        //check if the id exists
        Subscription existedSubscription = subscriptionRepository.findById(subscriptionId).orElseThrow(() -> new ResourceNotFoundException("subscription Id does not exist"));

        SubscriptionResponseDTO response = new SubscriptionResponseDTO();
        if(existedSubscription.getStatus() != SubscriptionStatus.CANCELLED){

            existedSubscription.setStatus(SubscriptionStatus.CANCELLED);
            existedSubscription.setAutoRenew(false);
            subscriptionRepository.save(existedSubscription);

            response.setId(existedSubscription.getId());
            response.setUserName(existedSubscription.getUser().getName());
            response.setPlanName(existedSubscription.getPlan().getName());
            if(existedSubscription.getCoupon() != null){
                response.setCouponCode(existedSubscription.getCoupon().getCode());
            }
            response.setStatus(SubscriptionStatus.CANCELLED);
            response.setStartDate(existedSubscription.getStartDate());
            response.setEndDate(existedSubscription.getEndDate());
            response.setAutoRenew(false);
            response.setFinalPrice(existedSubscription.getFinalPrice());
            response.setNextRetryDate(existedSubscription.getNextRetryDate());
            response.setGraceEndDate(existedSubscription.getGraceEndDate());

        }
        else{
            throw new IllegalArgumentException("Subscription is already cancelled");
        }
        return response;
    }

    @Transactional
    public ChangePlanResponseDTO changePlan(ChangePlanRequestDTO request){
        Subscription subscription = subscriptionRepository.findById(request.getSubscriptionId()).orElseThrow(()-> new ResourceNotFoundException("Subscription not found"));
        // status shd be active to proceed
        if(subscription.getStatus() != SubscriptionStatus.ACTIVE){
            throw new IllegalArgumentException("Subscription is not currently Active");
        }
        Plan newPlan = planService.getPlanById(request.getNewPlanId());
        // only upgrade wala plans work along with proration - no downgrades
        if(newPlan.getPrice().compareTo(subscription.getPlan().getPrice()) <= 0){
            throw new IllegalArgumentException("Plan can only be upgraded");
        }

        long daysUsed = ChronoUnit.DAYS.between(subscription.getStartDate(), LocalDate.now());
        BigDecimal pricePerDay = subscription.getPlan().getPrice().divide(BigDecimal.valueOf(subscription.getPlan().getDurationDays()),2, RoundingMode.HALF_UP);
        BigDecimal priceConsumed = pricePerDay.multiply(BigDecimal.valueOf(daysUsed));
        if(priceConsumed.compareTo(subscription.getPlan().getPrice()) > 0){
            priceConsumed = subscription.getPlan().getPrice();
        }
        // proration credit is based on the plan price only - existing add-ons are never
        // charged or credited here, their unused units just carry forward for free
        // since they were already paid for at attach/renewal time
        BigDecimal proratedPlanPrice = newPlan.getPrice().subtract(subscription.getPlan().getPrice().subtract(priceConsumed));

        // only NEWLY requested add-ons (bought at the moment of upgrade) are charged
        BigDecimal newAddOnCharge = addOnService.calculateAddOnPrice(request.getAddOns());
        BigDecimal totalAmount = proratedPlanPrice.add(newAddOnCharge);

        BigDecimal walletToUse = BigDecimal.ZERO;
        if(Boolean.TRUE.equals(request.getUseWalletBalance())){
            walletToUse = subscription.getUser().getWalletBalance().min(totalAmount);
        }
        BigDecimal amountToCharge = totalAmount.subtract(walletToUse);

        PaymentResponseDTO paymentResponse = paymentService.processPayment(subscription.getId(), amountToCharge,request.getPaymentMethod(),PaymentType.UPGRADE, walletToUse);

        ChangePlanResponseDTO response = new ChangePlanResponseDTO();

        if(paymentResponse.getPaymentStatus() == PaymentStatus.SUCCESS){

            subscription.setPlan(newPlan);
            subscription.setStartDate(LocalDate.now());
            subscription.setEndDate(subscription.getStartDate().plusDays(newPlan.getDurationDays()));
            subscription.setFinalPrice(totalAmount);
            subscription.setStatus(SubscriptionStatus.ACTIVE);

            // carry forward unused units of already-attached add-ons for free,
            // then merge in any newly-requested add-ons on top
            addOnService.carryForwardAndMergeAddOnsOnUpgrade(subscription, request.getAddOns());
        }
        else{
            // payment failed - the old plan was already paid for and is still valid,
            // so leave the subscription exactly as it was (still ACTIVE on the old plan).
            // Nothing to change here - response.paymentStatus below tells the caller it failed.
        }

        Subscription updatedSubscription = subscriptionRepository.save(subscription);

        //response
        response.setSubscriptionId(updatedSubscription.getId());
        response.setUserName(updatedSubscription.getUser().getName());
        response.setNewPlanName(updatedSubscription.getPlan().getName());
        response.setAutoRenew(updatedSubscription.getAutoRenew());
        response.setStatus(updatedSubscription.getStatus());
        response.setNewStartDate(updatedSubscription.getStartDate());
        response.setNewEndDate(updatedSubscription.getEndDate());
        response.setNewFinalPrice(updatedSubscription.getFinalPrice());
        response.setPaymentStatus(paymentResponse.getPaymentStatus());

        return response;
    }


    public SubscriptionResponseDTO getSubscriptionById(Long subscriptionId){
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found"));
        return toResponseDTO(subscription);
    }

    public List<SubscriptionResponseDTO> getSubscriptionsByUser(Long userId){
        return subscriptionRepository.findByUserId(userId).stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public List<SubscriptionResponseDTO> getAllSubscriptions(){
        return subscriptionRepository.findAll().stream()
                .map(this::toResponseDTO)
                .toList();
    }

    private SubscriptionResponseDTO toResponseDTO(Subscription subscription){
        SubscriptionResponseDTO response = new SubscriptionResponseDTO();
        response.setId(subscription.getId());
        response.setUserName(subscription.getUser().getName());
        response.setPlanName(subscription.getPlan().getName());
        if(subscription.getCoupon() != null){
            response.setCouponCode(subscription.getCoupon().getCode());
        }
        response.setStartDate(subscription.getStartDate());
        response.setEndDate(subscription.getEndDate());
        response.setStatus(subscription.getStatus());
        response.setFinalPrice(subscription.getFinalPrice());
        response.setAutoRenew(subscription.getAutoRenew());
        response.setNextRetryDate(subscription.getNextRetryDate());
        response.setGraceEndDate(subscription.getGraceEndDate());
        return response;
    }

    // usageLimit is a PER-USER cap (e.g. "usageLimit=2" means each user may redeem this
    // coupon twice, not that only 2 redemptions exist across the whole platform) - enforced
    // against CouponUsage, which is already tracked per (user, coupon) pair on every
    // successful redemption. coupon.usedCount stays as an aggregate stat for the admin view.
    private void validateCoupon(Coupon coupon, User user){
        if(!coupon.getActive()){
            throw new IllegalArgumentException("Coupon is inactive");
        }
        if(coupon.getExpiryDate().isBefore(LocalDate.now())){
            throw new IllegalArgumentException("Coupon is expired");
        }
        int userUsageCount = couponUsageRepository.findByUserAndCoupon(user, coupon)
                .map(CouponUsage::getUsageCount)
                .orElse(0);
        if(userUsageCount >= coupon.getUsageLimit()){
            throw new IllegalArgumentException("You've already used this coupon the maximum number of times");
        }
    }

    private BigDecimal calculateFinalPrice(Plan plan , Coupon coupon){
        if(coupon == null){
            return plan.getPrice();
        }

        BigDecimal finalPrice = plan.getPrice();
        if(coupon.getType() == CouponType.PERCENTAGE){
            BigDecimal discount = finalPrice.multiply(coupon.getDiscountPercentage()).divide(BigDecimal.valueOf(100));
            finalPrice = finalPrice.subtract(discount);

        }
        else if(coupon.getType() == CouponType.AMOUNT){
            finalPrice = finalPrice.subtract(coupon.getDiscountAmount());
        }
        else if(coupon.getType() == CouponType.BOTH){
            BigDecimal discount = finalPrice.multiply(coupon.getDiscountPercentage()).divide(BigDecimal.valueOf(100));
            finalPrice = finalPrice.subtract(discount).subtract(coupon.getDiscountAmount());

        }
        if(finalPrice.compareTo(BigDecimal.ZERO) < 0){ //TO CHECK IF VALUE IS LESS THAN ZERO WHEN DISCOUNT > PLANPRI
            finalPrice = BigDecimal.ZERO;
        }
        return finalPrice;
    }

}
