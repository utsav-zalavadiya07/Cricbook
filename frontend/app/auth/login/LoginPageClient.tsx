"use client";

import { useEffect, useState } from "react";
import AuthCard from "@/modules/auth/components/AuthCard";
import LoginForm, { LoginRole } from "@/modules/auth/components/LoginForm";
import { getLandingPathForRole } from "@/modules/auth/sessionPolicy";
import { useAuthStore } from "@/store/authStore";

export default function LoginPageClient() {
  const { hydrated, token, role } = useAuthStore();
  const [selectedRole, setSelectedRole] = useState<LoginRole>("USER");
  const [redirecting, setRedirecting] = useState(true);

  useEffect(() => {
    const frame = window.requestAnimationFrame(() => {
      const storedRole = window.localStorage.getItem("loginPortalRole");
      if (storedRole === "OWNER" || storedRole === "USER") {
        setSelectedRole(storedRole);
      }
    });

    return () => window.cancelAnimationFrame(frame);
  }, []);

  useEffect(() => {
    if (!hydrated) {
      return;
    }

    if (!token || !role) {
      setRedirecting(false);
      return;
    }

    const landingPath = getLandingPathForRole(role);
    window.location.replace(landingPath);
  }, [hydrated, role, token]);

  const handleRoleChange = (role: LoginRole) => {
    setSelectedRole(role);
    window.localStorage.setItem("loginPortalRole", role);
    window.dispatchEvent(new CustomEvent("loginPortalRoleChange", { detail: role }));
  };

  if (!hydrated || redirecting) {
    return (
      <div className="theme-page-player flex min-h-[calc(100vh-84px)] items-center justify-center px-4 py-8">
        <div className="theme-panel max-w-md rounded-3xl p-6 text-center">
          <p className="theme-soft text-xs font-black uppercase tracking-[0.24em]">Secure Session</p>
          <h1 className="theme-title mt-3 text-2xl font-black">{hydrated && token && role ? "Redirecting..." : "Checking access..."}</h1>
          <p className="theme-muted mt-2 text-sm">
            {hydrated && token && role
              ? "A valid session is already available in this browser."
              : "We are checking whether this browser already has a valid Cricbook session."}
          </p>
        </div>
      </div>
    );
  }

  return (
    <AuthCard title="Welcome back" portalRole={selectedRole}>
      <LoginForm selectedRole={selectedRole} onRoleChange={handleRoleChange} />
    </AuthCard>
  );
}
