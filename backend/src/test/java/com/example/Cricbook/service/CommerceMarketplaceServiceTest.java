package com.example.Cricbook.service;

import com.example.Cricbook.dto.CommerceDtos;
import com.example.Cricbook.entity.*;
import com.example.Cricbook.enums.*;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommerceMarketplaceServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private CommerceSellerRepository sellerRepository;
    @Mock
    private CommerceProductRepository productRepository;
    @Mock
    private CommerceInventoryRepository inventoryRepository;
    @Mock
    private CommerceCartItemRepository cartRepository;
    @Mock
    private CommerceOrderRepository orderRepository;
    @Mock
    private CommerceOrderItemRepository orderItemRepository;
    @Mock
    private CommerceRentalRepository rentalRepository;
    @Mock
    private CommerceAuditRepository auditRepository;

    @InjectMocks
    private CommerceMarketplaceService commerceMarketplaceService;

    @Test
    void checkoutReservesInventoryAndBuildsOrder() {
        User user = User.builder()
                .id(7L)
                .name("Player")
                .email("player@example.com")
                .phone("9999999999")
                .status(UserStatus.ACTIVE)
                .build();
        CommerceSeller seller = CommerceSeller.builder()
                .id(3L)
                .businessName("Cricket Store")
                .contactName("Owner")
                .email("seller@example.com")
                .phone("8888888888")
                .status(CommerceSellerStatus.VERIFIED)
                .trustScore(95)
                .build();
        CommerceProduct product = CommerceProduct.builder()
                .id(11L)
                .seller(seller)
                .name("English Willow Bat")
                .sku("BAT-001")
                .category("Bat")
                .sport("Cricket")
                .price(BigDecimal.valueOf(1200))
                .productType(CommerceProductType.SALE)
                .status(CommerceProductStatus.PUBLISHED)
                .featured(true)
                .rating(4.5)
                .build();
        CommerceInventory inventory = CommerceInventory.builder()
                .id(21L)
                .product(product)
                .variantLabel("Short handle")
                .sku("BAT-001-SH")
                .stockOnHand(10)
                .reservedStock(1)
                .lowStockThreshold(2)
                .build();
        CommerceCartItem cartItem = CommerceCartItem.builder()
                .id(31L)
                .user(user)
                .product(product)
                .inventory(inventory)
                .quantity(2)
                .build();
        CommerceOrder savedOrder = CommerceOrder.builder()
                .id(41L)
                .user(user)
                .orderNumber("CB-COM-1")
                .status(CommerceOrderStatus.PLACED)
                .subtotal(BigDecimal.valueOf(2400).setScale(2))
                .discount(BigDecimal.valueOf(240).setScale(2))
                .loyaltyPointsRedeemed(0)
                .shippingFee(BigDecimal.valueOf(99))
                .total(BigDecimal.valueOf(2259).setScale(2))
                .paymentStatus("PAID")
                .couponCode("SPORTS10")
                .shippingAddress("Ahmedabad")
                .build();

        when(userRepository.findByEmailOrPhone("player@example.com")).thenReturn(Optional.of(user));
        when(cartRepository.findByUserIdOrderByCreatedAtDesc(user.getId())).thenReturn(List.of(cartItem));
        when(orderRepository.save(any(CommerceOrder.class))).thenReturn(savedOrder);
        when(inventoryRepository.save(any(CommerceInventory.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderItemRepository.save(any(CommerceOrderItem.class))).thenAnswer(invocation -> {
            CommerceOrderItem item = invocation.getArgument(0);
            item.setId(51L);
            return item;
        });
        when(auditRepository.save(any(CommerceAudit.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CommerceDtos.OrderResponse response = commerceMarketplaceService.checkout(
                "player@example.com",
                CommerceDtos.CheckoutRequest.builder()
                        .shippingAddress("Ahmedabad")
                        .couponCode("SPORTS10")
                        .paymentReference("TEST-PAYMENT")
                        .build());

        assertEquals("CB-COM-1", response.getOrderNumber());
        assertEquals(CommerceOrderStatus.PLACED, response.getStatus());
        assertEquals(3, inventory.getReservedStock());
        assertEquals(1, response.getItems().size());
        assertEquals(0, BigDecimal.valueOf(2259).setScale(2).compareTo(response.getTotal()));
        verify(cartRepository).deleteByUserId(user.getId());
        verify(auditRepository).save(any(CommerceAudit.class));
    }

    @Test
    void createProductRejectsDuplicateSku() {
        when(productRepository.existsBySkuIgnoreCase("BAT-001")).thenReturn(true);

        CommerceDtos.ProductRequest request = CommerceDtos.ProductRequest.builder()
                .sellerId(1L)
                .name("English Willow Bat")
                .sku("BAT-001")
                .category("Bat")
                .sport("Cricket")
                .price(BigDecimal.valueOf(1200))
                .productType(CommerceProductType.SALE)
                .build();

        ApiException exception = assertThrows(ApiException.class, () -> commerceMarketplaceService.createProduct("owner@example.com", request));

        assertEquals("A commerce product already exists with this SKU.", exception.getMessage());
        verify(sellerRepository, never()).findById(anyLong());
    }
}
