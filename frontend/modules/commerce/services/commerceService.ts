import api from "@/services/api";

export type CommerceProductType = "SALE" | "RENTAL" | "AFFILIATE" | "CUSTOM_JERSEY";
export type CommerceProductStatus = "DRAFT" | "PUBLISHED" | "ARCHIVED";
export type CommerceOrderStatus = "PLACED" | "CONFIRMED" | "PACKED" | "SHIPPED" | "DELIVERED" | "CANCELLED" | "RETURN_REQUESTED" | "RETURNED" | "REFUNDED";

export type CommerceInventory = {
  id: number;
  variantLabel: string;
  sku: string;
  stockOnHand: number;
  reservedStock: number;
  availableStock: number;
  lowStockThreshold: number;
  lowStock: boolean;
};

export type CommerceProduct = {
  id: number;
  sellerId: number;
  sellerName: string;
  name: string;
  brand?: string;
  sku: string;
  category: string;
  sport: string;
  description?: string;
  price: number;
  mrp?: number;
  productType: CommerceProductType;
  status: CommerceProductStatus;
  imageUrl?: string;
  affiliateUrl?: string;
  rentalFeePerDay?: number;
  depositAmount?: number;
  rating?: number;
  featured: boolean;
  availableStock: number;
  lowStockThreshold: number;
  variants: CommerceInventory[];
  updatedAt?: string;
};

export type CommerceCartItem = {
  id: number;
  product: CommerceProduct;
  inventory: CommerceInventory;
  quantity: number;
  rentalStart?: string;
  rentalEnd?: string;
  lineTotal: number;
};

export type CommerceCart = {
  items: CommerceCartItem[];
  subtotal: number;
  rentalDeposits: number;
  itemCount: number;
};

export type CommerceOrder = {
  id: number;
  orderNumber: string;
  status: CommerceOrderStatus;
  subtotal: number;
  discount: number;
  loyaltyPointsRedeemed: number;
  shippingFee: number;
  total: number;
  paymentStatus: string;
  couponCode?: string;
  trackingNumber?: string;
  createdAt?: string;
};

export const getCommerceProducts = (params?: {
  query?: string;
  category?: string;
  sport?: string;
  productType?: CommerceProductType;
  maxPrice?: number;
}) => api.get<CommerceProduct[]>("/commerce/products", { params });

export const getCommerceCart = () => api.get<CommerceCart>("/commerce/cart");

export const addCommerceCartItem = (payload: {
  productId: number;
  inventoryId: number;
  quantity: number;
  rentalStart?: string;
  rentalEnd?: string;
}) => api.post<CommerceCart>("/commerce/cart", payload);

export const removeCommerceCartItem = (itemId: number) => api.delete<CommerceCart>(`/commerce/cart/${itemId}`);

export const checkoutCommerceCart = (payload: {
  shippingAddress?: string;
  couponCode?: string;
  loyaltyPoints?: number;
  paymentReference?: string;
}) => api.post<CommerceOrder>("/commerce/checkout", payload);
