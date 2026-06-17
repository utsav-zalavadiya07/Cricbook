import api from "@/services/api";

// Backend: POST /payments/create-order/{bookingId}  (path param, not body)
export const createOrder = (bookingId: number) =>
  api.post(`/payments/create-order/${bookingId}`);

export const createBatchOrder = (bookingIds: number[]) =>
  api.post("/payments/create-order/batch", { bookingIds });

export const verifyPayment = (data: {
  orderId: string;
  paymentId: string;
  signature: string;
  bookingId?: number;
  bookingIds?: number[];
}) => api.post("/payments/verify", data);

export const payWithWallet = (bookingId: number) =>
  api.post(`/payments/wallet/${bookingId}`);

export const getPaymentHistory = () => api.get("/payments/history");
