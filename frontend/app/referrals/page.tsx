"use client";

import { useEffect, useState } from "react";
import { getGrowthDashboard, getReferralCode, claimReferral, getMyReferrals } from "@/modules/membership/services/subscriptionService";
import { useToast } from "@/modules/ui/ToastProvider";

type ReferredFriend = {
  inviteeEmail: string;
  inviteeName: string;
  status: string;
  createdAt: string;
};

type GrowthDashboard = {
  referrals?: { total?: number; completed?: number; pending?: number };
  crm?: { customerLifetimeValue?: number; reEngagementStage?: string; vipQualified?: boolean };
  offers?: Array<{ key: string; title: string; status: string; message: string }>;
};

export default function ReferralsPage() {
  const [code, setCode] = useState<string>("");
  const [claimCode, setClaimCode] = useState<string>("");
  const [referredList, setReferredList] = useState<ReferredFriend[]>([]);
  const [growth, setGrowth] = useState<GrowthDashboard | null>(null);
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState<{ text: string; type: "success" | "error" } | null>(null);
  const { showError, showSuccess } = useToast();

  const loadData = () => {
    setLoading(true);
    Promise.all([getReferralCode(), getMyReferrals(), getGrowthDashboard()])
      .then(([codeRes, refRes, growthRes]) => {
        setCode(codeRes.data.code);
        setReferredList(refRes.data);
        setGrowth(growthRes.data);
      })
      .catch((err) => console.error(err))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    void Promise.resolve().then(loadData);
  }, []);

  const handleClaim = () => {
    setMessage(null);
    if (!claimCode.trim()) {
      setMessage({ text: "Referral code cannot be empty.", type: "error" });
      return;
    }

    claimReferral(claimCode)
      .then(() => {
        setMessage({
          text: "Referral code successfully linked! Perform your first turf booking to credit points to you and your friend.",
          type: "success",
        });
        setClaimCode("");
        loadData();
      })
      .catch((err) => {
        const errorMsg = err.response?.data?.error || "Failed to link referral code.";
        setMessage({ text: errorMsg, type: "error" });
      });
  };

  const copyToClipboard = () => {
    navigator.clipboard
      .writeText(code)
      .then(() => showSuccess("Referral Code Copied", "Share it with friends to earn loyalty points."))
      .catch(() => showError("Copy Failed", "Could not copy referral code to clipboard."));
  };

  return (
    <main className="theme-page-player min-h-screen px-4 py-6 sm:px-6 lg:px-8">
      <div className="w-full space-y-8">
        <div>
          <h1 className="text-3xl font-extrabold text-white">Referrals Program</h1>
          <p className="mt-1.5 text-sm text-slate-400">
            Invite friends to join Cricbook! Both earn massive loyalty points upon their first confirmed slot booking.
          </p>
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

        {loading ? (
          <div className="py-20 text-center text-slate-400 font-semibold animate-pulse">
            Loading referrals console...
          </div>
        ) : (
          <div className="grid gap-6 xl:grid-cols-3">
            {/* Referral Sharing widget */}
            <div className="xl:col-span-1 rounded-3xl border border-white/10 bg-white/[0.03] p-6 backdrop-blur-xl shadow-xl flex flex-col justify-between space-y-6">
              <div className="space-y-4">
                <h2 className="text-lg font-bold text-white">Share Invitation</h2>
                <p className="text-xs text-slate-400 leading-relaxed">
                  Earn **100 loyalty points** (equivalent to Rs 10 Cricbook cash) for every referred friend! Your friend gets **50 points**!
                </p>

                <div className="bg-black/20 rounded-2xl p-5 border border-white/5 space-y-3.5 text-center">
                  <span className="block text-2xs uppercase tracking-widest text-slate-500 font-extrabold">Your Referral Code</span>
                  <span className="block text-2xl font-black tracking-wider text-emerald-400">{code}</span>
                  <button
                    onClick={copyToClipboard}
                    className="inline-flex rounded-xl bg-white/5 border border-white/10 px-3.5 py-1.5 text-xs font-semibold text-slate-200 hover:bg-white/10"
                  >
                    Copy Code
                  </button>
                </div>

                <div className="grid grid-cols-3 gap-2 text-center">
                  <MiniReferralMetric label="Total" value={growth?.referrals?.total ?? referredList.length} />
                  <MiniReferralMetric label="Converted" value={growth?.referrals?.completed ?? 0} />
                  <MiniReferralMetric label="Pending" value={growth?.referrals?.pending ?? 0} />
                </div>
              </div>

              {/* Claim Referral widget */}
              <div className="border-t border-white/5 pt-6 space-y-4">
                <h3 className="text-sm font-bold text-white">Have an invite code?</h3>
                <div className="flex gap-2">
                  <input
                    value={claimCode}
                    onChange={(e) => setClaimCode(e.target.value)}
                    placeholder="CRIC-FRIEND-1"
                    className="flex-1 rounded-xl border border-white/10 bg-black/40 px-3.5 py-2 text-sm text-white outline-none placeholder:text-slate-600 focus:border-emerald-500"
                  />
                  <button
                    onClick={handleClaim}
                    className="rounded-xl bg-emerald-500 px-4 py-2 text-sm font-bold text-white transition-all hover:opacity-90 active:scale-95 shadow-lg shadow-emerald-500/10"
                  >
                    Submit
                  </button>
                </div>
              </div>
            </div>

            {/* Referred List Board */}
            <div className="xl:col-span-2 rounded-3xl border border-white/10 bg-white/[0.03] p-6 backdrop-blur-xl shadow-xl space-y-6">
              <h2 className="text-lg font-bold text-white">Invites History</h2>

              <div className="space-y-4 max-h-[400px] overflow-y-auto pr-2">
                {referredList.length === 0 ? (
                  <p className="text-sm text-slate-500 text-center py-16 border border-dashed border-white/10 rounded-2xl">
                    You have not referred any friends yet. Share your code to get started!
                  </p>
                ) : (
                  referredList.map((ref, idx) => (
                    <div
                      key={idx}
                      className="flex items-center justify-between rounded-2xl border border-white/5 bg-black/20 p-4"
                    >
                      <div className="space-y-0.5">
                        <h4 className="text-sm font-bold text-white">{ref.inviteeName}</h4>
                        <p className="text-2xs text-slate-500">Joined: {ref.createdAt.slice(0, 10)}</p>
                      </div>

                      <span
                        className={`inline-flex rounded-full px-2.5 py-0.5 text-2xs font-extrabold uppercase tracking-wide ${
                          ref.status === "COMPLETED"
                            ? "bg-emerald-500/10 text-emerald-400 border border-emerald-500/20"
                            : "bg-amber-500/10 text-amber-400 border border-amber-500/20"
                        }`}
                      >
                        {ref.status}
                      </span>
                    </div>
                  ))
                )}
              </div>
            </div>
          </div>
        )}
      </div>
    </main>
  );
}

function MiniReferralMetric({ label, value }: { label: string; value: string | number }) {
  return (
    <div className="rounded-2xl border border-white/10 bg-white/[0.03] p-3">
      <p className="text-2xs font-extrabold uppercase tracking-widest text-slate-500">{label}</p>
      <p className="mt-1 text-lg font-black text-white">{value}</p>
    </div>
  );
}
