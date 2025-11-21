package com.monkcommerce.coupons.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class Cart {

    private double totalPrice;
    private double totalDiscount;
    private double finalPrice;

    private List<CartItem> items;

    /**
     * Return a deep copy of this Cart suitable for dry-run calculations.
     * Items are copied but their discounts are reset to 0.0 to avoid
     * carrying over computed values from the source cart.
     */
    public Cart copy() {
        Cart copy = new Cart();
        copy.setTotalPrice(this.totalPrice);
        copy.setTotalDiscount(this.totalDiscount);
        copy.setFinalPrice(this.finalPrice);

        if (this.items != null) {
            List<CartItem> newItems = new ArrayList<>();
            for (CartItem it : this.items) {
                newItems.add(new CartItem(it.getProductId(), it.getQuantity(), it.getPrice(), 0.0));
            }
            copy.setItems(newItems);
        }

        return copy;
    }

}
