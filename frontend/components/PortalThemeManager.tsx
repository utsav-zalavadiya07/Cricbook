"use client";

import { useEffect } from "react";
import { usePortal } from "@/hooks/usePortal";

export default function PortalThemeManager() {
  const { isOwner } = usePortal();

  useEffect(() => {
    const portal = isOwner ? "owner" : "player";
    document.documentElement.dataset.portal = portal;
    document.body.dataset.portal = portal;
  }, [isOwner]);

  return null;
}
