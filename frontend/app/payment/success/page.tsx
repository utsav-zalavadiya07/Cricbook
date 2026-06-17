import { Suspense } from "react";
import PaymentSuccessContent from "./PaymentSuccessContent";

export const metadata = { title: "Payment Successful | Cricbook" };

export default function PaymentSuccessPage() {
  return (
    <Suspense fallback={
      <div className="theme-page-player flex min-h-screen items-center justify-center">
        <div className="theme-muted text-sm">Loading...</div>
      </div>
    }>
      <PaymentSuccessContent />
    </Suspense>
  );
}
