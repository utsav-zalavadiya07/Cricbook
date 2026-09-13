export default function DashboardLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="theme-page-owner min-h-screen">
      <main className="mx-auto min-w-0 max-w-[1440px] p-3 sm:p-5 lg:p-6">{children}</main>
    </div>
  );
}
