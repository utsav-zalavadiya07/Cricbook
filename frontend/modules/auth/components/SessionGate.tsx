"use client";

import { useEffect, useMemo } from "react";
import { usePathname, useRouter } from "next/navigation";
import { canRoleAccessPath, getRouteAccess } from "@/modules/auth/sessionPolicy";
import { useAuthStore } from "@/store/authStore";

export default function SessionGate({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const { hydrated, token, role } = useAuthStore();
  const access = useMemo(() => getRouteAccess(pathname), [pathname]);
  const allowed = access.public || Boolean(token && canRoleAccessPath(role, pathname));

  useEffect(() => {
    if (!hydrated || access.public || allowed) {
      return;
    }

    const next = `${pathname}${window.location.search}`;
    router.replace(`${access.loginPath}?next=${encodeURIComponent(next)}`);
  }, [access.loginPath, access.public, allowed, hydrated, pathname, router]);

  if (access.public) {
    return <>{children}</>;
  }

  if (!hydrated || !allowed) {
    return (
      <main className="theme-page-player flex min-h-screen items-center justify-center p-6">
        <div className="theme-panel max-w-md rounded-3xl p-6 text-center">
          <p className="theme-soft text-xs font-black uppercase tracking-[0.24em]">Secure Session</p>
          <h1 className="theme-title mt-3 text-2xl font-black">Checking access...</h1>
          <p className="theme-muted mt-2 text-sm">Protected pages load only after a valid Cricbook session is available.</p>
        </div>
      </main>
    );
  }

  return <>{children}</>;
}
