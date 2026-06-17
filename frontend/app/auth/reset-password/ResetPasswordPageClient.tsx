"use client";

import { Suspense, useEffect, useState } from "react";
import AuthCard from "@/modules/auth/components/AuthCard";
import ResetPasswordForm from "@/modules/auth/components/ResetPasswordForm";
import type { LoginRole } from "@/modules/auth/components/LoginForm";

export default function ResetPasswordPageClient() {
  const [selectedRole, setSelectedRole] = useState<LoginRole>("USER");

  useEffect(() => {
    const frame = window.requestAnimationFrame(() => {
      const storedRole = window.localStorage.getItem("loginPortalRole");
      if (storedRole === "OWNER" || storedRole === "USER") {
        setSelectedRole(storedRole);
      }
    });

    return () => window.cancelAnimationFrame(frame);
  }, []);

  return (
    <AuthCard
      title="Create a new password"
      subtitle={
        selectedRole === "OWNER"
          ? "Choose a strong password for your owner account."
          : "Choose a strong password to protect your account."
      }
      portalRole={selectedRole}
    >
      <Suspense fallback={<div className="theme-muted text-sm">Loading...</div>}>
        <ResetPasswordForm portalRole={selectedRole} />
      </Suspense>
    </AuthCard>
  );
}
