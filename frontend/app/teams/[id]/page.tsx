"use client";
import { TileInfo } from "@/components/ui";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import {
  addTeamFinance,
  createTeamAnnouncement,
  createTeamPoll,
  getTeamWorkspace,
  recordTeamAttendance,
  saveTeamLineup,
  setTeamAvailability,
  TeamWorkspace,
  voteTeamPoll,
} from "@/modules/team/services/teamWorkspaceService";
import { useToast } from "@/modules/ui/ToastProvider";

export default function TeamWorkspacePage() {
  const params = useParams<{ id: string }>();
  const teamId = Number(params.id);
  const { toast } = useToast();
  const [workspace, setWorkspace] = useState<TeamWorkspace | null>(null);
  const [availability, setAvailability] = useState({ date: new Date().toISOString().slice(0, 10), status: "AVAILABLE", note: "" });
  const [lineup, setLineup] = useState({ userId: "", matchId: "", position: "1", lineupRole: "PLAYER" });
  const [poll, setPoll] = useState({ question: "", options: "Friday, Saturday" });
  const [announcement, setAnnouncement] = useState({ title: "", content: "", pinned: false });
  const [finance, setFinance] = useState({ type: "CONTRIBUTION", amount: "", description: "" });
  const [attendance, setAttendance] = useState({ userId: "", matchId: "", status: "PRESENT", note: "" });

  async function load() {
    try {
      const res = await getTeamWorkspace(teamId);
      setWorkspace(res.data);
    } catch (error: unknown) {
      toast(apiMessage(error, "Could not load team workspace."), "error");
    }
  }

  useEffect(() => {
    const timer = window.setTimeout(() => void load(), 0);
    return () => window.clearTimeout(timer);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [teamId]);

  const run = async (action: () => Promise<unknown>, success: string) => {
    try {
      await action();
      toast(success, "success");
      await load();
    } catch (error: unknown) {
      toast(apiMessage(error, "Team action failed."), "error");
    }
  };

  if (!workspace) {
    return <main className="theme-page-player min-h-screen px-6 py-10"><p className="theme-muted text-center">Loading team workspace...</p></main>;
  }

  return (
    <main className="theme-page-player min-h-screen px-4 py-8 sm:px-6 lg:px-10">
      <div className="mx-auto max-w-7xl space-y-8">
        <section className="theme-panel-strong rounded-[2rem] p-6 sm:p-8">
          <p className="theme-accent text-xs font-black uppercase tracking-[0.34em]">EPIC 26 · Team Workspace</p>
          <h1 className="theme-title mt-3 text-4xl font-black sm:text-6xl">{workspace.teamName}
              <span className="inline-flex align-middle ml-3 mb-1">
                <TileInfo label="{workspace.teamName} information" focusable={false}>{workspace.goals || "Team availability, lineup, polls, finance, announcements and attendance in one place."}</TileInfo>
              </span></h1>
          <div className="mt-6 grid gap-3 sm:grid-cols-5">
            <Metric label="Members" value={workspace.memberCount} />
            <Metric label="Available Today" value={workspace.availableToday} />
            <Metric label="Maybe" value={workspace.maybeToday} />
            <Metric label="Balance" value={`₹${workspace.teamBalance || 0}`} />
            <Metric label="Attendance" value={`${workspace.attendanceRate || 0}%`} />
          </div>
        </section>

        <section className="grid gap-6 xl:grid-cols-[0.8fr_1.2fr]">
          <div className="space-y-6">
            <Panel title="Availability">
              <form onSubmit={(event) => { event.preventDefault(); void run(() => setTeamAvailability(teamId, { date: availability.date, status: availability.status as "AVAILABLE", note: availability.note }), "Availability saved."); }} className="grid gap-3">
                <Field label="Date" type="date" value={availability.date} onChange={(value) => setAvailability((current) => ({ ...current, date: value }))} />
                <select value={availability.status} onChange={(event) => setAvailability((current) => ({ ...current, status: event.target.value }))} className="theme-input rounded-xl px-3 py-2 text-sm">
                  <option>AVAILABLE</option><option>MAYBE</option><option>UNAVAILABLE</option>
                </select>
                <Field label="Note" value={availability.note} onChange={(value) => setAvailability((current) => ({ ...current, note: value }))} />
                <button className="rounded-xl bg-emerald-500 px-4 py-2 text-sm font-black text-white">Save RSVP</button>
              </form>
            </Panel>

            <Panel title="Captain Tools">
              <form onSubmit={(event) => { event.preventDefault(); void run(() => saveTeamLineup(teamId, { userId: Number(lineup.userId), matchId: lineup.matchId ? Number(lineup.matchId) : undefined, position: Number(lineup.position), lineupRole: lineup.lineupRole }), "Lineup saved."); }} className="grid gap-3">
                <Field label="Player User ID" value={lineup.userId} onChange={(value) => setLineup((current) => ({ ...current, userId: value }))} type="number" />
                <Field label="Match ID" value={lineup.matchId} onChange={(value) => setLineup((current) => ({ ...current, matchId: value }))} type="number" />
                <Field label="Batting Position" value={lineup.position} onChange={(value) => setLineup((current) => ({ ...current, position: value }))} type="number" />
                <Field label="Role" value={lineup.lineupRole} onChange={(value) => setLineup((current) => ({ ...current, lineupRole: value }))} />
                <button className="rounded-xl bg-emerald-500 px-4 py-2 text-sm font-black text-white">Save Lineup</button>
              </form>
            </Panel>
          </div>

          <div className="space-y-6">
            <Panel title="Members">
              <div className="grid gap-3 sm:grid-cols-2">
                {workspace.members.map((member) => <Card key={member.id} title={member.name} meta={`${member.role} · ${member.email}`} />)}
              </div>
            </Panel>

            <Panel title="Lineup">
              <div className="grid gap-3 sm:grid-cols-2">
                {workspace.lineup.map((item) => <Card key={item.id} title={`${item.position}. ${item.userName}`} meta={item.lineupRole} />)}
                {workspace.lineup.length === 0 && <Empty text="No lineup prepared yet." />}
              </div>
            </Panel>
          </div>
        </section>

        <section className="grid gap-6 xl:grid-cols-3">
          <Panel title="Polls">
            <form onSubmit={(event) => { event.preventDefault(); void run(() => createTeamPoll(teamId, { question: poll.question, options: poll.options.split(",").map((item) => item.trim()).filter(Boolean) }), "Poll created."); }} className="grid gap-3">
              <Field label="Question" value={poll.question} onChange={(value) => setPoll((current) => ({ ...current, question: value }))} />
              <Field label="Options" value={poll.options} onChange={(value) => setPoll((current) => ({ ...current, options: value }))} />
              <button className="rounded-xl bg-emerald-500 px-4 py-2 text-sm font-black text-white">Create Poll</button>
            </form>
            {workspace.polls.map((item) => (
              <div key={item.id} className="theme-panel-soft mt-3 rounded-2xl p-3">
                <p className="theme-title font-black">{item.question}</p>
                <div className="mt-2 flex flex-wrap gap-2">
                  {item.options.map((option) => <button key={option} onClick={() => run(() => voteTeamPoll(teamId, item.id, option), "Vote saved.")} className="theme-card rounded-full px-3 py-1 text-xs">{option} ({item.voteCounts?.[option] || 0})</button>)}
                </div>
              </div>
            ))}
          </Panel>

          <Panel title="Announcements">
            <form onSubmit={(event) => { event.preventDefault(); void run(() => createTeamAnnouncement(teamId, announcement), "Announcement posted."); }} className="grid gap-3">
              <Field label="Title" value={announcement.title} onChange={(value) => setAnnouncement((current) => ({ ...current, title: value }))} />
              <Field label="Content" value={announcement.content} onChange={(value) => setAnnouncement((current) => ({ ...current, content: value }))} />
              <button className="rounded-xl bg-emerald-500 px-4 py-2 text-sm font-black text-white">Post</button>
            </form>
            {workspace.announcements.map((item) => <Card key={item.id} title={item.title} meta={item.content} />)}
          </Panel>

          <Panel title="Finance & Attendance">
            <form onSubmit={(event) => { event.preventDefault(); void run(() => addTeamFinance(teamId, { type: finance.type as "CONTRIBUTION", amount: Number(finance.amount), description: finance.description }), "Finance entry saved."); }} className="grid gap-3">
              <select value={finance.type} onChange={(event) => setFinance((current) => ({ ...current, type: event.target.value }))} className="theme-input rounded-xl px-3 py-2 text-sm"><option>CONTRIBUTION</option><option>EXPENSE</option><option>REFUND</option></select>
              <Field label="Amount" value={finance.amount} onChange={(value) => setFinance((current) => ({ ...current, amount: value }))} type="number" />
              <Field label="Description" value={finance.description} onChange={(value) => setFinance((current) => ({ ...current, description: value }))} />
              <button className="rounded-xl bg-emerald-500 px-4 py-2 text-sm font-black text-white">Add Finance</button>
            </form>
            <form onSubmit={(event) => { event.preventDefault(); void run(() => recordTeamAttendance(teamId, { userId: Number(attendance.userId), matchId: attendance.matchId ? Number(attendance.matchId) : undefined, status: attendance.status as "PRESENT", note: attendance.note }), "Attendance recorded."); }} className="mt-4 grid gap-3">
              <Field label="Player User ID" value={attendance.userId} onChange={(value) => setAttendance((current) => ({ ...current, userId: value }))} type="number" />
              <Field label="Match ID" value={attendance.matchId} onChange={(value) => setAttendance((current) => ({ ...current, matchId: value }))} type="number" />
              <select value={attendance.status} onChange={(event) => setAttendance((current) => ({ ...current, status: event.target.value }))} className="theme-input rounded-xl px-3 py-2 text-sm"><option>PRESENT</option><option>LATE</option><option>ABSENT</option><option>EXCUSED</option></select>
              <button className="rounded-xl border border-emerald-500/40 px-4 py-2 text-sm font-black text-emerald-300">Record Attendance</button>
            </form>
          </Panel>
        </section>
      </div>
    </main>
  );
}

function Panel({ title, children }: { title: string; children: React.ReactNode }) {
  return <section className="theme-panel rounded-3xl p-6"><h2 className="theme-title text-xl font-black">{title}</h2><div className="mt-5">{children}</div></section>;
}

function Metric({ label, value }: { label: string; value: string | number }) {
  return <div className="theme-panel-soft rounded-2xl p-4"><p className="theme-soft text-xs font-black uppercase tracking-[0.2em]">{label}</p><p className="theme-title mt-2 text-2xl font-black">{value}</p></div>;
}

function Card({ title, meta }: { title: string; meta: string }) {
  return <article className="theme-panel-soft mt-3 rounded-2xl p-4"><p className="theme-title font-black">{title}</p><p className="theme-muted mt-1 text-sm">{meta}</p></article>;
}

function Empty({ text }: { text: string }) {
  return <p className="theme-card theme-muted rounded-2xl border-dashed p-8 text-center">{text}</p>;
}

function Field({ label, value, onChange, type = "text" }: { label: string; value: string; onChange: (value: string) => void; type?: string }) {
  return <label><span className="theme-title text-xs font-black uppercase tracking-wider">{label}</span><input type={type} value={value} onChange={(event) => onChange(event.target.value)} className="theme-input mt-2 w-full rounded-xl px-3 py-2 text-sm outline-none" /></label>;
}

function apiMessage(error: unknown, fallback: string) {
  const response = (error as { response?: { data?: { message?: string } } } | null)?.response;
  return response?.data?.message || fallback;
}
