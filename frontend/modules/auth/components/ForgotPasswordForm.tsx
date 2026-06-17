"use client";

import { useState } from "react";
import Link from "next/link";
import { useI18n } from "@/modules/i18n/I18nProvider";
import { requestPasswordReset } from "../services/authService";
import type { LoginRole } from "./LoginForm";

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const MOBILE_PATTERN = /^[0-9]{10}$/;

function errorMessage(error: unknown, fallback: string) {
  const apiError = error as { response?: { data?: { error?: string; message?: string } } };
  return apiError.response?.data?.error || apiError.response?.data?.message || fallback;
}

interface ForgotPasswordFormProps {
  portalRole?: LoginRole;
}

export default function ForgotPasswordForm({ portalRole = "USER" }: ForgotPasswordFormProps) {
  const { t } = useI18n();
  const [loginId, setLoginId] = useState("");
  const [error, setError] = useState("");
  const [serverMessage, setServerMessage] = useState("");
  const [loading, setLoading] = useState(false);

  const validate = () => {
    const value = loginId.trim();
    if (!value) {
      return t("auth.usernameRequired");
    }
    if (/^\d+$/.test(value)) {
      return MOBILE_PATTERN.test(value) ? "" : t("auth.mobileInvalid");
    }
    return EMAIL_PATTERN.test(value) ? "" : t("auth.usernameInvalid");
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
      const response = await requestPasswordReset({ loginId: loginId.trim() });
      setServerMessage(response.data?.message || t("auth.resetLinkSent"));
    } catch (err) {
      setError(errorMessage(err, t("auth.resetRequestFailed")));
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

  return (
    <div className="space-y-5">
      <div className="theme-card rounded-2xl p-4">
        <p className="theme-title text-sm font-semibold">{t("auth.resetHelpTitle")}</p>
        <p className="theme-muted mt-1 text-sm leading-6">{t("auth.resetHelpBody")}</p>
      </div>

      {serverMessage && (
        <div role="status" className={`rounded-xl border px-4 py-3 text-sm ${successClass}`}>
          {serverMessage}
        </div>
      )}

      <div>
        <label className="theme-muted mb-1.5 block text-sm font-medium">{t("auth.usernameLabel")}</label>
        <input
          type="text"
          value={loginId}
          onChange={(event) => setLoginId(event.target.value)}
          onKeyDown={(event) => event.key === "Enter" && void handleSubmit()}
          placeholder={t("auth.usernamePlaceholder")}
          aria-invalid={Boolean(error)}
          aria-describedby={error ? "forgot-password-login-id-error" : undefined}
          className={`theme-input w-full rounded-xl px-4 py-3 text-sm outline-none transition focus:ring-1 ${focusClass} ${error ? "border-red-500/50" : ""}`}
        />
        {error && (
          <p id="forgot-password-login-id-error" className="mt-1 text-xs text-red-400">
            {error}
          </p>
        )}
      </div>

      <button
        type="button"
        onClick={() => void handleSubmit()}
        disabled={loading}
        className={`w-full rounded-xl bg-gradient-to-r py-3 font-semibold text-white shadow-lg outline-none transition-[background,box-shadow,opacity,ring] duration-200 disabled:cursor-not-allowed disabled:opacity-50 ${buttonClass}`}
      >
        {loading ? t("auth.sendingResetLink") : t("auth.sendResetLink")}
      </button>

      <p className="theme-muted text-center text-sm">
        {t("auth.rememberPassword")}{" "}
        <Link href="/auth/login" className={`font-semibold transition ${linkClass}`}>
          {t("auth.signIn")}
        </Link>
      </p>
    </div>
  );
}
