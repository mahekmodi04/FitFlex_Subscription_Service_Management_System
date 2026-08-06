package com.fit.subscription.service;

import com.fit.subscription.entity.Coupon;
import com.fit.subscription.exception.ResourceNotFoundException;
import com.fit.subscription.repository.CouponRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CouponService {
    private final CouponRepository couponRepository;

    public CouponService(CouponRepository couponRepository){
        this.couponRepository = couponRepository;
    }
    //functions
    public Coupon createCoupon(Coupon coupon){
        if(couponRepository.findByCode(coupon.getCode()).isPresent()){
            throw new IllegalArgumentException("Code already exists");
        }
        return couponRepository.save(coupon);
    }
    //since we are here using Optional thing we do like this
//    public Optional<Coupon> getCouponById(Long id){
//        if(!couponRepository.existsById(id)){
//            throw new RuntimeException("Id does not exist");
//        }
//        return couponRepository.findById(id);
//    }
    //but if we want Coupon obj to be returned then do this
    public Coupon getCouponById(Long id){
        return couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Id does not exist"));
    }

    public Coupon getCouponByCode(String code){
        return couponRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Code does not exist"));
    }
    public List<Coupon> getAllCoupons(){
        return couponRepository.findAll();
    }
    public void deleteCoupon(Long id){
        if(!couponRepository.existsById(id)){
            throw new ResourceNotFoundException("Id does not exist, cannot delete");
        }
        couponRepository.deleteById(id);
    }
    public Coupon updateCoupon(Long id, Coupon updatedCoupon){
        Coupon existingCoupon = couponRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Coupon does not exist"));

        if (!existingCoupon.getCode().equals(updatedCoupon.getCode())
                && couponRepository.findByCode(updatedCoupon.getCode()).isPresent()) {
            throw new IllegalArgumentException("Coupon code already exists");
        }
        existingCoupon.setCode(updatedCoupon.getCode());
        existingCoupon.setDiscountPercentage(updatedCoupon.getDiscountPercentage());
        existingCoupon.setExpiryDate(updatedCoupon.getExpiryDate());
        existingCoupon.setType(updatedCoupon.getType());
        existingCoupon.setDiscountAmount(updatedCoupon.getDiscountAmount());
        existingCoupon.setUsageLimit(updatedCoupon.getUsageLimit());
        existingCoupon.setActive(updatedCoupon.getActive());

        return couponRepository.save(existingCoupon);
    }
}
