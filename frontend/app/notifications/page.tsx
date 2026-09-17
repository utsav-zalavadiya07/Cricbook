import { TileInfo } from "@/components/ui";
import NotificationList from "@/modules/notification/components/NotificationList";

export const metadata = { title: "Notifications | Cricbook" };

export default function NotificationsPage() {
  return (
    <main className="theme-page-player min-h-screen px-4 py-6 sm:px-6 lg:px-8">
      <div className="w-full">
        <NotificationList />
      </div>
    </main>
  );
}
