"use client";
import { TileInfo } from "@/components/ui";

import { useEffect, useState } from "react";
import { getMembershipPlans, getMyMemberships, subscribeMembership } from "@/modules/membership/services/membershipService";

type Plan = {
  id: number;
  name: string;
  type: "USER_PREMIUM" | "OWNER_FEATURED";
  price: number;
  durationDays: number;
  discountPercent: number;
  prioritySlots: boolean;
  featuredListing: boolean;
  description?: string;
};

type Membership = {
  id: number;
  planName: string;
  type: "USER_PREMIUM" | "OWNER_FEATURED";
  turfName?: string;
  status: "ACTIVE" | "EXPIRED" | "CANCELLED";
  endsAt: string;
};

export default function MembershipPage() {
  const [plans, setPlans] = useState<Plan[]>([]);
  const [memberships, setMemberships] = useState<Membership[]>([]);
  const [turfIds, setTurfIds] = useState<Record<number, string>>({});
  const [loading, setLoading] = useState(true);
  const [status, setStatus] = useState("");

  const load = () => {
    setLoading(true);
    Promise.all([getMembershipPlans(), getMyMemberships()])
      .then(([planRes, membershipRes]) => {
        setPlans(planRes.data);
        setMemberships(membershipRes.data);
      })
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    let active = true;
    Promise.all([getMembershipPlans(), getMyMemberships()])
      .then(([planRes, membershipRes]) => {
        if (!active) return;
        setPlans(planRes.data);
        setMemberships(membershipRes.data);
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, []);

  const subscribe = (plan: Plan) => {
    const turfId = plan.type === "OWNER_FEATURED" ? Number(turfIds[plan.id]) : undefined;
    if (plan.type === "OWNER_FEATURED" && !turfId) {
      setStatus("Turf id is required for owner featured plans");
      return;
    }
    subscribeMembership(plan.id, turfId)
      .then(() => {
        setStatus("Membership activated");
        load();
      })
      .catch((error) => setStatus(errorMessage(error, "Subscription failed")));
  };

  return (
    <main className="min-h-screen bg-gradient-to-br from-slate-900 via-emerald-950 to-slate-900 px-4 py-8 sm:px-6">
      <div className="mx-auto max-w-7xl space-y-6">
        <div>
          <h1 className="text-3xl font-bold text-white">Membership
              <span className="inline-flex align-middle ml-3 mb-1">
                <TileInfo label="Membership information" focusable={false}>Premium discounts, priority slots and owner featured listings.</TileInfo>
              </span></h1>
        </div>

        {status && <p className="rounded-xl border border-white/10 bg-white/5 px-4 py-3 text-sm text-emerald-200">{status}</p>}
        {loading && <p className="py-10 text-center text-slate-400">Loading memberships...</p>}

        {!loading && (
          <>
            <section className="grid gap-4 md:grid-cols-3">
              {plans.length === 0 && <Empty text="No plans available. Admin can create plans from API." />}
              {plans.map((plan) => (
                <div key={plan.id} className="rounded-2xl border border-white/10 bg-white/5 p-5">
                  <p className="text-xs font-semibold uppercase tracking-wider text-emerald-300">{plan.type}</p>
                  <h2 className="mt-2 text-xl font-bold text-white">{plan.name}</h2>
                  <p className="mt-2 text-3xl font-black text-white">Rs {plan.price}</p>
                  <p className="mt-2 text-sm text-slate-400">{plan.durationDays} days - {plan.discountPercent}% discount</p>
                  {plan.type === "OWNER_FEATURED" && (
                    <input value={turfIds[plan.id] || ""} onChange={(e) => setTurfIds({ ...turfIds, [plan.id]: e.target.value })} placeholder="Turf id" className="mt-4 w-full rounded-xl border border-white/10 bg-black/20 px-3 py-2 text-sm text-white outline-none" />
                  )}
                  <button onClick={() => subscribe(plan)} className="mt-4 w-full rounded-xl bg-emerald-500 px-4 py-2 text-sm font-bold text-white">Activate</button>
                </div>
              ))}
            </section>

            <section className="rounded-2xl border border-white/10 bg-white/5 p-5">
              <h2 className="text-lg font-bold text-white">My Memberships</h2>
              <div className="mt-4 grid gap-3 md:grid-cols-3">
                {memberships.length === 0 && <Empty text="No memberships yet." />}
                {memberships.map((membership) => (
                  <div key={membership.id} className="rounded-xl border border-white/10 bg-black/20 p-4">
                    <p className="font-semibold text-white">{membership.planName}</p>
                    <p className="mt-1 text-sm text-slate-400">{membership.status} - ends {membership.endsAt?.slice(0, 10)}</p>
                    {membership.turfName && <p className="mt-1 text-xs text-slate-500">{membership.turfName}</p>}
                  </div>
                ))}
              </div>
            </section>
          </>
        )}
      </div>
    </main>
  );
}

function Empty({ text }: { text: string }) {
  return <p className="rounded-xl border border-dashed border-white/10 p-4 text-center text-sm text-slate-400">{text}</p>;
}

function errorMessage(error: unknown, fallback: string) {
  const apiError = error as { response?: { data?: { error?: string } } };
  return apiError.response?.data?.error || fallback;
}
