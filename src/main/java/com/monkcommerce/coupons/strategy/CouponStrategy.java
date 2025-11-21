package com.monkcommerce.coupons.strategy;

import com.monkcommerce.coupons.model.Cart;

import java.util.Map;

public interface CouponStrategy {
    // Returns the canonical type string this strategy handles (e.g. "bxgy").
    String getType();

    boolean isApplicable(Cart cart, Map<String, Object> details);

    Cart applyDiscount(Cart cart, Map<String, Object> details);
}