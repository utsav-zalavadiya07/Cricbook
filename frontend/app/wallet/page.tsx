import WalletCenter from "@/modules/wallet/components/WalletCenter";

export default function WalletPage() {
  return (
    <main className="theme-page-player min-h-screen px-4 py-6 sm:px-6">
      <div className="mx-auto max-w-[1440px]">
        <WalletCenter portal="player" />
      </div>
    </main>
  );
}
