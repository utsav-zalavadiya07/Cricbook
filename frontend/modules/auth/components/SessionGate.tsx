"use client";

import { useEffect, useMemo } from "react";
import { usePathname } from "next/navigation";
import { getPortalRedirectPath, getRouteAccess } from "@/modules/auth/sessionPolicy";
import { useAuthStore } from "@/store/authStore";

export default function SessionGate({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const { hydrated, token, role } = useAuthStore();
  const access = useMemo(() => getRouteAccess(pathname), [pathname]);
  const portalRedirectPath = useMemo(
    () => token && role ? getPortalRedirectPath(role, pathname) : null,
    [pathname, role, token]
  );
  const allowed = access.public || Boolean(token && role && !portalRedirectPath);

  useEffect(() => {
    if (!hydrated || access.public) {
      return;
    }

    if (!token || !role) {
      window.location.replace(access.loginPath);
      return;
    }

    if (portalRedirectPath) {
      window.location.replace(portalRedirectPath);
    }
  }, [access.loginPath, access.public, hydrated, portalRedirectPath, role, token]);

  if (access.public) {
    return <>{children}</>;
  }

  if (!hydrated || !allowed) {
    return (
      <main className="theme-page-player flex min-h-screen items-center justify-center p-6">
        <div className="theme-panel max-w-md rounded-3xl p-6 text-center">
          <p className="theme-soft text-xs font-black uppercase tracking-[0.24em]">Secure Session</p>
          <h1 className="theme-title mt-3 text-2xl font-black">
            {portalRedirectPath ? "Opening your portal..." : "Checking access..."}
          </h1>
          <p className="theme-muted mt-2 text-sm">
            {portalRedirectPath
              ? "This page belongs to another portal. Taking you to the correct dashboard."
              : "Protected pages load only after a valid Cricbook session is available."}
          </p>
        </div>
      </main>
    );
  }

  return <>{children}</>;
}
