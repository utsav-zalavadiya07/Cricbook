"use client";

import { useEffect, useState } from "react";
import { useConfirmDialog } from "@/components/ui/ConfirmDialog";
import { getAdminPlans, createAdminPlan, updateAdminPlan, deleteAdminPlan } from "@/modules/membership/services/subscriptionService";

type Plan = {
  id: number;
  name: string;
  type: string;
  price: number;
  durationDays: number;
  discountPercent: number;
  prioritySlots: boolean;
  featuredListing: boolean;
  description: string;
};

export default function AdminPlansPage() {
  const [plans, setPlans] = useState<Plan[]>([]);
  const confirmAction = useConfirmDialog();
  const [loading, setLoading] = useState(true);
  const [editingPlan, setEditingPlan] = useState<Partial<Plan> | null>(null);
  const [message, setMessage] = useState<{ text: string; type: "success" | "error" } | null>(null);

  const loadPlans = () => {
    setLoading(true);
    getAdminPlans()
      .then((res) => setPlans(res.data))
      .catch((err) => console.error(err))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    const timer = window.setTimeout(loadPlans, 0);
    return () => window.clearTimeout(timer);
  }, []);

  const handleSave = () => {
    setMessage(null);
    if (!editingPlan?.name) {
      setMessage({ text: "Plan name is required.", type: "error" });
      return;
    }

    const payload = {
      ...editingPlan,
      price: Number(editingPlan.price || 0),
      durationDays: Number(editingPlan.durationDays || 30),
      discountPercent: Number(editingPlan.discountPercent || 0),
      prioritySlots: !!editingPlan.prioritySlots,
      featuredListing: !!editingPlan.featuredListing,
    };

    const action = editingPlan.id
      ? updateAdminPlan(editingPlan.id, payload)
      : createAdminPlan(payload);

    action
      .then(() => {
        setMessage({ text: "Plan saved successfully!", type: "success" });
        setEditingPlan(null);
        loadPlans();
      })
      .catch(() => {
        setMessage({ text: "Failed to save plan.", type: "error" });
      });
  };

  const handleDelete = async (id: number) => {
    const confirmed = await confirmAction({
      title: "Delete this plan?",
      description: "This removes the subscription plan from the admin catalogue. Existing subscriptions should be reviewed before deleting.",
      confirmLabel: "Delete plan",
      tone: "danger",
    });
    if (!confirmed) return;
    deleteAdminPlan(id)
      .then(() => {
        setMessage({ text: "Plan successfully deleted.", type: "success" });
        loadPlans();
      })
      .catch(() => {
        setMessage({ text: "Failed to delete plan.", type: "error" });
      });
  };

  return (
    <main className="min-h-screen bg-gradient-to-b from-slate-950 via-slate-900 to-slate-950 px-4 py-16 sm:px-6 text-white">
      <div className="mx-auto max-w-6xl space-y-8">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-extrabold tracking-tight">Monetization Plans</h1>
            <p className="mt-1.5 text-sm text-slate-400">
              Configure and deploy commercial tiers for users and turf owner segments.
            </p>
          </div>
          <button
            onClick={() => setEditingPlan({ type: "USER_PREMIUM", price: 0, durationDays: 30, discountPercent: 0, prioritySlots: false, featuredListing: false })}
            className="rounded-2xl bg-emerald-500 px-5 py-2.5 text-xs font-bold text-white transition-all hover:bg-emerald-400 shadow-lg shadow-emerald-500/10"
          >
            Create New Plan
          </button>
        </div>

        {message && (
          <div
            className={`rounded-2xl border p-4 text-center text-sm font-medium transition-all ${
              message.type === "success"
                ? "border-emerald-500/20 bg-emerald-500/10 text-emerald-300"
                : "border-red-500/20 bg-red-500/10 text-red-300"
            }`}
          >
            {message.text}
          </div>
        )}

        {/* Modal / Form section */}
        {editingPlan && (
          <div className="rounded-3xl border border-white/10 bg-white/[0.03] p-6 backdrop-blur-xl space-y-4">
            <h2 className="text-lg font-bold">{editingPlan.id ? "Edit Plan" : "Create Plan"}</h2>
            <div className="grid gap-4 sm:grid-cols-2 md:grid-cols-3 text-xs">
              <div>
                <label className="block text-slate-400 mb-1.5 font-semibold">Plan Name</label>
                <input
                  value={editingPlan.name || ""}
                  onChange={(e) => setEditingPlan({ ...editingPlan, name: e.target.value })}
                  placeholder="PRO / BUSINESS"
                  className="w-full rounded-xl border border-white/10 bg-black/40 px-3 py-2 text-white outline-none focus:border-emerald-500"
                />
              </div>
              <div>
                <label className="block text-slate-400 mb-1.5 font-semibold">Price (INR)</label>
                <input
                  type="number"
                  value={editingPlan.price || 0}
                  onChange={(e) => setEditingPlan({ ...editingPlan, price: Number(e.target.value) })}
                  className="w-full rounded-xl border border-white/10 bg-black/40 px-3 py-2 text-white outline-none focus:border-emerald-500"
                />
              </div>
              <div>
                <label className="block text-slate-400 mb-1.5 font-semibold">Duration (Days)</label>
                <input
                  type="number"
                  value={editingPlan.durationDays || 30}
                  onChange={(e) => setEditingPlan({ ...editingPlan, durationDays: Number(e.target.value) })}
                  className="w-full rounded-xl border border-white/10 bg-black/40 px-3 py-2 text-white outline-none focus:border-emerald-500"
                />
              </div>
              <div>
                <label className="block text-slate-400 mb-1.5 font-semibold">Type</label>
                <select
                  value={editingPlan.type || "USER_PREMIUM"}
                  onChange={(e) => setEditingPlan({ ...editingPlan, type: e.target.value })}
                  className="w-full rounded-xl border border-white/10 bg-black/40 px-3 py-2 text-white outline-none focus:border-emerald-500"
                >
                  <option value="USER_PREMIUM">Player Plan</option>
                  <option value="OWNER_FEATURED">Owner Plan</option>
                </select>
              </div>
              <div>
                <label className="block text-slate-400 mb-1.5 font-semibold">Direct Discount (%)</label>
                <input
                  type="number"
                  value={editingPlan.discountPercent || 0}
                  onChange={(e) => setEditingPlan({ ...editingPlan, discountPercent: Number(e.target.value) })}
                  className="w-full rounded-xl border border-white/10 bg-black/40 px-3 py-2 text-white outline-none focus:border-emerald-500"
                />
              </div>
              <div>
                <label className="block text-slate-400 mb-1.5 font-semibold">Description</label>
                <input
                  value={editingPlan.description || ""}
                  onChange={(e) => setEditingPlan({ ...editingPlan, description: e.target.value })}
                  placeholder="Brief plan tagline"
                  className="w-full rounded-xl border border-white/10 bg-black/40 px-3 py-2 text-white outline-none focus:border-emerald-500"
                />
              </div>
            </div>

            <div className="flex items-center gap-6 text-xs mt-4">
              <label className="flex items-center gap-2 text-slate-300 font-semibold cursor-pointer">
                <input
                  type="checkbox"
                  checked={!!editingPlan.prioritySlots}
                  onChange={(e) => setEditingPlan({ ...editingPlan, prioritySlots: e.target.checked })}
                  className="h-4 w-4 rounded border-white/10 bg-black/40 text-emerald-500 outline-none"
                />
                Priority Slots
              </label>
              <label className="flex items-center gap-2 text-slate-300 font-semibold cursor-pointer">
                <input
                  type="checkbox"
                  checked={!!editingPlan.featuredListing}
                  onChange={(e) => setEditingPlan({ ...editingPlan, featuredListing: e.target.checked })}
                  className="h-4 w-4 rounded border-white/10 bg-black/40 text-emerald-500 outline-none"
                />
                Featured Listing
              </label>
            </div>

            <div className="flex justify-end gap-3 pt-4 border-t border-white/5 text-xs">
              <button
                onClick={() => setEditingPlan(null)}
                className="rounded-xl border border-white/10 bg-white/5 px-4 py-2 font-bold hover:bg-white/10"
              >
                Cancel
              </button>
              <button
                onClick={handleSave}
                className="rounded-xl bg-emerald-500 px-5 py-2.5 font-bold hover:bg-emerald-400 shadow-lg shadow-emerald-500/10"
              >
                Save Plan
              </button>
            </div>
          </div>
        )}

        {/* Plans Grid */}
        {loading ? (
          <div className="py-20 text-center text-slate-400 font-semibold animate-pulse">
            Loading active plan configurations...
          </div>
        ) : (
          <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
            {plans.map((p) => (
              <div
                key={p.id}
                className="rounded-3xl border border-white/10 bg-white/[0.03] p-6 shadow-xl backdrop-blur-xl flex flex-col justify-between space-y-6"
              >
                <div className="space-y-3">
                  <div className="flex justify-between items-start">
                    <span className="inline-flex rounded-full bg-emerald-500/10 border border-emerald-500/20 px-2.5 py-0.5 text-3xs font-extrabold uppercase tracking-wide text-emerald-400">
                      {p.type.replace("USER_", "").replace("OWNER_", "")}
                    </span>
                    <span className="text-xs text-slate-500 font-semibold">#{p.id}</span>
                  </div>
                  <h3 className="text-xl font-bold text-white">{p.name}</h3>
                  <p className="text-xs text-slate-400 min-h-[32px]">{p.description || "No description provided."}</p>

                  <div className="space-y-2 text-xs border-t border-white/5 pt-3">
                    <div className="flex justify-between">
                      <span className="text-slate-500">Price</span>
                      <span className="font-bold text-white">Rs {p.price}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-slate-500">Duration</span>
                      <span className="font-semibold text-slate-300">{p.durationDays} Days</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-slate-500">Discount Percent</span>
                      <span className="font-semibold text-slate-300">{p.discountPercent}%</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-slate-500">Priority Slots</span>
                      <span className="font-semibold text-slate-300">{p.prioritySlots ? "Yes" : "No"}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-slate-500">Featured Listing</span>
                      <span className="font-semibold text-slate-300">{p.featuredListing ? "Yes" : "No"}</span>
                    </div>
                  </div>
                </div>

                <div className="flex gap-2 pt-3 border-t border-white/5 text-2xs">
                  <button
                    onClick={() => setEditingPlan(p)}
                    className="flex-1 rounded-xl border border-white/10 bg-white/5 py-2 font-bold hover:bg-white/10"
                  >
                    Edit Details
                  </button>
                  <button
                    onClick={() => handleDelete(p.id)}
                    className="rounded-xl border border-red-500/20 bg-red-500/15 px-3 py-2 font-bold text-red-300 hover:bg-red-500/25"
                  >
                    Delete
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </main>
  );
}
