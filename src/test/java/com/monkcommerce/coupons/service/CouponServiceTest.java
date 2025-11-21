package com.monkcommerce.coupons.service;

import com.monkcommerce.coupons.model.Cart;
import com.monkcommerce.coupons.model.Coupon;
import com.monkcommerce.coupons.repository.CouponRepository;
import com.monkcommerce.coupons.strategy.CouponStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CouponServiceTest {
    private CouponRepository couponRepository;
    private CouponStrategy strategy;
    private CouponService service;

    @BeforeEach
    void setUp() {
        couponRepository = Mockito.mock(CouponRepository.class);
        strategy = Mockito.mock(CouponStrategy.class);
        // Ensure the mock reports its canonical type BEFORE constructing the service
        Mockito.when(strategy.getType()).thenReturn("bxgy");

        List<CouponStrategy> strategies = List.of(strategy);
        service = new CouponService(couponRepository, strategies);
    }

    @Test
    void testCreateCoupon() {
        Coupon coupon = new Coupon();
        Mockito.when(couponRepository.save(coupon)).thenReturn(coupon);
        assertEquals(coupon, service.createCoupon(coupon));
    }

    @Test
    void testGetCouponFound() {
        Coupon coupon = new Coupon();
        coupon.setId(1L);
        Mockito.when(couponRepository.findById(1L)).thenReturn(Optional.of(coupon));
        assertEquals(coupon, service.getCoupon(1L));
    }

    @Test
    void testGetCouponNotFound() {
        Mockito.when(couponRepository.findById(2L)).thenReturn(Optional.empty());
        assertThrows(Exception.class, () -> service.getCoupon(2L));
    }

    @Test
    void testGetAllCoupons() {
        List<Coupon> coupons = List.of(new Coupon(), new Coupon());
        Mockito.when(couponRepository.findAll()).thenReturn(coupons);
        assertEquals(coupons, service.getAllCoupons());
    }

    @Test
    void testGetApplicableCoupons() {
        Cart cart = new Cart();
        cart.setItems(new ArrayList<>());
        Coupon coupon = new Coupon();
        coupon.setId(1L);
        coupon.setType("bxgy");
        coupon.setDetails(new HashMap<>());
        Mockito.when(couponRepository.findAll()).thenReturn(List.of(coupon));
        Mockito.when(strategy.getType()).thenReturn("bxgy");
        Mockito.when(strategy.isApplicable(cart, coupon.getDetails())).thenReturn(true);
        Mockito.when(strategy.applyDiscount(Mockito.any(), Mockito.any())).thenAnswer(invocation -> {
            Cart c = invocation.getArgument(0);
            c.setTotalDiscount(10.0);
            return c;
        });
        Map<String, Object> result = service.getApplicableCoupons(cart);
        assertTrue(result.containsKey("applicable_coupons"));
    }

    @Test
    void testApplyCouponValid() {
        Cart cart = new Cart();
        Coupon coupon = new Coupon();
        coupon.setId(1L);
        coupon.setType("bxgy");
        coupon.setDetails(new HashMap<>());
        Mockito.when(couponRepository.findById(1L)).thenReturn(Optional.of(coupon));
        Mockito.when(strategy.getType()).thenReturn("bxgy");
        Mockito.when(strategy.isApplicable(cart, coupon.getDetails())).thenReturn(true);
        Mockito.when(strategy.applyDiscount(cart, coupon.getDetails())).thenReturn(cart);
        assertEquals(cart, service.applyCoupon(1L, cart));
    }

    @Test
    void testApplyCouponInvalidType() {
        Cart cart = new Cart();
        Coupon coupon = new Coupon();
        coupon.setId(2L);
        coupon.setType("invalid");
        coupon.setDetails(new HashMap<>());
        Mockito.when(couponRepository.findById(2L)).thenReturn(Optional.of(coupon));
        assertThrows(com.monkcommerce.coupons.exception.InvalidCouponTypeException.class, () -> service.applyCoupon(2L, cart));
    }

    @Test
    void testApplyCouponCriteriaNotMet() {
        Cart cart = new Cart();
        Coupon coupon = new Coupon();
        coupon.setId(3L);
        coupon.setType("bxgy");
        coupon.setDetails(new HashMap<>());
        Mockito.when(couponRepository.findById(3L)).thenReturn(Optional.of(coupon));
        Mockito.when(strategy.getType()).thenReturn("bxgy");
        Mockito.when(strategy.isApplicable(cart, coupon.getDetails())).thenReturn(false);
        assertThrows(com.monkcommerce.coupons.exception.CouponCriteriaNotMetException.class, () -> service.applyCoupon(3L, cart));
    }

    @Test
    void testApplyCouponExpired() {
        Cart cart = new Cart();
        Coupon coupon = new Coupon();
        coupon.setId(4L);
        coupon.setType("bxgy");
        coupon.setDetails(new HashMap<>());
        coupon.setExpirationDate(LocalDateTime.now().minusDays(1));
        Mockito.when(couponRepository.findById(4L)).thenReturn(Optional.of(coupon));
        assertThrows(com.monkcommerce.coupons.exception.CouponExpiredException.class, () -> service.applyCoupon(4L, cart));
    }
}
