import { Suspense } from "react";
import PaymentPageContent from "./PaymentPageContent";

export const metadata = { title: "Payment | Cricbook" };

export default function PaymentPage() {
  return (
    <Suspense fallback={
      <div className="theme-page-player flex min-h-screen items-center justify-center">
        <div className="theme-muted text-sm">Loading payment...</div>
      </div>
    }>
      <PaymentPageContent />
    </Suspense>
  );
}
