package com.example.Cricbook.service;

import com.example.Cricbook.dto.CouponResponse;
import com.example.Cricbook.dto.CouponValidateRequest;
import com.example.Cricbook.entity.Coupon;
import com.example.Cricbook.entity.User;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.repository.CouponRepository;
import com.example.Cricbook.repository.CouponUsageRepository;
import com.example.Cricbook.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final CouponUsageRepository couponUsageRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<CouponResponse> activeCoupons() {
        LocalDate today = LocalDate.now();
        return couponRepository.findAll().stream()
                .filter(coupon -> Boolean.TRUE.equals(coupon.getActive()))
                .filter(coupon -> coupon.getValidFrom() == null || !today.isBefore(coupon.getValidFrom()))
                .filter(coupon -> coupon.getValidUntil() == null || !today.isAfter(coupon.getValidUntil()))
                .map(coupon -> CouponResponse.builder()
                        .code(coupon.getCode())
                        .discountType(coupon.getDiscountType())
                        .discountValue(coupon.getDiscountValue())
                        .validUntil(coupon.getValidUntil())
                        .valid(true)
                        .message("Available")
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public CouponResponse validateCoupon(String username, CouponValidateRequest request) {
        validateRequest(request);
        User user = userRepository.findByEmailOrPhone(username)
                .or(() -> userRepository.findByEmail(username))
                .orElseThrow(() -> new ApiException("User not found"));

        String normalizedCode = request.getCode().trim().toUpperCase();
        Coupon coupon = couponRepository.findByCodeIgnoreCase(normalizedCode)
                .orElse(null);
        if (coupon == null) {
            return invalid(normalizedCode, "Coupon not found");
        }

        LocalDate today = LocalDate.now();
        if (!Boolean.TRUE.equals(coupon.getActive())) {
            return invalid(coupon.getCode(), "Coupon is inactive");
        }
        if (coupon.getValidFrom() != null && today.isBefore(coupon.getValidFrom())) {
            return invalid(coupon.getCode(), "Coupon is not active yet");
        }
        if (coupon.getValidUntil() != null && today.isAfter(coupon.getValidUntil())) {
            return invalid(coupon.getCode(), "Coupon has expired");
        }
        if (coupon.getMinBookingAmount() != null && request.getBookingAmount().compareTo(coupon.getMinBookingAmount()) < 0) {
            return invalid(coupon.getCode(), "Minimum booking amount not met");
        }
        if (couponUsageRepository.countByUserIdAndCouponId(user.getId(), coupon.getId()) > 0) {
            return invalid(coupon.getCode(), "Coupon already used");
        }

        BigDecimal discount = calculateDiscount(coupon, request.getBookingAmount());
        BigDecimal payable = request.getBookingAmount().subtract(discount).max(BigDecimal.ZERO);

        return CouponResponse.builder()
                .code(coupon.getCode())
                .discountType(coupon.getDiscountType())
                .discountValue(coupon.getDiscountValue())
                .discountAmount(discount)
                .payableAmount(payable)
                .validUntil(coupon.getValidUntil())
                .valid(true)
                .message("Coupon applied")
                .build();
    }

    private void validateRequest(CouponValidateRequest request) {
        if (request == null) {
            throw ApiException.badRequest("Coupon details are required.");
        }
        if (request.getCode() == null || request.getCode().trim().isEmpty()) {
            throw ApiException.badRequest("Coupon code is required.");
        }
        if (request.getBookingAmount() == null) {
            throw ApiException.badRequest("Booking amount is required.");
        }
        if (request.getBookingAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw ApiException.badRequest("Booking amount must be greater than zero.");
        }
    }

    private CouponResponse invalid(String code, String message) {
        return CouponResponse.builder().code(code).valid(false).message(message).build();
    }

    private BigDecimal calculateDiscount(Coupon coupon, BigDecimal amount) {
        BigDecimal discount = "PERCENT".equalsIgnoreCase(coupon.getDiscountType())
                ? amount.multiply(coupon.getDiscountValue()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP)
                : coupon.getDiscountValue();

        if (coupon.getMaxDiscount() != null) {
            discount = discount.min(coupon.getMaxDiscount());
        }
        return discount.min(amount);
    }
}
