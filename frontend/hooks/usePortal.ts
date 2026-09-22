"use client";

import { usePathname } from "next/navigation";
import { useAuthStore } from "@/store/authStore";

export type PortalType = "player" | "owner" | "admin";
export type PortalAccent = "player" | "owner";

export function usePortal(): {
  portal: PortalType;
  isOwner: boolean;
  isPlayer: boolean;
  isAdmin: boolean;
  accent: PortalAccent;
} {
  const pathname = usePathname() || "";
  const role = useAuthStore((s) => s.role);

  const isAdmin = pathname.startsWith("/admin") || role === "ADMIN";
  const isOwner =
    !isAdmin &&
    (pathname.startsWith("/owner") ||
      pathname.startsWith("/turf/my-turfs") ||
      pathname.startsWith("/turf/create") ||
      role === "OWNER");

  const portal: PortalType = isAdmin ? "admin" : isOwner ? "owner" : "player";
  const accent: PortalAccent = isOwner ? "owner" : "player";

  return {
    portal,
    isOwner,
    isPlayer: !isOwner && !isAdmin,
    isAdmin,
    accent,
  };
}
