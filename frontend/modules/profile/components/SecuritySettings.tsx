"use client";

import { useState } from "react";
import { changePassword, deactivateAccount } from "../services/profileService";
import { useAuthStore } from "@/store/authStore";

type SecuritySettingsProps = {
  userEmail?: string;
  accent?: "sky" | "emerald";
};

export default function SecuritySettings({ userEmail, accent = "sky" }: SecuritySettingsProps) {
  const logout = useAuthStore((s) => s.logout);
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [showCurrent, setShowCurrent] = useState(false);
  const [showNew, setShowNew] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);

  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  const [deactivateOpen, setDeactivateOpen] = useState(false);
  const [deactivatePassword, setDeactivatePassword] = useState("");
  const [deactivateLoading, setDeactivateLoading] = useState(false);
  const [deactivateError, setDeactivateError] = useState("");

  const handlePasswordSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    setMessage("");

    if (!currentPassword || !newPassword || !confirmPassword) {
      setError("Please fill in all password fields.");
      return;
    }

    if (newPassword.length < 6) {
      setError("New password must be at least 6 characters long.");
      return;
    }

    if (newPassword !== confirmPassword) {
      setError("New password and confirm password do not match.");
      return;
    }

    setLoading(true);
    try {
      await changePassword({ currentPassword, newPassword, confirmPassword });
      setMessage("Password changed successfully!");
      setCurrentPassword("");
      setNewPassword("");
      setConfirmPassword("");
    } catch (err: unknown) {
      const apiErr = err as { response?: { data?: { message?: string } } };
      setError(apiErr.response?.data?.message || "Failed to change password. Please check your current password.");
    } finally {
      setLoading(false);
    }
  };

  const handleDeactivate = async () => {
    if (!deactivatePassword) {
      setDeactivateError("Please enter your password to confirm account deactivation.");
      return;
    }

    setDeactivateLoading(true);
    setDeactivateError("");
    try {
      await deactivateAccount({ password: deactivatePassword });
      logout();
      window.location.href = "/auth/login";
    } catch (err: unknown) {
      const apiErr = err as { response?: { data?: { message?: string } } };
      setDeactivateError(apiErr.response?.data?.message || "Failed to deactivate account. Incorrect password.");
    } finally {
      setDeactivateLoading(false);
    }
  };

  const inputCls = `theme-input w-full rounded-xl px-4 py-3 text-sm outline-none transition ${
    accent === "sky"
      ? "focus:border-sky-500 focus:ring-1 focus:ring-sky-500"
      : "focus:border-emerald-500 focus:ring-1 focus:ring-emerald-500"
  }`;

  return (
    <div className="space-y-6">
      {/* Password Change Card */}
      <div className="theme-panel overflow-hidden rounded-2xl border border-[var(--theme-border)]">
        <div className="border-b border-[var(--theme-border)] px-5 py-4 sm:px-6">
          <div className="flex items-center gap-2">
            <span className={`flex h-8 w-8 items-center justify-center rounded-lg ${accent === "sky" ? "bg-sky-500/15 text-sky-400" : "bg-emerald-500/15 text-emerald-400"}`}>
              <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
              </svg>
            </span>
            <div>
              <h3 className="theme-title text-lg font-black">Change Password</h3>
              <p className="theme-muted text-xs">Ensure your account uses a strong, unique password.</p>
            </div>
          </div>
        </div>

        <form onSubmit={handlePasswordSubmit} className="space-y-4 p-5 sm:p-6">
          {message && (
            <div className="rounded-xl border border-emerald-500/30 bg-emerald-500/10 px-4 py-3 text-sm font-semibold text-emerald-400">
              {message}
            </div>
          )}
          {error && (
            <div className="rounded-xl border border-red-500/30 bg-red-500/10 px-4 py-3 text-sm font-semibold text-red-400">
              {error}
            </div>
          )}

          <div>
            <label className="theme-muted mb-1.5 block text-xs font-bold uppercase tracking-wider">Current Password</label>
            <div className="relative">
              <input
                type={showCurrent ? "text" : "password"}
                className={inputCls}
                value={currentPassword}
                onChange={(e) => setCurrentPassword(e.target.value)}
                placeholder="Enter current password"
                autoComplete="current-password"
              />
              <button
                type="button"
                onClick={() => setShowCurrent(!showCurrent)}
                className="theme-soft hover:theme-text absolute right-3.5 top-1/2 -translate-y-1/2 text-xs font-medium"
              >
                {showCurrent ? "Hide" : "Show"}
              </button>
            </div>
          </div>

          <div className="grid gap-4 md:grid-cols-2">
            <div>
              <label className="theme-muted mb-1.5 block text-xs font-bold uppercase tracking-wider">New Password</label>
              <div className="relative">
                <input
                  type={showNew ? "text" : "password"}
                  className={inputCls}
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  placeholder="At least 6 characters"
                  autoComplete="new-password"
                />
                <button
                  type="button"
                  onClick={() => setShowNew(!showNew)}
                  className="theme-soft hover:theme-text absolute right-3.5 top-1/2 -translate-y-1/2 text-xs font-medium"
                >
                  {showNew ? "Hide" : "Show"}
                </button>
              </div>
            </div>

            <div>
              <label className="theme-muted mb-1.5 block text-xs font-bold uppercase tracking-wider">Confirm New Password</label>
              <div className="relative">
                <input
                  type={showConfirm ? "text" : "password"}
                  className={inputCls}
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  placeholder="Repeat new password"
                  autoComplete="new-password"
                />
                <button
                  type="button"
                  onClick={() => setShowConfirm(!showConfirm)}
                  className="theme-soft hover:theme-text absolute right-3.5 top-1/2 -translate-y-1/2 text-xs font-medium"
                >
                  {showConfirm ? "Hide" : "Show"}
                </button>
              </div>
            </div>
          </div>

          <div className="flex justify-end pt-2">
            <button
              type="submit"
              disabled={loading}
              className={`rounded-xl px-5 py-2.5 text-sm font-bold text-white shadow-sm transition disabled:opacity-50 ${
                accent === "sky" ? "bg-sky-500 hover:bg-sky-400" : "bg-emerald-500 hover:bg-emerald-400"
              }`}
            >
              {loading ? "Updating..." : "Update Password"}
            </button>
          </div>
        </form>
      </div>

      {/* Account Security Information Card */}
      <div className="theme-panel rounded-2xl border border-[var(--theme-border)] p-5 sm:p-6">
        <h4 className="theme-title text-base font-bold">Account Security Overview</h4>
        <p className="theme-muted mt-1 text-xs">Security parameters and verified contact channels.</p>

        <div className="mt-4 grid gap-3 sm:grid-cols-2">
          <div className="theme-panel-soft flex items-center justify-between rounded-xl p-3.5">
            <div>
              <p className="theme-soft text-xs font-bold uppercase tracking-wider">Login Email</p>
              <p className="theme-title mt-0.5 text-sm font-semibold">{userEmail || "Not provided"}</p>
            </div>
            <span className="rounded-full bg-emerald-500/15 px-2.5 py-0.5 text-[11px] font-bold text-emerald-300">
              Verified
            </span>
          </div>

          <div className="theme-panel-soft flex items-center justify-between rounded-xl p-3.5">
            <div>
              <p className="theme-soft text-xs font-bold uppercase tracking-wider">Session Protection</p>
              <p className="theme-title mt-0.5 text-sm font-semibold">Active JWT encryption</p>
            </div>
            <span className="rounded-full bg-sky-500/15 px-2.5 py-0.5 text-[11px] font-bold text-sky-300">
              Protected
            </span>
          </div>
        </div>
      </div>

      {/* Danger Zone Card */}
      <div className="theme-panel rounded-2xl border border-red-500/20 bg-red-500/[0.03] p-5 sm:p-6">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h4 className="text-base font-bold text-red-400">Deactivate Account</h4>
            <p className="theme-muted mt-1 text-xs">
              Temporarily deactivate your account and revoke active venue and booking listings.
            </p>
          </div>
          <button
            type="button"
            onClick={() => setDeactivateOpen(!deactivateOpen)}
            className="rounded-xl border border-red-500/30 bg-red-500/10 px-4 py-2 text-xs font-bold text-red-400 transition hover:bg-red-500/20"
          >
            {deactivateOpen ? "Cancel" : "Deactivate..."}
          </button>
        </div>

        {deactivateOpen && (
          <div className="mt-4 border-t border-red-500/20 pt-4 animate-in fade-in">
            <p className="text-xs text-red-300">
              Please enter your current password to confirm account deactivation:
            </p>
            <div className="mt-2 flex flex-col gap-2 sm:flex-row sm:items-center">
              <input
                type="password"
                className="theme-input max-w-sm rounded-xl px-4 py-2 text-sm outline-none focus:border-red-500"
                placeholder="Confirm password"
                value={deactivatePassword}
                onChange={(e) => setDeactivatePassword(e.target.value)}
              />
              <button
                type="button"
                onClick={handleDeactivate}
                disabled={deactivateLoading}
                className="rounded-xl bg-red-600 px-4 py-2 text-sm font-bold text-white transition hover:bg-red-500 disabled:opacity-50"
              >
                {deactivateLoading ? "Deactivating..." : "Confirm Deactivation"}
              </button>
            </div>
            {deactivateError && <p className="mt-2 text-xs text-red-400 font-semibold">{deactivateError}</p>}
          </div>
        )}
      </div>
    </div>
  );
}
