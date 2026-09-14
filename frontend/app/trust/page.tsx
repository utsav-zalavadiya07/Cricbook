"use client";
import { TileInfo } from "@/components/ui";

import { FormEvent, useEffect, useState } from "react";
import { getMyTrustProfile, getTrustAttendance, submitTrustReport, TrustAttendance, TrustReportReason, TrustScore } from "@/modules/trust/services/trustService";
import { useToast } from "@/modules/ui/ToastProvider";

const reportReasons: TrustReportReason[] = ["TOXIC_BEHAVIOR", "AGGRESSIVE_BEHAVIOR", "CHEATING", "NO_PAYMENT", "MISCONDUCT", "FAKE_BOOKING", "OTHER"];

export default function TrustPage() {
  const { toast } = useToast();
  const [profile, setProfile] = useState<TrustScore | null>(null);
  const [attendance, setAttendance] = useState<TrustAttendance[]>([]);
  const [loading, setLoading] = useState(true);
  const [reportedUserId, setReportedUserId] = useState("");
  const [bookingId, setBookingId] = useState("");
  const [reason, setReason] = useState<TrustReportReason>("TOXIC_BEHAVIOR");
  const [description, setDescription] = useState("");

  async function load() {
    setLoading(true);
    try {
      const res = await getMyTrustProfile();
      setProfile(res.data);
      const history = await getTrustAttendance(res.data.userId);
      setAttendance(history.data || []);
    } catch {
      toast("Could not load trust profile.", "error");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    const timer = window.setTimeout(() => void load(), 0);
    return () => window.clearTimeout(timer);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleReport = async (event: FormEvent) => {
    event.preventDefault();
    const userId = Number(reportedUserId);
    if (!userId) {
      toast("Enter the player user ID to report.", "error");
      return;
    }
    try {
      await submitTrustReport({
        reportedUserId: userId,
        bookingId: bookingId ? Number(bookingId) : undefined,
        reason,
        description,
      });
      setReportedUserId("");
      setBookingId("");
      setDescription("");
      toast("Report submitted for moderation.", "success");
    } catch (error: unknown) {
      toast(apiMessage(error, "Could not submit report."), "error");
    }
  };

  if (loading) {
    return <main className="theme-page-player min-h-screen px-4 py-10"><p className="theme-muted text-center">Loading trust profile...</p></main>;
  }

  return (
    <main className="theme-page-player min-h-screen px-4 py-8 sm:px-6 lg:px-10">
      <div className="mx-auto max-w-7xl space-y-8">
        <section className="theme-panel-strong overflow-hidden rounded-[2rem] p-6 sm:p-8">
          <div className="grid gap-8 lg:grid-cols-[1.2fr_0.8fr] lg:items-center">
            <div>
              <p className="theme-accent text-xs font-black uppercase tracking-[0.34em]">Trust profile</p>
              <h1 className="theme-title mt-3 text-4xl font-black tracking-tight sm:text-6xl">Reliability that follows every match.
              <span className="inline-flex align-middle ml-3 mb-1">
                <TileInfo label="Reliability that follows every match. information" focusable={false}>Your Cricbook trust score reflects attendance, punctuality, cancellations, reports, payment behavior, and match completion.</TileInfo>
              </span></h1>
              <div className="mt-5 flex flex-wrap gap-2">
                {(profile?.badges || []).map((badge) => <span key={badge} className="theme-badge rounded-full px-3 py-1 text-xs font-bold">{badge}</span>)}
              </div>
            </div>
            <TrustMeter profile={profile} />
          </div>
        </section>

        {profile && (
          <section className="grid gap-4 md:grid-cols-3 lg:grid-cols-6">
            <Factor label="Attendance" value={profile.attendanceScore} />
            <Factor label="Punctuality" value={profile.punctualityScore} />
            <Factor label="Cancellation" value={profile.cancellationScore} />
            <Factor label="Reports" value={profile.reportScore} />
            <Factor label="Payment" value={profile.paymentScore} />
            <Factor label="Completion" value={profile.matchCompletionScore} />
          </section>
        )}

        <section className="grid gap-6 lg:grid-cols-[0.95fr_1.05fr]">
          <div className="theme-panel rounded-3xl p-6">
            <h2 className="theme-title text-xl font-black">Attendance History</h2>
            <p className="theme-muted mt-1 text-sm">Recent owner check-ins and punctuality impact.</p>
            <div className="mt-5 space-y-3">
              {attendance.length === 0 && <p className="theme-card theme-muted rounded-2xl border-dashed p-8 text-center">No attendance records yet.</p>}
              {attendance.map((item) => (
                <div key={item.id} className="theme-panel-soft rounded-2xl p-4">
                  <div className="flex items-center justify-between gap-3">
                    <p className="theme-title font-bold">Booking #{item.bookingId}</p>
                    <span className="theme-card rounded-full px-3 py-1 text-xs font-bold">{item.status}</span>
                  </div>
                  <p className="theme-muted mt-1 text-sm">{item.delayMinutes} min delay · {item.impact.replaceAll("_", " ")}</p>
                </div>
              ))}
            </div>
          </div>

          <form onSubmit={handleReport} className="theme-panel rounded-3xl p-6">
            <h2 className="theme-title text-xl font-black">Report Sportsmanship Issue</h2>
            <p className="theme-muted mt-1 text-sm">Reports go to admin moderation before stronger penalties are applied.</p>
            <div className="mt-5 grid gap-4 sm:grid-cols-2">
              <Field label="Reported User ID" value={reportedUserId} onChange={setReportedUserId} />
              <Field label="Booking ID (optional)" value={bookingId} onChange={setBookingId} />
              <label className="sm:col-span-2">
                <span className="theme-title text-sm font-bold">Reason</span>
                <select value={reason} onChange={(event) => setReason(event.target.value as TrustReportReason)} className="theme-input mt-2 w-full rounded-2xl px-4 py-3 text-sm outline-none">
                  {reportReasons.map((item) => <option key={item} value={item}>{item.replaceAll("_", " ")}</option>)}
                </select>
              </label>
              <label className="sm:col-span-2">
                <span className="theme-title text-sm font-bold">Description</span>
                <textarea value={description} onChange={(event) => setDescription(event.target.value)} className="theme-input mt-2 min-h-28 w-full rounded-2xl px-4 py-3 text-sm outline-none" placeholder="Add context for the moderation team." />
              </label>
            </div>
            <button className="mt-5 rounded-2xl bg-emerald-500 px-5 py-3 text-sm font-black text-white shadow-lg shadow-emerald-500/20 hover:bg-emerald-400">Submit Report</button>
          </form>
        </section>
      </div>
    </main>
  );
}

function apiMessage(error: unknown, fallback: string) {
  const response = (error as { response?: { data?: { message?: string } } } | null)?.response;
  return response?.data?.message || fallback;
}

function TrustMeter({ profile }: { profile: TrustScore | null }) {
  const score = profile?.score ?? 0;
  return (
    <div className="theme-card-strong rounded-[1.6rem] p-6">
      <p className="theme-soft text-xs font-black uppercase tracking-[0.24em]">Current Score</p>
      <div className="mt-4 flex items-end gap-4">
        <span className="theme-title text-7xl font-black">{score}</span>
        <span className="theme-muted pb-3 text-lg font-bold">/100</span>
      </div>
      <div className="theme-panel-soft mt-5 h-3 overflow-hidden rounded-full">
        <div className="h-full rounded-full bg-gradient-to-r from-amber-400 via-emerald-400 to-cyan-400" style={{ width: `${score}%` }} />
      </div>
      <p className="theme-title mt-5 text-xl font-black">{profile?.tier || "Building Trust"}</p>
      <p className="theme-muted mt-2 text-sm">{profile?.riskReason || "Play consistently to build a verified profile."}</p>
      <div className="mt-4 space-y-2">
        {(profile?.recommendations || []).map((item) => <p key={item} className="theme-panel-soft rounded-xl px-3 py-2 text-sm">{item}</p>)}
      </div>
    </div>
  );
}

function Factor({ label, value }: { label: string; value: number }) {
  return (
    <div className="theme-panel rounded-2xl p-4">
      <p className="theme-soft text-[11px] font-black uppercase tracking-[0.2em]">{label}</p>
      <p className="theme-title mt-3 text-3xl font-black">{value}</p>
    </div>
  );
}

function Field({ label, value, onChange }: { label: string; value: string; onChange: (value: string) => void }) {
  return (
    <label>
      <span className="theme-title text-sm font-bold">{label}</span>
      <input value={value} onChange={(event) => onChange(event.target.value)} className="theme-input mt-2 w-full rounded-2xl px-4 py-3 text-sm outline-none" />
    </label>
  );
}
