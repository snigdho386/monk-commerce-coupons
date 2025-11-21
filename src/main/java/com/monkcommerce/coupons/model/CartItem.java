package com.monkcommerce.coupons.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItem {

    private Long productId;
    private int quantity;
    private double price;

    private double totalDiscount = 0.0;

}
