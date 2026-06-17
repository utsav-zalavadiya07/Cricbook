import { redirect } from "next/navigation";

export default async function EditTurfRedirectPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  redirect(`/turf/my-turfs?edit=${id}`);
}
