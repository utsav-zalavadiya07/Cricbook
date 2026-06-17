package com.example.Cricbook.controller;

import com.example.Cricbook.dto.AddMoneyRequest;
import com.example.Cricbook.dto.WalletResponse;
import com.example.Cricbook.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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

    @PostMapping("/add-money")
    public ResponseEntity<WalletResponse> addMoney(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody AddMoneyRequest request) {
        return ResponseEntity.ok(walletService.addMoney(userDetails.getUsername(), request));
    }
}
