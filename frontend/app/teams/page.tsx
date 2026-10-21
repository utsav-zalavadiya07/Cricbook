"use client";
import { TileInfo } from "@/components/ui";

import { ReactNode, useCallback, useEffect, useMemo, useState } from "react";
import { useConfirmDialog } from "@/components/ui/ConfirmDialog";
import { useI18n } from "@/modules/i18n/I18nProvider";
import AvatarUploader from "@/modules/media/components/AvatarUploader";
import { removeTeamLogo, uploadTeamLogo } from "@/modules/media/services/mediaService";
import { useAuthStore } from "@/store/authStore";
import { getImageUrl } from "@/utils/image";
import {
  createTeam,
  deleteTeam,
  discoverTeams,
  getMyTeams,
  getTeam,
  getTeamInvites,
  getTeamJoinRequests,
  inviteTeamMember,
  leaveTeam,
  removeTeamMember,
  requestToJoinTeam,
  respondTeamJoinRequest,
  respondTeamInvite,
  transferCaptain,
  updateTeamRole,
} from "@/modules/social/services/socialService";
import { Team, TeamInvite, TeamJoinRequest, TeamMember, TeamRole, TeamVisibility } from "@/modules/social/types/social.types";

const teamSizes = [5, 7, 9, 11, 15];

const emptyTeamForm = {
  name: "",
  city: "",
  description: "",
  logoUrl: "",
  maxMembers: 11,
  visibility: "PUBLIC" as TeamVisibility,
};

export default function TeamsPage() {
  const { t } = useI18n();
  const confirmAction = useConfirmDialog();
  const authUser = useAuthStore((state) => state.user);
  const [teams, setTeams] = useState<Team[]>([]);
  const [invites, setInvites] = useState<TeamInvite[]>([]);
  const [joinRequests, setJoinRequests] = useState<TeamJoinRequest[]>([]);
  const [discoverQuery, setDiscoverQuery] = useState("");
  const [discoverCity, setDiscoverCity] = useState("");
  const [discoverVisibility, setDiscoverVisibility] = useState<TeamVisibility | "">("");
  const [discoverOpenOnly, setDiscoverOpenOnly] = useState(true);
  const [discoverResults, setDiscoverResults] = useState<Team[]>([]);
  const [joinMessages, setJoinMessages] = useState<Record<number, string>>({});
  const [selectedTeamId, setSelectedTeamId] = useState<number | null>(null);
  const [selectedTeam, setSelectedTeam] = useState<Team | null>(null);
  const [teamForm, setTeamForm] = useState(emptyTeamForm);
  const [inviteLoginId, setInviteLoginId] = useState("");
  const [status, setStatus] = useState("");
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);

  const loadJoinRequests = useCallback(async () => {
    try {
      const response = await getTeamJoinRequests();
      return response.data;
    } catch {
      return [] as TeamJoinRequest[];
    }
  }, []);

  const loadDiscoverTeams = async () => {
    const response = await discoverTeams({
      query: discoverQuery.trim() || undefined,
      city: discoverCity.trim() || undefined,
      visibility: discoverVisibility || undefined,
      openOnly: discoverOpenOnly,
    });
    setDiscoverResults(response.data);
  };

  const load = async (preferredTeamId?: number | null) => {
    setLoading(true);
    try {
      const [teamsRes, invitesRes, nextJoinRequests] = await Promise.all([getMyTeams(), getTeamInvites(), loadJoinRequests()]);
      const nextTeams = teamsRes.data as Team[];
      const nextInvites = invitesRes.data as TeamInvite[];
      setTeams(nextTeams);
      setInvites(nextInvites);
      setJoinRequests(nextJoinRequests);

      const nextSelectedId = preferredTeamId ?? selectedTeamId ?? (nextTeams.length > 0 ? nextTeams[0].id : null);
      setSelectedTeamId(nextSelectedId);

      if (nextSelectedId) {
        const detailRes = await getTeam(nextSelectedId);
        setSelectedTeam(detailRes.data as Team);
      } else {
        setSelectedTeam(null);
      }
      await loadDiscoverTeams();
    } catch (error) {
      setStatus(errorMessage(error, t("team.status.loadError")));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    let active = true;

    const bootstrap = async () => {
      setLoading(true);
      try {
        const [teamsRes, invitesRes, nextJoinRequests, discoverRes] = await Promise.all([
          getMyTeams(),
          getTeamInvites(),
          loadJoinRequests(),
          discoverTeams({ openOnly: true }),
        ]);
        if (!active) {
          return;
        }

        const nextTeams = teamsRes.data as Team[];
        const nextInvites = invitesRes.data as TeamInvite[];
        const initialTeamId = nextTeams.length > 0 ? nextTeams[0].id : null;

        setTeams(nextTeams);
        setInvites(nextInvites);
        setJoinRequests(nextJoinRequests);
        setDiscoverResults(discoverRes.data);
        setSelectedTeamId(initialTeamId);

        if (initialTeamId) {
          const detailRes = await getTeam(initialTeamId);
          if (active) {
            setSelectedTeam(detailRes.data as Team);
          }
        } else {
          setSelectedTeam(null);
        }
      } catch (error) {
        if (active) {
          setStatus(errorMessage(error, t("team.status.loadError")));
        }
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    };

    bootstrap();

    return () => {
      active = false;
    };
  }, [loadJoinRequests, t]);

  const currentUserId = authUser?.id ?? null;
  const isCaptain = !!selectedTeam && !!currentUserId && selectedTeam.captainId === currentUserId;
  const currentMembership = selectedTeam?.members?.find((member) => member.userId === currentUserId && member.status === "ACTIVE");
  const isTeamManager =
    isCaptain || currentMembership?.role === "VICE_CAPTAIN" || currentMembership?.role === "MANAGER" || currentMembership?.role === "OWNER";
  const activeInvites = useMemo(() => invites.filter((invite) => invite.status === "PENDING"), [invites]);
  const pendingJoinRequests = useMemo(
    () => joinRequests.filter((request) => request.status === "PENDING"),
    [joinRequests]
  );

  const runAction = async (action: () => Promise<unknown>, successMessage: string, nextSelected?: number | null) => {
    setBusy(true);
    setStatus("");
    try {
      await action();
      setStatus(successMessage);
      await load(nextSelected);
    } catch (error) {
      setStatus(errorMessage(error, t("team.status.actionFailed")));
    } finally {
      setBusy(false);
    }
  };

  const runDiscoverSearch = async () => {
    setBusy(true);
    setStatus("");
    try {
      await loadDiscoverTeams();
      setStatus("Team discovery updated.");
    } catch (error) {
      setStatus(errorMessage(error, "Could not load team discovery."));
    } finally {
      setBusy(false);
    }
  };

  const clearDiscoverSearch = async () => {
    setBusy(true);
    setStatus("");
    try {
      setDiscoverQuery("");
      setDiscoverCity("");
      setDiscoverVisibility("");
      setDiscoverOpenOnly(true);
      const response = await discoverTeams({ openOnly: true });
      setDiscoverResults(response.data);
      setStatus("Showing open teams.");
    } catch (error) {
      setStatus(errorMessage(error, "Could not reset team discovery."));
    } finally {
      setBusy(false);
    }
  };

  return (
    <main className="theme-page-player min-h-screen px-4 py-8 sm:px-6">
      <div className="mx-auto max-w-7xl space-y-6">
        <section className="theme-panel rounded-[32px] p-6">
          <div className="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
            <div>
              <p className="text-xs font-semibold uppercase tracking-[0.3em] text-emerald-300/70">{t("team.epic")}</p>
              <h1 className="theme-title mt-2 text-3xl font-black tracking-tight sm:text-4xl">{t("team.heroTitle")}
              <span className="inline-flex align-middle ml-3 mb-1">
                <TileInfo label="Page information" focusable={false}>{t("team.heroBody")}</TileInfo>
              </span></h1>
            </div>
            <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
              <HeroStat label={t("team.myTeams")} value={teams.length} />
              <HeroStat label={t("team.pendingInvites")} value={activeInvites.length} />
              <HeroStat label={t("team.members")} value={teams.reduce((total, team) => total + team.memberCount, 0)} />
              <HeroStat label={t("team.privateTeams")} value={teams.filter((team) => team.visibility === "PRIVATE").length} />
            </div>
          </div>
        </section>

        {status && <p className="theme-panel rounded-2xl px-4 py-3 text-sm text-emerald-200">{status}</p>}

        <section className="grid gap-6 xl:grid-cols-[320px_1fr]">
          <aside className="space-y-6">
            <div className="theme-panel rounded-[28px] p-5">
              <div className="flex items-center justify-between">
                <h2 className="theme-title text-lg font-bold">{t("team.section.myTeams")}</h2>
                <span className="theme-card theme-muted rounded-full px-3 py-1 text-xs font-semibold">{teams.length}</span>
              </div>
              <div className="mt-4 space-y-3">
                {loading && <EmptyCard text={t("team.loadingTeams")} />}
                {!loading && teams.length === 0 && <EmptyCard text={t("team.emptyTeams")} />}
                {teams.map((team) => {
                  const active = selectedTeamId === team.id;
                  return (
                    <button
                      key={team.id}
                      type="button"
                      onClick={() => {
                        setSelectedTeamId(team.id);
                        setStatus("");
                        getTeam(team.id)
                          .then((res) => setSelectedTeam(res.data as Team))
                          .catch((error) => setStatus(errorMessage(error, t("team.openError"))));
                      }}
                      className={`w-full rounded-3xl border px-4 py-4 text-left transition ${
                        active
                          ? "border-emerald-400/40 bg-emerald-500/10"
                          : "theme-card hover:border-white/20 hover:bg-white/[0.03]"
                      }`}
                    >
                      <div className="flex items-start justify-between gap-3">
                        <div className="flex items-center gap-3">
                          <TeamAvatar team={team} size="sm" />
                          <div>
                            <p className="theme-title text-base font-semibold">{team.name}</p>
                            <p className="theme-muted mt-1 text-sm">{team.city}</p>
                          </div>
                        </div>
                        <span className="theme-card theme-muted rounded-full px-2.5 py-1 text-[11px] font-bold uppercase tracking-wide">
                          {translateVisibility(team.visibility ?? "PUBLIC", t)}
                        </span>
                      </div>
                      <div className="theme-muted mt-4 flex items-center justify-between text-xs">
                        <span>
                          {team.memberCount}/{team.maxMembers} {t("common.players")}
                        </span>
                        <span>{team.captainName}</span>
                      </div>
                    </button>
                  );
                })}
              </div>
            </div>

            <div className="theme-panel rounded-[28px] p-5">
              <h2 className="theme-title text-lg font-bold">{t("team.invitations")}</h2>
              <div className="mt-4 space-y-3">
                {invites.length === 0 && <EmptyCard text={t("team.emptyInvites")} />}
                {invites.map((invite) => (
                  <div key={invite.id} className="theme-panel-soft rounded-3xl p-4">
                    <div className="flex items-start justify-between gap-3">
                      <div>
                        <p className="theme-title font-semibold">{invite.teamName}</p>
                        <p className="theme-muted mt-1 text-sm">{t("team.invitedBy", { name: invite.senderName })}</p>
                      </div>
                      <span className="theme-card theme-muted rounded-full px-2.5 py-1 text-[11px] font-bold uppercase tracking-wide">
                        {invite.status}
                      </span>
                    </div>
                    {invite.status === "PENDING" && (
                      <div className="mt-4 flex gap-2">
                        <ActionButton label={t("team.accept")} disabled={busy} variant="primary" onClick={() => runAction(() => respondTeamInvite(invite.id, true), t("team.status.inviteAccepted"))} />
                        <ActionButton label={t("team.decline")} disabled={busy} variant="secondary" onClick={() => runAction(() => respondTeamInvite(invite.id, false), t("team.status.inviteDeclined"))} />
                      </div>
                    )}
                  </div>
                ))}
              </div>
            </div>

            <div className="theme-panel rounded-[28px] p-5">
              <div className="flex items-center justify-between">
                <h2 className="theme-title text-lg font-bold">Join requests</h2>
                <span className="theme-card theme-muted rounded-full px-3 py-1 text-xs font-semibold">{pendingJoinRequests.length}</span>
              </div>
              <div className="mt-4 space-y-3">
                {pendingJoinRequests.length === 0 && <EmptyCard text="No pending join requests." />}
                {pendingJoinRequests.map((request) => (
                  <div key={request.id} className="theme-panel-soft rounded-3xl p-4">
                    <div className="flex items-start justify-between gap-3">
                      <div>
                        <p className="theme-title font-semibold">{request.requesterName}</p>
                        <p className="theme-muted mt-1 text-sm">{request.teamName}</p>
                        {request.message && <p className="theme-muted mt-2 text-xs">{request.message}</p>}
                      </div>
                      <span className="theme-card theme-muted rounded-full px-2.5 py-1 text-[11px] font-bold uppercase tracking-wide">
                        {request.status}
                      </span>
                    </div>
                    <div className="mt-4 flex gap-2">
                      <ActionButton
                        label="Approve"
                        disabled={busy}
                        variant="primary"
                        onClick={() => runAction(() => respondTeamJoinRequest(request.id, true), "Join request approved.", selectedTeamId)}
                      />
                      <ActionButton
                        label="Decline"
                        disabled={busy}
                        variant="secondary"
                        onClick={() => runAction(() => respondTeamJoinRequest(request.id, false), "Join request declined.", selectedTeamId)}
                      />
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </aside>

          <div className="space-y-6">
            <section className="theme-panel rounded-[28px] p-6">
              <div className="flex flex-col gap-4 xl:flex-row xl:items-end xl:justify-between">
                <div>
                  <h2 className="theme-title text-xl font-bold">Discover teams</h2>
                  <p className="theme-muted mt-1 text-sm">Find active teams by name, city, visibility, and open roster slots.</p>
                </div>
                <div className="flex flex-wrap gap-2">
                  <ActionButton label="Clear" disabled={busy} variant="secondary" onClick={clearDiscoverSearch} />
                  <ActionButton label="Search teams" disabled={busy} variant="primary" onClick={runDiscoverSearch} />
                </div>
              </div>
              <div className="mt-5 grid gap-3 md:grid-cols-2 xl:grid-cols-4">
                <Field label="Team or captain">
                  <input value={discoverQuery} onChange={(event) => setDiscoverQuery(event.target.value)} placeholder="Strikers, captain..." className={inputClass} />
                </Field>
                <Field label={t("label.city")}>
                  <input value={discoverCity} onChange={(event) => setDiscoverCity(event.target.value)} placeholder={t("team.placeholder.city")} className={inputClass} />
                </Field>
                <Field label={t("team.field.visibility")}>
                  <select value={discoverVisibility} onChange={(event) => setDiscoverVisibility(event.target.value as TeamVisibility | "")} className={inputClass}>
                    <option value="">Any visibility</option>
                    <option value="PUBLIC">{t("common.public")}</option>
                    <option value="INVITE_ONLY">Invite only</option>
                    <option value="PRIVATE">{t("common.private")}</option>
                  </select>
                </Field>
                <label className="theme-card flex min-h-[54px] items-center gap-3 rounded-2xl px-4 py-3 text-sm font-semibold">
                  <input
                    type="checkbox"
                    checked={discoverOpenOnly}
                    onChange={(event) => setDiscoverOpenOnly(event.target.checked)}
                    className="h-4 w-4 accent-emerald-500"
                  />
                  Open roster spots only
                </label>
              </div>
              <div className="mt-5 grid gap-4 xl:grid-cols-2">
                {discoverResults.length === 0 && <EmptyCard text="No teams match this discovery search." />}
                {discoverResults.map((team) => (
                  <div key={team.id} className="theme-panel-soft rounded-3xl p-5">
                    <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
                      <div className="flex items-start gap-3">
                        <TeamAvatar team={team} size="sm" />
                        <div>
                          <p className="theme-title text-base font-semibold">{team.name}</p>
                          <p className="theme-muted mt-1 text-sm">{team.city} · {team.captainName}</p>
                          <p className="theme-muted mt-2 text-xs">{team.description || "No team description yet."}</p>
                        </div>
                      </div>
                      <span className="theme-card theme-muted rounded-full px-2.5 py-1 text-[11px] font-bold uppercase tracking-wide">
                        {translateVisibility(team.visibility ?? "PUBLIC", t)}
                      </span>
                    </div>
                    <div className="mt-4 grid grid-cols-3 gap-2">
                      <HeroStat label="Members" value={`${team.memberCount}/${team.maxMembers}`} />
                      <HeroStat label="Open" value={team.openSpots ?? Math.max(team.maxMembers - team.memberCount, 0)} />
                      <HeroStat label="Rep" value={`${team.reputationScore ?? 75}%`} />
                    </div>
                    <div className="mt-4 space-y-3">
                      <input
                        value={joinMessages[team.id] || ""}
                        onChange={(event) => setJoinMessages((current) => ({ ...current, [team.id]: event.target.value }))}
                        placeholder="Optional note for the captain"
                        className={inputClass}
                      />
                      <ActionButton
                        label="Request to join"
                        disabled={busy || (team.openSpots ?? team.maxMembers - team.memberCount) <= 0}
                        variant="primary"
                        onClick={() =>
                          runAction(
                            () => requestToJoinTeam(team.id, joinMessages[team.id]?.trim() || undefined),
                            "Join request sent.",
                            selectedTeamId
                          )
                        }
                      />
                    </div>
                  </div>
                ))}
              </div>
            </section>

            <section className="theme-panel rounded-[28px] p-6">
              <div className="flex items-center justify-between gap-4">
                <div>
                  <h2 className="theme-title text-xl font-bold">{t("team.createTitle")}</h2>
                  <p className="theme-muted mt-1 text-sm">{t("team.createBody")}</p>
                </div>
              </div>
              <div className="mt-5 grid gap-3 md:grid-cols-2 xl:grid-cols-3">
                <Field label={t("team.field.teamName")}>
                  <input value={teamForm.name} onChange={(e) => setTeamForm({ ...teamForm, name: e.target.value })} placeholder={t("team.placeholder.teamName")} className={inputClass} />
                </Field>
                <Field label={t("label.city")}>
                  <input value={teamForm.city} onChange={(e) => setTeamForm({ ...teamForm, city: e.target.value })} placeholder={t("team.placeholder.city")} className={inputClass} />
                </Field>
                <Field label={t("team.field.logoUrl")}>
                  <input value={teamForm.logoUrl} onChange={(e) => setTeamForm({ ...teamForm, logoUrl: e.target.value })} placeholder={t("team.placeholder.logoUrl")} className={inputClass} />
                </Field>
                <Field label={t("team.field.teamSize")}>
                  <select value={teamForm.maxMembers} onChange={(e) => setTeamForm({ ...teamForm, maxMembers: Number(e.target.value) })} className={inputClass}>
                    {teamSizes.map((size) => (
                      <option key={size} value={size}>
                        {size} {t("common.players")}
                      </option>
                    ))}
                  </select>
                </Field>
                <Field label={t("team.field.visibility")}>
                  <select value={teamForm.visibility} onChange={(e) => setTeamForm({ ...teamForm, visibility: e.target.value as TeamVisibility })} className={inputClass}>
                    <option value="PUBLIC">{t("common.public")}</option>
                    <option value="INVITE_ONLY">Invite only</option>
                    <option value="PRIVATE">{t("common.private")}</option>
                  </select>
                </Field>
                <Field label={t("team.field.description")} className="md:col-span-2 xl:col-span-1">
                  <textarea value={teamForm.description} onChange={(e) => setTeamForm({ ...teamForm, description: e.target.value })} placeholder={t("team.placeholder.description")} className={`${inputClass} min-h-[112px]`} />
                </Field>
              </div>
              <div className="mt-5 flex justify-end">
                <ActionButton
                  label={busy ? t("team.creating") : t("team.create")}
                  disabled={busy}
                  variant="primary"
                  onClick={() =>
                    runAction(() => createTeam({ ...teamForm, sport: "Cricket" }), t("team.status.teamCreated")).then(() => setTeamForm(emptyTeamForm))
                  }
                />
              </div>
            </section>

            <section className="theme-panel rounded-[28px] p-6">
              {!selectedTeam && !loading && <EmptyCard text={t("team.selectPrompt")} />}
              {selectedTeam && (
                <div className="space-y-6">
                  <div className="flex flex-col gap-5 xl:flex-row xl:items-start xl:justify-between">
                    <div className="flex items-start gap-4">
                      <TeamAvatar team={selectedTeam} />
                      <div>
                        <p className="text-xs font-semibold uppercase tracking-[0.3em] text-emerald-300/70">
                          {translateVisibility(selectedTeam.visibility ?? "PUBLIC", t)} {t("team.squad")}
                        </p>
                        <h2 className="theme-title mt-1 text-2xl font-black">{selectedTeam.name}</h2>
                        <p className="theme-muted mt-2 max-w-2xl text-sm">{selectedTeam.description || t("team.noDescription")}</p>
                      </div>
                    </div>
                    <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
                      <HeroStat label={t("team.city")} value={selectedTeam.city} />
                      <HeroStat label={t("team.captain")} value={selectedTeam.captainName} />
                      <HeroStat label={t("team.playersLabel")} value={`${selectedTeam.memberCount}/${selectedTeam.maxMembers}`} />
                      <HeroStat label="Open spots" value={selectedTeam.openSpots ?? Math.max(selectedTeam.maxMembers - selectedTeam.memberCount, 0)} />
                      <HeroStat label="Reputation" value={`${selectedTeam.reputationScore ?? 75}%`} />
                    </div>
                  </div>

                  {isCaptain && (
                    <AvatarUploader
                      title={t("team.media.logoTitle") === "team.media.logoTitle" ? "Team Logo" : t("team.media.logoTitle")}
                      body={
                        t("team.media.logoBody") === "team.media.logoBody"
                          ? "Upload a team logo for lineups, tournaments, leaderboards, and team cards."
                          : t("team.media.logoBody")
                      }
                      imageUrl={selectedTeam.logoUrl}
                      fallbackText={selectedTeam.name}
                      onUpload={async (file) => {
                        const response = await uploadTeamLogo(selectedTeam.id, file);
                        return response.data;
                      }}
                      onRemove={async () => {
                        const response = await removeTeamLogo(selectedTeam.id);
                        return response.data;
                      }}
                      onChange={(asset) => {
                        setSelectedTeam({ ...selectedTeam, logoUrl: asset.imageUrl });
                        setTeams((current) => current.map((team) => (team.id === selectedTeam.id ? { ...team, logoUrl: asset.imageUrl } : team)));
                      }}
                    />
                  )}

                  <div className="grid gap-6 xl:grid-cols-[1.2fr_0.8fr]">
                    <div className="theme-panel-soft rounded-3xl p-5">
                      <div className="flex items-center justify-between">
                        <h3 className="theme-title text-lg font-bold">{t("team.section.members")}</h3>
                        <span className="theme-muted text-sm">{t("team.activeCount", { count: selectedTeam.members?.length ?? 0 })}</span>
                      </div>
                      <div className="mt-4 space-y-3">
                        {selectedTeam.members?.map((member) => (
                          <MemberCard
                            key={member.id}
                            member={member}
                            currentUserId={currentUserId}
                            isCaptain={isCaptain}
                            busy={busy}
                            onTransfer={(userId) => runAction(() => transferCaptain(selectedTeam.id, userId), t("team.status.captainTransferred"), selectedTeam.id)}
                            onRoleChange={(userId, role) => runAction(() => updateTeamRole(selectedTeam.id, userId, role), t("team.status.roleUpdated"), selectedTeam.id)}
                            onRemove={(userId) => runAction(() => removeTeamMember(selectedTeam.id, userId), t("team.status.memberRemoved"), selectedTeam.id)}
                          />
                        ))}
                        {(!selectedTeam.members || selectedTeam.members.length === 0) && <EmptyCard text={t("team.emptyMembers")} />}
                      </div>
                    </div>

                    <div className="space-y-4">
                      <div className="theme-panel-soft rounded-3xl p-5">
                        <h3 className="theme-title text-lg font-bold">{t("team.invitePlayer")}</h3>
                        <p className="theme-muted mt-1 text-sm">
                          {isTeamManager ? t("team.inviteBody") : "Only captains, vice captains, and managers can invite players."}
                        </p>
                        {isTeamManager && (
                          <div className="mt-4 space-y-3">
                            <input value={inviteLoginId} onChange={(e) => setInviteLoginId(e.target.value)} placeholder={t("team.invitePlaceholder")} className={inputClass} />
                            <ActionButton
                              label={t("team.sendInvite")}
                              disabled={busy || !inviteLoginId.trim()}
                              variant="primary"
                              onClick={() =>
                                runAction(() => inviteTeamMember(selectedTeam.id, inviteLoginId.trim()), t("team.status.inviteSent"), selectedTeam.id).then(() => setInviteLoginId(""))
                              }
                            />
                          </div>
                        )}
                      </div>

                      <div className="theme-panel-soft rounded-3xl p-5">
                        <h3 className="theme-title text-lg font-bold">{t("team.operations")}</h3>
                        <div className="mt-4 space-y-3">
                          <ActionButton label={t("team.refresh")} disabled={busy} variant="secondary" onClick={() => runAction(() => getTeam(selectedTeam.id), t("team.status.teamRefreshed"), selectedTeam.id)} />
                          <ActionButton label={t("team.leave")} disabled={busy} variant="secondary" onClick={() => runAction(() => leaveTeam(selectedTeam.id), t("team.status.leftTeam"), null)} />
                          {isCaptain && (
                            <ActionButton
                              label={t("team.delete")}
                              disabled={busy}
                              variant="danger"
                              onClick={async () => {
                                const confirmed = await confirmAction({
                                  title: t("team.delete"),
                                  description: t("team.deleteConfirm"),
                                  confirmLabel: t("team.delete"),
                                  tone: "danger",
                                });
                                if (!confirmed) return;
                                runAction(() => deleteTeam(selectedTeam.id), t("team.status.teamDeleted"), null);
                              }}
                            />
                          )}
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              )}
            </section>
          </div>
        </section>
      </div>
    </main>
  );
}

function MemberCard({
  member,
  currentUserId,
  isCaptain,
  busy,
  onTransfer,
  onRoleChange,
  onRemove,
}: {
  member: TeamMember;
  currentUserId: number | null;
  isCaptain: boolean;
  busy: boolean;
  onTransfer: (userId: number) => void;
  onRoleChange: (userId: number, role: TeamRole) => void;
  onRemove: (userId: number) => void;
}) {
  const { t } = useI18n();
  const canManage = isCaptain && currentUserId !== member.userId;

  return (
    <div className="theme-card rounded-3xl p-4">
      <div className="flex flex-col gap-4 xl:flex-row xl:items-center xl:justify-between">
        <div>
          <div className="flex flex-wrap items-center gap-2">
            <p className="theme-title font-semibold">{member.name}</p>
            <span className="theme-card theme-muted rounded-full px-2.5 py-1 text-[11px] font-bold uppercase tracking-wide">
              {member.role.replace("_", " ")}
            </span>
            <span className="rounded-full border border-emerald-500/25 bg-emerald-500/10 px-2.5 py-1 text-[11px] font-bold uppercase tracking-wide text-emerald-200">
              {member.status}
            </span>
          </div>
          <p className="theme-muted mt-1 text-sm">{member.email}</p>
        </div>
        {canManage && (
          <div className="flex flex-wrap gap-2">
            {member.role !== "VICE_CAPTAIN" && <ActionButton label={t("team.role.viceCaptain")} disabled={busy} variant="secondary" onClick={() => onRoleChange(member.userId, "VICE_CAPTAIN")} />}
            {member.role !== "MANAGER" && <ActionButton label="Manager" disabled={busy} variant="secondary" onClick={() => onRoleChange(member.userId, "MANAGER")} />}
            {member.role !== "PLAYER" && <ActionButton label={t("team.role.player")} disabled={busy} variant="secondary" onClick={() => onRoleChange(member.userId, "PLAYER")} />}
            {member.role !== "CAPTAIN" && <ActionButton label={t("team.role.transferCaptain")} disabled={busy} variant="secondary" onClick={() => onTransfer(member.userId)} />}
            <ActionButton label={t("team.remove")} disabled={busy} variant="danger" onClick={() => onRemove(member.userId)} />
          </div>
        )}
      </div>
    </div>
  );
}

function HeroStat({ label, value }: { label: string; value: string | number }) {
  return (
    <div className="theme-card rounded-2xl px-4 py-3">
      <p className="theme-soft break-words text-[11px] font-semibold uppercase leading-5 tracking-[0.24em]">{label}</p>
      <p className="theme-title mt-2 text-lg font-bold">{value}</p>
    </div>
  );
}

function TeamAvatar({ team, size = "lg" }: { team: Team; size?: "sm" | "lg" }) {
  const className = size === "sm" ? "h-11 w-11 rounded-2xl text-sm" : "h-16 w-16 rounded-3xl text-xl";
  const imageUrl = getImageUrl(team.logoUrl);
  return (
    <div className={`flex shrink-0 items-center justify-center overflow-hidden bg-emerald-500/85 font-black text-white ${className}`}>
      {imageUrl ? (
        // eslint-disable-next-line @next/next/no-img-element
        <img src={imageUrl} alt={team.name} className="h-full w-full object-cover" />
      ) : (
        (team.name || "T").slice(0, 2).toUpperCase()
      )}
    </div>
  );
}

function Field({ label, children, className }: { label: string; children: ReactNode; className?: string }) {
  return (
    <label className={`block ${className || ""}`}>
      <span className="theme-text mb-2 block break-words text-sm font-semibold leading-6">{label}</span>
      {children}
    </label>
  );
}

function ActionButton({
  label,
  onClick,
  variant,
  disabled,
}: {
  label: string;
  onClick: () => void | Promise<void>;
  variant: "primary" | "secondary" | "danger";
  disabled?: boolean;
}) {
  const styles =
    variant === "primary"
      ? "border-emerald-400/30 bg-emerald-500 text-white hover:bg-emerald-400"
      : variant === "danger"
        ? "border-red-500/30 bg-red-500/10 text-red-200 hover:bg-red-500/15"
        : "theme-card theme-text hover:bg-white/[0.08]";

  return (
    <button type="button" onClick={onClick} disabled={disabled} className={`rounded-2xl border px-4 py-2.5 text-center text-sm font-semibold leading-5 transition disabled:cursor-not-allowed disabled:opacity-60 ${styles}`}>
      {label}
    </button>
  );
}

function EmptyCard({ text }: { text: string }) {
  return <div className="theme-card theme-muted rounded-3xl border-dashed p-5 text-center text-sm">{text}</div>;
}

function errorMessage(error: unknown, fallback: string) {
  const apiError = error as { response?: { data?: { error?: string; message?: string } } };
  return apiError.response?.data?.message || apiError.response?.data?.error || fallback;
}

function translateVisibility(visibility: TeamVisibility, t: (key: string, values?: Record<string, string | number>) => string) {
  if (visibility === "PRIVATE") {
    return t("common.private");
  }
  if (visibility === "INVITE_ONLY") {
    return "Invite only";
  }
  return t("common.public");
}

const inputClass =
  "theme-input w-full rounded-2xl px-4 py-3 text-sm outline-none transition focus:border-emerald-400/40";
