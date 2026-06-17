"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import {
  AdminMetric,
  AdminMetricCard,
  AdminPageHeader,
  AdminStatusBadge,
  AdminActionButton,
  AdminDataTable,
  AdminTableRow,
  formatAdminDate,
} from "./AdminPrimitives";
import {
  AdminOpsUser,
  AdminPagePermissionCatalogItem,
  AdminPermission,
  AdminRbacMatrix,
  AdminRole,
  PermissionAudit,
  assignAdminRole,
  createAdminRole,
  getAdminPagePermissions,
  getAdminPermissions,
  getAdminRbacMatrix,
  getAdminRbacUsers,
  getAdminRoles,
  getPermissionAudit,
} from "@/modules/admin/services/adminService";

type AccessLevel = "NONE" | "READ" | "WRITE" | "FULL";
type PortalKey = "PLAYER" | "OWNER" | "ADMIN";
type RbacPanel = "BUILDER" | "MATRIX" | "ASSIGNMENTS" | "AUDIT";

const accessLabels: Record<AccessLevel, string> = {
  NONE: "No access",
  READ: "Read only",
  WRITE: "Write only",
  FULL: "Full access",
};

const portalLabels: Record<PortalKey, string> = {
  PLAYER: "Player Portal",
  OWNER: "Owner Portal",
  ADMIN: "Admin Portal",
};

const panelLabels: Record<RbacPanel, string> = {
  BUILDER: "Role Builder",
  MATRIX: "Matrix",
  ASSIGNMENTS: "Assignments",
  AUDIT: "Audit",
};

export default function AdminRoleBuilderPage() {
  const [roles, setRoles] = useState<AdminRole[]>([]);
  const [permissions, setPermissions] = useState<AdminPermission[]>([]);
  const [pages, setPages] = useState<AdminPagePermissionCatalogItem[]>([]);
  const [adminUsers, setAdminUsers] = useState<AdminOpsUser[]>([]);
  const [audit, setAudit] = useState<PermissionAudit[]>([]);
  const [matrix, setMatrix] = useState<AdminRbacMatrix | null>(null);
  const [selectedRoleId, setSelectedRoleId] = useState<number | "NEW">("NEW");
  const [roleName, setRoleName] = useState("OWNER_OPERATIONS_READ_ONLY");
  const [description, setDescription] = useState("Owner operations staff with page-level access.");
  const [pageAccess, setPageAccess] = useState<Record<string, AccessLevel>>({});
  const [apiPermissionCodes, setApiPermissionCodes] = useState<string[]>([]);
  const [activePortal, setActivePortal] = useState<PortalKey>("PLAYER");
  const [activePanel, setActivePanel] = useState<RbacPanel>("BUILDER");
  const [expandedApiModules, setExpandedApiModules] = useState<string[]>([]);
  const [query, setQuery] = useState("");
  const [assignUserId, setAssignUserId] = useState("");
  const [assignRoleName, setAssignRoleName] = useState("");
  const [notice, setNotice] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const [rolesRes, permissionsRes, pagesRes, usersRes, auditRes, matrixRes] = await Promise.all([
        getAdminRoles(),
        getAdminPermissions(),
        getAdminPagePermissions(),
        getAdminRbacUsers(),
        getPermissionAudit(),
        getAdminRbacMatrix(),
      ]);
      setRoles(rolesRes.data);
      setPermissions(permissionsRes.data);
      setPages(pagesRes.data);
      setAdminUsers(usersRes.data);
      setAudit(auditRes.data);
      setMatrix(matrixRes.data);
      setAssignRoleName((current) => current || rolesRes.data[0]?.name || "");
      if (selectedRoleId !== "NEW" && !rolesRes.data.some((role) => role.id === selectedRoleId)) {
        setSelectedRoleId("NEW");
      }
    } catch (loadError) {
      setError(readableError(loadError));
    } finally {
      setLoading(false);
    }
  }, [selectedRoleId]);

  useEffect(() => {
    const timer = window.setTimeout(() => void load(), 0);
    return () => window.clearTimeout(timer);
  }, [load]);

  const selectedRole = useMemo(() => roles.find((role) => role.id === selectedRoleId), [roles, selectedRoleId]);

  useEffect(() => {
    if (!selectedRole) {
      return;
    }
    const timer = window.setTimeout(() => {
      setRoleName(selectedRole.name);
      setDescription(selectedRole.description);
      setPageAccess(accessFromRole(selectedRole, pages));
      setApiPermissionCodes(selectedRole.permissions.filter((permission) => !isPagePermission(permission.code, pages)).map((permission) => permission.code));
    }, 0);
    return () => window.clearTimeout(timer);
  }, [pages, selectedRole]);

  const pagePermissionCodes = useMemo(() => new Set(pages.flatMap((page) => [page.permissions.read, page.permissions.write, page.permissions.full])), [pages]);
  const apiPermissions = useMemo(() => permissions.filter((permission) => !pagePermissionCodes.has(permission.code)), [pagePermissionCodes, permissions]);
  const groupedApiPermissions = useMemo(() => groupPermissions(apiPermissions), [apiPermissions]);
  const visiblePages = useMemo(() => {
    const needle = query.trim().toLowerCase();
    return pages.filter((page) => page.portal === activePortal).filter((page) => {
      if (!needle) return true;
      return [page.label, page.route, page.description, page.key].join(" ").toLowerCase().includes(needle);
    });
  }, [activePortal, pages, query]);
  const selectedPermissionCodes = useMemo(() => buildPermissionCodes(pages, pageAccess, apiPermissionCodes), [apiPermissionCodes, pageAccess, pages]);
  const portalCounts = useMemo(() => {
    return (Object.keys(portalLabels) as PortalKey[]).reduce<Record<PortalKey, { total: number; selected: number }>>((accumulator, portal) => {
      const portalPages = pages.filter((page) => page.portal === portal);
      accumulator[portal] = {
        total: portalPages.length,
        selected: portalPages.filter((page) => (pageAccess[page.key] || "NONE") !== "NONE").length,
      };
      return accumulator;
    }, {
      PLAYER: { total: 0, selected: 0 },
      OWNER: { total: 0, selected: 0 },
      ADMIN: { total: 0, selected: 0 },
    });
  }, [pageAccess, pages]);
  const apiModuleSummaries = useMemo(() => Object.entries(groupedApiPermissions)
    .map(([module, items]) => ({
      module,
      items,
      selected: items.filter((permission) => apiPermissionCodes.includes(permission.code)).length,
    }))
    .sort((first, second) => first.module.localeCompare(second.module)), [apiPermissionCodes, groupedApiPermissions]);

  const metrics = useMemo<AdminMetric[]>(() => [
    { label: "Roles", value: roles.length, detail: "System and custom roles", tone: "emerald" },
    { label: "Portal Pages", value: pages.length, detail: "Player, owner, and admin route controls", tone: "sky" },
    { label: "Selected Permissions", value: selectedPermissionCodes.length, detail: "Codes saved to this role", tone: "violet" },
    { label: "RBAC Gaps", value: matrix?.summary.rolesWithoutPermissions ?? 0, detail: `${matrix?.summary.inactiveRoles ?? 0} inactive roles`, tone: "amber" },
  ], [matrix?.summary.inactiveRoles, matrix?.summary.rolesWithoutPermissions, pages.length, roles.length, selectedPermissionCodes.length]);

  const auditRows = useMemo<AdminTableRow[]>(() => audit.map((item) => ({
    id: item.id,
    title: item.action,
    subtitle: item.details,
    meta: [item.actor, item.entity],
    status: item.action,
    updatedAt: item.timestamp,
  })), [audit]);

  const handleNewRole = () => {
    setSelectedRoleId("NEW");
    setRoleName("CUSTOM_PORTAL_ROLE");
    setDescription("Custom role with page-level access.");
    setPageAccess({});
    setApiPermissionCodes([]);
    setNotice("");
    setError("");
  };

  const handleSave = async () => {
    const normalizedName = roleName.trim().toUpperCase().replace(/\s+/g, "_");
    if (!normalizedName) {
      setError("Role name is required.");
      return;
    }
    setSaving(true);
    setError("");
    setNotice("");
    try {
      const response = await createAdminRole({
        name: normalizedName,
        description: description.trim() || normalizedName,
        permissionCodes: selectedPermissionCodes,
      });
      setNotice(`Saved role ${response.data.name} with ${selectedPermissionCodes.length} permissions.`);
      setSelectedRoleId(response.data.id);
      await load();
    } catch (saveError) {
      setError(readableError(saveError));
    } finally {
      setSaving(false);
    }
  };

  const handleAssignRole = async () => {
    const userId = Number(assignUserId);
    if (!userId || !assignRoleName) {
      setError("Select an admin user and role before assigning.");
      return;
    }
    setSaving(true);
    setError("");
    setNotice("");
    try {
      await assignAdminRole(userId, assignRoleName);
      setNotice(`Assigned ${assignRoleName} to admin user #${userId}.`);
      await load();
    } catch (assignError) {
      setError(readableError(assignError));
    } finally {
      setSaving(false);
    }
  };

  const setPortalAccess = (level: AccessLevel) => {
    setPageAccess((current) => {
      const next = { ...current };
      for (const page of pages.filter((item) => item.portal === activePortal)) {
        next[page.key] = level;
      }
      return next;
    });
  };

  const toggleApiPermission = (code: string) => {
    setApiPermissionCodes((current) => current.includes(code) ? current.filter((item) => item !== code) : [...current, code]);
  };

  const toggleApiModule = (module: string) => {
    setExpandedApiModules((current) => current.includes(module) ? current.filter((item) => item !== module) : [...current, module]);
  };

  return (
    <div className="space-y-6">
      <AdminPageHeader
        eyebrow="EPIC 38 - RBAC control center"
        title="Roles, page permissions, and access assignment"
        description="Create a role, choose page-level access for Player, Owner, and Admin portals, add backend API permissions, then assign that role to an admin user."
        actions={
          <>
            <AdminActionButton label="New Role" onClick={handleNewRole} />
            <AdminActionButton label={saving ? "Saving..." : "Save Role"} intent="primary" disabled={saving || loading} onClick={() => void handleSave()} />
          </>
        }
      />

      {notice ? <div className="rounded-2xl border border-emerald-500/20 bg-emerald-500/10 px-4 py-3 text-sm font-black text-emerald-200">{notice}</div> : null}
      {error ? <div className="rounded-2xl border border-rose-500/20 bg-rose-500/10 px-4 py-3 text-sm font-black text-rose-200">{error}</div> : null}

      <section className="grid gap-4 md:grid-cols-2 2xl:grid-cols-4">
        {metrics.map((metric) => <AdminMetricCard key={metric.label} {...metric} />)}
      </section>

      <section className="grid gap-5 xl:grid-cols-[320px_minmax(0,1fr)]">
        <aside className="space-y-4 xl:sticky xl:top-6 xl:self-start">
          <div className="rounded-3xl border border-white/10 bg-slate-900/75 p-4 shadow-xl">
            <div className="flex items-center justify-between gap-3">
              <div>
                <h2 className="text-lg font-black text-white">Roles</h2>
                <p className="mt-1 text-xs text-slate-500">Select or create a role.</p>
              </div>
              <AdminActionButton label="New" onClick={handleNewRole} />
            </div>
            <div className="mt-4 max-h-[430px] space-y-2 overflow-y-auto pr-1">
              <button
                type="button"
                onClick={handleNewRole}
                className={`w-full rounded-2xl border px-4 py-3 text-left transition ${selectedRoleId === "NEW" ? "border-emerald-500/35 bg-emerald-500/15" : "border-white/10 bg-slate-950/40 hover:bg-white/5"}`}
              >
                <p className="font-black text-white">Create new role</p>
                <p className="mt-1 text-xs text-slate-500">Start from a blank matrix.</p>
              </button>
              {roles.map((role) => (
                <button
                  key={role.id}
                  type="button"
                  onClick={() => setSelectedRoleId(role.id)}
                  className={`w-full rounded-2xl border px-4 py-3 text-left transition ${selectedRoleId === role.id ? "border-emerald-500/35 bg-emerald-500/15" : "border-white/10 bg-slate-950/40 hover:bg-white/5"}`}
                >
                  <div className="flex items-start justify-between gap-3">
                    <div>
                      <p className="text-sm font-black text-white">{role.name}</p>
                      <p className="mt-1 line-clamp-2 text-xs text-slate-500">{role.description}</p>
                    </div>
                    <AdminStatusBadge value={role.systemRole ? "SYSTEM" : "CUSTOM"} />
                  </div>
                  <p className="mt-2 text-xs font-semibold text-slate-400">{role.permissions.length} permissions</p>
                </button>
              ))}
            </div>
          </div>

          <div className="rounded-3xl border border-white/10 bg-slate-900/75 p-4 shadow-xl">
            <h2 className="text-lg font-black text-white">Current scope</h2>
            <div className="mt-4 space-y-3">
              {(Object.keys(portalLabels) as PortalKey[]).map((portal) => (
                <button
                  key={portal}
                  type="button"
                  onClick={() => {
                    setActivePanel("BUILDER");
                    setActivePortal(portal);
                  }}
                  className={`flex w-full items-center justify-between rounded-2xl border px-4 py-3 text-left transition ${activePortal === portal && activePanel === "BUILDER" ? "border-emerald-500/35 bg-emerald-500/15" : "border-white/10 bg-slate-950/45 hover:bg-white/5"}`}
                >
                  <span>
                    <span className="block text-sm font-black text-white">{portalLabels[portal]}</span>
                    <span className="mt-1 block text-xs text-slate-500">{portalCounts[portal].selected} of {portalCounts[portal].total} pages selected</span>
                  </span>
                  <span className="rounded-full bg-slate-800 px-2.5 py-1 text-xs font-black text-slate-300">{portalCounts[portal].selected}</span>
                </button>
              ))}
            </div>
          </div>
        </aside>

        <section className="space-y-5">
          <div className="rounded-3xl border border-white/10 bg-slate-900/75 p-2 shadow-xl">
            <div className="grid gap-2 sm:grid-cols-4">
              {(Object.keys(panelLabels) as RbacPanel[]).map((panel) => (
                <button
                  key={panel}
                  type="button"
                  onClick={() => setActivePanel(panel)}
                  className={`rounded-2xl px-4 py-3 text-sm font-black transition ${activePanel === panel ? "bg-emerald-500 text-slate-950" : "bg-slate-950/60 text-slate-300 hover:bg-white/10"}`}
                >
                  {panelLabels[panel]}
                </button>
              ))}
            </div>
          </div>

          {activePanel === "BUILDER" ? (
            <>
              <div className="rounded-3xl border border-white/10 bg-slate-900/75 p-5 shadow-xl">
                <div className="grid gap-4 xl:grid-cols-[minmax(0,1fr)_minmax(0,1.4fr)]">
                  <label className="block">
                    <span className="text-xs font-black uppercase tracking-[0.18em] text-slate-500">Role name</span>
                    <input
                      value={roleName}
                      onChange={(event) => setRoleName(event.target.value)}
                      placeholder="OWNER_FINANCE_READ_ONLY"
                      className="mt-2 h-11 w-full rounded-2xl border border-white/10 bg-slate-950/80 px-4 text-sm font-black uppercase text-white outline-none placeholder:text-slate-600 focus:border-emerald-400/60"
                    />
                  </label>
                  <label className="block">
                    <span className="text-xs font-black uppercase tracking-[0.18em] text-slate-500">Description</span>
                    <input
                      value={description}
                      onChange={(event) => setDescription(event.target.value)}
                      placeholder="What this role can do"
                      className="mt-2 h-11 w-full rounded-2xl border border-white/10 bg-slate-950/80 px-4 text-sm font-semibold text-white outline-none placeholder:text-slate-600 focus:border-emerald-400/60"
                    />
                  </label>
                </div>
                <div className="mt-4 flex flex-col gap-3 xl:flex-row xl:items-center xl:justify-between">
                  <div className="flex flex-wrap gap-2">
                    <AdminActionButton label="Clear Portal" onClick={() => setPortalAccess("NONE")} />
                    <AdminActionButton label="Read Only" onClick={() => setPortalAccess("READ")} />
                    <AdminActionButton label="Write Only" onClick={() => setPortalAccess("WRITE")} />
                    <AdminActionButton label="Full Access" intent="primary" onClick={() => setPortalAccess("FULL")} />
                  </div>
                  <p className="text-xs font-black uppercase tracking-[0.18em] text-slate-500">{selectedPermissionCodes.length} total permission codes</p>
                </div>
              </div>

              <div className="rounded-3xl border border-white/10 bg-slate-900/75 shadow-xl">
                <div className="border-b border-white/10 p-4">
                  <div className="flex flex-col gap-4 xl:flex-row xl:items-end xl:justify-between">
                    <div>
                      <h2 className="text-xl font-black text-white">Page access matrix</h2>
                      <p className="mt-1 text-sm text-slate-400">Only one portal is shown at a time to keep the page compact.</p>
                    </div>
                    <input
                      value={query}
                      onChange={(event) => setQuery(event.target.value)}
                      placeholder="Search page or route"
                      className="h-11 w-full rounded-2xl border border-white/10 bg-slate-950/80 px-4 text-sm font-semibold text-white outline-none placeholder:text-slate-600 focus:border-emerald-400/60 xl:w-[320px]"
                    />
                  </div>
                  <div className="mt-4 flex gap-2 overflow-x-auto">
                    {(Object.keys(portalLabels) as PortalKey[]).map((portal) => (
                      <button
                        key={portal}
                        type="button"
                        onClick={() => setActivePortal(portal)}
                        className={`whitespace-nowrap rounded-2xl border px-4 py-2 text-sm font-black ${activePortal === portal ? "border-emerald-500/35 bg-emerald-500/15 text-emerald-200" : "border-white/10 bg-slate-950/70 text-slate-300"}`}
                      >
                        {portalLabels[portal]} · {portalCounts[portal].selected}/{portalCounts[portal].total}
                      </button>
                    ))}
                  </div>
                </div>
                <div className="max-h-[460px] overflow-auto">
                  <table className="min-w-full divide-y divide-white/10">
                    <thead className="sticky top-0 z-10 bg-slate-950">
                      <tr>
                        <th className="px-4 py-3 text-left text-[11px] font-black uppercase tracking-[0.2em] text-slate-500">Page</th>
                        <th className="px-4 py-3 text-left text-[11px] font-black uppercase tracking-[0.2em] text-slate-500">Route</th>
                        <th className="px-4 py-3 text-left text-[11px] font-black uppercase tracking-[0.2em] text-slate-500">Access</th>
                        <th className="px-4 py-3 text-left text-[11px] font-black uppercase tracking-[0.2em] text-slate-500">Codes</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-white/10">
                      {visiblePages.map((page) => (
                        <tr key={page.key} className="bg-slate-900/35 align-top hover:bg-emerald-500/[0.04]">
                          <td className="min-w-[240px] px-4 py-3">
                            <p className="text-sm font-black text-white">{page.label}</p>
                            <p className="mt-1 line-clamp-1 text-xs text-slate-500">{page.description}</p>
                          </td>
                          <td className="whitespace-nowrap px-4 py-3 text-xs font-semibold text-slate-400">{page.route}</td>
                          <td className="min-w-[170px] px-4 py-3">
                            <select
                              value={pageAccess[page.key] || "NONE"}
                              onChange={(event) => setPageAccess((current) => ({ ...current, [page.key]: event.target.value as AccessLevel }))}
                              className="h-10 w-full rounded-xl border border-white/10 bg-slate-950/80 px-3 text-xs font-black text-white outline-none focus:border-emerald-400/60"
                            >
                              {(Object.keys(accessLabels) as AccessLevel[]).map((level) => <option key={level} value={level}>{accessLabels[level]}</option>)}
                            </select>
                          </td>
                          <td className="min-w-[300px] px-4 py-3">
                            <div className="flex flex-wrap gap-1.5">
                              {codesForLevel(page, pageAccess[page.key] || "NONE").map((code) => (
                                <span key={code} className="rounded-full border border-white/10 bg-slate-950/70 px-2.5 py-1 text-[11px] font-semibold text-slate-300">{code}</span>
                              ))}
                              {(pageAccess[page.key] || "NONE") === "NONE" ? <span className="text-xs font-semibold text-slate-600">No access</span> : null}
                            </div>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>

              <div className="rounded-3xl border border-white/10 bg-slate-900/75 p-5 shadow-xl">
                <div className="flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
                  <div>
                    <h2 className="text-xl font-black text-white">Backend/API permissions</h2>
                    <p className="mt-1 text-sm text-slate-400">Modules are collapsed by default so the RBAC page stays short.</p>
                  </div>
                  <p className="text-xs font-black uppercase tracking-[0.18em] text-slate-500">{apiPermissionCodes.length} selected</p>
                </div>
                <div className="mt-5 grid gap-3 xl:grid-cols-2">
                  {apiModuleSummaries.map(({ module, items, selected }) => {
                    const expanded = expandedApiModules.includes(module);
                    return (
                      <div key={module} className="rounded-2xl border border-white/10 bg-slate-950/45">
                        <button
                          type="button"
                          onClick={() => toggleApiModule(module)}
                          className="flex w-full items-center justify-between gap-3 px-4 py-3 text-left"
                        >
                          <span>
                            <span className="block font-black text-white">{module}</span>
                            <span className="mt-1 block text-xs font-semibold text-slate-500">{selected} selected · {items.length} available</span>
                          </span>
                          <span className="rounded-full bg-slate-800 px-3 py-1 text-xs font-black text-slate-300">{expanded ? "Hide" : "Open"}</span>
                        </button>
                        {expanded ? (
                          <div className="max-h-[310px] space-y-2 overflow-y-auto border-t border-white/10 p-3">
                            {items.map((permission) => (
                              <label key={permission.code} className="flex cursor-pointer items-start gap-3 rounded-xl border border-white/10 bg-slate-900/60 p-3 hover:bg-white/[0.04]">
                                <input
                                  type="checkbox"
                                  checked={apiPermissionCodes.includes(permission.code)}
                                  onChange={() => toggleApiPermission(permission.code)}
                                  className="mt-1 h-4 w-4 accent-emerald-500"
                                />
                                <span>
                                  <span className="block text-sm font-black text-slate-100">{permission.code}</span>
                                  <span className="mt-1 block text-xs leading-5 text-slate-500">{permission.description}</span>
                                </span>
                              </label>
                            ))}
                          </div>
                        ) : null}
                      </div>
                    );
                  })}
                </div>
              </div>
            </>
          ) : null}

          {activePanel === "MATRIX" ? (
            <div className="rounded-3xl border border-white/10 bg-slate-900/75 p-5 shadow-xl">
              <div className="flex flex-wrap items-start justify-between gap-3">
                <div>
                  <p className="text-xs font-black uppercase tracking-[0.18em] text-slate-500">EPIC 17 RBAC matrix</p>
                  <h2 className="mt-2 text-lg font-black text-white">Role access coverage</h2>
                  <p className="mt-1 text-sm text-slate-400">Shows permission breadth and portal access levels for every admin role.</p>
                </div>
                <AdminStatusBadge value={`${matrix?.summary.permissionCount ?? permissions.length} permissions`} />
              </div>
              <div className="mt-5 overflow-x-auto">
                <table className="min-w-full text-left text-sm">
                  <thead>
                    <tr className="border-b border-white/10 text-[11px] font-black uppercase tracking-[0.18em] text-slate-500">
                      <th className="px-4 py-3">Role</th>
                      <th className="px-4 py-3">Status</th>
                      <th className="px-4 py-3">Permissions</th>
                      <th className="px-4 py-3">Admin Pages</th>
                      <th className="px-4 py-3">Owner Pages</th>
                      <th className="px-4 py-3">Player Pages</th>
                    </tr>
                  </thead>
                  <tbody>
                    {(matrix?.roles || []).map((role) => (
                      <tr key={role.roleId} className="border-b border-white/5">
                        <td className="px-4 py-3">
                          <p className="font-black text-white">{role.roleName}</p>
                          <p className="mt-1 line-clamp-1 text-xs text-slate-500">{role.description}</p>
                        </td>
                        <td className="px-4 py-3"><AdminStatusBadge value={role.active === false ? "INACTIVE" : role.systemRole ? "SYSTEM" : "CUSTOM"} /></td>
                        <td className="px-4 py-3 font-black text-slate-200">{role.permissionCount}</td>
                        <td className="px-4 py-3 text-slate-300">{accessCount(role.pageAccess, "ADMIN")}</td>
                        <td className="px-4 py-3 text-slate-300">{accessCount(role.pageAccess, "OWNER")}</td>
                        <td className="px-4 py-3 text-slate-300">{accessCount(role.pageAccess, "PLAYER")}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          ) : null}

          {activePanel === "ASSIGNMENTS" ? (
            <div className="rounded-3xl border border-white/10 bg-slate-900/75 p-5 shadow-xl">
              <div className="grid gap-4 xl:grid-cols-[minmax(0,1fr)_220px]">
                <div className="grid gap-4 lg:grid-cols-2">
                  <label className="block">
                    <span className="text-xs font-black uppercase tracking-[0.18em] text-slate-500">Admin user</span>
                    <select value={assignUserId} onChange={(event) => setAssignUserId(event.target.value)} className="mt-2 h-11 w-full rounded-2xl border border-white/10 bg-slate-950/80 px-4 text-sm font-semibold text-white outline-none">
                      <option value="">Select admin user</option>
                      {adminUsers.map((user) => <option key={user.id} value={user.id}>{user.email} - {user.role}</option>)}
                    </select>
                  </label>
                  <label className="block">
                    <span className="text-xs font-black uppercase tracking-[0.18em] text-slate-500">Role</span>
                    <select value={assignRoleName} onChange={(event) => setAssignRoleName(event.target.value)} className="mt-2 h-11 w-full rounded-2xl border border-white/10 bg-slate-950/80 px-4 text-sm font-semibold text-white outline-none">
                      <option value="">Select role</option>
                      {roles.map((role) => <option key={role.id} value={role.name}>{role.name}</option>)}
                    </select>
                  </label>
                </div>
                <div className="flex items-end">
                  <AdminActionButton label={saving ? "Assigning..." : "Assign Role"} intent="primary" disabled={saving} onClick={() => void handleAssignRole()} />
                </div>
              </div>
              <div className="mt-5 max-h-[520px] overflow-auto rounded-2xl border border-white/10">
                <table className="min-w-full divide-y divide-white/10">
                  <thead className="sticky top-0 z-10 bg-slate-950">
                    <tr>
                      <th className="px-4 py-3 text-left text-[11px] font-black uppercase tracking-[0.2em] text-slate-500">Admin</th>
                      <th className="px-4 py-3 text-left text-[11px] font-black uppercase tracking-[0.2em] text-slate-500">Role</th>
                      <th className="px-4 py-3 text-left text-[11px] font-black uppercase tracking-[0.2em] text-slate-500">Status</th>
                      <th className="px-4 py-3 text-left text-[11px] font-black uppercase tracking-[0.2em] text-slate-500">Created</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-white/10">
                    {adminUsers.map((user) => (
                      <tr key={user.id} className="bg-slate-900/35">
                        <td className="px-4 py-3 text-sm font-black text-white">{user.email}</td>
                        <td className="px-4 py-3 text-sm text-slate-300">{user.role}</td>
                        <td className="px-4 py-3"><AdminStatusBadge value={user.status} /></td>
                        <td className="px-4 py-3 text-sm text-slate-500">{formatAdminDate(user.createdAt)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          ) : null}

          {activePanel === "AUDIT" ? (
            <AdminDataTable title="Permission audit" description="Recent RBAC changes and assignment events." rows={auditRows} loading={loading} />
          ) : null}
        </section>
      </section>
    </div>
  );
}

function accessFromRole(role: AdminRole, pages: AdminPagePermissionCatalogItem[]): Record<string, AccessLevel> {
  const codes = new Set(role.permissions.map((permission) => permission.code));
  const next: Record<string, AccessLevel> = {};
  for (const page of pages) {
    const hasRead = codes.has(page.permissions.read);
    const hasWrite = codes.has(page.permissions.write);
    const hasFull = codes.has(page.permissions.full);
    if (hasFull || (hasRead && hasWrite)) {
      next[page.key] = "FULL";
    } else if (hasWrite) {
      next[page.key] = "WRITE";
    } else if (hasRead) {
      next[page.key] = "READ";
    } else {
      next[page.key] = "NONE";
    }
  }
  return next;
}

function buildPermissionCodes(pages: AdminPagePermissionCatalogItem[], pageAccess: Record<string, AccessLevel>, apiPermissionCodes: string[]) {
  const codes = new Set(apiPermissionCodes);
  for (const page of pages) {
    for (const code of codesForLevel(page, pageAccess[page.key] || "NONE")) {
      codes.add(code);
    }
  }
  return Array.from(codes).sort();
}

function codesForLevel(page: AdminPagePermissionCatalogItem, level: AccessLevel) {
  if (level === "READ") {
    return [page.permissions.read];
  }
  if (level === "WRITE") {
    return [page.permissions.write];
  }
  if (level === "FULL") {
    return [page.permissions.read, page.permissions.write, page.permissions.full];
  }
  return [];
}

function isPagePermission(code: string, pages: AdminPagePermissionCatalogItem[]) {
  return pages.some((page) => page.permissions.read === code || page.permissions.write === code || page.permissions.full === code);
}

function groupPermissions(permissions: AdminPermission[]) {
  return permissions.reduce<Record<string, AdminPermission[]>>((accumulator, permission) => {
    const moduleName = permission.module || "SYSTEM";
    accumulator[moduleName] = accumulator[moduleName] || [];
    accumulator[moduleName].push(permission);
    return accumulator;
  }, {});
}

function accessCount(access: AdminRbacMatrix["roles"][number]["pageAccess"], portal: PortalKey) {
  const portalAccess = access.filter((item) => item.portal === portal);
  const enabled = portalAccess.filter((item) => item.level !== "NONE").length;
  return `${enabled}/${portalAccess.length}`;
}

function readableError(error: unknown) {
  const record = error && typeof error === "object" ? error as Record<string, unknown> : {};
  const response = record.response && typeof record.response === "object" ? record.response as Record<string, unknown> : {};
  const data = response.data && typeof response.data === "object" ? response.data as Record<string, unknown> : {};
  return typeof data.message === "string" ? data.message : "RBAC request failed. Check backend logs for details.";
}
