package com.example.Cricbook.service;

import com.example.Cricbook.dto.CouponResponse;
import com.example.Cricbook.dto.CouponValidateRequest;
import com.example.Cricbook.entity.Coupon;
import com.example.Cricbook.entity.User;
import com.example.Cricbook.repository.CouponRepository;
import com.example.Cricbook.repository.CouponUsageRepository;
import com.example.Cricbook.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CouponServiceTest {

    @Mock
    private CouponRepository couponRepository;
    @Mock
    private CouponUsageRepository couponUsageRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CouponService couponService;

    @Test
    void validateCouponRejectsBlankCode() {
        CouponValidateRequest request = new CouponValidateRequest();
        request.setCode(" ");
        request.setBookingAmount(new BigDecimal("1000"));

        Exception exception = assertThrows(RuntimeException.class,
                () -> couponService.validateCoupon("test@test.com", request));

        assertEquals("Coupon code is required.", exception.getMessage());
    }

    @Test
    void validateCouponCalculatesPercentageDiscountWithCap() {
        User user = User.builder().id(1L).email("test@test.com").build();
        Coupon coupon = Coupon.builder()
                .id(10L)
                .code("CRICKET20")
                .discountType("PERCENT")
                .discountValue(new BigDecimal("20"))
                .maxDiscount(new BigDecimal("150"))
                .active(true)
                .validFrom(LocalDate.now().minusDays(1))
                .validUntil(LocalDate.now().plusDays(1))
                .build();
        CouponValidateRequest request = new CouponValidateRequest();
        request.setCode("cricket20");
        request.setBookingAmount(new BigDecimal("1000"));

        when(userRepository.findByEmailOrPhone("test@test.com")).thenReturn(Optional.of(user));
        when(couponRepository.findByCodeIgnoreCase("CRICKET20")).thenReturn(Optional.of(coupon));
        when(couponUsageRepository.countByUserIdAndCouponId(1L, 10L)).thenReturn(0L);

        CouponResponse response = couponService.validateCoupon("test@test.com", request);

        assertTrue(response.getValid());
        assertEquals(new BigDecimal("150"), response.getDiscountAmount());
        assertEquals(new BigDecimal("850"), response.getPayableAmount());
    }

    @Test
    void validateCouponReturnsInvalidForAlreadyUsed() {
        User user = User.builder().id(1L).email("test@test.com").build();
        Coupon coupon = Coupon.builder()
                .id(10L)
                .code("WELCOME100")
                .discountType("FLAT")
                .discountValue(new BigDecimal("100"))
                .active(true)
                .build();
        CouponValidateRequest request = new CouponValidateRequest();
        request.setCode("WELCOME100");
        request.setBookingAmount(new BigDecimal("1000"));

        when(userRepository.findByEmailOrPhone("test@test.com")).thenReturn(Optional.of(user));
        when(couponRepository.findByCodeIgnoreCase("WELCOME100")).thenReturn(Optional.of(coupon));
        when(couponUsageRepository.countByUserIdAndCouponId(1L, 10L)).thenReturn(1L);

        CouponResponse response = couponService.validateCoupon("test@test.com", request);

        assertFalse(response.getValid());
        assertEquals("Coupon already used", response.getMessage());
    }

    @Test
    void activeCouponsFiltersExpiredCoupons() {
        Coupon active = Coupon.builder()
                .code("ACTIVE")
                .discountType("FLAT")
                .discountValue(new BigDecimal("50"))
                .active(true)
                .validUntil(LocalDate.now().plusDays(1))
                .build();
        Coupon expired = Coupon.builder()
                .code("OLD")
                .discountType("FLAT")
                .discountValue(new BigDecimal("50"))
                .active(true)
                .validUntil(LocalDate.now().minusDays(1))
                .build();
        when(couponRepository.findAll()).thenReturn(List.of(active, expired));

        List<CouponResponse> response = couponService.activeCoupons();

        assertEquals(1, response.size());
        assertEquals("ACTIVE", response.get(0).getCode());
    }
}
