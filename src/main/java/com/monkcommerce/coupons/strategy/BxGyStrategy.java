package com.monkcommerce.coupons.strategy;

import com.monkcommerce.coupons.model.Cart;
import com.monkcommerce.coupons.model.CartItem;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class BxGyStrategy implements CouponStrategy {

    private static final Logger log = LoggerFactory.getLogger(BxGyStrategy.class);

    @Override
    public String getType() {
        return "bxgy";
    }

    @Override
    public boolean isApplicable(Cart cart, Map<String, Object> details) {
        // Null-safe: guard against missing cart or details to avoid NPEs.
        if (cart == null || details == null) {
            log.debug("BxGyStrategy: cart or details is null -> not applicable");
            return false;
        }
        List<Map<String, Object>> buyProducts = (List<Map<String, Object>>) details.get("buy_products");
        List<Map<String, Object>> getProducts = (List<Map<String, Object>>) details.get("get_products");

        // Null-safe: require both buy and get product lists and cart items
        if (buyProducts == null || buyProducts.isEmpty() || getProducts == null || getProducts.isEmpty()) {
            log.debug("BxGyStrategy: buy/get product lists missing or empty");
            return false;
        }
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            log.debug("BxGyStrategy: cart has no items");
            return false;
        }

        // Logic: Must have at least one item from "Buy" list and one from "Get" list
        boolean hasBuyItem = cart.getItems().stream().anyMatch(item ->
            buyProducts.stream().anyMatch(b ->
                java.util.Objects.equals(((Number) b.get("product_id")).longValue(), item.getProductId())));

        boolean hasGetItem = cart.getItems().stream().anyMatch(item ->
            getProducts.stream().anyMatch(g ->
                java.util.Objects.equals(((Number) g.get("product_id")).longValue(), item.getProductId())));

        boolean applicable = hasBuyItem && hasGetItem;
        log.debug("BxGyStrategy: hasBuyItem={}, hasGetItem={}, applicable={}", hasBuyItem, hasGetItem, applicable);
        return applicable;
    }

    @Override
    public Cart applyDiscount(Cart cart, Map<String, Object> details) {
        // Null-safe: if inputs are missing, return the cart unchanged and log.
        if (cart == null || details == null) {
            log.debug("BxGyStrategy.applyDiscount: cart or details is null -> no-op");
            return cart;
        }
        List<Map<String, Object>> buyProducts = (List<Map<String, Object>>) details.get("buy_products");
        List<Map<String, Object>> getProducts = (List<Map<String, Object>>) details.get("get_products");

        if (buyProducts == null || buyProducts.isEmpty() || getProducts == null || getProducts.isEmpty() || cart.getItems() == null || cart.getItems().isEmpty()) {
            log.debug("BxGyStrategy.applyDiscount: configuration or cart items missing -> no-op");
            return cart;
        }

        int limit = 0;
        Object rep = details.get("repetition_limit");
        if (rep instanceof Number) limit = ((Number) rep).intValue();

        int repeatSets = 0;

        for (Map<String, Object> buyConfig : buyProducts) {

            int buyCountInCart = 0;

            for (CartItem item : cart.getItems()) {
                Object pid = buyConfig.get("product_id");
                Object qtyObj = buyConfig.get("quantity");
                if (pid != null && qtyObj instanceof Number && item.getProductId().equals(((Number) pid).longValue())) {
                    buyCountInCart += item.getQuantity();
                }
            }

            Object qtyObj = buyConfig.get("quantity");
            int reqQty = (qtyObj instanceof Number) ? ((Number) qtyObj).intValue() : 1;
            repeatSets += buyCountInCart / reqQty;
        }

        // Calculating repetition and early exit when nothing to apply
        if (repeatSets > limit) repeatSets = limit;
        if (repeatSets == 0) {
            log.debug("BxGyStrategy.applyDiscount: repeatSets=0 -> nothing to apply");
            return cart;
        }

        // Applying Free Items
        int getQtyReward = 0;
        for (Map<String, Object> getConfig : getProducts) {
            Object q = getConfig.get("quantity");
            if (q instanceof Number) getQtyReward += ((Number) q).intValue();
        }

        int totalFreeItems = repeatSets * getQtyReward;
        double totalDiscount = 0;

        // Assumption: which ever items are applicable are proccessed in FCFS fashion
        for (CartItem item : cart.getItems()) {
            if (totalFreeItems <= 0) break;
            // Checking if this item is in the "Get" list
            boolean isGetItem = getProducts.stream().anyMatch(g -> {
                Object pid = g.get("product_id");
                return pid != null && java.util.Objects.equals(((Number) pid).longValue(), item.getProductId());
            });

            if (isGetItem) {
                int quantityToDiscount = Math.min(item.getQuantity(), totalFreeItems);
                double discount = quantityToDiscount * item.getPrice();
                item.setTotalDiscount(item.getTotalDiscount() + discount);
                totalDiscount += discount;
                totalFreeItems -= quantityToDiscount;
            }
        }

        cart.setTotalDiscount(totalDiscount);
        cart.setFinalPrice(cart.getTotalPrice() - totalDiscount);
        log.debug("BxGyStrategy.applyDiscount: applied totalDiscount={}", totalDiscount);
        return cart;
    }

}
