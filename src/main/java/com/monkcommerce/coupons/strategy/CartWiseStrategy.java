package com.monkcommerce.coupons.strategy;

import com.monkcommerce.coupons.model.Cart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class CartWiseStrategy implements CouponStrategy {

    private static final Logger log = LoggerFactory.getLogger(CartWiseStrategy.class);

    @Override
    public String getType() {
        return "cart-wise";
    }

    @Override
    public boolean isApplicable(Cart cart, Map<String, Object> details) {
        // Null-safe: reject when inputs missing
        if (cart == null || details == null) {
            log.debug("CartWiseStrategy: cart or details is null -> not applicable");
            return false;
        }
        Object thr = details.get("threshold");
        if (!(thr instanceof Number)) {
            log.debug("CartWiseStrategy: threshold missing or not numeric");
            return false;
        }
        double threshold = ((Number) thr).doubleValue();
        boolean applicable = cart.getTotalPrice() > threshold;
        log.debug("CartWiseStrategy: totalPrice={}, threshold={}, applicable={}", cart.getTotalPrice(), threshold, applicable);
        return applicable;
    }

    @Override
    public Cart applyDiscount(Cart cart, Map<String, Object> details) {
        if (cart == null || details == null) {
            log.debug("CartWiseStrategy.applyDiscount: cart or details is null -> no-op");
            return cart;
        }
        Object disc = details.get("discount");
        if (!(disc instanceof Number)) {
            log.debug("CartWiseStrategy.applyDiscount: discount missing or not numeric -> no-op");
            return cart;
        }
        double discountPercent = ((Number) disc).doubleValue();
        double discountAmount = cart.getTotalPrice() * (discountPercent / 100.0);

        cart.setTotalDiscount(discountAmount);
        cart.setFinalPrice(cart.getTotalPrice() - discountAmount);
        log.debug("CartWiseStrategy.applyDiscount: applied discountPercent={}, discountAmount={}", discountPercent, discountAmount);
        return cart;
    }
}
