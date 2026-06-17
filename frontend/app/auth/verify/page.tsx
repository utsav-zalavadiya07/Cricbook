import { Suspense } from "react";
import AuthCard from "@/modules/auth/components/AuthCard";
import OTPForm from "@/modules/auth/components/OTPForm";

export const metadata = { title: "Verify OTP | Cricbook" };

export default function VerifyPage() {
  return (
    <AuthCard title="Verify your email" subtitle="Enter the OTP we sent you">
      <Suspense fallback={<div className="text-slate-400 text-sm">Loading...</div>}>
        <OTPForm />
      </Suspense>
    </AuthCard>
  );
}
