"use client";
import { TileInfo } from "@/components/ui";

import { useEffect, useState } from "react";
import { getGrowthDashboard, getLoyaltyBalance, getLoyaltyHistory, redeemLoyaltyPoints } from "@/modules/membership/services/subscriptionService";

type GrowthDashboard = {
  loyalty?: { nextMilestonePoints?: number; walletEquivalent?: number };
  membership?: { tier?: string; planName?: string; active?: boolean };
  referrals?: { completed?: number; pending?: number };
  crm?: { customerLifetimeValue?: number; projectedClv?: number; reEngagementStage?: string; vipQualified?: boolean };
};

export default function RewardsDashboard() {
  const [points, setPoints] = useState<number>(0);
  const [history, setHistory] = useState<string>("");
  const [growth, setGrowth] = useState<GrowthDashboard | null>(null);
  const [redeemVal, setRedeemVal] = useState<number>(10);
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState<{ text: string; type: "success" | "error" } | null>(null);

  const loadData = () => {
    setLoading(true);
    Promise.all([getLoyaltyBalance(), getLoyaltyHistory(), getGrowthDashboard()])
      .then(([balRes, histRes, growthRes]) => {
        setPoints(balRes.data.balance);
        setHistory(histRes.data.history || "No history logged yet.");
        setGrowth(growthRes.data);
        setMessage(null);
      })
      .catch((err) => {
        const errorMsg = extractErrorMessage(err, "Unable to load your rewards right now.");
        setMessage({ text: errorMsg, type: "error" });
        setPoints(0);
        setHistory("No history logged yet.");
      })
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    void Promise.resolve().then(loadData);
  }, []);

  const handleRedeem = () => {
    setMessage(null);
    if (redeemVal <= 0 || redeemVal % 10 !== 0) {
      setMessage({ text: "Redemption amount must be in multiples of 10 points.", type: "error" });
      return;
    }
    if (points < redeemVal) {
      setMessage({ text: "Insufficient points balance.", type: "error" });
      return;
    }

    redeemLoyaltyPoints(redeemVal)
      .then(() => {
        setMessage({
          text: `Redemption successful! Rs ${redeemVal / 10} credited to your Cricbook Wallet.`,
          type: "success",
        });
        setRedeemVal(10);
        loadData();
      })
      .catch((err) => {
        const errorMsg = extractErrorMessage(err, "Failed to redeem loyalty points.");
        setMessage({ text: errorMsg, type: "error" });
      });
  };

  // Convert points history logs to structured lines
  const getHistoryLines = () => {
    if (!history) return [];
    return history.split("\n").filter((line) => line.trim().length > 0).reverse();
  };

  return (
    <main className="theme-page-player min-h-screen px-4 py-6 sm:px-6 lg:px-8">
      <div className="w-full space-y-8">
        <div>
          <h1 className="theme-title text-3xl font-extrabold">Loyalty Rewards
              <span className="inline-flex align-middle ml-3 mb-1">
                <TileInfo label="Loyalty Rewards information" focusable={false}>Earn points for turf slot bookings, tournament events, and friend invites. Redeem for wallet balance!</TileInfo>
              </span></h1>
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
          <div className="theme-muted py-20 text-center font-semibold animate-pulse">
            Accessing rewards dashboard...
          </div>
        ) : (
          <div className="grid gap-6 xl:grid-cols-3">
            <div className="theme-panel xl:col-span-3 rounded-3xl p-5">
              <div className="grid gap-3 md:grid-cols-4">
                <MiniMetric label="Growth Tier" value={growth?.membership?.tier || "BRONZE"} />
                <MiniMetric label="CLV" value={`Rs ${growth?.crm?.customerLifetimeValue ?? 0}`} />
                <MiniMetric label="Projected CLV" value={`Rs ${growth?.crm?.projectedClv ?? 0}`} />
                <MiniMetric label="Next Reward" value={`${growth?.loyalty?.nextMilestonePoints ?? 100} pts`} />
              </div>
            </div>
            {/* Points Circle Progression Card */}
            <div className="theme-panel xl:col-span-1 flex flex-col items-center justify-center rounded-3xl p-8 text-center space-y-6">
              <h2 className="theme-title text-lg font-bold uppercase tracking-wider">Rewards Points Balance</h2>

              {/* Progress Ring */}
              <div className="relative flex items-center justify-center">
                <svg className="h-44 w-44 transform -rotate-90">
                  <circle
                    cx="88"
                    cy="88"
                    r="76"
                    className="text-white/5"
                    strokeWidth="12"
                    stroke="currentColor"
                    fill="transparent"
                  />
                  <circle
                    cx="88"
                    cy="88"
                    r="76"
                    className="text-emerald-400"
                    strokeWidth="12"
                    strokeDasharray={477}
                    strokeDashoffset={Math.max(477 - (points / 500) * 477, 0)}
                    strokeLinecap="round"
                    stroke="currentColor"
                    fill="transparent"
                  />
                </svg>
                <div className="absolute space-y-1">
                  <span className="block text-4xl font-black text-white">{points}</span>
                  <span className="block text-2xs uppercase tracking-widest text-slate-500 font-extrabold">Points</span>
                </div>
              </div>

              <div className="theme-muted space-y-1 text-xs">
                <p>10 points = Rs 1 Cricbook cash</p>
                <p className="font-semibold text-emerald-400/80">Value: Rs {(points / 10).toFixed(2)}</p>
              </div>
            </div>

            {/* Redemption Calculator and Slider Card */}
            <div className="theme-panel xl:col-span-1 rounded-3xl p-6 flex flex-col justify-between space-y-6">
              <div className="space-y-4">
                <h2 className="theme-title text-lg font-bold">Redeem Points</h2>
                <p className="theme-muted text-xs leading-relaxed">
                  Convert your rewards directly to cash balance in your Cricbook wallet.
                </p>

                <div className="theme-panel-soft space-y-4 rounded-2xl p-4">
                  <div className="flex justify-between items-center">
                    <span className="theme-muted text-xs">Points to Redeem</span>
                    <input
                      type="number"
                      step="10"
                      min="10"
                      max={points}
                      value={redeemVal}
                      onChange={(e) => setRedeemVal(Number(e.target.value))}
                      className="theme-input w-24 rounded-lg px-3 py-1.5 text-center text-sm font-bold outline-none"
                    />
                  </div>

                  <div className="flex justify-between items-center text-sm">
                    <span className="theme-muted">Cash Received</span>
                    <span className="font-black text-emerald-400 text-base">Rs {redeemVal / 10}</span>
                  </div>
                </div>
              </div>

              <button
                disabled={points < 10 || points < redeemVal}
                onClick={handleRedeem}
                className="w-full rounded-2xl bg-emerald-500 disabled:opacity-30 disabled:cursor-not-allowed py-3.5 text-sm font-bold text-white transition-all hover:opacity-90 active:scale-95 shadow-lg shadow-emerald-500/10"
              >
                Convert to Wallet Balance
              </button>
            </div>

            {/* Loyalty History Logs */}
            <div className="theme-panel xl:col-span-1 rounded-3xl p-6 flex flex-col space-y-6">
              <h2 className="theme-title text-lg font-bold">Activity Logs</h2>

              <div className="theme-muted space-y-3 max-h-[300px] overflow-y-auto pr-1 text-2xs flex-1">
                {getHistoryLines().length === 0 ? (
                  <p className="theme-soft py-12 text-center">No transactions recorded yet.</p>
                ) : (
                  getHistoryLines().map((line, idx) => (
                    <div key={idx} className="theme-panel-soft rounded-xl p-3 flex gap-2">
                      <span className="text-emerald-400 shrink-0">✦</span>
                      <span className="leading-relaxed">{line}</span>
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

function MiniMetric({ label, value }: { label: string; value: string | number }) {
  return (
    <div className="theme-panel-soft rounded-2xl p-4">
      <p className="theme-muted text-2xs font-extrabold uppercase tracking-widest">{label}</p>
      <p className="theme-title mt-2 text-lg font-black">{value}</p>
    </div>
  );
}

function extractErrorMessage(error: unknown, fallback: string) {
  const apiError = error as {
    response?: {
      data?: {
        message?: string;
        error?: string;
      };
    };
  };

  return apiError.response?.data?.message || apiError.response?.data?.error || fallback;
}
