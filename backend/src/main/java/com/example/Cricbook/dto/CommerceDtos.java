package com.example.Cricbook.dto;

import com.example.Cricbook.enums.CommerceOrderStatus;
import com.example.Cricbook.enums.CommerceProductStatus;
import com.example.Cricbook.enums.CommerceProductType;
import com.example.Cricbook.enums.CommerceSellerStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class CommerceDtos {

    private CommerceDtos() {
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SellerRequest {
        private String businessName;
        private String contactName;
        private String email;
        private String phone;
        private String gstNumber;
        private String city;
        private String documentUrl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SellerStatusRequest {
        private CommerceSellerStatus status;
        private String notes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductRequest {
        private Long sellerId;
        private String name;
        private String brand;
        private String sku;
        private String category;
        private String sport;
        private String description;
        private BigDecimal price;
        private BigDecimal mrp;
        private CommerceProductType productType;
        private CommerceProductStatus status;
        private String imageUrl;
        private String affiliateUrl;
        private BigDecimal rentalFeePerDay;
        private BigDecimal depositAmount;
        private Double rating;
        private Boolean featured;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InventoryRequest {
        private String variantLabel;
        private String sku;
        private Integer stockOnHand;
        private Integer reservedStock;
        private Integer lowStockThreshold;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartRequest {
        private Long productId;
        private Long inventoryId;
        private Integer quantity;
        private LocalDate rentalStart;
        private LocalDate rentalEnd;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CheckoutRequest {
        private String shippingAddress;
        private String couponCode;
        private Integer loyaltyPoints;
        private String paymentReference;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderStatusRequest {
        private CommerceOrderStatus status;
        private String shippingProvider;
        private String trackingNumber;
        private String note;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReturnRequest {
        private String reason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SellerResponse {
        private Long id;
        private String businessName;
        private String contactName;
        private String email;
        private String phone;
        private String gstNumber;
        private String city;
        private CommerceSellerStatus status;
        private Integer trustScore;
        private String verificationNotes;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductResponse {
        private Long id;
        private Long sellerId;
        private String sellerName;
        private String name;
        private String brand;
        private String sku;
        private String category;
        private String sport;
        private String description;
        private BigDecimal price;
        private BigDecimal mrp;
        private CommerceProductType productType;
        private CommerceProductStatus status;
        private String imageUrl;
        private String affiliateUrl;
        private BigDecimal rentalFeePerDay;
        private BigDecimal depositAmount;
        private Double rating;
        private Boolean featured;
        private Integer availableStock;
        private Integer lowStockThreshold;
        private List<InventoryResponse> variants;
        private LocalDateTime updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InventoryResponse {
        private Long id;
        private String variantLabel;
        private String sku;
        private Integer stockOnHand;
        private Integer reservedStock;
        private Integer availableStock;
        private Integer lowStockThreshold;
        private Boolean lowStock;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartResponse {
        private List<CartItemResponse> items;
        private BigDecimal subtotal;
        private BigDecimal rentalDeposits;
        private Integer itemCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartItemResponse {
        private Long id;
        private ProductResponse product;
        private InventoryResponse inventory;
        private Integer quantity;
        private LocalDate rentalStart;
        private LocalDate rentalEnd;
        private BigDecimal lineTotal;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderResponse {
        private Long id;
        private String orderNumber;
        private CommerceOrderStatus status;
        private BigDecimal subtotal;
        private BigDecimal discount;
        private Integer loyaltyPointsRedeemed;
        private BigDecimal shippingFee;
        private BigDecimal total;
        private String paymentStatus;
        private String couponCode;
        private String shippingAddress;
        private String shippingProvider;
        private String trackingNumber;
        private List<OrderItemResponse> items;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemResponse {
        private Long id;
        private Long productId;
        private String productName;
        private String sku;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal lineTotal;
        private Boolean rentalItem;
        private LocalDate rentalStart;
        private LocalDate rentalEnd;
        private BigDecimal depositAmount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DashboardResponse {
        private long products;
        private long sellers;
        private long placedOrders;
        private long shippedOrders;
        private long activeRentals;
        private BigDecimal gmv;
        private List<ProductResponse> featuredProducts;
        private List<OrderResponse> recentOrders;
    }
}
