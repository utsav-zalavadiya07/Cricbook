package com.example.Cricbook.service;

import com.example.Cricbook.dto.CommerceDtos;
import com.example.Cricbook.entity.*;
import com.example.Cricbook.enums.*;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommerceMarketplaceService {
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final BigDecimal SHIPPING_FREE_LIMIT = BigDecimal.valueOf(2500);
    private static final BigDecimal STANDARD_SHIPPING_FEE = BigDecimal.valueOf(99);
    private static final int DEFAULT_LOW_STOCK_THRESHOLD = 3;

    private final UserRepository userRepository;
    private final CommerceSellerRepository sellerRepository;
    private final CommerceProductRepository productRepository;
    private final CommerceInventoryRepository inventoryRepository;
    private final CommerceCartItemRepository cartRepository;
    private final CommerceOrderRepository orderRepository;
    private final CommerceOrderItemRepository orderItemRepository;
    private final CommerceRentalRepository rentalRepository;
    private final CommerceAuditRepository auditRepository;

    @Transactional(readOnly = true)
    public List<CommerceDtos.ProductResponse> searchProducts(String query, String category, String sport, CommerceProductType productType, BigDecimal maxPrice) {
        return productRepository.searchPublished(
                        CommerceProductStatus.PUBLISHED,
                        clean(query),
                        clean(category),
                        clean(sport),
                        productType,
                        maxPrice)
                .stream()
                .map(this::mapProduct)
                .toList();
    }

    @Transactional(readOnly = true)
    public CommerceDtos.ProductResponse getProduct(Long productId) {
        CommerceProduct product = productRepository.findById(productId)
                .orElseThrow(() -> ApiException.notFound("Commerce product was not found."));
        if (product.getStatus() != CommerceProductStatus.PUBLISHED) {
            throw ApiException.notFound("Commerce product was not found.");
        }
        return mapProduct(product);
    }

    @Transactional
    public CommerceDtos.SellerResponse registerSeller(String loginId, CommerceDtos.SellerRequest request) {
        validateSellerRequest(request);
        if (sellerRepository.existsByEmailIgnoreCase(request.getEmail().trim())) {
            throw ApiException.conflict("A commerce seller already exists with this email.");
        }
        if (hasText(request.getGstNumber()) && sellerRepository.existsByGstNumberIgnoreCase(request.getGstNumber().trim())) {
            throw ApiException.conflict("A commerce seller already exists with this GST number.");
        }
        User owner = loginId == null ? null : findUser(loginId);
        CommerceSeller seller = sellerRepository.save(CommerceSeller.builder()
                .owner(owner)
                .businessName(request.getBusinessName().trim())
                .contactName(request.getContactName().trim())
                .email(request.getEmail().trim())
                .phone(request.getPhone().trim())
                .gstNumber(clean(request.getGstNumber()))
                .city(clean(request.getCity()))
                .documentUrl(clean(request.getDocumentUrl()))
                .status(CommerceSellerStatus.PENDING)
                .trustScore(75)
                .verificationNotes("Seller registration submitted for verification.")
                .build());
        audit(owner, "SELLER_REGISTERED", "SELLER", seller.getId(), seller.getBusinessName());
        return mapSeller(seller);
    }

    @Transactional
    public CommerceDtos.SellerResponse updateSellerStatus(String loginId, Long sellerId, CommerceDtos.SellerStatusRequest request) {
        CommerceSeller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> ApiException.notFound("Commerce seller was not found."));
        CommerceSellerStatus status = request == null || request.getStatus() == null ? CommerceSellerStatus.VERIFIED : request.getStatus();
        seller.setStatus(status);
        seller.setVerificationNotes(clean(request == null ? null : request.getNotes()));
        seller.setTrustScore(status == CommerceSellerStatus.VERIFIED ? Math.max(90, seller.getTrustScore()) : seller.getTrustScore());
        CommerceSeller saved = sellerRepository.save(seller);
        audit(findUser(loginId), "SELLER_STATUS_UPDATED", "SELLER", saved.getId(), status.name());
        return mapSeller(saved);
    }

    @Transactional
    public CommerceDtos.ProductResponse createProduct(String loginId, CommerceDtos.ProductRequest request) {
        validateProductRequest(request);
        if (productRepository.existsBySkuIgnoreCase(request.getSku().trim())) {
            throw ApiException.conflict("A commerce product already exists with this SKU.");
        }
        CommerceSeller seller = sellerRepository.findById(request.getSellerId())
                .orElseThrow(() -> ApiException.notFound("Commerce seller was not found."));
        if (seller.getStatus() != CommerceSellerStatus.VERIFIED) {
            throw ApiException.conflict("Only verified sellers can publish commerce products.");
        }
        CommerceProductType type = request.getProductType() == null ? CommerceProductType.SALE : request.getProductType();
        CommerceProductStatus status = request.getStatus() == null ? CommerceProductStatus.DRAFT : request.getStatus();
        CommerceProduct product = productRepository.save(CommerceProduct.builder()
                .seller(seller)
                .name(request.getName().trim())
                .brand(clean(request.getBrand()))
                .sku(request.getSku().trim())
                .category(request.getCategory().trim())
                .sport(request.getSport().trim())
                .description(clean(request.getDescription()))
                .price(money(request.getPrice()))
                .mrp(moneyOrNull(request.getMrp()))
                .productType(type)
                .status(status)
                .imageUrl(clean(request.getImageUrl()))
                .affiliateUrl(clean(request.getAffiliateUrl()))
                .rentalFeePerDay(moneyOrNull(request.getRentalFeePerDay()))
                .depositAmount(moneyOrZero(request.getDepositAmount()))
                .rating(request.getRating() == null ? 0.0 : Math.max(0.0, Math.min(5.0, request.getRating())))
                .featured(Boolean.TRUE.equals(request.getFeatured()))
                .build());
        audit(findUser(loginId), "PRODUCT_CREATED", "PRODUCT", product.getId(), product.getName());
        return mapProduct(product);
    }

    @Transactional
    public CommerceDtos.InventoryResponse addInventory(String loginId, Long productId, CommerceDtos.InventoryRequest request) {
        if (request == null || !hasText(request.getVariantLabel()) || !hasText(request.getSku())) {
            throw ApiException.badRequest("Variant label and SKU are required.");
        }
        if (inventoryRepository.existsBySkuIgnoreCase(request.getSku().trim())) {
            throw ApiException.conflict("A commerce inventory variant already exists with this SKU.");
        }
        CommerceProduct product = productRepository.findById(productId)
                .orElseThrow(() -> ApiException.notFound("Commerce product was not found."));
        int stock = request.getStockOnHand() == null ? 0 : Math.max(0, request.getStockOnHand());
        int reserved = request.getReservedStock() == null ? 0 : Math.max(0, request.getReservedStock());
        if (reserved > stock) {
            throw ApiException.badRequest("Reserved stock cannot exceed stock on hand.");
        }
        CommerceInventory inventory = inventoryRepository.save(CommerceInventory.builder()
                .product(product)
                .variantLabel(request.getVariantLabel().trim())
                .sku(request.getSku().trim())
                .stockOnHand(stock)
                .reservedStock(reserved)
                .lowStockThreshold(request.getLowStockThreshold() == null ? DEFAULT_LOW_STOCK_THRESHOLD : Math.max(0, request.getLowStockThreshold()))
                .build());
        audit(findUser(loginId), "INVENTORY_ADDED", "PRODUCT", product.getId(), inventory.getSku());
        return mapInventory(inventory);
    }

    @Transactional(readOnly = true)
    public CommerceDtos.CartResponse getCart(String loginId) {
        User user = findUser(loginId);
        return mapCart(cartRepository.findByUserIdOrderByCreatedAtDesc(user.getId()));
    }

    @Transactional
    public CommerceDtos.CartResponse addToCart(String loginId, CommerceDtos.CartRequest request) {
        if (request == null || request.getProductId() == null || request.getInventoryId() == null) {
            throw ApiException.badRequest("Product and inventory variant are required.");
        }
        int quantity = request.getQuantity() == null ? 1 : Math.max(1, request.getQuantity());
        User user = findUser(loginId);
        CommerceProduct product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> ApiException.notFound("Commerce product was not found."));
        if (product.getStatus() != CommerceProductStatus.PUBLISHED) {
            throw ApiException.conflict("Only published products can be added to cart.");
        }
        CommerceInventory inventory = inventoryRepository.findById(request.getInventoryId())
                .orElseThrow(() -> ApiException.notFound("Commerce inventory variant was not found."));
        if (!inventory.getProduct().getId().equals(product.getId())) {
            throw ApiException.badRequest("Inventory variant does not belong to the selected product.");
        }
        validateRentalWindow(product, request.getRentalStart(), request.getRentalEnd());
        if (availableStock(inventory) < quantity) {
            throw ApiException.conflict("Requested quantity is not available.");
        }
        if (product.getProductType() == CommerceProductType.RENTAL) {
            cartRepository.save(CommerceCartItem.builder()
                    .user(user)
                    .product(product)
                    .inventory(inventory)
                    .quantity(quantity)
                    .rentalStart(request.getRentalStart())
                    .rentalEnd(request.getRentalEnd())
                    .build());
        } else {
            CommerceCartItem item = cartRepository.findByUserIdAndInventoryId(user.getId(), inventory.getId())
                    .orElseGet(() -> CommerceCartItem.builder()
                            .user(user)
                            .product(product)
                            .inventory(inventory)
                            .quantity(0)
                            .build());
            int nextQuantity = item.getQuantity() + quantity;
            if (availableStock(inventory) < nextQuantity) {
                throw ApiException.conflict("Requested quantity is not available.");
            }
            item.setQuantity(nextQuantity);
            cartRepository.save(item);
        }
        audit(user, "CART_ITEM_ADDED", "PRODUCT", product.getId(), product.getSku());
        return mapCart(cartRepository.findByUserIdOrderByCreatedAtDesc(user.getId()));
    }

    @Transactional
    public CommerceDtos.CartResponse removeCartItem(String loginId, Long itemId) {
        User user = findUser(loginId);
        CommerceCartItem item = cartRepository.findById(itemId)
                .orElseThrow(() -> ApiException.notFound("Cart item was not found."));
        if (!item.getUser().getId().equals(user.getId())) {
            throw ApiException.forbidden("You can only remove your own cart items.");
        }
        cartRepository.delete(item);
        audit(user, "CART_ITEM_REMOVED", "CART", itemId, item.getProduct().getSku());
        return mapCart(cartRepository.findByUserIdOrderByCreatedAtDesc(user.getId()));
    }

    @Transactional
    public CommerceDtos.OrderResponse checkout(String loginId, CommerceDtos.CheckoutRequest request) {
        User user = findUser(loginId);
        List<CommerceCartItem> items = cartRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        if (items.isEmpty()) {
            throw ApiException.badRequest("Cart is empty.");
        }
        for (CommerceCartItem item : items) {
            validateRentalWindow(item.getProduct(), item.getRentalStart(), item.getRentalEnd());
            if (availableStock(item.getInventory()) < item.getQuantity()) {
                throw ApiException.conflict("Insufficient stock for " + item.getProduct().getName() + ".");
            }
        }

        BigDecimal subtotal = cartSubtotal(items);
        BigDecimal discount = discountFor(request, subtotal);
        int loyaltyPoints = request == null || request.getLoyaltyPoints() == null ? 0 : Math.max(0, request.getLoyaltyPoints());
        BigDecimal loyaltyDiscount = BigDecimal.valueOf(loyaltyPoints).divide(BigDecimal.valueOf(100), 2, RoundingMode.DOWN);
        BigDecimal maxDiscount = subtotal.min(discount.add(loyaltyDiscount));
        BigDecimal shippingFee = subtotal.compareTo(SHIPPING_FREE_LIMIT) >= 0 ? ZERO : STANDARD_SHIPPING_FEE;
        BigDecimal total = subtotal.subtract(maxDiscount).add(shippingFee).max(ZERO).setScale(2, RoundingMode.HALF_UP);

        CommerceOrder order = orderRepository.save(CommerceOrder.builder()
                .user(user)
                .orderNumber("CB-COM-" + System.currentTimeMillis() + "-" + user.getId())
                .status(CommerceOrderStatus.PLACED)
                .subtotal(subtotal)
                .discount(maxDiscount)
                .loyaltyPointsRedeemed(loyaltyPoints)
                .shippingFee(shippingFee)
                .total(total)
                .paymentStatus(hasText(request == null ? null : request.getPaymentReference()) ? "PAID" : "PENDING")
                .couponCode(clean(request == null ? null : request.getCouponCode()))
                .shippingAddress(clean(request == null ? null : request.getShippingAddress()))
                .auditNote(clean(request == null ? null : request.getPaymentReference()))
                .build());

        List<CommerceOrderItem> savedItems = new ArrayList<>();
        for (CommerceCartItem cartItem : items) {
            CommerceInventory inventory = cartItem.getInventory();
            inventory.setReservedStock(inventory.getReservedStock() + cartItem.getQuantity());
            inventoryRepository.save(inventory);
            CommerceOrderItem orderItem = orderItemRepository.save(CommerceOrderItem.builder()
                    .order(order)
                    .product(cartItem.getProduct())
                    .inventory(inventory)
                    .productName(cartItem.getProduct().getName())
                    .sku(inventory.getSku())
                    .quantity(cartItem.getQuantity())
                    .unitPrice(unitPrice(cartItem))
                    .lineTotal(lineTotal(cartItem))
                    .rentalItem(cartItem.getProduct().getProductType() == CommerceProductType.RENTAL)
                    .rentalStart(cartItem.getRentalStart())
                    .rentalEnd(cartItem.getRentalEnd())
                    .depositAmount(moneyOrZero(cartItem.getProduct().getDepositAmount()))
                    .build());
            savedItems.add(orderItem);
            if (Boolean.TRUE.equals(orderItem.getRentalItem())) {
                rentalRepository.save(CommerceRental.builder()
                        .orderItem(orderItem)
                        .product(cartItem.getProduct())
                        .user(user)
                        .rentalStart(cartItem.getRentalStart())
                        .rentalEnd(cartItem.getRentalEnd())
                        .depositAmount(moneyOrZero(cartItem.getProduct().getDepositAmount()))
                        .lateFee(ZERO)
                        .damageFee(ZERO)
                        .status(CommerceRentalStatus.RESERVED)
                        .build());
            }
        }
        cartRepository.deleteByUserId(user.getId());
        audit(user, "ORDER_PLACED", "ORDER", order.getId(), order.getOrderNumber());
        return mapOrder(order, savedItems);
    }

    @Transactional(readOnly = true)
    public List<CommerceDtos.OrderResponse> myOrders(String loginId) {
        User user = findUser(loginId);
        return orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::mapOrder)
                .toList();
    }

    @Transactional
    public CommerceDtos.OrderResponse updateOrderStatus(String loginId, Long orderId, CommerceDtos.OrderStatusRequest request) {
        if (request == null || request.getStatus() == null) {
            throw ApiException.badRequest("Order status is required.");
        }
        CommerceOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> ApiException.notFound("Commerce order was not found."));
        CommerceOrderStatus previous = order.getStatus();
        if (previous == request.getStatus()) {
            return mapOrder(order);
        }
        applyInventoryTransition(order, previous, request.getStatus());
        order.setStatus(request.getStatus());
        order.setShippingProvider(clean(request.getShippingProvider()));
        order.setTrackingNumber(clean(request.getTrackingNumber()));
        order.setAuditNote(clean(request.getNote()));
        CommerceOrder saved = orderRepository.save(order);
        audit(findUser(loginId), "ORDER_STATUS_UPDATED", "ORDER", order.getId(), previous + " -> " + request.getStatus());
        return mapOrder(saved);
    }

    @Transactional
    public CommerceDtos.OrderResponse requestReturn(String loginId, Long orderId, CommerceDtos.ReturnRequest request) {
        User user = findUser(loginId);
        CommerceOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> ApiException.notFound("Commerce order was not found."));
        if (!order.getUser().getId().equals(user.getId())) {
            throw ApiException.forbidden("You can only request returns for your own orders.");
        }
        if (order.getStatus() != CommerceOrderStatus.DELIVERED) {
            throw ApiException.conflict("Only delivered orders can enter return workflow.");
        }
        order.setStatus(CommerceOrderStatus.RETURN_REQUESTED);
        order.setAuditNote(clean(request == null ? null : request.getReason()));
        CommerceOrder saved = orderRepository.save(order);
        audit(user, "RETURN_REQUESTED", "ORDER", order.getId(), order.getAuditNote());
        return mapOrder(saved);
    }

    @Transactional
    public CommerceDtos.OrderResponse refundOrder(String loginId, Long orderId, CommerceDtos.OrderStatusRequest request) {
        CommerceOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> ApiException.notFound("Commerce order was not found."));
        applyInventoryTransition(order, order.getStatus(), CommerceOrderStatus.REFUNDED);
        order.setStatus(CommerceOrderStatus.REFUNDED);
        order.setPaymentStatus("REFUNDED");
        order.setAuditNote(clean(request == null ? null : request.getNote()));
        CommerceOrder saved = orderRepository.save(order);
        audit(findUser(loginId), "ORDER_REFUNDED", "ORDER", order.getId(), order.getOrderNumber());
        return mapOrder(saved);
    }

    @Transactional(readOnly = true)
    public CommerceDtos.DashboardResponse dashboard() {
        return CommerceDtos.DashboardResponse.builder()
                .products(productRepository.count())
                .sellers(sellerRepository.count())
                .placedOrders(orderRepository.countByStatus(CommerceOrderStatus.PLACED))
                .shippedOrders(orderRepository.countByStatus(CommerceOrderStatus.SHIPPED))
                .activeRentals(rentalRepository.countByStatus(CommerceRentalStatus.ACTIVE) + rentalRepository.countByStatus(CommerceRentalStatus.RESERVED))
                .gmv(orderRepository.totalGmvExcluding(CommerceOrderStatus.CANCELLED))
                .featuredProducts(productRepository.findTop8ByStatusOrderByFeaturedDescUpdatedAtDesc(CommerceProductStatus.PUBLISHED).stream()
                        .map(this::mapProduct)
                        .toList())
                .recentOrders(orderRepository.findTop20ByOrderByCreatedAtDesc().stream()
                        .map(this::mapOrder)
                        .toList())
                .build();
    }

    private void applyInventoryTransition(CommerceOrder order, CommerceOrderStatus previous, CommerceOrderStatus next) {
        List<CommerceOrderItem> items = orderItemRepository.findByOrderIdOrderByIdAsc(order.getId());
        if (next == CommerceOrderStatus.CANCELLED || next == CommerceOrderStatus.REFUNDED) {
            if (previous != CommerceOrderStatus.CANCELLED && previous != CommerceOrderStatus.REFUNDED && previous != CommerceOrderStatus.DELIVERED) {
                releaseReserved(items);
            }
        }
        if (next == CommerceOrderStatus.DELIVERED && previous != CommerceOrderStatus.DELIVERED) {
            fulfillReserved(items);
        }
    }

    private void releaseReserved(List<CommerceOrderItem> items) {
        for (CommerceOrderItem item : items) {
            CommerceInventory inventory = item.getInventory();
            inventory.setReservedStock(Math.max(0, inventory.getReservedStock() - item.getQuantity()));
            inventoryRepository.save(inventory);
        }
    }

    private void fulfillReserved(List<CommerceOrderItem> items) {
        for (CommerceOrderItem item : items) {
            CommerceInventory inventory = item.getInventory();
            inventory.setReservedStock(Math.max(0, inventory.getReservedStock() - item.getQuantity()));
            inventory.setStockOnHand(Math.max(0, inventory.getStockOnHand() - item.getQuantity()));
            inventoryRepository.save(inventory);
        }
    }

    private CommerceDtos.CartResponse mapCart(List<CommerceCartItem> items) {
        List<CommerceDtos.CartItemResponse> responses = items.stream()
                .map(item -> CommerceDtos.CartItemResponse.builder()
                        .id(item.getId())
                        .product(mapProduct(item.getProduct()))
                        .inventory(mapInventory(item.getInventory()))
                        .quantity(item.getQuantity())
                        .rentalStart(item.getRentalStart())
                        .rentalEnd(item.getRentalEnd())
                        .lineTotal(lineTotal(item))
                        .build())
                .toList();
        BigDecimal subtotal = cartSubtotal(items);
        BigDecimal rentalDeposits = items.stream()
                .filter(item -> item.getProduct().getProductType() == CommerceProductType.RENTAL)
                .map(item -> moneyOrZero(item.getProduct().getDepositAmount()).multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        int itemCount = items.stream().mapToInt(CommerceCartItem::getQuantity).sum();
        return CommerceDtos.CartResponse.builder()
                .items(responses)
                .subtotal(subtotal)
                .rentalDeposits(rentalDeposits)
                .itemCount(itemCount)
                .build();
    }

    private CommerceDtos.OrderResponse mapOrder(CommerceOrder order) {
        return mapOrder(order, orderItemRepository.findByOrderIdOrderByIdAsc(order.getId()));
    }

    private CommerceDtos.OrderResponse mapOrder(CommerceOrder order, List<CommerceOrderItem> items) {
        return CommerceDtos.OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus())
                .subtotal(order.getSubtotal())
                .discount(order.getDiscount())
                .loyaltyPointsRedeemed(order.getLoyaltyPointsRedeemed())
                .shippingFee(order.getShippingFee())
                .total(order.getTotal())
                .paymentStatus(order.getPaymentStatus())
                .couponCode(order.getCouponCode())
                .shippingAddress(order.getShippingAddress())
                .shippingProvider(order.getShippingProvider())
                .trackingNumber(order.getTrackingNumber())
                .items(items.stream().map(this::mapOrderItem).toList())
                .createdAt(order.getCreatedAt())
                .build();
    }

    private CommerceDtos.OrderItemResponse mapOrderItem(CommerceOrderItem item) {
        return CommerceDtos.OrderItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productName(item.getProductName())
                .sku(item.getSku())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .lineTotal(item.getLineTotal())
                .rentalItem(item.getRentalItem())
                .rentalStart(item.getRentalStart())
                .rentalEnd(item.getRentalEnd())
                .depositAmount(item.getDepositAmount())
                .build();
    }

    private CommerceDtos.ProductResponse mapProduct(CommerceProduct product) {
        List<CommerceInventory> variants = inventoryRepository.findByProductIdOrderByVariantLabelAsc(product.getId());
        int availableStock = variants.stream().mapToInt(this::availableStock).sum();
        int lowStockThreshold = variants.stream()
                .mapToInt(CommerceInventory::getLowStockThreshold)
                .min()
                .orElse(DEFAULT_LOW_STOCK_THRESHOLD);
        return CommerceDtos.ProductResponse.builder()
                .id(product.getId())
                .sellerId(product.getSeller().getId())
                .sellerName(product.getSeller().getBusinessName())
                .name(product.getName())
                .brand(product.getBrand())
                .sku(product.getSku())
                .category(product.getCategory())
                .sport(product.getSport())
                .description(product.getDescription())
                .price(product.getPrice())
                .mrp(product.getMrp())
                .productType(product.getProductType())
                .status(product.getStatus())
                .imageUrl(product.getImageUrl())
                .affiliateUrl(product.getAffiliateUrl())
                .rentalFeePerDay(product.getRentalFeePerDay())
                .depositAmount(product.getDepositAmount())
                .rating(product.getRating())
                .featured(product.getFeatured())
                .availableStock(availableStock)
                .lowStockThreshold(lowStockThreshold)
                .variants(variants.stream().map(this::mapInventory).toList())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    private CommerceDtos.InventoryResponse mapInventory(CommerceInventory inventory) {
        int available = availableStock(inventory);
        return CommerceDtos.InventoryResponse.builder()
                .id(inventory.getId())
                .variantLabel(inventory.getVariantLabel())
                .sku(inventory.getSku())
                .stockOnHand(inventory.getStockOnHand())
                .reservedStock(inventory.getReservedStock())
                .availableStock(available)
                .lowStockThreshold(inventory.getLowStockThreshold())
                .lowStock(available <= inventory.getLowStockThreshold())
                .build();
    }

    private CommerceDtos.SellerResponse mapSeller(CommerceSeller seller) {
        return CommerceDtos.SellerResponse.builder()
                .id(seller.getId())
                .businessName(seller.getBusinessName())
                .contactName(seller.getContactName())
                .email(seller.getEmail())
                .phone(seller.getPhone())
                .gstNumber(seller.getGstNumber())
                .city(seller.getCity())
                .status(seller.getStatus())
                .trustScore(seller.getTrustScore())
                .verificationNotes(seller.getVerificationNotes())
                .createdAt(seller.getCreatedAt())
                .build();
    }

    private BigDecimal cartSubtotal(List<CommerceCartItem> items) {
        return items.stream().map(this::lineTotal).reduce(ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal lineTotal(CommerceCartItem item) {
        return unitPrice(item).multiply(BigDecimal.valueOf(item.getQuantity())).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal unitPrice(CommerceCartItem item) {
        CommerceProduct product = item.getProduct();
        if (product.getProductType() == CommerceProductType.RENTAL) {
            long days = rentalDays(item.getRentalStart(), item.getRentalEnd());
            BigDecimal rentalFee = moneyOrZero(product.getRentalFeePerDay()).multiply(BigDecimal.valueOf(days));
            return rentalFee.add(moneyOrZero(product.getDepositAmount())).setScale(2, RoundingMode.HALF_UP);
        }
        return money(product.getPrice());
    }

    private long rentalDays(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            return 1;
        }
        return Math.max(1, ChronoUnit.DAYS.between(start, end) + 1);
    }

    private BigDecimal discountFor(CommerceDtos.CheckoutRequest request, BigDecimal subtotal) {
        String coupon = clean(request == null ? null : request.getCouponCode());
        if (coupon == null) {
            return ZERO;
        }
        if ("SPORTS10".equalsIgnoreCase(coupon)) {
            return subtotal.multiply(BigDecimal.valueOf(0.10)).min(BigDecimal.valueOf(500)).setScale(2, RoundingMode.HALF_UP);
        }
        if ("TEAM5".equalsIgnoreCase(coupon)) {
            return subtotal.multiply(BigDecimal.valueOf(0.05)).setScale(2, RoundingMode.HALF_UP);
        }
        return ZERO;
    }

    private int availableStock(CommerceInventory inventory) {
        return Math.max(0, inventory.getStockOnHand() - inventory.getReservedStock());
    }

    private void validateSellerRequest(CommerceDtos.SellerRequest request) {
        if (request == null || !hasText(request.getBusinessName()) || !hasText(request.getContactName()) || !hasText(request.getEmail()) || !hasText(request.getPhone())) {
            throw ApiException.badRequest("Business name, contact name, email, and phone are required.");
        }
        if (!request.getEmail().contains("@")) {
            throw ApiException.badRequest("Seller email is invalid.");
        }
    }

    private void validateProductRequest(CommerceDtos.ProductRequest request) {
        if (request == null || request.getSellerId() == null || !hasText(request.getName()) || !hasText(request.getSku()) || !hasText(request.getCategory()) || !hasText(request.getSport())) {
            throw ApiException.badRequest("Seller, product name, SKU, category, and sport are required.");
        }
        if (request.getPrice() == null || request.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw ApiException.badRequest("Product price cannot be negative.");
        }
        if (request.getProductType() == CommerceProductType.RENTAL && (request.getRentalFeePerDay() == null || request.getRentalFeePerDay().compareTo(BigDecimal.ZERO) <= 0)) {
            throw ApiException.badRequest("Rental products require a rental fee per day.");
        }
    }

    private void validateRentalWindow(CommerceProduct product, LocalDate start, LocalDate end) {
        if (product.getProductType() != CommerceProductType.RENTAL) {
            return;
        }
        if (start == null || end == null) {
            throw ApiException.badRequest("Rental start and end dates are required.");
        }
        if (end.isBefore(start)) {
            throw ApiException.badRequest("Rental end date cannot be before start date.");
        }
    }

    private BigDecimal money(BigDecimal amount) {
        if (amount == null) {
            throw ApiException.badRequest("Amount is required.");
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal moneyOrNull(BigDecimal amount) {
        return amount == null ? null : amount.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal moneyOrZero(BigDecimal amount) {
        return amount == null ? ZERO : amount.setScale(2, RoundingMode.HALF_UP);
    }

    private User findUser(String loginId) {
        if (!hasText(loginId)) {
            throw ApiException.unauthorized("User account is required.");
        }
        return userRepository.findByEmailOrPhone(loginId)
                .orElseThrow(() -> ApiException.notFound("User account was not found."));
    }

    private void audit(User actor, String action, String entityType, Long entityId, String summary) {
        auditRepository.save(CommerceAudit.builder()
                .actor(actor)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .summary(clean(summary))
                .build());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String clean(String value) {
        return hasText(value) ? value.trim() : null;
    }
}
