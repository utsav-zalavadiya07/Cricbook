import { TileInfo } from "@/components/ui";
import NotificationList from "@/modules/notification/components/NotificationList";

export const metadata = { title: "Notifications | Cricbook" };

export default function NotificationsPage() {
  return (
    <main className="theme-page-player min-h-screen px-4 py-6 sm:px-6 lg:px-8">
      <div className="w-full space-y-4">
        <div>
          <h1 className="theme-title text-2xl font-black tracking-tight sm:text-3xl">Notifications
              <span className="inline-flex align-middle ml-3 mb-1">
                <TileInfo label="Notifications information" focusable={false}>Latest updates first.</TileInfo>
              </span></h1>
        </div>
        <NotificationList />
      </div>
    </main>
  );
}
