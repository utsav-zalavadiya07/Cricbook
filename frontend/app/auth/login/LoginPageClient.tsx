"use client";

import { useEffect, useState } from "react";
import AuthCard from "@/modules/auth/components/AuthCard";
import LoginForm, { LoginRole } from "@/modules/auth/components/LoginForm";

export default function LoginPageClient() {
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

  const handleRoleChange = (role: LoginRole) => {
    setSelectedRole(role);
    window.localStorage.setItem("loginPortalRole", role);
    window.dispatchEvent(new CustomEvent("loginPortalRoleChange", { detail: role }));
  };

  return (
    <AuthCard title="Welcome back" portalRole={selectedRole}>
      <LoginForm selectedRole={selectedRole} onRoleChange={handleRoleChange} />
    </AuthCard>
  );
}
