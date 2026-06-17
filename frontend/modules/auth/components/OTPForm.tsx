"use client";

import Link from "next/link";
import { useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { validators } from "@/utils/validators";
import { resendOtp, verifyOtp } from "../services/authService";

function errorMessage(error: unknown, fallback: string) {
  const apiError = error as { response?: { data?: { error?: string; message?: string } } };
  return apiError.response?.data?.error || apiError.response?.data?.message || fallback;
}

export default function OTPForm() {
  const router = useRouter();
  const params = useSearchParams();
  const email = params.get("email") || "";

  const [otp, setOtp] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const [resending, setResending] = useState(false);
  const [resendMsg, setResendMsg] = useState("");

  const handleVerify = async () => {
    const err = validators.otp(otp);
    if (err) {
      setError(err);
      return;
    }

    setLoading(true);
    setError("");
    try {
      await verifyOtp({ email, otp });
      router.push("/auth/login");
    } catch (error: unknown) {
      setError(errorMessage(error, "Invalid OTP. Please try again."));
    } finally {
      setLoading(false);
    }
  };

  const handleResend = async () => {
    setResending(true);
    setResendMsg("");
    try {
      await resendOtp(email);
      setResendMsg("OTP sent! Check your email.");
    } catch {
      setResendMsg("Failed to resend. Try again.");
    } finally {
      setResending(false);
    }
  };

  return (
    <div className="space-y-5">
      <div className="rounded-xl border border-emerald-500/20 bg-emerald-500/10 px-4 py-3">
        <p className="theme-text text-sm">
          OTP sent to <span className="font-medium text-emerald-600 dark:text-emerald-400">{email}</span>
        </p>
      </div>

      {error && <div className="rounded-xl border border-red-500/30 bg-red-500/10 px-4 py-3 text-sm text-red-500 dark:text-red-400">{error}</div>}
      {resendMsg && <div className="rounded-xl border border-emerald-500/30 bg-emerald-500/10 px-4 py-3 text-sm text-emerald-600 dark:text-emerald-400">{resendMsg}</div>}

      <div>
        <label className="theme-text mb-1.5 block text-sm font-medium">Enter OTP</label>
        <input
          type="text"
          maxLength={6}
          placeholder="6-digit code"
          value={otp}
          onChange={(e) => setOtp(e.target.value.replace(/\D/g, ""))}
          onKeyDown={(e) => e.key === "Enter" && void handleVerify()}
          className={`theme-input w-full rounded-xl border px-4 py-3 text-center text-sm tracking-widest placeholder:text-slate-500 transition focus:border-emerald-500 focus:outline-none focus:ring-1 focus:ring-emerald-500 ${error ? "border-red-500/50" : "border-white/10"}`}
        />
      </div>

      <button
        onClick={() => void handleVerify()}
        disabled={loading}
        className="w-full rounded-xl bg-gradient-to-r from-emerald-500 to-teal-600 py-3 font-semibold text-white shadow-lg shadow-emerald-500/20 transition-all duration-200 hover:-translate-y-0.5 hover:from-emerald-400 hover:to-teal-500 disabled:opacity-50"
      >
        {loading ? "Verifying..." : "Verify OTP"}
      </button>

      <div className="flex items-center justify-between text-sm">
        <button onClick={() => void handleResend()} disabled={resending} className="theme-muted transition hover:text-emerald-500 disabled:opacity-50">
          {resending ? "Sending..." : "Resend OTP"}
        </button>
        <Link href="/auth/login" className="theme-muted transition hover:text-current">
          Back to Login
        </Link>
      </div>
    </div>
  );
}
