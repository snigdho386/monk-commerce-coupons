package com.monkcommerce.coupons.controller;

import com.monkcommerce.coupons.model.Cart;
import com.monkcommerce.coupons.model.Coupon;
import com.monkcommerce.coupons.service.CouponService;
import org.apache.coyote.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class CouponController {

    private final CouponService couponService;

    public CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    @GetMapping("/home")
    public ResponseEntity<String> home(){
        return ResponseEntity.ok().body("Hey there! The app is working 🚀");
    }

    @PostMapping("/coupons")
    public Coupon createCoupon(@RequestBody Coupon coupon) {
        return couponService.createCoupon(coupon);
    }

    @GetMapping("/coupons")
    public List<Coupon> getAllCoupons() {
        return couponService.getAllCoupons();
    }

    @GetMapping("/coupons/{id}")
    public Coupon getCoupon(@PathVariable Long id) {
        return couponService.getCoupon(id);
    }

    @PostMapping("/applicable-coupons")
    public Map<String, Object> getApplicableCoupons(@RequestBody Cart cart) {
        return couponService.getApplicableCoupons(cart);
    }

    @PostMapping("/apply-coupon/{id}")
    public Map<String, Object> applyCoupon(@PathVariable Long id, @RequestBody Cart cart) {
        Cart updatedCart = couponService.applyCoupon(id, cart);
        return Map.of("updated_cart", updatedCart);
    }
}