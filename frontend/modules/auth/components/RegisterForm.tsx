"use client";

import { ChangeEvent, FormEvent, useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { registerUser } from "../services/authService";
import { validators } from "@/utils/validators";
import type { LoginRole } from "./LoginForm";

const pwdRules = [
  { label: "8+ characters", test: (value: string) => value.length >= 8 },
  { label: "Uppercase", test: (value: string) => /[A-Z]/.test(value) },
  { label: "Lowercase", test: (value: string) => /[a-z]/.test(value) },
  { label: "Number", test: (value: string) => /[0-9]/.test(value) },
  { label: "Symbol", test: (value: string) => /[@$!%*?&]/.test(value) },
];

const ownerBenefits = [
  "Add and manage multiple turfs",
  "Accept bookings and manage availability",
  "Track earnings, payouts and analytics",
  "Build trust with reviews and ratings",
];

const playerBenefits = [
  "Book cricket turfs faster",
  "Create teams and join tournaments",
  "Track rewards, wallet and activity",
  "Get recommendations and match alerts",
];

type RegisterFormProps = {
  selectedRole: LoginRole;
  onRoleChange: (role: LoginRole) => void;
};

type RegisterFormState = {
  name: string;
  email: string;
  phone: string;
  password: string;
  role: LoginRole;
};

type RegisterErrors = {
  name: string;
  email: string;
  phone: string;
  password: string;
  confirmPassword: string;
};

function apiErrorMessage(error: unknown, fallback: string) {
  const apiError = error as { response?: { data?: { error?: string; message?: string } } };
  return apiError.response?.data?.error || apiError.response?.data?.message || fallback;
}

function FieldIcon({ type }: { type: "user" | "mail" | "phone" | "lock" }) {
  const common = "h-5 w-5";
  if (type === "user") {
    return (
      <svg aria-hidden="true" className={common} viewBox="0 0 24 24" fill="none">
        <path d="M12 12.35a3.8 3.8 0 1 0 0-7.6 3.8 3.8 0 0 0 0 7.6Z" stroke="currentColor" strokeWidth="1.7" />
        <path d="M5.6 19.25c.72-3.08 3-4.62 6.4-4.62s5.68 1.54 6.4 4.62" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" />
      </svg>
    );
  }
  if (type === "mail") {
    return (
      <svg aria-hidden="true" className={common} viewBox="0 0 24 24" fill="none">
        <path d="M4.75 6.75h14.5v10.5H4.75V6.75Z" stroke="currentColor" strokeWidth="1.7" strokeLinejoin="round" />
        <path d="m5.25 7.25 6.4 5.2a.55.55 0 0 0 .7 0l6.4-5.2" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" />
      </svg>
    );
  }
  if (type === "phone") {
    return (
      <svg aria-hidden="true" className={common} viewBox="0 0 24 24" fill="none">
        <path d="M7.2 4.75h2.4l1.25 4.05-1.6 1.05c.72 1.45 1.86 2.59 3.4 3.4l1.05-1.6 4.05 1.25v2.4c0 1.02-.8 1.95-1.88 1.88-6.14-.4-11.08-5.34-11.48-11.48-.07-1.08.86-1.88 1.88-1.88Z" stroke="currentColor" strokeWidth="1.7" strokeLinejoin="round" />
      </svg>
    );
  }
  return (
    <svg aria-hidden="true" className={common} viewBox="0 0 24 24" fill="none">
      <path d="M7.75 10.5V8.35a4.25 4.25 0 0 1 8.5 0v2.15" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" />
      <path d="M6.5 10.5h11A1.5 1.5 0 0 1 19 12v5.25a1.5 1.5 0 0 1-1.5 1.5h-11A1.5 1.5 0 0 1 5 17.25V12a1.5 1.5 0 0 1 1.5-1.5Z" stroke="currentColor" strokeWidth="1.7" strokeLinejoin="round" />
    </svg>
  );
}

function SocialLogo({ provider }: { provider: "google" | "apple" | "microsoft" }) {
  if (provider === "google") {
    return <span className="text-2xl font-black text-[#4285f4]">G</span>;
  }
  if (provider === "apple") {
    return <span className="text-2xl"></span>;
  }
  return (
    <span className="grid grid-cols-2 gap-0.5">
      <span className="h-3 w-3 bg-[#f25022]" />
      <span className="h-3 w-3 bg-[#7fba00]" />
      <span className="h-3 w-3 bg-[#00a4ef]" />
      <span className="h-3 w-3 bg-[#ffb900]" />
    </span>
  );
}

export default function RegisterForm({ selectedRole, onRoleChange }: RegisterFormProps) {
  const router = useRouter();
  const [form, setForm] = useState<RegisterFormState>({ name: "", email: "", phone: "", password: "", role: selectedRole });
  const [confirmPassword, setConfirmPassword] = useState("");
  const [errors, setErrors] = useState<RegisterErrors>({ name: "", email: "", phone: "", password: "", confirmPassword: "" });
  const [loading, setLoading] = useState(false);
  const [serverError, setServerError] = useState("");
  const [notice, setNotice] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);

  const isOwner = selectedRole === "OWNER";
  const accentText = isOwner ? "text-sky-300" : "text-emerald-300";
  const accentBorder = isOwner ? "focus:border-sky-400 focus:ring-sky-400" : "focus:border-emerald-400 focus:ring-emerald-400";
  const gradientClass = isOwner
    ? "from-sky-400 via-sky-500 to-blue-600 hover:from-sky-300 hover:to-blue-500 shadow-sky-500/25"
    : "from-emerald-400 via-emerald-500 to-teal-600 hover:from-emerald-300 hover:to-teal-500 shadow-emerald-500/25";
  const benefits = isOwner ? ownerBenefits : playerBenefits;

  useEffect(() => {
    const timer = window.setTimeout(() => {
      setForm((current) => ({ ...current, role: selectedRole }));
    }, 0);
    return () => window.clearTimeout(timer);
  }, [selectedRole]);

  const passedRules = useMemo(() => pwdRules.filter((rule) => rule.test(form.password)).length, [form.password]);
  const strengthLabel = passedRules >= 5 ? "Strong" : passedRules >= 3 ? "Good" : form.password ? "Weak" : "Use 8+ characters with a mix of letters, numbers & symbols";

  const handleRoleChange = (role: LoginRole) => {
    onRoleChange(role);
    setForm((current) => ({ ...current, role }));
    setNotice("");
  };

  const setField = (field: keyof RegisterFormState) => (event: ChangeEvent<HTMLInputElement>) => {
    const value = field === "phone" ? event.target.value.replace(/\D/g, "").slice(0, 10) : event.target.value;
    setForm((current) => ({ ...current, [field]: value }));
    if (field in errors) {
      setErrors((current) => ({ ...current, [field]: "" }));
    }
  };

  const validatePassword = (value: string) => {
    if (!value) return "Password is required.";
    if (!pwdRules.every((rule) => rule.test(value))) return "Password must include uppercase, lowercase, number, and symbol.";
    return "";
  };

  const validate = () => {
    const nextErrors = {
      name: validators.name(form.name),
      email: validators.email(form.email),
      phone: validators.phone(form.phone),
      password: validatePassword(form.password),
      confirmPassword: confirmPassword ? "" : "Confirm your password.",
    };

    if (!nextErrors.confirmPassword && confirmPassword !== form.password) {
      nextErrors.confirmPassword = "Passwords do not match.";
    }

    setErrors(nextErrors);
    return !Object.values(nextErrors).some(Boolean);
  };

  const handleSubmit = async (event?: FormEvent<HTMLFormElement>) => {
    event?.preventDefault();
    if (!validate()) return;
    setLoading(true);
    setServerError("");
    setNotice("");

    try {
      await registerUser({ ...form, email: form.email.trim(), name: form.name.trim(), phone: form.phone.trim() });
      router.push(`/auth/verify?email=${encodeURIComponent(form.email.trim())}`);
    } catch (error) {
      setServerError(apiErrorMessage(error, "Registration failed. Please try again."));
    } finally {
      setLoading(false);
    }
  };

  const inputClass = (field: keyof RegisterErrors) =>
    `auth-form-field min-h-12 w-full rounded-2xl border py-3 text-sm outline-none transition-all duration-200 focus:ring-1 ${
      errors[field] ? "border-red-500/60" : "border-white/10"
    } ${accentBorder}`;

  const socialNotice = () => {
    setServerError("");
    setNotice("Social sign-up is not configured yet. Use email registration for now.");
  };

  return (
    <div className="auth-register-card w-full max-w-[31rem] overflow-hidden rounded-[1.5rem] border border-white/10 bg-slate-950/70 p-4 shadow-2xl shadow-sky-950/40 backdrop-blur-2xl sm:p-5">
      <div className="text-center">
        <div className={`mx-auto flex h-14 w-14 items-center justify-center rounded-full border text-white shadow-xl ${isOwner ? "border-sky-300/30 bg-sky-500/20 shadow-sky-500/20" : "border-emerald-300/30 bg-emerald-500/20 shadow-emerald-500/20"}`}>
          <FieldIcon type="user" />
        </div>
        <h1 className="auth-copy-primary mt-2.5 text-2xl font-black tracking-tight">
          {isOwner ? "Create your owner account" : "Create your player account"}
        </h1>
        <p className="auth-copy-muted mt-1 text-sm">
          {isOwner ? "Let’s get your turf business online 🚀" : "Start booking, playing and building your cricket profile 🚀"}
        </p>
      </div>

      <form onSubmit={(event) => void handleSubmit(event)} className="mt-4 space-y-3">
        <div className="auth-role-switch flex rounded-2xl p-1">
          <button
            type="button"
            onClick={() => handleRoleChange("USER")}
            className={`flex min-h-12 flex-1 items-center justify-center gap-2 rounded-xl px-3 py-2.5 text-sm font-black transition-all duration-200 ${
              selectedRole === "USER" ? "bg-gradient-to-r from-emerald-400 to-emerald-600 text-white shadow-lg shadow-emerald-500/20" : "auth-copy-muted hover:bg-emerald-500/10 hover:text-emerald-400"
            }`}
          >
            <FieldIcon type="user" />
            Player
          </button>
          <button
            type="button"
            onClick={() => handleRoleChange("OWNER")}
            className={`flex min-h-12 flex-1 items-center justify-center gap-2 rounded-xl px-3 py-2.5 text-sm font-black transition-all duration-200 ${
              selectedRole === "OWNER" ? "bg-gradient-to-r from-sky-400 to-blue-600 text-white shadow-lg shadow-sky-500/20" : "auth-copy-muted hover:bg-sky-500/10 hover:text-sky-400"
            }`}
          >
            🛡
            Turf Owner
          </button>
        </div>

        {serverError ? (
          <div role="alert" className="rounded-xl border border-red-500/30 bg-red-500/10 px-3 py-2 text-sm text-red-400">
            {serverError}
          </div>
        ) : null}
        {notice ? (
          <div role="status" className="rounded-xl border border-sky-400/25 bg-sky-500/10 px-3 py-2 text-sm text-sky-200">
            {notice}
          </div>
        ) : null}

        <div>
          <label className="auth-copy-primary mb-1 block text-sm font-black" htmlFor="register-name">Full Name</label>
          <div className="group relative">
            <span className="auth-copy-soft absolute left-4 top-1/2 -translate-y-1/2 transition-colors group-focus-within:text-sky-300">
              <FieldIcon type="user" />
            </span>
            <input id="register-name" type="text" placeholder="Enter your full name" value={form.name} onChange={setField("name")} className={`${inputClass("name")} pl-12 pr-4`} aria-invalid={Boolean(errors.name)} aria-describedby={errors.name ? "register-name-error" : undefined} />
          </div>
          {errors.name ? <p id="register-name-error" className="mt-1 text-xs text-red-400">{errors.name}</p> : null}
        </div>

        <div>
          <label className="auth-copy-primary mb-1 block text-sm font-black" htmlFor="register-email">Email Address</label>
          <div className="group relative">
            <span className="auth-copy-soft absolute left-4 top-1/2 -translate-y-1/2 transition-colors group-focus-within:text-sky-300">
              <FieldIcon type="mail" />
            </span>
            <input id="register-email" type="email" placeholder="Enter your email address" value={form.email} onChange={setField("email")} className={`${inputClass("email")} pl-12 pr-4`} aria-invalid={Boolean(errors.email)} aria-describedby={errors.email ? "register-email-error" : undefined} />
          </div>
          {errors.email ? <p id="register-email-error" className="mt-1 text-xs text-red-400">{errors.email}</p> : null}
        </div>

        <div>
          <label className="auth-copy-primary mb-1 block text-sm font-black" htmlFor="register-phone">Phone Number</label>
          <div className={`auth-form-field flex min-h-12 items-center rounded-2xl border ${errors.phone ? "border-red-500/60" : "border-white/10"} ${accentBorder} focus-within:ring-1`}>
            <span className="flex h-12 items-center gap-2 border-r border-white/10 px-4 text-sm font-black text-slate-100">
              🇮🇳 <span className="auth-copy-muted">+91</span>
            </span>
            <span className="auth-copy-soft pl-4">
              <FieldIcon type="phone" />
            </span>
            <input id="register-phone" type="tel" inputMode="numeric" placeholder="Enter your phone number" value={form.phone} onChange={setField("phone")} maxLength={10} className="min-h-12 flex-1 bg-transparent px-4 text-sm text-slate-100 outline-none placeholder:text-slate-500" aria-invalid={Boolean(errors.phone)} aria-describedby={errors.phone ? "register-phone-error" : undefined} />
          </div>
          {errors.phone ? <p id="register-phone-error" className="mt-1 text-xs text-red-400">{errors.phone}</p> : null}
        </div>

        <div>
          <label className="auth-copy-primary mb-1 block text-sm font-black" htmlFor="register-password">Password</label>
          <div className="group relative">
            <span className="auth-copy-soft absolute left-4 top-1/2 -translate-y-1/2 transition-colors group-focus-within:text-sky-300">
              <FieldIcon type="lock" />
            </span>
            <input id="register-password" type={showPassword ? "text" : "password"} placeholder="Create a strong password" value={form.password} onChange={setField("password")} className={`${inputClass("password")} pl-12 pr-20`} aria-invalid={Boolean(errors.password)} aria-describedby={errors.password ? "register-password-error register-password-strength" : "register-password-strength"} />
            <button type="button" onClick={() => setShowPassword((value) => !value)} className="auth-copy-soft absolute right-4 top-1/2 -translate-y-1/2 rounded-lg px-2 py-1 text-xs font-black hover:bg-white/5 hover:text-slate-100">
              {showPassword ? "Hide" : "Show"}
            </button>
          </div>
          <div id="register-password-strength" className="mt-1.5 flex items-center gap-2 text-xs">
            <span className={`inline-flex h-4 w-4 items-center justify-center rounded text-[10px] ${passedRules >= 5 ? "bg-emerald-500/20 text-emerald-300" : "bg-white/5 text-slate-400"}`}>🔒</span>
            <span className={passedRules >= 5 ? "text-emerald-300" : "auth-copy-muted"}>{strengthLabel}</span>
            <span className="ml-auto flex gap-1">
              {[1, 2, 3, 4].map((bar) => (
                <span key={bar} className={`h-1 w-6 rounded-full ${passedRules >= bar + 1 ? (isOwner ? "bg-sky-400" : "bg-emerald-400") : "bg-slate-600"}`} />
              ))}
            </span>
          </div>
          {errors.password ? <p id="register-password-error" className="mt-1 text-xs text-red-400">{errors.password}</p> : null}
        </div>

        <div>
          <label className="auth-copy-primary mb-1 block text-sm font-black" htmlFor="register-confirm-password">Confirm Password</label>
          <div className="group relative">
            <span className="auth-copy-soft absolute left-4 top-1/2 -translate-y-1/2 transition-colors group-focus-within:text-sky-300">
              <FieldIcon type="lock" />
            </span>
            <input id="register-confirm-password" type={showConfirmPassword ? "text" : "password"} placeholder="Confirm your password" value={confirmPassword} onChange={(event) => { setConfirmPassword(event.target.value); setErrors((current) => ({ ...current, confirmPassword: "" })); }} className={`${inputClass("confirmPassword")} pl-12 pr-20`} aria-invalid={Boolean(errors.confirmPassword)} aria-describedby={errors.confirmPassword ? "register-confirm-password-error" : undefined} />
            <button type="button" onClick={() => setShowConfirmPassword((value) => !value)} className="auth-copy-soft absolute right-4 top-1/2 -translate-y-1/2 rounded-lg px-2 py-1 text-xs font-black hover:bg-white/5 hover:text-slate-100">
              {showConfirmPassword ? "Hide" : "Show"}
            </button>
          </div>
          {errors.confirmPassword ? <p id="register-confirm-password-error" className="mt-1 text-xs text-red-400">{errors.confirmPassword}</p> : null}
        </div>

        <div className="rounded-2xl border border-white/10 bg-white/[0.04] p-3">
          <p className={`text-sm font-black ${accentText}`}>With your account, you can:</p>
          <ul className="mt-2 grid gap-1.5 sm:grid-cols-2">
            {benefits.map((benefit) => (
              <li key={benefit} className="flex items-start gap-2 text-xs leading-5 text-slate-300">
                <span className={`mt-0.5 inline-flex h-4 w-4 shrink-0 items-center justify-center rounded-full text-[10px] text-white ${isOwner ? "bg-sky-500" : "bg-emerald-500"}`}>✓</span>
                {benefit}
              </li>
            ))}
          </ul>
        </div>

        <button type="submit" disabled={loading} className={`flex min-h-12 w-full items-center justify-center gap-3 rounded-2xl bg-gradient-to-r py-3 text-base font-black text-white shadow-xl outline-none transition-all duration-200 hover:-translate-y-0.5 focus-visible:ring-4 disabled:cursor-not-allowed disabled:opacity-50 ${gradientClass}`}>
          {loading ? "Creating account..." : `Create ${isOwner ? "Owner" : "Player"} Account`}
          <span aria-hidden="true">→</span>
        </button>

        <div className="grid grid-cols-[1fr_auto_1fr] items-center gap-3">
          <span className="h-px bg-white/10" />
          <span className="auth-copy-soft text-xs font-black uppercase tracking-[0.16em]">Or continue with</span>
          <span className="h-px bg-white/10" />
        </div>

        <div className="grid grid-cols-3 gap-2.5">
          {(["google", "apple", "microsoft"] as const).map((provider) => (
            <button key={provider} type="button" onClick={socialNotice} className="flex min-h-12 items-center justify-center rounded-2xl border border-white/10 bg-white/[0.03] text-slate-100 transition hover:border-white/20 hover:bg-white/[0.06]" aria-label={`Continue with ${provider}`}>
              <SocialLogo provider={provider} />
            </button>
          ))}
        </div>

        <p className="auth-copy-soft text-center text-xs leading-5">
          By creating an account, you agree to our{" "}
          <Link href="/terms" className={`${accentText} font-semibold hover:opacity-80`}>Terms of Service</Link>
          {" "}and{" "}
          <Link href="/privacy" className={`${accentText} font-semibold hover:opacity-80`}>Privacy Policy</Link>.
        </p>
      </form>
    </div>
  );
}
