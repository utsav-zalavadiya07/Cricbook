"use client";

import { useEffect, useMemo, useState } from "react";
import {
  activateAdminSaasDomain,
  addAdminSaasDomain,
  createAdminSaasComplianceRequest,
  createAdminSaasSubscription,
  createAdminSaasTenant,
  generateAdminSaasInvoice,
  getAdminSaasAudit,
  getAdminSaasDashboard,
  installAdminSaasModule,
  payAdminSaasInvoice,
  publishAdminSaasMarketplaceModule,
  requestAdminSaasAppBuild,
  SaasMap,
  saveAdminSaasBranding,
  setAdminSaasFeature,
  verifyAdminSaasDomain,
} from "@/modules/saas/services/saasPlatformService";
import { CompactRow, DataTable, errorMessage, Field, formatNumber, MetricCard, Panel, SelectField } from "@/modules/saas/components/SaasUi";

const defaultTenant = {
  name: "",
  domain: "",
  tenantType: "ENTERPRISE",
  countryCode: "IN",
  regionCode: "IN-GJ",
  defaultLocale: "en",
  currency: "INR",
  timezone: "Asia/Kolkata",
};

const defaultBranding = {
  appName: "",
  logoUrl: "",
  faviconUrl: "",
  primaryColor: "222.2 47.4% 11.2%",
  secondaryColor: "142.1 76.2% 36.3%",
  fontFamily: "Inter",
  published: true,
};

const defaultModule = {
  moduleCode: "BOOKING_ADDON",
  name: "Booking Add-on",
  category: "CORE",
  pricingModel: "FREE",
  price: "0",
  currency: "INR",
  certificationStatus: "CERTIFIED",
};

export default function AdminSaasPage() {
  const [dashboard, setDashboard] = useState<SaasMap | null>(null);
  const [audit, setAudit] = useState<SaasMap[]>([]);
  const [tenantDraft, setTenantDraft] = useState(defaultTenant);
  const [brandingDraft, setBrandingDraft] = useState(defaultBranding);
  const [moduleDraft, setModuleDraft] = useState(defaultModule);
  const [selectedTenantId, setSelectedTenantId] = useState("");
  const [selectedDomainId, setSelectedDomainId] = useState("");
  const [selectedModuleId, setSelectedModuleId] = useState("");
  const [selectedInvoiceId, setSelectedInvoiceId] = useState("");
  const [selectedSubscriptionId, setSelectedSubscriptionId] = useState("");
  const [customDomain, setCustomDomain] = useState("");
  const [featureKey, setFeatureKey] = useState("BOOKINGS");
  const [planCode, setPlanCode] = useState("ENTERPRISE");
  const [billingCycle, setBillingCycle] = useState("MONTHLY");
  const [subscriptionPrice, setSubscriptionPrice] = useState("9999");
  const [invoiceAmount, setInvoiceAmount] = useState("9999");
  const [taxAmount, setTaxAmount] = useState("1799.82");
  const [paymentAmount, setPaymentAmount] = useState("11798.82");
  const [appVersion, setAppVersion] = useState("1.0.0");
  const [complianceType, setComplianceType] = useState("DATA_EXPORT");
  const [status, setStatus] = useState("");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  async function loadSaas() {
    try {
      setLoading(true);
      const [dashboardRes, auditRes] = await Promise.all([getAdminSaasDashboard(), getAdminSaasAudit()]);
      setDashboard(dashboardRes.data);
      setAudit(auditRes.data);
      const firstTenant = dashboardRes.data.tenants?.[0];
      const firstDomain = dashboardRes.data.domains?.[0];
      const firstModule = dashboardRes.data.marketplaceModules?.[0];
      const firstInvoice = dashboardRes.data.invoices?.[0];
      const firstSubscription = dashboardRes.data.subscriptions?.[0];
      if (firstTenant?.id) setSelectedTenantId(String(firstTenant.id));
      if (firstDomain?.id) setSelectedDomainId(String(firstDomain.id));
      if (firstModule?.id) setSelectedModuleId(String(firstModule.id));
      if (firstInvoice?.id) setSelectedInvoiceId(String(firstInvoice.id));
      if (firstSubscription?.id) setSelectedSubscriptionId(String(firstSubscription.id));
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
      { label: "Tenants", value: formatNumber(dashboard?.kpis?.tenants), detail: `${formatNumber(dashboard?.kpis?.activeTenants)} active` },
      { label: "White Label", value: formatNumber(dashboard?.kpis?.whiteLabelTenants), detail: "branded tenant apps" },
      { label: "Custom Domains", value: formatNumber(dashboard?.kpis?.customDomains), detail: `${formatNumber(dashboard?.kpis?.activeDomains)} active domains` },
      { label: "MRR", value: `Rs ${formatNumber(dashboard?.kpis?.mrr)}`, detail: "active subscription run-rate" },
      { label: "Outstanding", value: `Rs ${formatNumber(dashboard?.kpis?.outstandingInvoices)}`, detail: "open SaaS invoices" },
      { label: "Compliance", value: formatNumber(dashboard?.kpis?.openComplianceRequests), detail: "open regional requests" },
    ],
    [dashboard]
  );

  async function runAction(action: () => Promise<unknown>, message: string) {
    setSaving(true);
    setStatus("");
    try {
      await action();
      setStatus(message);
      await loadSaas();
    } catch (error) {
      setStatus(errorMessage(error, "SaaS governance action failed."));
    } finally {
      setSaving(false);
    }
  }

  const tenantId = Number(selectedTenantId);
  const domainId = Number(selectedDomainId);
  const moduleId = Number(selectedModuleId);
  const invoiceId = Number(selectedInvoiceId);
  const subscriptionId = Number(selectedSubscriptionId);

  return (
    <main className="min-h-screen bg-slate-950 px-4 py-8 text-white sm:px-8" data-i18n-skip="true">
      <div className="mx-auto max-w-7xl space-y-8">
        <header className="rounded-[2rem] border border-white/10 bg-gradient-to-br from-emerald-500/15 via-slate-900 to-cyan-500/10 p-6 shadow-2xl">
          <div className="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
            <div>
              <p className="text-xs font-black uppercase tracking-[0.26em] text-emerald-300">EPIC 36 SaaS Platform Governance</p>
              <h1 className="mt-3 text-3xl font-black text-white sm:text-4xl">Global tenants, white-label apps, domains, feature flags and SaaS billing</h1>
              <p className="mt-3 max-w-3xl text-sm leading-6 text-slate-300">
                Govern multi-tenant isolation, branding, custom domains, subscriptions, app builds, marketplace modules and regional compliance from one control plane.
              </p>
            </div>
            <button onClick={() => void loadSaas()} className="rounded-2xl bg-emerald-400 px-4 py-3 text-sm font-black text-slate-950 hover:bg-emerald-300">
              Refresh SaaS
            </button>
          </div>
        </header>

        {status ? <p className="rounded-2xl border border-emerald-400/20 bg-emerald-400/10 px-4 py-3 text-sm font-bold text-emerald-200">{status}</p> : null}

        {loading ? (
          <section className="rounded-[2rem] border border-white/10 bg-white/[0.04] p-10 text-center text-sm font-bold text-slate-300">Loading SaaS governance...</section>
        ) : (
          <>
            <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
              {metrics.map((metric) => (
                <MetricCard key={metric.label} {...metric} />
              ))}
            </section>

            <section className="grid gap-6 xl:grid-cols-[1fr_1fr]">
              <Panel title="Create Tenant" subtitle="Country, locale, currency and timezone become the tenant operating context.">
                <div className="grid gap-3 sm:grid-cols-2">
                  <Field label="Tenant name" value={tenantDraft.name} onChange={(value) => setTenantDraft((current) => ({ ...current, name: value }))} placeholder="Truf UAE" />
                  <Field label="Domain" value={tenantDraft.domain} onChange={(value) => setTenantDraft((current) => ({ ...current, domain: value }))} placeholder="uae.trufsports.com" />
                  <SelectField label="Tenant type" value={tenantDraft.tenantType} onChange={(value) => setTenantDraft((current) => ({ ...current, tenantType: value }))} options={["STANDARD", "WHITE_LABEL", "ENTERPRISE", "FRANCHISE", "GOVERNMENT"]} />
                  <SelectField label="Country" value={tenantDraft.countryCode} onChange={(value) => setTenantDraft((current) => ({ ...current, countryCode: value }))} options={["IN", "AE", "US", "AU", "GB"]} />
                  <Field label="Region" value={tenantDraft.regionCode} onChange={(value) => setTenantDraft((current) => ({ ...current, regionCode: value }))} />
                  <SelectField label="Currency" value={tenantDraft.currency} onChange={(value) => setTenantDraft((current) => ({ ...current, currency: value }))} options={["INR", "AED", "USD", "AUD", "GBP"]} />
                </div>
                <button disabled={saving} onClick={() => void runAction(() => createAdminSaasTenant(tenantDraft), "Tenant created.")} className="mt-4 w-full rounded-2xl bg-emerald-400 px-4 py-3 text-sm font-black text-slate-950 hover:bg-emerald-300 disabled:opacity-60">
                  Create Tenant
                </button>
              </Panel>

              <Panel title="Branding and Custom Domain" subtitle="White-label branding uses existing theme tokens and custom domain verification states.">
                <div className="grid gap-3 sm:grid-cols-2">
                  <SelectField label="Tenant id" value={selectedTenantId} onChange={setSelectedTenantId} options={(dashboard?.tenants || []).map((tenant: SaasMap) => String(tenant.id))} />
                  <Field label="App name" value={brandingDraft.appName} onChange={(value) => setBrandingDraft((current) => ({ ...current, appName: value }))} placeholder="Truf Sports UAE" />
                  <Field label="Logo URL" value={brandingDraft.logoUrl} onChange={(value) => setBrandingDraft((current) => ({ ...current, logoUrl: value }))} />
                  <Field label="Font" value={brandingDraft.fontFamily} onChange={(value) => setBrandingDraft((current) => ({ ...current, fontFamily: value }))} />
                  <Field label="Custom domain" value={customDomain} onChange={setCustomDomain} placeholder="book.brand.com" />
                  <SelectField label="Domain id" value={selectedDomainId} onChange={setSelectedDomainId} options={(dashboard?.domains || []).map((domain: SaasMap) => String(domain.id))} />
                </div>
                <div className="mt-4 grid gap-3 sm:grid-cols-2">
                  <button disabled={saving || !tenantId} onClick={() => void runAction(() => saveAdminSaasBranding({ tenantId, ...brandingDraft }), "Branding saved.")} className="rounded-2xl bg-emerald-400 px-4 py-3 text-sm font-black text-slate-950 disabled:opacity-50">
                    Save Branding
                  </button>
                  <button disabled={saving || !tenantId || !customDomain} onClick={() => void runAction(() => addAdminSaasDomain({ tenantId, domain: customDomain, routingMode: "CNAME" }), "Domain added.")} className="rounded-2xl border border-white/10 bg-white/[0.06] px-4 py-3 text-sm font-black text-white disabled:opacity-50">
                    Add Domain
                  </button>
                  <button disabled={saving || !domainId} onClick={() => void runAction(() => verifyAdminSaasDomain(domainId), "Domain verified.")} className="rounded-2xl border border-emerald-400/30 bg-emerald-400/10 px-4 py-3 text-sm font-black text-emerald-100 disabled:opacity-50">
                    Verify Domain
                  </button>
                  <button disabled={saving || !domainId} onClick={() => void runAction(() => activateAdminSaasDomain(domainId), "Domain activated.")} className="rounded-2xl border border-emerald-400/30 bg-emerald-400/10 px-4 py-3 text-sm font-black text-emerald-100 disabled:opacity-50">
                    Activate Domain
                  </button>
                </div>
              </Panel>
            </section>

            <section className="grid gap-6 xl:grid-cols-3">
              <Panel title="Feature and Subscription" subtitle="Tenant flags and subscription entitlements control platform modules.">
                <div className="grid gap-3">
                  <SelectField label="Tenant id" value={selectedTenantId} onChange={setSelectedTenantId} options={(dashboard?.tenants || []).map((tenant: SaasMap) => String(tenant.id))} />
                  <SelectField label="Feature" value={featureKey} onChange={setFeatureKey} options={["BOOKINGS", "COMMERCE", "ACADEMY", "TOURNAMENTS", "AI", "CRM", "MARKETPLACE", "API_PLATFORM", "FAN_ECONOMY"]} />
                  <SelectField label="Plan" value={planCode} onChange={setPlanCode} options={["STARTER", "PROFESSIONAL", "ENTERPRISE", "GOVERNMENT"]} />
                  <SelectField label="Cycle" value={billingCycle} onChange={setBillingCycle} options={["MONTHLY", "QUARTERLY", "YEARLY"]} />
                  <Field label="Price" type="number" value={subscriptionPrice} onChange={setSubscriptionPrice} />
                </div>
                <div className="mt-4 grid gap-3 sm:grid-cols-2">
                  <button disabled={saving || !tenantId} onClick={() => void runAction(() => setAdminSaasFeature({ tenantId, featureKey, enabled: true, rolloutStrategy: "TENANT_OVERRIDE" }), "Feature enabled.")} className="rounded-2xl bg-emerald-400 px-4 py-3 text-sm font-black text-slate-950 disabled:opacity-50">
                    Enable Feature
                  </button>
                  <button disabled={saving || !tenantId} onClick={() => void runAction(() => createAdminSaasSubscription({ tenantId, planCode, billingCycle, price: Number(subscriptionPrice || 1) }), "Subscription created.")} className="rounded-2xl border border-white/10 bg-white/[0.06] px-4 py-3 text-sm font-black text-white disabled:opacity-50">
                    Create Subscription
                  </button>
                </div>
              </Panel>

              <Panel title="Billing" subtitle="Manual invoices support SaaS billing reconciliation and payment tracking.">
                <div className="grid gap-3">
                  <SelectField label="Subscription id" value={selectedSubscriptionId} onChange={setSelectedSubscriptionId} options={(dashboard?.subscriptions || []).map((item: SaasMap) => String(item.id))} />
                  <SelectField label="Invoice id" value={selectedInvoiceId} onChange={setSelectedInvoiceId} options={(dashboard?.invoices || []).map((item: SaasMap) => String(item.id))} />
                  <Field label="Invoice amount" type="number" value={invoiceAmount} onChange={setInvoiceAmount} />
                  <Field label="Tax amount" type="number" value={taxAmount} onChange={setTaxAmount} />
                  <Field label="Payment amount" type="number" value={paymentAmount} onChange={setPaymentAmount} />
                </div>
                <div className="mt-4 grid gap-3 sm:grid-cols-2">
                  <button disabled={saving || !tenantId} onClick={() => void runAction(() => generateAdminSaasInvoice({ tenantId, subscriptionId: subscriptionId || undefined, amount: Number(invoiceAmount || 1), taxAmount: Number(taxAmount || 0) }), "Invoice generated.")} className="rounded-2xl bg-emerald-400 px-4 py-3 text-sm font-black text-slate-950 disabled:opacity-50">
                    Generate Invoice
                  </button>
                  <button disabled={saving || !invoiceId} onClick={() => void runAction(() => payAdminSaasInvoice(invoiceId, { amount: Number(paymentAmount || 1), paymentReference: "MANUAL-WEB" }), "Invoice payment recorded.")} className="rounded-2xl border border-white/10 bg-white/[0.06] px-4 py-3 text-sm font-black text-white disabled:opacity-50">
                    Pay Invoice
                  </button>
                </div>
              </Panel>

              <Panel title="Marketplace and Compliance" subtitle="Module certification, tenant installs, app builds and regulatory workflows.">
                <div className="grid gap-3">
                  <Field label="Module code" value={moduleDraft.moduleCode} onChange={(value) => setModuleDraft((current) => ({ ...current, moduleCode: value }))} />
                  <Field label="Module name" value={moduleDraft.name} onChange={(value) => setModuleDraft((current) => ({ ...current, name: value }))} />
                  <SelectField label="Module id" value={selectedModuleId} onChange={setSelectedModuleId} options={(dashboard?.marketplaceModules || []).map((item: SaasMap) => String(item.id))} />
                  <Field label="App version" value={appVersion} onChange={setAppVersion} />
                  <SelectField label="Compliance type" value={complianceType} onChange={setComplianceType} options={["DATA_EXPORT", "DATA_DELETION", "CONSENT_REVIEW", "RETENTION_HOLD", "DATA_RESIDENCY"]} />
                </div>
                <div className="mt-4 grid gap-3 sm:grid-cols-2">
                  <button disabled={saving} onClick={() => void runAction(() => publishAdminSaasMarketplaceModule({ ...moduleDraft, price: Number(moduleDraft.price || 0) }), "Marketplace module published.")} className="rounded-2xl bg-emerald-400 px-4 py-3 text-sm font-black text-slate-950 disabled:opacity-50">
                    Publish Module
                  </button>
                  <button disabled={saving || !tenantId || !moduleId} onClick={() => void runAction(() => installAdminSaasModule({ tenantId, moduleId, licenseType: "FREE" }), "Module installed.")} className="rounded-2xl border border-white/10 bg-white/[0.06] px-4 py-3 text-sm font-black text-white disabled:opacity-50">
                    Install Module
                  </button>
                  <button disabled={saving || !tenantId} onClick={() => void runAction(() => requestAdminSaasAppBuild({ tenantId, appType: "WHITE_LABEL", platform: "WEB", version: appVersion }), "App build requested.")} className="rounded-2xl border border-emerald-400/30 bg-emerald-400/10 px-4 py-3 text-sm font-black text-emerald-100 disabled:opacity-50">
                    Request Build
                  </button>
                  <button disabled={saving || !tenantId} onClick={() => void runAction(() => createAdminSaasComplianceRequest({ tenantId, regulation: "DPDP", requestType: complianceType, notes: "Created from admin SaaS console" }), "Compliance request created.")} className="rounded-2xl border border-emerald-400/30 bg-emerald-400/10 px-4 py-3 text-sm font-black text-emerald-100 disabled:opacity-50">
                    Open Compliance
                  </button>
                </div>
              </Panel>
            </section>

            <section className="grid gap-6 lg:grid-cols-3">
              <Panel title="Tenants" subtitle="Tenant context and lifecycle state.">
                <DataTable rows={dashboard?.tenants || []} columns={[["tenantCode", "Code"], ["name", "Tenant"], ["countryCode", "Country"], ["status", "Status"], ["lifecycleStage", "Stage"]]} empty="No tenants yet." />
              </Panel>
              <Panel title="Domains and Features" subtitle="Domain verification plus tenant-scoped flags.">
                <DataTable rows={dashboard?.domains || []} columns={[["domain", "Domain"], ["tenantId", "Tenant"], ["status", "Status"], ["routingMode", "Routing"]]} empty="No custom domains yet." />
                <div className="mt-4">
                  <DataTable rows={dashboard?.featureToggles || []} columns={[["tenantId", "Tenant"], ["featureKey", "Feature"], ["enabled", "Enabled"], ["rolloutStrategy", "Strategy"]]} empty="No feature toggles yet." />
                </div>
              </Panel>
              <Panel title="Invoices and Audit" subtitle="Billing reconciliation and immutable governance trail.">
                <DataTable rows={dashboard?.invoices || []} columns={[["invoiceNumber", "Invoice"], ["tenantId", "Tenant"], ["status", "Status"], ["amount", "Amount"], ["paidAmount", "Paid"]]} empty="No invoices yet." />
                <div className="mt-4">
                  <DataTable rows={audit} columns={[["action", "Action"], ["entityType", "Entity"], ["actor", "Actor"], ["createdAt", "Created"]]} empty="No audit rows yet." />
                </div>
              </Panel>
            </section>

            <Panel title="Global Controls and Region Readiness" subtitle="Reference controls for tenant isolation, OpenFeature-style flags, subscriptions and compliance.">
              <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-3">
                {(dashboard?.globalControls || []).map((item: SaasMap) => (
                  <CompactRow key={String(item.name)} title={String(item.name)} meta={String(item.detail)} status={String(item.status)} />
                ))}
                {(dashboard?.regionReadiness || []).map((item: SaasMap) => (
                  <CompactRow key={String(item.countryCode)} title={`${item.label} (${item.countryCode})`} meta={`Tenants: ${formatNumber(item.tenants)} · Residency: ${item.dataResidency}`} status={String(item.status)} />
                ))}
              </div>
            </Panel>
          </>
        )}
      </div>
    </main>
  );
}
