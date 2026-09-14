"use client";
import { TileInfo } from "@/components/ui";

import { useEffect, useMemo, useState } from "react";
import { getSaasMarketplaceModules, getSaasRegions, getSaasTenantContext, SaasMap } from "@/modules/saas/services/saasPlatformService";
import { CompactRow, DataTable, errorMessage, Field, formatNumber, MetricCard, Panel } from "@/modules/saas/components/SaasUi";

export default function SaasPage() {
  const [tenantContext, setTenantContext] = useState<SaasMap | null>(null);
  const [modules, setModules] = useState<SaasMap[]>([]);
  const [regions, setRegions] = useState<SaasMap[]>([]);
  const [tenantId, setTenantId] = useState("");
  const [status, setStatus] = useState("");
  const [loading, setLoading] = useState(true);

  async function loadSaas(selectedTenantId?: number) {
    try {
      setLoading(true);
      setStatus("");
      const [tenantRes, modulesRes, regionsRes] = await Promise.all([getSaasTenantContext(selectedTenantId), getSaasMarketplaceModules(), getSaasRegions()]);
      setTenantContext(tenantRes.data);
      setModules(modulesRes.data);
      setRegions(regionsRes.data);
    } catch (error) {
      setStatus(errorMessage(error, "SaaS platform context is unavailable."));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    const timer = window.setTimeout(() => void loadSaas(), 0);
    return () => window.clearTimeout(timer);
  }, []);

  const metrics = useMemo(
    () => [
      { label: "Tenant", value: String(tenantContext?.tenantCode || tenantContext?.id || "--"), detail: String(tenantContext?.name || "Resolved by tenant context") },
      { label: "Locale", value: String(tenantContext?.defaultLocale || "en").toUpperCase(), detail: `${tenantContext?.currency || "INR"} · ${tenantContext?.timezone || "Asia/Kolkata"}` },
      { label: "Features", value: formatNumber(tenantContext?.features?.length), detail: "tenant-scoped feature flags" },
      { label: "Domains", value: formatNumber(tenantContext?.domains?.length), detail: "verified or pending routes" },
      { label: "Modules", value: formatNumber(modules.length), detail: "published marketplace modules" },
      { label: "Regions", value: formatNumber(regions.length), detail: "global expansion packs" },
    ],
    [tenantContext, modules.length, regions.length]
  );

  const branding = tenantContext?.branding || {};

  return (
    <main className="theme-page-player min-h-screen px-4 py-8 sm:px-6" data-i18n-skip="true">
      <div className="mx-auto max-w-7xl space-y-6">
        <section className="theme-card rounded-[2rem] border p-6 shadow-2xl">
          <div className="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
            <div>
              <p className="text-xs font-black uppercase tracking-[0.26em] text-emerald-300">EPIC 36 SaaS Platform</p>
              <h1 className="mt-3 text-3xl font-black theme-title sm:text-4xl">Tenant context, white-label readiness and marketplace modules
              <span className="inline-flex align-middle ml-3 mb-1">
                <TileInfo label="Tenant context, white-label readiness and marketplace modules information" focusable={false}>Preview how Truf Sports resolves tenant branding, enabled features, regional readiness and add-on modules for global white-label deployments.</TileInfo>
              </span></h1>
            </div>
            <button onClick={() => void loadSaas(tenantId ? Number(tenantId) : undefined)} className="rounded-2xl bg-emerald-500 px-4 py-3 text-sm font-black text-white hover:bg-emerald-400">
              Refresh SaaS
            </button>
          </div>
        </section>

        {status ? <p className="theme-card rounded-2xl border border-emerald-400/20 px-4 py-3 text-sm font-bold text-emerald-200">{status}</p> : null}

        <Panel title="Tenant Lookup" subtitle="Use a tenant id to preview a specific brand context, or leave blank for the active request context.">
          <div className="grid gap-3 sm:grid-cols-[1fr_auto] sm:items-end">
            <Field label="Tenant id" value={tenantId} onChange={setTenantId} placeholder="1" type="number" />
            <button onClick={() => void loadSaas(tenantId ? Number(tenantId) : undefined)} className="rounded-2xl bg-emerald-500 px-4 py-3 text-sm font-black text-white hover:bg-emerald-400">
              Load Tenant
            </button>
          </div>
        </Panel>

        {loading ? (
          <section className="theme-card rounded-[2rem] border p-10 text-center text-sm font-bold theme-soft">Loading SaaS platform...</section>
        ) : (
          <>
            <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
              {metrics.map((metric) => (
                <MetricCard key={metric.label} {...metric} />
              ))}
            </section>

            <section className="grid gap-6 xl:grid-cols-[0.8fr_1.2fr]">
              <Panel title="White Label Snapshot" subtitle="Branding keeps the existing design system intact while swapping tenant assets and identity.">
                <div className="rounded-3xl border border-white/10 bg-white/[0.04] p-5">
                  <p className="text-xs font-black uppercase tracking-[0.2em] text-emerald-300">Brand</p>
                  <h2 className="mt-3 text-3xl font-black theme-title">{branding.appName || tenantContext?.name || "Truf Sports"}</h2>
                  <p className="mt-2 text-sm theme-soft">Logo: {branding.logoUrl || "/assets/brand-default.png"}</p>
                  <p className="mt-1 text-sm theme-soft">Font: {branding.fontFamily || "Inter"}</p>
                  <div className="mt-4 grid gap-3 sm:grid-cols-2">
                    <CompactRow title="Primary token" meta={String(branding.primaryColor || "222.2 47.4% 11.2%")} status={branding.published ? "PUBLISHED" : "DRAFT"} />
                    <CompactRow title="Secondary token" meta={String(branding.secondaryColor || "142.1 76.2% 36.3%")} status={tenantContext?.whiteLabelEnabled ? "ENABLED" : "READY"} />
                  </div>
                </div>
              </Panel>

              <Panel title="Tenant Controls" subtitle="Feature flags and custom domains are scoped to the active tenant.">
                <div className="grid gap-3 md:grid-cols-2">
                  {(tenantContext?.features || []).map((feature: SaasMap) => (
                    <CompactRow key={String(feature.id || feature.featureKey)} title={String(feature.featureKey)} meta={String(feature.rolloutStrategy || "TENANT_OVERRIDE")} status={feature.enabled ? "ENABLED" : "DISABLED"} />
                  ))}
                  {(tenantContext?.domains || []).map((domain: SaasMap) => (
                    <CompactRow key={String(domain.id || domain.domain)} title={String(domain.domain)} meta={String(domain.routingMode || "CNAME")} status={String(domain.status)} />
                  ))}
                  {!tenantContext?.features?.length && !tenantContext?.domains?.length ? <CompactRow title="Tenant controls" meta="No tenant flags or custom domains are configured yet." status="READY" /> : null}
                </div>
              </Panel>
            </section>

            <section className="grid gap-6 lg:grid-cols-2">
              <Panel title="Marketplace Modules" subtitle="Certified add-ons available for tenant installations.">
                <DataTable rows={modules} columns={[["moduleCode", "Code"], ["name", "Module"], ["category", "Category"], ["pricingModel", "Pricing"], ["status", "Status"]]} empty="No published modules yet." />
              </Panel>
              <Panel title="Regional Expansion Packs" subtitle="Locale, currency and compliance readiness by country.">
                <div className="grid gap-3">
                  {regions.map((region) => (
                    <CompactRow key={String(region.countryCode)} title={`${region.label} (${region.countryCode})`} meta={`Tenants: ${formatNumber(region.tenants)} · Data residency: ${region.dataResidency}`} status={String(region.status)} />
                  ))}
                </div>
              </Panel>
            </section>
          </>
        )}
      </div>
    </main>
  );
}
