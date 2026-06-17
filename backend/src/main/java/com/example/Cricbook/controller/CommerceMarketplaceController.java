package com.example.Cricbook.controller;

import com.example.Cricbook.dto.CommerceDtos;
import com.example.Cricbook.enums.CommerceProductType;
import com.example.Cricbook.service.CommerceMarketplaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/commerce")
@RequiredArgsConstructor
public class CommerceMarketplaceController {
    private final CommerceMarketplaceService commerceService;

    @GetMapping("/products")
    public ResponseEntity<List<CommerceDtos.ProductResponse>> products(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String sport,
            @RequestParam(required = false) CommerceProductType productType,
            @RequestParam(required = false) BigDecimal maxPrice) {
        return ResponseEntity.ok(commerceService.searchProducts(query, category, sport, productType, maxPrice));
    }

    @GetMapping("/products/{productId}")
    public ResponseEntity<CommerceDtos.ProductResponse> product(@PathVariable Long productId) {
        return ResponseEntity.ok(commerceService.getProduct(productId));
    }

    @PostMapping("/sellers")
    public ResponseEntity<CommerceDtos.SellerResponse> registerSeller(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody CommerceDtos.SellerRequest request) {
        return ResponseEntity.ok(commerceService.registerSeller(userDetails.getUsername(), request));
    }

    @PostMapping("/sellers/{sellerId}/verify")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CommerceDtos.SellerResponse> updateSellerStatus(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long sellerId,
            @RequestBody(required = false) CommerceDtos.SellerStatusRequest request) {
        return ResponseEntity.ok(commerceService.updateSellerStatus(userDetails.getUsername(), sellerId, request));
    }

    @PostMapping("/products")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<CommerceDtos.ProductResponse> createProduct(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody CommerceDtos.ProductRequest request) {
        return ResponseEntity.ok(commerceService.createProduct(userDetails.getUsername(), request));
    }

    @PostMapping("/products/{productId}/inventory")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<CommerceDtos.InventoryResponse> addInventory(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long productId,
            @RequestBody CommerceDtos.InventoryRequest request) {
        return ResponseEntity.ok(commerceService.addInventory(userDetails.getUsername(), productId, request));
    }

    @GetMapping("/cart")
    public ResponseEntity<CommerceDtos.CartResponse> cart(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(commerceService.getCart(userDetails.getUsername()));
    }

    @PostMapping("/cart")
    public ResponseEntity<CommerceDtos.CartResponse> addToCart(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody CommerceDtos.CartRequest request) {
        return ResponseEntity.ok(commerceService.addToCart(userDetails.getUsername(), request));
    }

    @DeleteMapping("/cart/{itemId}")
    public ResponseEntity<CommerceDtos.CartResponse> removeCartItem(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long itemId) {
        return ResponseEntity.ok(commerceService.removeCartItem(userDetails.getUsername(), itemId));
    }

    @PostMapping("/checkout")
    public ResponseEntity<CommerceDtos.OrderResponse> checkout(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody(required = false) CommerceDtos.CheckoutRequest request) {
        return ResponseEntity.ok(commerceService.checkout(userDetails.getUsername(), request));
    }

    @GetMapping("/orders")
    public ResponseEntity<List<CommerceDtos.OrderResponse>> myOrders(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(commerceService.myOrders(userDetails.getUsername()));
    }

    @PostMapping("/orders/{orderId}/returns")
    public ResponseEntity<CommerceDtos.OrderResponse> requestReturn(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long orderId,
            @RequestBody(required = false) CommerceDtos.ReturnRequest request) {
        return ResponseEntity.ok(commerceService.requestReturn(userDetails.getUsername(), orderId, request));
    }

    @PostMapping("/orders/{orderId}/status")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<CommerceDtos.OrderResponse> updateOrderStatus(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long orderId,
            @RequestBody CommerceDtos.OrderStatusRequest request) {
        return ResponseEntity.ok(commerceService.updateOrderStatus(userDetails.getUsername(), orderId, request));
    }

    @PostMapping("/orders/{orderId}/refund")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CommerceDtos.OrderResponse> refundOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long orderId,
            @RequestBody(required = false) CommerceDtos.OrderStatusRequest request) {
        return ResponseEntity.ok(commerceService.refundOrder(userDetails.getUsername(), orderId, request));
    }

    @GetMapping("/admin/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CommerceDtos.DashboardResponse> dashboard() {
        return ResponseEntity.ok(commerceService.dashboard());
    }
}
