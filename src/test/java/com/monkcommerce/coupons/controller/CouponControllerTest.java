package com.monkcommerce.coupons.controller;

import com.monkcommerce.coupons.model.Cart;
import com.monkcommerce.coupons.model.Coupon;
import com.monkcommerce.coupons.service.CouponService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CouponControllerTest {
    private CouponService couponService;
    private CouponController controller;

    @BeforeEach
    void setUp() {
        couponService = Mockito.mock(CouponService.class);
        controller = new CouponController(couponService);
    }

    @Test
    void testHome() {
        ResponseEntity<String> response = controller.home();
        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody().contains("app is working"));
    }

    @Test
    void testCreateCoupon() {
        Coupon coupon = new Coupon();
        Mockito.when(couponService.createCoupon(coupon)).thenReturn(coupon);
        assertEquals(coupon, controller.createCoupon(coupon));
    }

    @Test
    void testGetAllCoupons() {
        List<Coupon> coupons = List.of(new Coupon(), new Coupon());
        Mockito.when(couponService.getAllCoupons()).thenReturn(coupons);
        assertEquals(coupons, controller.getAllCoupons());
    }

    @Test
    void testGetCoupon() {
        Coupon coupon = new Coupon();
        coupon.setId(1L);
        Mockito.when(couponService.getCoupon(1L)).thenReturn(coupon);
        assertEquals(coupon, controller.getCoupon(1L));
    }

    @Test
    void testGetApplicableCoupons() {
        Cart cart = new Cart();
        Map<String, Object> result = Map.of("applicable_coupons", List.of());
        Mockito.when(couponService.getApplicableCoupons(cart)).thenReturn(result);
        assertEquals(result, controller.getApplicableCoupons(cart));
    }

    @Test
    void testApplyCoupon() {
        Cart cart = new Cart();
        Cart updatedCart = new Cart();
        Map<String, Object> result = Map.of("updated_cart", updatedCart);
        Mockito.when(couponService.applyCoupon(1L, cart)).thenReturn(updatedCart);
        assertEquals(result, controller.applyCoupon(1L, cart));
    }
}
