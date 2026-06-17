"use client";

import { useEffect, useState } from "react";
import RegisterForm from "@/modules/auth/components/RegisterForm";
import RegisterPortalShell from "@/modules/auth/components/RegisterPortalShell";
import type { LoginRole } from "@/modules/auth/components/LoginForm";

export default function RegisterPageClient() {
  const [selectedRole, setSelectedRole] = useState<LoginRole>("OWNER");

  useEffect(() => {
    const frame = window.requestAnimationFrame(() => {
      const storedRole = window.localStorage.getItem("loginPortalRole");
      if (storedRole === "OWNER" || storedRole === "USER") {
        setSelectedRole(storedRole);
      } else {
        window.localStorage.setItem("loginPortalRole", "OWNER");
        window.dispatchEvent(new CustomEvent("loginPortalRoleChange", { detail: "OWNER" }));
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
    <RegisterPortalShell portalRole={selectedRole}>
      <RegisterForm selectedRole={selectedRole} onRoleChange={handleRoleChange} />
    </RegisterPortalShell>
  );
}
