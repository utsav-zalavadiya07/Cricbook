"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { adminLogin, adminVerifyOtp } from "@/modules/admin/services/adminService";
import { canRoleAccessPath, safeNextPath } from "@/modules/auth/sessionPolicy";
import { useAuthStore } from "@/store/authStore";

export default function AdminLoginPage() {
  const router = useRouter();
  const setAuth = useAuthStore((state) => state.setAuth);
  const [email, setEmail] = useState("admin@cricbook.com");
  const [password, setPassword] = useState("admin123");
  const [otp, setOtp] = useState("");
  const [step, setStep] = useState<"LOGIN" | "2FA">("LOGIN");
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [loading, setLoading] = useState(false);

  const handleLogin = async (event: React.FormEvent) => {
    event.preventDefault();
    setError("");
    setMessage("");
    setLoading(true);

    try {
      const response = await adminLogin({ email, password });
      if (response.data.status === "OTP_REQUIRED") {
        setStep("2FA");
        setOtp(response.data.simulatedOtp || "");
        setMessage(`Two-factor verification required. Dev OTP: ${response.data.simulatedOtp || "123456"}`);
      } else {
        setError("Unexpected login response from admin API.");
      }
    } catch (error: unknown) {
      setError(readableError(error, "Invalid admin credentials."));
    } finally {
      setLoading(false);
    }
  };

  const handleVerify = async (event: React.FormEvent) => {
    event.preventDefault();
    setError("");
    setLoading(true);

    try {
      const response = await adminVerifyOtp({ email, otp });
      await setAuth({ email: response.data.email, name: "Cricbook Admin" }, response.data.accessToken, response.data.role, response.data.refreshToken);
      window.localStorage.setItem("adminEmail", response.data.email);
      const requestedPath = safeNextPath(new URLSearchParams(window.location.search).get("next"), "/admin");
      router.replace(canRoleAccessPath(response.data.role, requestedPath) ? requestedPath : "/admin");
    } catch (error: unknown) {
      setError(readableError(error, "Invalid 2FA code."));
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="min-h-screen bg-slate-950 px-4 py-8 text-slate-100">
      <div className="mx-auto grid min-h-[calc(100vh-4rem)] max-w-6xl items-center gap-8 lg:grid-cols-[1.1fr_0.9fr]">
        <section className="rounded-[2rem] border border-white/10 bg-gradient-to-br from-slate-900 via-slate-900 to-emerald-950/35 p-8 shadow-2xl">
          <p className="text-xs font-black uppercase tracking-[0.32em] text-emerald-400">Cricbook Admin Portal</p>
          <h1 className="mt-5 text-4xl font-black tracking-tight text-white sm:text-5xl">Secure operations login</h1>
          <p className="mt-4 max-w-2xl text-sm leading-6 text-slate-300">
            Admin access is separate from player and owner portals. Use this login for platform operations, RBAC, finance, bookings, support, and system controls.
          </p>
          <div className="mt-8 grid gap-3 sm:grid-cols-3">
            {["Role based access", "2FA verification", "Audit ready actions"].map((item) => (
              <div key={item} className="rounded-2xl border border-white/10 bg-slate-950/50 p-4 text-sm font-black text-slate-200">
                {item}
              </div>
            ))}
          </div>
        </section>

        <section className="rounded-[2rem] border border-white/10 bg-slate-900/90 p-6 shadow-2xl">
          <div className="mb-6">
            <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-emerald-500 text-sm font-black text-white">CB</div>
            <h2 className="mt-4 text-2xl font-black text-white">{step === "LOGIN" ? "Sign in" : "Verify 2FA"}</h2>
            <p className="mt-1 text-sm text-slate-400">Development account: admin@cricbook.com / admin123</p>
          </div>

          {error ? <Alert tone="error" message={error} /> : null}
          {message ? <Alert tone="success" message={message} /> : null}

          {step === "LOGIN" ? (
            <form onSubmit={handleLogin} className="space-y-4">
              <AdminLoginField label="Email" type="email" value={email} onChange={setEmail} placeholder="admin@cricbook.com" />
              <AdminLoginField label="Password" type="password" value={password} onChange={setPassword} placeholder="admin123" />
              <button
                type="submit"
                disabled={loading}
                className="h-12 w-full rounded-2xl bg-emerald-500 text-sm font-black text-white shadow-xl shadow-emerald-950/40 transition hover:bg-emerald-400 disabled:cursor-not-allowed disabled:opacity-60"
              >
                {loading ? "Checking credentials..." : "Continue to 2FA"}
              </button>
            </form>
          ) : (
            <form onSubmit={handleVerify} className="space-y-4">
              <AdminLoginField label="Six digit OTP" type="text" value={otp} onChange={setOtp} placeholder="123456" maxLength={6} />
              <button
                type="submit"
                disabled={loading}
                className="h-12 w-full rounded-2xl bg-emerald-500 text-sm font-black text-white shadow-xl shadow-emerald-950/40 transition hover:bg-emerald-400 disabled:cursor-not-allowed disabled:opacity-60"
              >
                {loading ? "Verifying..." : "Enter Admin Portal"}
              </button>
              <button
                type="button"
                onClick={() => {
                  setStep("LOGIN");
                  setOtp("");
                  setMessage("");
                }}
                className="h-11 w-full rounded-2xl border border-white/10 bg-slate-950/70 text-sm font-black text-slate-300 hover:bg-white/10"
              >
                Back to credentials
              </button>
            </form>
          )}
        </section>
      </div>
    </main>
  );
}

function AdminLoginField({
  label,
  type,
  value,
  onChange,
  placeholder,
  maxLength,
}: {
  label: string;
  type: string;
  value: string;
  onChange: (value: string) => void;
  placeholder: string;
  maxLength?: number;
}) {
  return (
    <label className="block">
      <span className="text-xs font-black uppercase tracking-[0.18em] text-slate-500">{label}</span>
      <input
        required
        type={type}
        value={value}
        placeholder={placeholder}
        maxLength={maxLength}
        onChange={(event) => onChange(event.target.value)}
        className="mt-2 h-12 w-full rounded-2xl border border-white/10 bg-slate-950/80 px-4 text-sm font-semibold text-white outline-none placeholder:text-slate-600 focus:border-emerald-400/60"
      />
    </label>
  );
}

function Alert({ tone, message }: { tone: "error" | "success"; message: string }) {
  const className = tone === "error"
    ? "border-rose-500/25 bg-rose-500/10 text-rose-200"
    : "border-emerald-500/25 bg-emerald-500/10 text-emerald-200";

  return <div className={`mb-4 rounded-2xl border px-4 py-3 text-sm font-semibold ${className}`}>{message}</div>;
}

function readableError(error: unknown, fallback: string) {
  const record = error && typeof error === "object" ? error as Record<string, unknown> : {};
  const response = record.response && typeof record.response === "object" ? record.response as Record<string, unknown> : {};
  const data = response.data && typeof response.data === "object" ? response.data as Record<string, unknown> : {};
  return typeof data.message === "string" ? data.message : fallback;
}
