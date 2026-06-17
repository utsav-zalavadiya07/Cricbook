import NotificationList from "@/modules/notification/components/NotificationList";

export const metadata = { title: "Notifications | Cricbook" };

export default function NotificationsPage() {
  return (
    <main className="theme-page-player min-h-screen px-4 py-6 sm:px-6 lg:px-8">
      <div className="w-full space-y-4">
        <div>
          <h1 className="theme-title text-2xl font-black tracking-tight sm:text-3xl">Notifications</h1>
          <p className="theme-muted mt-1 text-sm">Latest updates first.</p>
        </div>
        <NotificationList />
      </div>
    </main>
  );
}
