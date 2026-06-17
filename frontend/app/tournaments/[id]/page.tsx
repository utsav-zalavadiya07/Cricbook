import { TournamentDashboardClient } from "@/modules/tournament/components/TournamentDashboardClient";

export default function TournamentDetailPage({
  params,
}: {
  params: { id: string };
}) {
  return <TournamentDashboardClient tournamentId={Number(params.id)} />;
}
