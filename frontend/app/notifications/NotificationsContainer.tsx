"use client";

import { usePortal } from "@/hooks/usePortal";
import NotificationList from "@/modules/notification/components/NotificationList";

export default function NotificationsContainer() {
  const { isOwner } = usePortal();

  return (
    <main
      className={`${
        isOwner ? "theme-page-owner" : "theme-page-player"
      } min-h-screen px-4 py-6 sm:px-6 lg:px-8`}
    >
      <div className="w-full">
        <NotificationList />
      </div>
    </main>
  );
}
