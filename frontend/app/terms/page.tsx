export const metadata = { title: "Terms of Service | Cricbook" };

export default function TermsPage() {
  return (
    <main className="theme-page min-h-screen px-4 py-10 sm:px-6">
      <section className="mx-auto max-w-4xl rounded-3xl border border-[var(--theme-border)] bg-[var(--theme-card)] p-6 shadow-xl sm:p-8">
        <p className="text-sm font-black uppercase tracking-[0.24em] text-sky-400">Cricbook Legal</p>
        <h1 className="theme-title mt-3 text-4xl font-black tracking-tight">Terms of Service</h1>
        <p className="theme-muted mt-3 leading-7">
          These terms outline the expected use of Cricbook for turf booking, owner operations, payments, tournaments, and related platform features.
        </p>
        <div className="mt-8 grid gap-5">
          {[
            ["Account responsibility", "Keep your login details secure, provide accurate profile information, and use the correct player or owner portal for your role."],
            ["Bookings and payments", "Bookings, cancellations, refunds, wallet activity, and owner payouts follow the rules displayed in the product at the time of transaction."],
            ["Owner listings", "Owners are responsible for accurate turf details, availability, pricing, photos, service quality, and operational compliance."],
            ["Fair usage", "Do not abuse promotions, referral rewards, reviews, messaging, APIs, or any platform workflow intended for legitimate cricket activity."],
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
