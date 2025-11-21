package com.monkcommerce.coupons.service;

import com.monkcommerce.coupons.exception.ResourceNotFoundException;
import com.monkcommerce.coupons.model.Cart;
import com.monkcommerce.coupons.model.Coupon;
import com.monkcommerce.coupons.repository.CouponRepository;
import com.monkcommerce.coupons.strategy.CouponStrategy;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CouponService {

    private static final Logger log = LoggerFactory.getLogger(CouponService.class);

    private final CouponRepository couponRepository;
    private final Map<String, CouponStrategy> strategyMap = new HashMap<>();

    // Spring automatically gives us all classes that implement CouponStrategy
    public CouponService(CouponRepository couponRepository, List<CouponStrategy> strategies) {
        this.couponRepository = couponRepository;

        // Map each strategy by its declared type (e.g. strategy.getType() -> strategy)
        for (CouponStrategy strategy : strategies) {
            try {
                String type = strategy.getType();
                if (type != null) {
                    strategyMap.put(type, strategy);
                }
            } catch (Exception e) {
                log.warn("Skipping strategy during registration due to error: {}", e.getMessage());
            }
        }
    }

    public Coupon createCoupon(Coupon coupon) {
        return couponRepository.save(coupon);
    }

    public Coupon getCoupon(Long id) throws ResourceNotFoundException {
        return couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found with id : "+id));
    }

    public List<Coupon> getAllCoupons() {
        return couponRepository.findAll();
    }

    public Map<String, Object> getApplicableCoupons(Cart cart) {
        List<Map<String, Object>> applicableCoupons = new ArrayList<>();
        List<Coupon> allCoupons = couponRepository.findAll();

        for (Coupon coupon : allCoupons) {
            // Skip inactive or expired coupons
            if (!coupon.isActive() || (coupon.getExpirationDate() != null && coupon.getExpirationDate().isBefore(LocalDateTime.now()))) {
                log.debug("Skipping coupon {} because it is inactive or expired", coupon.getId());
                continue;
            }

            CouponStrategy strategy = strategyMap.get(coupon.getType());
            if (strategy != null && strategy.isApplicable(cart, coupon.getDetails())) {

                // Use Cart.copy() to perform a dry-run without mutating the original
                Cart tempCart = cart.copy();
                strategy.applyDiscount(tempCart, coupon.getDetails());
                log.debug("Coupon {} applicable — dry-run discount={}", coupon.getId(), tempCart.getTotalDiscount());

                Map<String, Object> result = new HashMap<>();
                result.put("coupon_id", coupon.getId());
                result.put("type", coupon.getType());
                result.put("discount", tempCart.getTotalDiscount());
                applicableCoupons.add(result);
            }
        }

        return Map.of("applicable_coupons", applicableCoupons);
    }

    public Cart applyCoupon(Long id, Cart cart) throws RuntimeException {
        Coupon coupon = getCoupon(id);
        // Check active + expiration
        if (!coupon.isActive() || (coupon.getExpirationDate() != null && coupon.getExpirationDate().isBefore(LocalDateTime.now()))) {
            log.info("Attempt to apply expired/inactive coupon {}", coupon.getId());
            throw new com.monkcommerce.coupons.exception.CouponExpiredException("Coupon expired or inactive: " + coupon.getId());
        }

        CouponStrategy strategy = strategyMap.get(coupon.getType());

        if (strategy == null) {
            throw new com.monkcommerce.coupons.exception.InvalidCouponTypeException("Invalid coupon type: " + coupon.getType());
        }

        if (!strategy.isApplicable(cart, coupon.getDetails())) {
            throw new com.monkcommerce.coupons.exception.CouponCriteriaNotMetException("Coupon criteria not met");
        }

        // This actually modifies the cart and returns it
        log.info("Applying coupon {} to cart", coupon.getId());
        return strategy.applyDiscount(cart, coupon.getDetails());
    }
}