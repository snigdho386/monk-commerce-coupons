package com.monkcommerce.coupons.exception;

public class CouponExpiredException extends RuntimeException {
    public CouponExpiredException(String message) {
        super(message);
    }
}
