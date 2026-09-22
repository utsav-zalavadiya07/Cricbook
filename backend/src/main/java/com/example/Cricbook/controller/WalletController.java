package com.example.Cricbook.controller;

import com.example.Cricbook.dto.AddMoneyRequest;
import com.example.Cricbook.dto.WalletResponse;
import com.example.Cricbook.dto.WalletTopupOrderRequest;
import com.example.Cricbook.dto.WalletTopupOrderResponse;
import com.example.Cricbook.dto.WalletTopupVerifyRequest;
import com.example.Cricbook.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @GetMapping
    public ResponseEntity<WalletResponse> getWallet(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(walletService.getWallet(userDetails.getUsername()));
    }

    @PostMapping("/create-order")
    public ResponseEntity<WalletTopupOrderResponse> createTopupOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody WalletTopupOrderRequest request) {
        return ResponseEntity.ok(walletService.createTopupOrder(userDetails.getUsername(), request));
    }

    @PostMapping("/verify-payment")
    public ResponseEntity<WalletResponse> verifyTopupPayment(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody WalletTopupVerifyRequest request) {
        return ResponseEntity.ok(walletService.verifyTopupPayment(userDetails.getUsername(), request));
    }

    @PostMapping("/cancel-order")
    public ResponseEntity<WalletResponse> cancelTopupOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody com.example.Cricbook.dto.WalletTopupCancelRequest request) {
        return ResponseEntity.ok(walletService.cancelTopupOrder(userDetails.getUsername(), request.getOrderId()));
    }

    /**

     * Restricted direct manual adjustment: only administrators can invoke direct add-money.
     * Regular players must use /create-order and /verify-payment through the payment gateway.
     */
    @PostMapping("/add-money")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WalletResponse> addMoney(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody AddMoneyRequest request) {
        return ResponseEntity.ok(walletService.addMoney(userDetails.getUsername(), request));
    }
}
