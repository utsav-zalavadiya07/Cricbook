"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useI18n } from "@/modules/i18n/I18nProvider";
import { getLandingPathForRole } from "@/modules/auth/sessionPolicy";
import { useAuthStore } from "@/store/authStore";
import { loginUser } from "../services/authService";

export type LoginRole = "USER" | "OWNER";

const REMEMBER_LOGIN_KEY = "cricbook.rememberLogin";
const REMEMBER_LOGIN_ID_KEY = "cricbook.rememberLoginId";
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const MOBILE_PATTERN = /^[0-9]{10}$/;

function MailIcon() {
  return (
    <svg aria-hidden="true" className="h-5 w-5" viewBox="0 0 24 24" fill="none">
      <path d="M4.75 6.75h14.5v10.5H4.75V6.75Z" stroke="currentColor" strokeWidth="1.7" strokeLinejoin="round" />
      <path d="m5.25 7.25 6.4 5.2a.55.55 0 0 0 .7 0l6.4-5.2" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" />
    </svg>
  );
}

function LockIcon() {
  return (
    <svg aria-hidden="true" className="h-5 w-5" viewBox="0 0 24 24" fill="none">
      <path d="M7.75 10.5V8.35a4.25 4.25 0 0 1 8.5 0v2.15" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" />
      <path d="M6.5 10.5h11A1.5 1.5 0 0 1 19 12v5.25a1.5 1.5 0 0 1-1.5 1.5h-11A1.5 1.5 0 0 1 5 17.25V12a1.5 1.5 0 0 1 1.5-1.5Z" stroke="currentColor" strokeWidth="1.7" strokeLinejoin="round" />
      <path d="M12 14v1.6" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" />
    </svg>
  );
}

function UserIcon() {
  return (
    <svg aria-hidden="true" className="h-5 w-5" viewBox="0 0 24 24" fill="none">
      <path d="M12 12.35a3.8 3.8 0 1 0 0-7.6 3.8 3.8 0 0 0 0 7.6Z" stroke="currentColor" strokeWidth="1.7" />
      <path d="M5.6 19.25c.72-3.08 3-4.62 6.4-4.62s5.68 1.54 6.4 4.62" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" />
    </svg>
  );
}

function ShieldIcon() {
  return (
    <svg aria-hidden="true" className="h-5 w-5" viewBox="0 0 24 24" fill="none">
      <path d="M12 3.75 18.25 6v4.65c0 4.05-2.5 7.55-6.25 9.05-3.75-1.5-6.25-5-6.25-9.05V6L12 3.75Z" stroke="currentColor" strokeWidth="1.7" strokeLinejoin="round" />
      <path d="m9.25 12 1.8 1.8 3.9-4.15" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

function EyeIcon({ hidden }: { hidden: boolean }) {
  return (
    <svg aria-hidden="true" className="h-5 w-5" viewBox="0 0 24 24" fill="none">
      {hidden ? <path d="m4 4 16 16" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" /> : null}
      <path d="M3.75 12s2.9-5.25 8.25-5.25S20.25 12 20.25 12s-2.9 5.25-8.25 5.25S3.75 12 3.75 12Z" stroke="currentColor" strokeWidth="1.7" strokeLinejoin="round" />
      <path d="M12 14.35a2.35 2.35 0 1 0 0-4.7 2.35 2.35 0 0 0 0 4.7Z" stroke="currentColor" strokeWidth="1.7" />
    </svg>
  );
}

function CheckIcon() {
  return (
    <svg aria-hidden="true" className="h-3.5 w-3.5" viewBox="0 0 16 16" fill="none">
      <path d="m3.25 8.25 3 3 6.5-6.75" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

function loginErrorMessage(error: unknown, fallback: string) {
  const apiError = error as { response?: { status?: number; data?: { error?: string; message?: string } } };
  const status = apiError.response?.status;
  const rawError = apiError.response?.data?.error?.trim();
  const rawMessage = apiError.response?.data?.message?.trim();

  if (status === 401 || status === 403 || rawError === "Unauthorized") {
    return fallback;
  }

  return rawMessage || rawError || fallback;
}

export default function LoginForm({
  selectedRole,
  onRoleChange,
}: {
  selectedRole: LoginRole;
  onRoleChange: (role: LoginRole) => void;
}) {
  const router = useRouter();
  const { t } = useI18n();
  const setAuth = useAuthStore((state) => state.setAuth);

  const [form, setForm] = useState({ username: "", password: "" });
  const [errors, setErrors] = useState({ username: "", password: "" });
  const [loading, setLoading] = useState(false);
  const [serverError, setServerError] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [rememberMe, setRememberMe] = useState(false);

  useEffect(() => {
    const frame = window.requestAnimationFrame(() => {
      const remembered = window.localStorage.getItem(REMEMBER_LOGIN_KEY) === "true";
      const rememberedId = window.localStorage.getItem(REMEMBER_LOGIN_ID_KEY) ?? "";
      setRememberMe(remembered);
      if (rememberedId) {
        setForm((current) => ({ ...current, username: rememberedId }));
      }
    });

    return () => window.cancelAnimationFrame(frame);
  }, []);

  const validate = () => {
    const username = form.username.trim();
    const isNum = /^\d+$/.test(username);
    let usernameError = "";

    if (!username) {
      usernameError = t("auth.usernameRequired");
    } else if (isNum) {
      usernameError = MOBILE_PATTERN.test(username) ? "" : t("auth.mobileInvalid");
    } else {
      usernameError = EMAIL_PATTERN.test(username) ? "" : t("auth.usernameInvalid");
    }

    const nextErrors = {
      username: usernameError,
      password: form.password.trim() ? "" : t("auth.passwordRequired"),
    };
    setErrors(nextErrors);
    return !nextErrors.username && !nextErrors.password;
  };

  const handleLogin = async () => {
    if (!validate()) {
      return;
    }

    setLoading(true);
    setServerError("");

    try {
      const username = form.username.trim();
      const res = await loginUser({ ...form, username, rememberMe });
      const canUseSelectedPortal = res.data.role === selectedRole || (selectedRole === "OWNER" && res.data.role === "ADMIN");

      if (!canUseSelectedPortal) {
        setServerError(
          t("auth.notRegisteredAs", {
            role: selectedRole === "USER" ? t("auth.role.player") : t("auth.role.owner"),
          })
        );
        setLoading(false);
        return;
      }

      if (rememberMe) {
        window.localStorage.setItem(REMEMBER_LOGIN_KEY, "true");
        window.localStorage.setItem(REMEMBER_LOGIN_ID_KEY, username);
      } else {
        window.localStorage.removeItem(REMEMBER_LOGIN_KEY);
        window.localStorage.removeItem(REMEMBER_LOGIN_ID_KEY);
      }

      await setAuth({ email: username }, res.data.accessToken, res.data.role, res.data.refreshToken, { remember: rememberMe });

      router.replace(getLandingPathForRole(res.data.role));
    } catch (error: unknown) {
      setServerError(loginErrorMessage(error, t("auth.invalidCredentials")));
    } finally {
      setLoading(false);
    }
  };

  const borderClass = selectedRole === "USER" ? "focus:border-emerald-500 focus:ring-emerald-500" : "focus:border-sky-500 focus:ring-sky-500";
  const gradientClass =
    selectedRole === "USER"
      ? "from-emerald-500 to-teal-600 hover:from-emerald-400 hover:to-teal-500 shadow-emerald-500/20 hover:shadow-emerald-500/30 hover:ring-4 hover:ring-emerald-400/20 focus-visible:ring-4 focus-visible:ring-emerald-300/40"
      : "from-sky-500 to-blue-600 hover:from-sky-400 hover:to-blue-500 shadow-sky-500/20 hover:shadow-sky-500/30 hover:ring-4 hover:ring-sky-400/20 focus-visible:ring-4 focus-visible:ring-sky-300/40";
  const focusIconClass = selectedRole === "USER" ? "group-focus-within:text-emerald-500" : "group-focus-within:text-sky-500";
  const passwordToggleClass = selectedRole === "USER" ? "hover:text-emerald-300" : "hover:text-sky-300";
  const accentTextClass = selectedRole === "USER" ? "text-emerald-300" : "text-sky-300";

  return (
    <div className="space-y-4">
      <div className="auth-role-switch flex rounded-2xl p-1">
        <button
          type="button"
          onClick={() => onRoleChange("USER")}
          className={`flex min-h-12 flex-1 items-center justify-center gap-2 rounded-xl px-3 py-2.5 text-sm font-black transition-all duration-200 ${
            selectedRole === "USER" ? "bg-gradient-to-r from-emerald-400 to-emerald-600 text-white shadow-lg shadow-emerald-500/20" : "auth-copy-muted hover:bg-emerald-500/10 hover:text-emerald-500"
          }`}
        >
          <span className="inline-flex h-6 w-6 items-center justify-center rounded-full border border-current/25">
            <UserIcon />
          </span>
          <span className="break-words">{t("auth.role.player")}</span>
        </button>
        <button
          type="button"
          onClick={() => onRoleChange("OWNER")}
          className={`flex min-h-12 flex-1 items-center justify-center gap-2 rounded-xl px-3 py-2.5 text-sm font-black transition-all duration-200 ${
            selectedRole === "OWNER" ? "bg-gradient-to-r from-sky-400 to-blue-600 text-white shadow-lg shadow-sky-500/20" : "auth-copy-muted hover:bg-sky-500/10 hover:text-sky-500"
          }`}
        >
          <span className="inline-flex h-6 w-6 items-center justify-center rounded-full border border-current/25">
            <ShieldIcon />
          </span>
          <span className="break-words">{t("auth.role.owner")}</span>
        </button>
      </div>

      <p className={`text-sm leading-5 transition-colors duration-300 ${selectedRole === "USER" ? "text-emerald-400/80" : "text-sky-400/80"}`}>
        {selectedRole === "OWNER" ? t("auth.ownerPitch") : t("auth.playerPitch")}
      </p>

      {serverError && (
        <div role="alert" className="animate-in fade-in slide-in-from-top-1 rounded-xl border border-red-500/30 bg-red-500/10 px-4 py-2.5 text-sm text-red-400 duration-200">
          {serverError}
        </div>
      )}

      <div>
        <label className="auth-copy-primary mb-1.5 block text-sm font-black">{t("auth.usernameLabel")}</label>
        <div className="group relative">
          <div className={`auth-copy-soft absolute left-4 top-1/2 inline-flex -translate-y-1/2 items-center justify-center transition-colors duration-200 ${focusIconClass}`}>
            <MailIcon />
          </div>
          <input
            type="text"
            placeholder={t("auth.usernamePlaceholder")}
            value={form.username}
            onChange={(event) => setForm({ ...form, username: event.target.value })}
            aria-invalid={Boolean(errors.username)}
            aria-describedby={errors.username ? "login-username-error" : undefined}
            className={`auth-form-field min-h-12 w-full rounded-2xl border py-3 pl-12 pr-4 text-sm outline-none focus:ring-1 transition-all duration-200 ${errors.username ? "border-red-500/50" : ""} ${borderClass}`}
          />
        </div>
        {errors.username && (
          <p id="login-username-error" className="mt-1 text-xs text-red-400">
            {errors.username}
          </p>
        )}
      </div>

      <div>
        <label className="auth-copy-primary mb-1.5 block text-sm font-black">{t("auth.passwordLabel")}</label>
        <div className="group relative">
          <div className={`auth-copy-soft absolute left-4 top-1/2 inline-flex -translate-y-1/2 items-center justify-center transition-colors duration-200 ${focusIconClass}`}>
            <LockIcon />
          </div>
          <input
            type={showPassword ? "text" : "password"}
            placeholder={t("auth.passwordPlaceholder")}
            value={form.password}
            onChange={(event) => setForm({ ...form, password: event.target.value })}
            onKeyDown={(event) => event.key === "Enter" && void handleLogin()}
            aria-invalid={Boolean(errors.password)}
            aria-describedby={errors.password ? "login-password-error" : undefined}
            className={`auth-form-field min-h-12 w-full rounded-2xl border py-3 pl-12 pr-14 text-sm outline-none focus:ring-1 transition-all duration-200 ${errors.password ? "border-red-500/50" : ""} ${borderClass}`}
          />
          <button
            type="button"
            onClick={() => setShowPassword((value) => !value)}
            className={`auth-copy-soft absolute right-4 top-1/2 -translate-y-1/2 rounded-lg p-1.5 ${passwordToggleClass}`}
            aria-label={showPassword ? t("auth.hidePassword") : t("auth.showPassword")}
          >
            <EyeIcon hidden={!showPassword} />
          </button>
        </div>
        {errors.password && (
          <p id="login-password-error" className="mt-1 text-xs text-red-400">
            {errors.password}
          </p>
        )}
      </div>

      <div className="flex flex-wrap items-center justify-between gap-3">
        <label className="auth-copy-primary inline-flex cursor-pointer items-center gap-3 text-sm font-semibold">
          <span className={`auth-checkbox-box ${rememberMe ? (selectedRole === "USER" ? "auth-checkbox-player" : "auth-checkbox-owner") : ""}`}>
            <input type="checkbox" checked={rememberMe} onChange={(event) => setRememberMe(event.target.checked)} className="sr-only" />
            {rememberMe ? <CheckIcon /> : null}
          </span>
          {t("auth.rememberMe")}
        </label>
        <Link href="/auth/forgot-password" className={`text-sm font-semibold transition ${accentTextClass} hover:opacity-80`}>
          {t("auth.forgotPassword")}
        </Link>
      </div>

      <button
        type="button"
        onClick={() => void handleLogin()}
        disabled={loading}
        className={`flex min-h-12 w-full items-center justify-center rounded-2xl bg-gradient-to-r py-3 font-black text-white shadow-lg outline-none transition-[background,box-shadow,opacity,ring] duration-200 disabled:cursor-not-allowed disabled:opacity-50 ${gradientClass}`}
      >
        {loading ? (
          <span className="flex items-center justify-center gap-2">
            <svg className="h-4 w-4 animate-spin" viewBox="0 0 24 24" fill="none">
              <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
              <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z" />
            </svg>
            {t("auth.signingIn")}
          </span>
        ) : (
          <span>
            {t("auth.signInAs", {
              role: selectedRole === "USER" ? t("auth.role.player") : t("auth.role.owner"),
            })}
          </span>
        )}
      </button>

      <p className="auth-copy-soft text-center text-sm">
        {t("auth.registerPrompt")}{" "}
        <Link href="/auth/register" className={`${selectedRole === "USER" ? "text-emerald-400 hover:text-emerald-300" : "text-sky-400 hover:text-sky-300"} font-medium transition`}>
          {t("auth.register")}
        </Link>
      </p>
    </div>
  );
}
