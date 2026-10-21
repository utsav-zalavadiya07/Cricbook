import DashboardLayout from "@/modules/turf/components/DashboardLayout";
import WalletCenter from "@/modules/wallet/components/WalletCenter";

export default function OwnerWalletPage() {
  return (
    <DashboardLayout>
      <WalletCenter portal="owner" />
    </DashboardLayout>
  );
}
