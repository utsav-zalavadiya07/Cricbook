import { TileInfo } from "@/components/ui";
export const metadata = { title: "Privacy Policy | Cricbook" };

export default function PrivacyPage() {
  return (
    <main className="theme-page min-h-screen px-4 py-10 sm:px-6">
      <section className="mx-auto max-w-4xl rounded-3xl border border-[var(--theme-border)] bg-[var(--theme-card)] p-6 shadow-xl sm:p-8">
        <p className="text-sm font-black uppercase tracking-[0.24em] text-emerald-400">Cricbook Privacy</p>
        <h1 className="theme-title mt-3 text-4xl font-black tracking-tight">Privacy Policy
              <span className="inline-flex align-middle ml-3 mb-1">
                <TileInfo label="Privacy Policy information" focusable={false}>Cricbook uses account, booking, payment, turf, and engagement data to operate the platform, protect users, and improve sports experiences.</TileInfo>
              </span></h1>
        <div className="mt-8 grid gap-5">
          {[
            ["Data we collect", "We collect details needed for account creation, verification, booking, payments, owner operations, support, security, and notifications."],
            ["How data is used", "Data is used to process bookings, prevent fraud, personalize recommendations, calculate analytics, and communicate platform updates."],
            ["Access and control", "Users can manage profile information, preferences, notification settings, and security controls from account settings."],
            ["Security", "We apply role-based access, authentication controls, audit trails, and platform monitoring to reduce unauthorized access risks."],
          ].map(([title, body]) => (
            <article key={title} className="rounded-2xl border border-[var(--theme-border)] bg-[var(--theme-panel-soft)] p-5">
              <h2 className="theme-title text-lg font-black">{title}</h2>
              <p className="theme-muted mt-2 leading-6">{body}</p>
            </article>
          ))}
        </div>
      </section>
    </main>
  );
}
