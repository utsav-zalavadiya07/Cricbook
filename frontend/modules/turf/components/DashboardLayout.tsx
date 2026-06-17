export default function DashboardLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="theme-page-owner min-h-screen">
      <main className="mx-auto min-w-0 max-w-[1500px] p-4 sm:p-6 lg:p-8">{children}</main>
    </div>
  );
}
