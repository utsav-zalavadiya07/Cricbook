"use client";

import { useState } from "react";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { useI18n } from "@/modules/i18n/I18nProvider";
import { validators } from "@/utils/validators";
import { resetPassword } from "../services/authService";
import type { LoginRole } from "./LoginForm";

function errorMessage(error: unknown, fallback: string) {
  const apiError = error as { response?: { data?: { error?: string; message?: string; details?: Record<string, string> } } };
  const details = apiError.response?.data?.details;
  return details?.newPassword || apiError.response?.data?.error || apiError.response?.data?.message || fallback;
}

interface ResetPasswordFormProps {
  portalRole?: LoginRole;
}

export default function ResetPasswordForm({ portalRole = "USER" }: ResetPasswordFormProps) {
  const searchParams = useSearchParams();
  const { t } = useI18n();
  const token = searchParams.get("token") || "";
  const [form, setForm] = useState({ password: "", confirmPassword: "" });
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [error, setError] = useState("");
  const [serverMessage, setServerMessage] = useState("");
  const [loading, setLoading] = useState(false);

  const validate = () => {
    if (!token) {
      return t("auth.resetTokenMissing");
    }

    const passwordError = validators.password(form.password);
    if (passwordError) {
      return passwordError;
    }

    if (!/[A-Z]/.test(form.password) || !/[a-z]/.test(form.password) || !/\d/.test(form.password) || !/[@$!%*?&]/.test(form.password)) {
      return t("auth.passwordComplexity");
    }

    if (form.password !== form.confirmPassword) {
      return t("auth.passwordMismatch");
    }

    return "";
  };

  const handleSubmit = async () => {
    const validationError = validate();
    setError(validationError);
    setServerMessage("");

    if (validationError) {
      return;
    }

    setLoading(true);
    try {
      const response = await resetPassword({ token, newPassword: form.password });
      setServerMessage(response.data?.message || t("auth.passwordResetSuccess"));
      setForm({ password: "", confirmPassword: "" });
    } catch (err) {
      setError(errorMessage(err, t("auth.passwordResetFailed")));
    } finally {
      setLoading(false);
    }
  };

  const isOwner = portalRole === "OWNER";
  const focusClass = isOwner ? "focus:border-sky-500 focus:ring-sky-500" : "focus:border-emerald-500 focus:ring-emerald-500";
  const successClass = isOwner
    ? "border-sky-500/30 bg-sky-500/10 text-sky-300"
    : "border-emerald-500/30 bg-emerald-500/10 text-emerald-300";
  const buttonClass = isOwner
    ? "from-sky-500 to-blue-600 hover:from-sky-400 hover:to-blue-500 shadow-sky-500/20 hover:shadow-sky-500/30 hover:ring-4 hover:ring-sky-400/20 focus-visible:ring-4 focus-visible:ring-sky-300/40"
    : "from-emerald-500 to-teal-600 hover:from-emerald-400 hover:to-teal-500 shadow-emerald-500/20 hover:shadow-emerald-500/30 hover:ring-4 hover:ring-emerald-400/20 focus-visible:ring-4 focus-visible:ring-emerald-300/40";
  const linkClass = isOwner ? "text-sky-400 hover:text-sky-300" : "text-emerald-400 hover:text-emerald-300";

  const passwordInput = (
    key: "password" | "confirmPassword",
    label: string,
    visible: boolean,
    onToggle: () => void
  ) => (
    <div>
      <label className="theme-muted mb-1.5 block text-sm font-medium">{label}</label>
      <div className="relative">
        <input
          type={visible ? "text" : "password"}
          value={form[key]}
          onChange={(event) => setForm({ ...form, [key]: event.target.value })}
          onKeyDown={(event) => event.key === "Enter" && void handleSubmit()}
          placeholder="********"
          className={`theme-input w-full rounded-xl px-4 py-3 pr-20 text-sm outline-none transition focus:ring-1 ${focusClass}`}
        />
        <button
          type="button"
          onClick={onToggle}
          className="theme-muted absolute right-3 top-1/2 -translate-y-1/2 rounded-lg px-2 py-1 text-xs font-semibold hover:bg-white/5 hover:text-[var(--theme-text)]"
        >
          {visible ? t("auth.hidePassword") : t("auth.showPassword")}
        </button>
      </div>
    </div>
  );

  return (
    <div className="space-y-5">
      {serverMessage && <div className={`rounded-xl border px-4 py-3 text-sm ${successClass}`}>{serverMessage}</div>}
      {error && <div className="rounded-xl border border-red-500/30 bg-red-500/10 px-4 py-3 text-sm text-red-400">{error}</div>}

      {passwordInput("password", t("auth.newPassword"), showPassword, () => setShowPassword((value) => !value))}
      {passwordInput("confirmPassword", t("auth.confirmPassword"), showConfirmPassword, () => setShowConfirmPassword((value) => !value))}

      <div className="theme-card rounded-2xl p-4 text-xs">
        <p className="theme-title font-semibold">{t("auth.passwordRulesTitle")}</p>
        <p className="theme-muted mt-1">{t("auth.passwordRulesBody")}</p>
      </div>

      <button
        type="button"
        onClick={() => void handleSubmit()}
        disabled={loading || !token}
        className={`w-full rounded-xl bg-gradient-to-r py-3 font-semibold text-white shadow-lg outline-none transition-[background,box-shadow,opacity,ring] duration-200 disabled:cursor-not-allowed disabled:opacity-50 ${buttonClass}`}
      >
        {loading ? t("auth.resettingPassword") : t("auth.resetPassword")}
      </button>

      <p className="theme-muted text-center text-sm">
        <Link href="/auth/login" className={`font-semibold transition ${linkClass}`}>
          {t("auth.backToLogin")}
        </Link>
      </p>
    </div>
  );
}
