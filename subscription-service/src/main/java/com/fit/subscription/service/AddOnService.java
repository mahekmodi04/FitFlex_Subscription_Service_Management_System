package com.fit.subscription.service;

import com.fit.subscription.dto.AddOnRequestDTO;
import com.fit.subscription.dto.PaymentResponseDTO;
import com.fit.subscription.dto.SubscriptionAddOnResponseDTO;
import com.fit.subscription.entity.AddOn;
import com.fit.subscription.entity.Payment;
import com.fit.subscription.entity.Subscription;
import com.fit.subscription.entity.SubscriptionAddOn;
import com.fit.subscription.enums.PaymentStatus;
import com.fit.subscription.enums.PaymentType;
import com.fit.subscription.enums.SubscriptionStatus;
import com.fit.subscription.exception.ResourceNotFoundException;
import com.fit.subscription.repository.AddOnRepository;
import com.fit.subscription.repository.PaymentRepository;
import com.fit.subscription.repository.SubscriptionAddOnRepository;
import com.fit.subscription.repository.SubscriptionRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class AddOnService {
    private final AddOnRepository addOnRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionAddOnRepository subscriptionAddOnRepository;
    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;

    public AddOnService(AddOnRepository addOnRepository,
                        SubscriptionRepository subscriptionRepository,
                        SubscriptionAddOnRepository subscriptionAddOnRepository,
                        PaymentService paymentService,
                        PaymentRepository paymentRepository){
        this.addOnRepository = addOnRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.subscriptionAddOnRepository = subscriptionAddOnRepository;
        this.paymentService = paymentService;
        this.paymentRepository = paymentRepository;
    }
    @Transactional
    public AddOn createAddOn(AddOn addOn){
        if(addOnRepository.findByNameIgnoreCaseAndActiveTrue(addOn.getName()).isPresent()){
            throw new IllegalArgumentException(" AddOn already exists");
        }
        addOn.setActive(true);
        return addOnRepository.save(addOn);
    }

    public List<AddOn> getAllActiveAddOns(){
        return addOnRepository.findByActiveTrue();
    }

    public List<SubscriptionAddOnResponseDTO> getAddOnsForSubscription(Long subscriptionId){
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription does not exist"));

        return subscriptionAddOnRepository.findBySubscription(subscription).stream()
                .map(this::toResponseDTO)
                .toList();
    }

    private SubscriptionAddOnResponseDTO toResponseDTO(SubscriptionAddOn subscriptionAddOn) {
        return new SubscriptionAddOnResponseDTO(
                subscriptionAddOn.getAddOn().getId(),
                subscriptionAddOn.getAddOn().getName(),
                subscriptionAddOn.getUnitsIncluded(),
                subscriptionAddOn.getUnitsUsed(),
                subscriptionAddOn.getAddOn().getUnitPrice(),
                subscriptionAddOn.getBillingCycleStart(),
                subscriptionAddOn.getBillingCycleEnd()
        );
    }

    @Transactional
    public SubscriptionAddOnResponseDTO attachAddOn(Long subscriptionId , Long addOnId, Integer unitsIncluded){
        //validations
        Subscription subscription = subscriptionRepository.findById(subscriptionId).orElseThrow(() -> new ResourceNotFoundException("Subscription does not exist"));
        AddOn addOn = addOnRepository.findById(addOnId).orElseThrow(()-> new ResourceNotFoundException("addOn does not exist"));

        if(!subscription.getStatus().equals(SubscriptionStatus.ACTIVE)){
            throw new IllegalArgumentException("Subscription is not currently Active");
        }
        if(!addOn.getActive()){
            throw new IllegalArgumentException("AddOn is currently not active");
        }
        if(unitsIncluded <= 0){
            throw new IllegalArgumentException("Units Included cannot be negative");
        }
//        if(subscriptionAddOnRepository.findBySubscriptionAndAddOn(subscription , addOn).isPresent()){
//            throw new RuntimeException("AddOn already attached to this subscription");
//        }
        SubscriptionAddOn subscriptionAddOn = subscriptionAddOnRepository.findBySubscriptionAndAddOn(subscription, addOn).orElse(null);

        //calculate amount for addon
        BigDecimal addOnAmount = addOn.getUnitPrice().multiply(BigDecimal.valueOf(unitsIncluded));

        //last payment method
        Payment lastPayment = paymentRepository
                .findTopBySubscriptionOrderByPaymentDateDesc(subscription)
                .orElseThrow(() -> new ResourceNotFoundException("No previous payment found"));

        //call payment process to payment
        PaymentResponseDTO paymentResponse = paymentService.processPayment(subscription.getId(), addOnAmount, lastPayment.getPaymentMethod(), PaymentType.ADDON);

        if(paymentResponse.getPaymentStatus() != PaymentStatus.SUCCESS){
            throw new IllegalStateException("AddOn payment failed");
        }
        else{
            if(subscriptionAddOn == null){

                subscriptionAddOn = new SubscriptionAddOn();
                subscriptionAddOn.setSubscription(subscription);
                subscriptionAddOn.setAddOn(addOn);
                subscriptionAddOn.setUnitsIncluded(unitsIncluded);
                subscriptionAddOn.setUnitsUsed(0);
                subscriptionAddOn.setBillingCycleStart(subscription.getStartDate());
                subscriptionAddOn.setBillingCycleEnd(subscription.getEndDate());

            }
            else{
                subscriptionAddOn.setUnitsIncluded(subscriptionAddOn.getUnitsIncluded() + unitsIncluded);
                subscriptionAddOn.setBillingCycleStart(subscription.getStartDate());
                subscriptionAddOn.setBillingCycleEnd(subscription.getEndDate());
            }
        }

        return toResponseDTO(subscriptionAddOnRepository.save(subscriptionAddOn));
    }

    @Transactional
    public SubscriptionAddOnResponseDTO recordUsage(Long subscriptionId, Long addOnId, Integer units){
        Subscription subscription = subscriptionRepository.findById(subscriptionId).orElseThrow(()-> new ResourceNotFoundException("Subscription id does not exist"));
        AddOn addOn = addOnRepository.findById(addOnId).orElseThrow(()->new ResourceNotFoundException("AddOn id does not exist"));

        SubscriptionAddOn subscriptionAddOn = subscriptionAddOnRepository.findBySubscriptionAndAddOn(subscription ,addOn).orElseThrow(()->new ResourceNotFoundException("This SubscriptionAddOn does not exist"));

        //validations
        if(units <= 0){
            throw new IllegalArgumentException("Units used cannot be negative");
        }

        int updatedUsage = subscriptionAddOn.getUnitsUsed() + units;
        if(updatedUsage > subscriptionAddOn.getUnitsIncluded()){
            throw new IllegalArgumentException("AddOn usage limit exceeded");
        }
        subscriptionAddOn.setUnitsUsed(updatedUsage);
        return toResponseDTO(subscriptionAddOnRepository.save(subscriptionAddOn));

    }
//this method for create/change subscription -> so called in subscription service
    public BigDecimal calculateAddOnPrice(List<AddOnRequestDTO> addOns) {
        if (addOns == null || addOns.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal totalAddOnPrice = BigDecimal.ZERO;

        for (AddOnRequestDTO addOnRequest : addOns) {

            AddOn addOn = addOnRepository.findById(addOnRequest.getAddOnId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException("AddOn does not exist"));
            //validations
            if (!addOn.getActive()) {
                throw new IllegalArgumentException("AddOn " + addOn.getName() + " is not active");
            }
            if (addOnRequest.getUnitsIncluded() <= 0) {
                throw new IllegalArgumentException("Units Included must be greater than 0");
            }
            BigDecimal addOnPrice = addOn.getUnitPrice()
                    .multiply(BigDecimal.valueOf(addOnRequest.getUnitsIncluded()));

            totalAddOnPrice = totalAddOnPrice.add(addOnPrice);

        }
        return totalAddOnPrice;
    }
    // Called only after a plan upgrade payment has succeeded. Two things happen:
    // 1) every add-on already attached to this subscription carries forward for FREE -
    //    only the unused units (unitsIncluded - unitsUsed) survive into the new cycle,
    //    usage resets to 0, billing cycle dates move to the new subscription dates.
    //    No charge/credit is applied for this - it was already paid for.
    // 2) any newly-requested add-ons (already charged for as part of the upgrade payment,
    //    see calculateAddOnPrice) get merged in on top of the carried-forward units.
    @Transactional
    public void carryForwardAndMergeAddOnsOnUpgrade(Subscription subscription, List<AddOnRequestDTO> newAddOns) {

        List<SubscriptionAddOn> existingAddOns = subscriptionAddOnRepository.findBySubscription(subscription);

        for (SubscriptionAddOn subscriptionAddOn : existingAddOns) {
            int unused = subscriptionAddOn.getUnitsIncluded() - subscriptionAddOn.getUnitsUsed();
            if (unused < 0) {
                unused = 0;
            }
            subscriptionAddOn.setUnitsIncluded(unused);
            subscriptionAddOn.setUnitsUsed(0);
            subscriptionAddOn.setBillingCycleStart(subscription.getStartDate());
            subscriptionAddOn.setBillingCycleEnd(subscription.getEndDate());
        }
        subscriptionAddOnRepository.saveAll(existingAddOns);

        if (newAddOns == null || newAddOns.isEmpty()) {
            return;
        }

        for (AddOnRequestDTO request : newAddOns) {
            AddOn addOn = addOnRepository.findById(request.getAddOnId())
                    .orElseThrow(() -> new ResourceNotFoundException("AddOn does not exist"));

            SubscriptionAddOn subscriptionAddOn = existingAddOns.stream()
                    .filter(existing -> existing.getAddOn().getId().equals(addOn.getId()))
                    .findFirst()
                    .orElse(null);

            if (subscriptionAddOn == null) {
                subscriptionAddOn = new SubscriptionAddOn();
                subscriptionAddOn.setSubscription(subscription);
                subscriptionAddOn.setAddOn(addOn);
                subscriptionAddOn.setUnitsIncluded(request.getUnitsIncluded());
                subscriptionAddOn.setUnitsUsed(0);
                subscriptionAddOn.setBillingCycleStart(subscription.getStartDate());
                subscriptionAddOn.setBillingCycleEnd(subscription.getEndDate());
            } else {
                subscriptionAddOn.setUnitsIncluded(subscriptionAddOn.getUnitsIncluded() + request.getUnitsIncluded());
            }
            subscriptionAddOnRepository.save(subscriptionAddOn);
        }
    }

    @Transactional
    public void attachAddOnsDuringSubscriptionCreation(Subscription subscription, List<AddOnRequestDTO> addOns) {

        if(addOns == null || addOns.isEmpty()){
            return;
        }
        for (AddOnRequestDTO request : addOns) {
            AddOn addOn = addOnRepository.findById(request.getAddOnId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException("AddOn does not exist"));
            if (!addOn.getActive()) {
                throw new IllegalArgumentException("AddOn is currently not active");
            }
            if (request.getUnitsIncluded() <= 0) {
                throw new IllegalArgumentException("Units Included must be greater than 0");
            }

            SubscriptionAddOn subscriptionAddOn = new SubscriptionAddOn();

            subscriptionAddOn.setSubscription(subscription);
            subscriptionAddOn.setAddOn(addOn);
            subscriptionAddOn.setUnitsIncluded(request.getUnitsIncluded());
            subscriptionAddOn.setUnitsUsed(0);
            subscriptionAddOn.setBillingCycleStart(subscription.getStartDate());
            subscriptionAddOn.setBillingCycleEnd(subscription.getEndDate());

            subscriptionAddOnRepository.save(subscriptionAddOn);
        }
    }

}
