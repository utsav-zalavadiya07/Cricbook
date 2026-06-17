"use client";

import { useEffect, useState } from "react";
import AuthCard from "@/modules/auth/components/AuthCard";
import ForgotPasswordForm from "@/modules/auth/components/ForgotPasswordForm";
import type { LoginRole } from "@/modules/auth/components/LoginForm";

export default function ForgotPasswordPageClient() {
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
      title="Reset your password"
      subtitle={
        selectedRole === "OWNER"
          ? "Recover access to your turf business dashboard."
          : "Enter your registered email or mobile number."
      }
      portalRole={selectedRole}
    >
      <ForgotPasswordForm portalRole={selectedRole} />
    </AuthCard>
  );
}
