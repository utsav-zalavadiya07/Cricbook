export type WalletTransaction = {
  id: number;
  amount: number;
  type: string;
  status: string;
  reference?: string;
  description?: string;
  createdAt?: string;
};

export type Wallet = {
  walletId: number;
  balance: number;
  transactions: WalletTransaction[];
};

export type Coupon = {
  code: string;
  discountType: string;
  discountValue: number;
  validUntil?: string;
  valid: boolean;
  message?: string;
};

export type WalletTopupOrder = {
  orderId: string;
  amount: number;
  currency: string;
  keyId?: string;
};
