package com.monkcommerce.coupons.strategy;

import com.monkcommerce.coupons.model.Cart;
import com.monkcommerce.coupons.model.CartItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ProductWiseStrategy implements CouponStrategy {

    private static final Logger log = LoggerFactory.getLogger(ProductWiseStrategy.class);

    @Override
    public String getType() {
        return "product-wise";
    }

    @Override
    public boolean isApplicable(Cart cart, Map<String, Object> details) {
        // Null-safe guards
        if (cart == null || details == null) {
            log.debug("ProductWiseStrategy: cart or details is null -> not applicable");
            return false;
        }
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            log.debug("ProductWiseStrategy: cart has no items");
            return false;
        }
        Object pidObj = details.get("product_id");
        if (!(pidObj instanceof Number)) {
            log.debug("ProductWiseStrategy: product_id missing or not a number");
            return false;
        }
        Long targetProductId = ((Number) pidObj).longValue();
        boolean present = cart.getItems().stream().anyMatch(item -> item.getProductId().equals(targetProductId));
        log.debug("ProductWiseStrategy: targetProductId={}, present={}", targetProductId, present);
        return present;
    }

    @Override
    public Cart applyDiscount(Cart cart, Map<String, Object> details) {
        if (cart == null || details == null) {
            log.debug("ProductWiseStrategy.applyDiscount: cart or details is null -> no-op");
            return cart;
        }
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            log.debug("ProductWiseStrategy.applyDiscount: cart has no items -> no-op");
            return cart;
        }
        Object pidObj = details.get("product_id");
        Object discObj = details.get("discount");
        if (!(pidObj instanceof Number) || !(discObj instanceof Number)) {
            log.debug("ProductWiseStrategy.applyDiscount: invalid product_id or discount -> no-op");
            return cart;
        }
        Long targetProductId = ((Number) pidObj).longValue();
        double discountPercent = ((Number) discObj).doubleValue();

        double totalDiscount = 0;
        for (CartItem item : cart.getItems()) {
            if (item.getProductId().equals(targetProductId)) {
                double discount = (item.getPrice() * item.getQuantity()) * (discountPercent / 100.0);
                item.setTotalDiscount(discount);
                totalDiscount += discount;
            }
        }

        cart.setTotalDiscount(totalDiscount);
        cart.setFinalPrice(cart.getTotalPrice() - totalDiscount);
        log.debug("ProductWiseStrategy.applyDiscount: applied totalDiscount={}", totalDiscount);
        return cart;
    }

}