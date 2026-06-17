package com.example.Cricbook.controller;

import com.example.Cricbook.dto.CouponResponse;
import com.example.Cricbook.dto.CouponValidateRequest;
import com.example.Cricbook.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @GetMapping
    public ResponseEntity<List<CouponResponse>> activeCoupons() {
        return ResponseEntity.ok(couponService.activeCoupons());
    }

    @PostMapping("/validate")
    public ResponseEntity<CouponResponse> validateCoupon(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody CouponValidateRequest request) {
        return ResponseEntity.ok(couponService.validateCoupon(userDetails.getUsername(), request));
    }
}
