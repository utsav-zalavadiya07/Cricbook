"use client";

import { useEffect, useState } from "react";
import DashboardLayout from "@/modules/turf/components/DashboardLayout";
import { AttendanceStatus, getOwnerRiskBoard, TrustScore, updateBookingAttendance } from "@/modules/trust/services/trustService";
import { getOwnerBookings } from "@/modules/owner/services/ownerService";
import { Booking } from "@/modules/booking/types/booking.types";
import { useToast } from "@/modules/ui/ToastProvider";

const statuses: AttendanceStatus[] = ["ARRIVED", "LATE", "NO_SHOW", "CANCELLED"];

export default function OwnerTrustPage() {
  const { toast } = useToast();
  const [riskBoard, setRiskBoard] = useState<TrustScore[]>([]);
  const [bookings, setBookings] = useState<Booking[]>([]);
  const [loading, setLoading] = useState(true);

  async function load() {
    setLoading(true);
    try {
      const [riskRes, bookingRes] = await Promise.all([getOwnerRiskBoard(), getOwnerBookings("CONFIRMED")]);
      setRiskBoard(riskRes.data || []);
      setBookings(bookingRes.data || []);
    } catch (error: unknown) {
      toast(apiMessage(error, "Could not load trust controls."), "error");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    const timer = window.setTimeout(() => void load(), 0);
    return () => window.clearTimeout(timer);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const markAttendance = async (bookingId: number, status: AttendanceStatus) => {
    try {
      await updateBookingAttendance(bookingId, status);
      toast(`Attendance marked ${status.replaceAll("_", " ")}.`, "success");
      await load();
    } catch (error: unknown) {
      toast(apiMessage(error, "Could not update attendance."), "error");
    }
  };

  return (
    <DashboardLayout>
      <main className="space-y-8">
        <section className="theme-panel-strong rounded-[2rem] p-6">
          <p className="theme-accent-owner text-xs font-black uppercase tracking-[0.34em]">Owner Trust Controls</p>
          <h1 className="theme-title mt-3 text-3xl font-black sm:text-5xl">Approve reliable players. Flag risky behavior early.</h1>
          <p className="theme-muted mt-3 max-w-3xl text-sm leading-6">
            Track attendance, identify high-risk bookings, and use player trust signals before repeated no-shows hurt your turf operations.
          </p>
        </section>

        <section className="grid gap-6 xl:grid-cols-[0.9fr_1.1fr]">
          <div className="theme-panel rounded-3xl p-6">
            <h2 className="theme-title text-xl font-black">Risk Analytics</h2>
            <p className="theme-muted mt-1 text-sm">Lowest trust players who booked your turfs.</p>
            <div className="mt-5 space-y-3">
              {loading && <p className="theme-muted py-10 text-center">Loading risk board...</p>}
              {!loading && riskBoard.length === 0 && <p className="theme-card theme-muted rounded-2xl border-dashed p-8 text-center">No risk signals yet.</p>}
              {riskBoard.map((profile) => <TrustCard key={profile.userId} profile={profile} />)}
            </div>
          </div>

          <div className="theme-panel rounded-3xl p-6">
            <h2 className="theme-title text-xl font-black">Match-Day Check-in</h2>
            <p className="theme-muted mt-1 text-sm">Manual attendance updates immediately affect trust scoring.</p>
            <div className="mt-5 space-y-3">
              {loading && <p className="theme-muted py-10 text-center">Loading bookings...</p>}
              {!loading && bookings.length === 0 && <p className="theme-card theme-muted rounded-2xl border-dashed p-8 text-center">No confirmed bookings to check in.</p>}
              {bookings.map((booking) => (
                <div key={booking.id} className="theme-panel-soft rounded-2xl p-4">
                  <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
                    <div>
                      <p className="theme-title font-bold">{booking.turfName || `Turf #${booking.turfId}`}</p>
                      <p className="theme-muted text-sm">{booking.bookingDate} · {booking.startTime || "--"} - {booking.endTime || "--"} · Booking #{booking.id}</p>
                    </div>
                    <span className="theme-card rounded-full px-3 py-1 text-xs font-bold">{booking.status}</span>
                  </div>
                  <div className="mt-4 flex flex-wrap gap-2">
                    {statuses.map((status) => (
                      <button key={status} onClick={() => markAttendance(booking.id, status)} className="theme-card rounded-xl px-3 py-2 text-xs font-black transition hover:border-sky-400/50 hover:text-sky-500">
                        {status.replaceAll("_", " ")}
                      </button>
                    ))}
                  </div>
                </div>
              ))}
            </div>
          </div>
        </section>
      </main>
    </DashboardLayout>
  );
}

function apiMessage(error: unknown, fallback: string) {
  const response = (error as { response?: { data?: { message?: string } } } | null)?.response;
  return response?.data?.message || fallback;
}

function TrustCard({ profile }: { profile: TrustScore }) {
  const riskClass = profile.riskLevel === "HIGH" ? "text-red-500" : profile.riskLevel === "MEDIUM" ? "text-amber-500" : "text-emerald-500";
  return (
    <div className="theme-panel-soft rounded-2xl p-4">
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="theme-title font-bold">{profile.playerName || `User #${profile.userId}`}</p>
          <p className="theme-muted mt-1 text-sm">{profile.tier} · {profile.badges.join(", ")}</p>
        </div>
        <div className="text-right">
          <p className="theme-title text-2xl font-black">{profile.score}</p>
          <p className={`text-xs font-black ${riskClass}`}>{profile.riskLevel}</p>
        </div>
      </div>
      <p className="theme-muted mt-3 text-sm">{profile.riskReason}</p>
    </div>
  );
}
