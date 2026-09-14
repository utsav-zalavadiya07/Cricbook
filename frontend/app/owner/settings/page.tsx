"use client";
import { TileInfo } from "@/components/ui";

import { useEffect, useMemo, useState } from "react";
import DashboardLayout from "@/modules/turf/components/DashboardLayout";
import ThemeToggle from "@/modules/theme/ThemeToggle";
import AvatarUploader from "@/modules/media/components/AvatarUploader";
import { removeOwnerLogo, uploadOwnerLogo } from "@/modules/media/services/mediaService";
import { useI18n } from "@/modules/i18n/I18nProvider";
import {
  getOwnerSettings,
  OwnerSettingsPayload,
  OwnerSettingsResponse,
  requestOwnerDataExport,
  updateOwnerSettings,
} from "@/modules/owner/services/ownerSettingsService";

type SectionKey =
  | "business"
  | "turfs"
  | "booking"
  | "pricing"
  | "staff"
  | "notifications"
  | "payments"
  | "weather"
  | "marketplace"
  | "community"
  | "branding"
  | "security"
  | "automation"
  | "reports";

const emptySettings: OwnerSettingsPayload = {
  businessName: "",
  supportPhone: "",
  businessAddress: "",
  gstNumber: "",
  businessLogoUrl: "",
  supportEmail: "",
  businessType: "SINGLE_TURF",
  operatingHours: "06:00 - 23:00",
  defaultSlotDurationMinutes: 60,
  openingTime: "06:00",
  closingTime: "23:00",
  indoorOutdoor: "OUTDOOR",
  amenities: "",
  advanceBookingDays: 14,
  minimumBookingMinutes: 60,
  allowInstantBooking: true,
  approvalRequired: false,
  trustedPlayersOnly: false,
  cancellationPolicy: "FLEXIBLE",
  dynamicPricingEnabled: false,
  weekendPricingEnabled: true,
  peakPricingEnabled: true,
  pricingMode: "MANUAL",
  peakMultiplier: 1.2,
  weekendMultiplier: 1.15,
  staffInviteEmail: "",
  staffRole: "MANAGER",
  staffPermissions: "Bookings, slots, attendance, payouts",
  bankAccountName: "",
  bankAccountNumber: "",
  bankIfsc: "",
  upiId: "",
  settlementSchedule: "WEEKLY",
  bookingAlerts: true,
  paymentAlerts: true,
  reviewAlerts: true,
  marketingAlerts: false,
  promotionWhatsappEnabled: false,
  reminderLeadMinutes: 120,
  partialPaymentEnabled: true,
  minimumPaymentPercent: 50,
  refundMode: "WALLET_CREDIT",
  rainRefundAllowed: true,
  freeWeatherReschedule: true,
  weatherRiskAlerts: true,
  rainThresholdPercent: 70,
  resaleEnabled: true,
  transferEnabled: true,
  maxResaleMarkupPercent: 0,
  restrictSuspiciousAccounts: true,
  allowPublicTournaments: true,
  communityVisibility: "PUBLIC",
  teamRegistrationRule: "APPROVAL_REQUIRED",
  brandPrimaryColor: "#0EA5E9",
  brandTheme: "system",
  compactDashboard: false,
  twoFactorEnabled: false,
  sessionTimeoutMinutes: 60,
  ipRestrictions: "",
  autoPricingEnabled: false,
  autoRescheduleEnabled: true,
  aiCoachEnabled: true,
  smartPromotionsEnabled: true,
  revenueExportEnabled: true,
  gstReportsEnabled: true,
  exportFormat: "CSV",
  dataExportRequested: false,
};

export default function OwnerSettingsPage() {
  const { translateText } = useI18n();
  const [activeSection, setActiveSection] = useState<SectionKey>("business");
  const [settings, setSettings] = useState<OwnerSettingsPayload>(emptySettings);
  const [account, setAccount] = useState<OwnerSettingsResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;
    getOwnerSettings()
      .then((response) => {
        if (!active) return;
        setAccount(response.data);
        setSettings(toPayload(response.data));
      })
      .catch((err) => setError(readApiError(err, "Unable to load owner settings.")))
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
    };
  }, []);

  const readiness = account?.readinessScore ?? localReadiness(settings);
  const automationCount = [
    settings.dynamicPricingEnabled,
    settings.autoPricingEnabled,
    settings.autoRescheduleEnabled,
    settings.aiCoachEnabled,
    settings.smartPromotionsEnabled,
  ].filter(Boolean).length;

  const setField = <K extends keyof OwnerSettingsPayload>(field: K, value: OwnerSettingsPayload[K]) => {
    setSettings((current) => ({ ...current, [field]: value }));
    setError("");
  };

  const save = async () => {
    setSaving(true);
    setError("");
    try {
      const response = await updateOwnerSettings(settings);
      setAccount(response.data);
      setSettings(toPayload(response.data));
    } catch (err) {
      setError(readApiError(err, "Unable to save owner settings."));
    } finally {
      setSaving(false);
    }
  };

  const requestExport = async () => {
    setSaving(true);
    setError("");
    try {
      const response = await requestOwnerDataExport();
      setAccount(response.data);
      setSettings(toPayload(response.data));
    } catch (err) {
      setError(readApiError(err, "Unable to request owner data export."));
    } finally {
      setSaving(false);
    }
  };

  const sectionTitle = useMemo(() => sectionTabs.find((tab) => tab.key === activeSection)?.title || "Settings", [activeSection]);

  return (
    <DashboardLayout>
      <div className="mx-auto max-w-7xl space-y-6">
        <section className="theme-panel-strong overflow-hidden rounded-[2rem] p-6 sm:p-8">
          <div className="flex flex-col gap-6 xl:flex-row xl:items-end xl:justify-between">
            <div>
              <p className="theme-accent-owner text-xs font-black uppercase tracking-[0.34em]">{translateText("Owner Business Control Center")}</p>
              <h1 className="theme-title mt-3 text-4xl font-black tracking-tight">{translateText("Owner Settings")}
              <span className="inline-flex align-middle ml-3 mb-1">
                <TileInfo label="Page information" focusable={false}>{translateText("Configure business identity, turf operations, booking policies, pricing, payments, staff access, automation, and exports from one professional control center.")}</TileInfo>
              </span></h1>
            </div>
            <div className="grid gap-3 sm:grid-cols-4 xl:min-w-[620px]">
              <MetricCard label={translateText("Readiness")} value={`${readiness}%`} />
              <MetricCard label={translateText("Bookings")} value={settings.allowInstantBooking ? translateText("Instant") : translateText("Approval")} />
              <MetricCard label={translateText("Automation")} value={`${automationCount}/5`} />
              <MetricCard label={translateText("Settlement")} value={settings.settlementSchedule} />
            </div>
          </div>
        </section>

        {error && <StatusBanner tone="error" text={translateText(error)} />}

        <div className="grid gap-6 xl:grid-cols-[320px_1fr]">
          <aside className="theme-panel h-fit rounded-3xl p-4">
            <div className="grid gap-2 sm:grid-cols-2 xl:grid-cols-1">
              {sectionTabs.map((tab) => (
                <button
                  key={tab.key}
                  type="button"
                  onClick={() => setActiveSection(tab.key)}
                  className={`rounded-2xl px-4 py-3 text-left transition ${
                    activeSection === tab.key ? "bg-sky-500 text-white shadow-lg shadow-sky-500/20" : "theme-panel-soft theme-text hover:translate-x-1"
                  }`}
                >
                  <span className="block text-sm font-black">{translateText(tab.title)}</span>
                  <span className={`mt-1 block text-xs leading-5 ${activeSection === tab.key ? "text-sky-50" : "theme-muted"}`}>
                    {translateText(tab.body)}
                  </span>
                </button>
              ))}
            </div>

            <div className="theme-panel-soft mt-5 rounded-2xl p-4">
              <p className="theme-soft text-xs font-black uppercase tracking-[0.24em]">{translateText("Signed in as")}</p>
              <p className="theme-title mt-2 break-words text-sm font-bold">{account?.ownerName || "Owner"}</p>
              <p className="theme-muted mt-1 break-words text-xs">{account?.ownerEmail || "Loading..."}</p>
            </div>
          </aside>

          <main className="space-y-6">
            {loading ? (
              <section className="theme-panel rounded-3xl p-10 text-center">
                <p className="theme-muted text-sm font-semibold">{translateText("Loading owner settings...")}</p>
              </section>
            ) : (
              <>
                <SettingsPanel eyebrow={translateText("Active Module")} title={translateText(sectionTitle)} body={translateText(sectionHelp[activeSection])}>
                  {activeSection === "business" && (
                    <div className="grid gap-4 md:grid-cols-2">
                      <div className="md:col-span-2">
                        <AvatarUploader
                          title={translateText("Business Logo")}
                          body={translateText("Upload the logo customers will see in turf listings, invoices, tournaments, and owner branding surfaces.")}
                          imageUrl={settings.businessLogoUrl}
                          fallbackText={settings.businessName || account?.ownerName || "CB"}
                          accent="sky"
                          onUpload={async (file) => {
                            const response = await uploadOwnerLogo(file);
                            return response.data;
                          }}
                          onRemove={async () => {
                            const response = await removeOwnerLogo();
                            return response.data;
                          }}
                          onChange={(asset) => setField("businessLogoUrl", asset.imageUrl)}
                        />
                      </div>
                      <Field label={translateText("Business name")} value={settings.businessName} onChange={(v) => setField("businessName", v)} placeholder="Blue Box Sports" />
                      <Field label={translateText("Support phone")} value={settings.supportPhone} onChange={(v) => setField("supportPhone", v)} placeholder="+91 9876543210" />
                      <Field label={translateText("Support email")} value={settings.supportEmail} onChange={(v) => setField("supportEmail", v)} placeholder="support@cricbook.com" />
                      <Field label={translateText("GST number")} value={settings.gstNumber} onChange={(v) => setField("gstNumber", v)} placeholder="24ABCDE1234F1Z5" />
                      <SelectField label={translateText("Business type")} value={settings.businessType} onChange={(v) => setField("businessType", v as OwnerSettingsPayload["businessType"])} options={["SINGLE_TURF", "MULTI_TURF", "FRANCHISE"]} />
                      <Field label={translateText("Logo URL")} value={settings.businessLogoUrl} onChange={(v) => setField("businessLogoUrl", v)} placeholder="https://..." />
                      <Field label={translateText("Business address")} value={settings.businessAddress} onChange={(v) => setField("businessAddress", v)} placeholder="Full business address" multiline />
                    </div>
                  )}

                  {activeSection === "turfs" && (
                    <div className="grid gap-4 md:grid-cols-2">
                      <NumberField label={translateText("Default slot duration")} value={settings.defaultSlotDurationMinutes} onChange={(v) => setField("defaultSlotDurationMinutes", v)} suffix="min" />
                      <SelectField label={translateText("Turf environment")} value={settings.indoorOutdoor} onChange={(v) => setField("indoorOutdoor", v as OwnerSettingsPayload["indoorOutdoor"])} options={["OUTDOOR", "INDOOR", "SEMI_INDOOR"]} />
                      <Field label={translateText("Opening time")} value={settings.openingTime} onChange={(v) => setField("openingTime", v)} placeholder="06:00" />
                      <Field label={translateText("Closing time")} value={settings.closingTime} onChange={(v) => setField("closingTime", v)} placeholder="23:00" />
                      <Field label={translateText("Operating hours label")} value={settings.operatingHours} onChange={(v) => setField("operatingHours", v)} placeholder="6 AM - 11 PM" />
                      <Field label={translateText("Amenities")} value={settings.amenities} onChange={(v) => setField("amenities", v)} placeholder="Parking, flood lights, drinking water" />
                    </div>
                  )}

                  {activeSection === "booking" && (
                    <div className="grid gap-4 md:grid-cols-2">
                      <NumberField label={translateText("Advance booking limit")} value={settings.advanceBookingDays} onChange={(v) => setField("advanceBookingDays", v)} suffix="days" />
                      <NumberField label={translateText("Minimum booking duration")} value={settings.minimumBookingMinutes} onChange={(v) => setField("minimumBookingMinutes", v)} suffix="min" />
                      <SelectField label={translateText("Cancellation policy")} value={settings.cancellationPolicy} onChange={(v) => setField("cancellationPolicy", v as OwnerSettingsPayload["cancellationPolicy"])} options={["FLEXIBLE", "MODERATE", "STRICT"]} />
                      <Toggle label={translateText("Instant booking")} body={translateText("Customers can confirm eligible slots without manual approval.")} checked={settings.allowInstantBooking} onChange={(v) => setField("allowInstantBooking", v)} />
                      <Toggle label={translateText("Approval required")} body={translateText("Force manual approval for sensitive booking flows.")} checked={settings.approvalRequired} onChange={(v) => setField("approvalRequired", v)} />
                      <Toggle label={translateText("Trusted players only")} body={translateText("Restrict high-risk players using trust scoring.")} checked={settings.trustedPlayersOnly} onChange={(v) => setField("trustedPlayersOnly", v)} />
                    </div>
                  )}

                  {activeSection === "pricing" && (
                    <div className="grid gap-4 md:grid-cols-2">
                      <SelectField label={translateText("Pricing mode")} value={settings.pricingMode} onChange={(v) => setField("pricingMode", v as OwnerSettingsPayload["pricingMode"])} options={["MANUAL", "AI_SUGGESTED", "AUTOMATIC"]} />
                      <NumberField label={translateText("Peak multiplier")} value={settings.peakMultiplier} onChange={(v) => setField("peakMultiplier", v)} suffix="x" step="0.05" />
                      <NumberField label={translateText("Weekend multiplier")} value={settings.weekendMultiplier} onChange={(v) => setField("weekendMultiplier", v)} suffix="x" step="0.05" />
                      <Toggle label={translateText("Dynamic pricing")} body={translateText("Allow recommendations to use occupancy and demand signals.")} checked={settings.dynamicPricingEnabled} onChange={(v) => setField("dynamicPricingEnabled", v)} />
                      <Toggle label={translateText("Peak pricing")} body={translateText("Mark evening and high-demand slots as premium.")} checked={settings.peakPricingEnabled} onChange={(v) => setField("peakPricingEnabled", v)} />
                      <Toggle label={translateText("Weekend pricing")} body={translateText("Use separate rules for Saturdays and Sundays.")} checked={settings.weekendPricingEnabled} onChange={(v) => setField("weekendPricingEnabled", v)} />
                    </div>
                  )}

                  {activeSection === "staff" && (
                    <div className="grid gap-4 md:grid-cols-2">
                      <Field label={translateText("Staff invite email")} value={settings.staffInviteEmail} onChange={(v) => setField("staffInviteEmail", v)} placeholder="manager@example.com" />
                      <SelectField label={translateText("Default staff role")} value={settings.staffRole} onChange={(v) => setField("staffRole", v as OwnerSettingsPayload["staffRole"])} options={["MANAGER", "RECEPTIONIST", "GROUND_STAFF", "FINANCE_STAFF", "TOURNAMENT_ADMIN"]} />
                      <Field label={translateText("Permission notes")} value={settings.staffPermissions} onChange={(v) => setField("staffPermissions", v)} placeholder="Bookings, slots, attendance" multiline />
                    </div>
                  )}

                  {activeSection === "notifications" && (
                    <div className="grid gap-4 md:grid-cols-2">
                      <NumberField label={translateText("Reminder lead time")} value={settings.reminderLeadMinutes} onChange={(v) => setField("reminderLeadMinutes", v)} suffix="min" />
                      <Toggle label={translateText("Booking alerts")} body={translateText("New bookings, cancellations, and schedule changes.")} checked={settings.bookingAlerts} onChange={(v) => setField("bookingAlerts", v)} />
                      <Toggle label={translateText("Payment alerts")} body={translateText("Collections, invoices, refunds, and settlements.")} checked={settings.paymentAlerts} onChange={(v) => setField("paymentAlerts", v)} />
                      <Toggle label={translateText("Review alerts")} body={translateText("New reviews and rating changes.")} checked={settings.reviewAlerts} onChange={(v) => setField("reviewAlerts", v)} />
                      <Toggle label={translateText("Marketing alerts")} body={translateText("Growth and promotion recommendations.")} checked={settings.marketingAlerts} onChange={(v) => setField("marketingAlerts", v)} />
                      <Toggle label={translateText("WhatsApp promotions")} body={translateText("Enable WhatsApp-ready promotion communication.")} checked={settings.promotionWhatsappEnabled} onChange={(v) => setField("promotionWhatsappEnabled", v)} />
                    </div>
                  )}

                  {activeSection === "payments" && (
                    <div className="grid gap-4 md:grid-cols-2">
                      <Field label={translateText("Account holder name")} value={settings.bankAccountName} onChange={(v) => setField("bankAccountName", v)} placeholder="Blue Box Sports" />
                      <Field label={translateText("Bank account number")} value={settings.bankAccountNumber} onChange={(v) => setField("bankAccountNumber", v)} placeholder="1234567890" />
                      <Field label={translateText("IFSC code")} value={settings.bankIfsc} onChange={(v) => setField("bankIfsc", v)} placeholder="HDFC0001234" />
                      <Field label={translateText("UPI ID")} value={settings.upiId} onChange={(v) => setField("upiId", v)} placeholder="owner@upi" />
                      <SelectField label={translateText("Settlement schedule")} value={settings.settlementSchedule} onChange={(v) => setField("settlementSchedule", v as OwnerSettingsPayload["settlementSchedule"])} options={["DAILY", "WEEKLY", "MONTHLY"]} />
                      <SelectField label={translateText("Refund mode")} value={settings.refundMode} onChange={(v) => setField("refundMode", v as OwnerSettingsPayload["refundMode"])} options={["FULL", "PARTIAL", "WALLET_CREDIT", "RESCHEDULE_ONLY"]} />
                      <NumberField label={translateText("Minimum payment")} value={settings.minimumPaymentPercent} onChange={(v) => setField("minimumPaymentPercent", v)} suffix="%" />
                      <Toggle label={translateText("Partial payment")} body={translateText("Allow deposits before final slot confirmation.")} checked={settings.partialPaymentEnabled} onChange={(v) => setField("partialPaymentEnabled", v)} />
                    </div>
                  )}

                  {activeSection === "weather" && (
                    <div className="grid gap-4 md:grid-cols-2">
                      <NumberField label={translateText("Rain threshold")} value={settings.rainThresholdPercent} onChange={(v) => setField("rainThresholdPercent", v)} suffix="%" />
                      <Toggle label={translateText("Rain refund")} body={translateText("Allow weather-triggered refund workflows.")} checked={settings.rainRefundAllowed} onChange={(v) => setField("rainRefundAllowed", v)} />
                      <Toggle label={translateText("Free weather reschedule")} body={translateText("Let users move risky bookings to safer slots.")} checked={settings.freeWeatherReschedule} onChange={(v) => setField("freeWeatherReschedule", v)} />
                      <Toggle label={translateText("Weather risk alerts")} body={translateText("Notify staff and customers when weather risk rises.")} checked={settings.weatherRiskAlerts} onChange={(v) => setField("weatherRiskAlerts", v)} />
                    </div>
                  )}

                  {activeSection === "marketplace" && (
                    <div className="grid gap-4 md:grid-cols-2">
                      <NumberField label={translateText("Max resale markup")} value={settings.maxResaleMarkupPercent} onChange={(v) => setField("maxResaleMarkupPercent", v)} suffix="%" />
                      <Toggle label={translateText("Allow slot resale")} body={translateText("Customers can list eligible bookings in marketplace.")} checked={settings.resaleEnabled} onChange={(v) => setField("resaleEnabled", v)} />
                      <Toggle label={translateText("Allow transfer")} body={translateText("Allow direct booking transfer to another user.")} checked={settings.transferEnabled} onChange={(v) => setField("transferEnabled", v)} />
                      <Toggle label={translateText("Restrict suspicious accounts")} body={translateText("Use trust and abuse signals to reduce marketplace risk.")} checked={settings.restrictSuspiciousAccounts} onChange={(v) => setField("restrictSuspiciousAccounts", v)} />
                    </div>
                  )}

                  {activeSection === "community" && (
                    <div className="grid gap-4 md:grid-cols-2">
                      <SelectField label={translateText("Community visibility")} value={settings.communityVisibility} onChange={(v) => setField("communityVisibility", v as OwnerSettingsPayload["communityVisibility"])} options={["PUBLIC", "INVITE_ONLY", "PRIVATE"]} />
                      <SelectField label={translateText("Team registration")} value={settings.teamRegistrationRule} onChange={(v) => setField("teamRegistrationRule", v as OwnerSettingsPayload["teamRegistrationRule"])} options={["OPEN", "APPROVAL_REQUIRED", "INVITE_ONLY"]} />
                      <Toggle label={translateText("Public tournaments")} body={translateText("Allow public team registration for owner tournaments.")} checked={settings.allowPublicTournaments} onChange={(v) => setField("allowPublicTournaments", v)} />
                    </div>
                  )}

                  {activeSection === "branding" && (
                    <div className="grid gap-4 md:grid-cols-2">
                      <Field label={translateText("Brand color")} value={settings.brandPrimaryColor} onChange={(v) => setField("brandPrimaryColor", v)} placeholder="#0EA5E9" />
                      <SelectField label={translateText("Brand theme")} value={settings.brandTheme} onChange={(v) => setField("brandTheme", v as OwnerSettingsPayload["brandTheme"])} options={["system", "light", "dark"]} />
                      <Toggle label={translateText("Compact dashboard")} body={translateText("Use denser owner dashboard spacing for operations-heavy days.")} checked={settings.compactDashboard} onChange={(v) => setField("compactDashboard", v)} />
                      <div className="theme-panel-soft rounded-3xl p-5">
                        <h3 className="theme-title text-base font-black">{translateText("Current app theme")}</h3>
                        <p className="theme-muted mt-2 text-sm">{translateText("This controls your local Cricbook UI theme.")}</p>
                        <ThemeToggle className="mt-4" />
                      </div>
                    </div>
                  )}

                  {activeSection === "security" && (
                    <div className="grid gap-4 md:grid-cols-2">
                      <NumberField label={translateText("Session timeout")} value={settings.sessionTimeoutMinutes} onChange={(v) => setField("sessionTimeoutMinutes", v)} suffix="min" />
                      <Field label={translateText("IP restrictions")} value={settings.ipRestrictions} onChange={(v) => setField("ipRestrictions", v)} placeholder="Optional office IP allowlist" />
                      <Toggle label={translateText("Two-factor authentication")} body={translateText("Prepare owner account for OTP or authenticator-based login.")} checked={settings.twoFactorEnabled} onChange={(v) => setField("twoFactorEnabled", v)} />
                      <AccountCard account={account} translateText={translateText} />
                    </div>
                  )}

                  {activeSection === "automation" && (
                    <div className="grid gap-4 md:grid-cols-2">
                      <Toggle label={translateText("Auto pricing")} body={translateText("Allow future automation to adjust eligible slot pricing.")} checked={settings.autoPricingEnabled} onChange={(v) => setField("autoPricingEnabled", v)} />
                      <Toggle label={translateText("Auto reschedule")} body={translateText("Use weather and availability signals to suggest safer slots.")} checked={settings.autoRescheduleEnabled} onChange={(v) => setField("autoRescheduleEnabled", v)} />
                      <Toggle label={translateText("AI business coach")} body={translateText("Enable AI recommendations in owner insights.")} checked={settings.aiCoachEnabled} onChange={(v) => setField("aiCoachEnabled", v)} />
                      <Toggle label={translateText("Smart promotions")} body={translateText("Use occupancy gaps to suggest offers.")} checked={settings.smartPromotionsEnabled} onChange={(v) => setField("smartPromotionsEnabled", v)} />
                    </div>
                  )}

                  {activeSection === "reports" && (
                    <div className="grid gap-4 md:grid-cols-2">
                      <SelectField label={translateText("Export format")} value={settings.exportFormat} onChange={(v) => setField("exportFormat", v as OwnerSettingsPayload["exportFormat"])} options={["CSV", "EXCEL", "PDF"]} />
                      <Toggle label={translateText("Revenue exports")} body={translateText("Enable owner revenue report exports.")} checked={settings.revenueExportEnabled} onChange={(v) => setField("revenueExportEnabled", v)} />
                      <Toggle label={translateText("GST reports")} body={translateText("Enable GST-ready owner report exports.")} checked={settings.gstReportsEnabled} onChange={(v) => setField("gstReportsEnabled", v)} />
                      <div className="theme-panel-soft rounded-3xl p-5">
                        <h3 className="theme-title text-base font-black">{translateText("Data export")}</h3>
                        <p className="theme-muted mt-2 text-sm">{translateText("Request a fresh export package for business, booking, revenue, and customer configuration records.")}</p>
                        <p className="theme-muted mt-3 text-xs">{translateText("Last requested")}: {account?.lastExportRequestedAt || translateText("Never")}</p>
                        <button type="button" onClick={requestExport} disabled={saving} className="mt-4 rounded-2xl border border-sky-400/40 px-4 py-3 text-sm font-black text-sky-600 transition hover:-translate-y-0.5 dark:text-sky-200">
                          {translateText("Request Export")}
                        </button>
                      </div>
                    </div>
                  )}
                </SettingsPanel>

                <div className="sticky bottom-4 z-10 flex justify-end">
                  <button
                    type="button"
                    onClick={save}
                    disabled={saving}
                    className="rounded-2xl bg-gradient-to-r from-sky-500 to-blue-700 px-6 py-3 text-sm font-black text-white shadow-xl shadow-sky-500/25 transition hover:-translate-y-0.5 disabled:opacity-60"
                  >
                    {saving ? translateText("Saving...") : translateText("Save Owner Settings")}
                  </button>
                </div>
              </>
            )}
          </main>
        </div>
      </div>
    </DashboardLayout>
  );
}

const sectionTabs: Array<{ key: SectionKey; title: string; body: string }> = [
  { key: "business", title: "Business Profile", body: "Identity, GST and support" },
  { key: "turfs", title: "Turfs", body: "Slots, hours and amenities" },
  { key: "booking", title: "Booking Policies", body: "Approval, trust and cancellation" },
  { key: "pricing", title: "Pricing", body: "Peak, weekend and dynamic rules" },
  { key: "staff", title: "Staff", body: "Roles and permission notes" },
  { key: "notifications", title: "Notifications", body: "Operational communication" },
  { key: "payments", title: "Payments & Payouts", body: "Bank, UPI and refunds" },
  { key: "weather", title: "Weather Rules", body: "Rain and reschedule policy" },
  { key: "marketplace", title: "Marketplace Rules", body: "Resale and transfer control" },
  { key: "community", title: "Tournaments", body: "Community and team rules" },
  { key: "branding", title: "Branding", body: "Theme and dashboard style" },
  { key: "security", title: "Security", body: "Access and account safety" },
  { key: "automation", title: "Automation", body: "AI and smart operations" },
  { key: "reports", title: "Reports & Export", body: "Compliance and data export" },
];

const sectionHelp: Record<SectionKey, string> = {
  business: "Manage your turf business identity used across owner operations, support, invoices and customer communication.",
  turfs: "Set defaults that keep turf configuration consistent across slot generation, operations and availability planning.",
  booking: "Control how customers book, how strict approval should be, and how trust rules affect booking quality.",
  pricing: "Configure the business rules that support peak pricing, weekend pricing and future AI pricing recommendations.",
  staff: "Prepare staff access structure with role and permission defaults before adding full multi-staff login flows.",
  notifications: "Tune owner communication so operational alerts stay useful without becoming spam.",
  payments: "Manage payout details, split-payment protection, refund mode and settlement cadence.",
  weather: "Configure rain protection and reschedule rules that reduce disputes during risky weather.",
  marketplace: "Control slot resale, booking transfers and abuse protection for the live slot marketplace.",
  community: "Manage tournament registration and community visibility for owner-led cricket activity.",
  branding: "Keep owner portal appearance consistent while preparing future white-label customization.",
  security: "Centralize owner account safety controls, session timeout and access restrictions.",
  automation: "Choose how much AI and automation should participate in pricing, rescheduling, promotions and insights.",
  reports: "Control compliance exports, revenue reports and owner data requests.",
};

function toPayload(data: OwnerSettingsResponse): OwnerSettingsPayload {
  return { ...emptySettings, ...data };
}

function localReadiness(settings: OwnerSettingsPayload) {
  const fields = [settings.businessName, settings.supportPhone, settings.supportEmail, settings.businessAddress, settings.openingTime, settings.closingTime, settings.bankAccountName, settings.upiId, settings.cancellationPolicy, settings.pricingMode];
  return Math.round((fields.filter((field) => String(field || "").trim()).length / fields.length) * 100);
}

function SettingsPanel({ eyebrow, title, body, children }: { eyebrow: string; title: string; body: string; children: React.ReactNode }) {
  return (
    <section className="theme-panel rounded-[2rem] p-6 sm:p-8">
      <p className="theme-accent-owner text-xs font-black uppercase tracking-[0.28em]">{eyebrow}</p>
      <h2 className="theme-title mt-3 text-2xl font-black">{title}</h2>
      <p className="theme-muted mt-2 max-w-3xl text-sm leading-6">{body}</p>
      <div className="mt-6">{children}</div>
    </section>
  );
}

function Field({ label, value, onChange, placeholder, multiline = false }: { label: string; value: string; onChange: (value: string) => void; placeholder: string; multiline?: boolean }) {
  return (
    <label className={multiline ? "md:col-span-2" : ""}>
      <span className="theme-title text-sm font-bold">{label}</span>
      {multiline ? (
        <textarea value={value || ""} onChange={(event) => onChange(event.target.value)} placeholder={placeholder} rows={4} className="theme-input mt-2 w-full resize-none rounded-2xl px-4 py-3 text-sm outline-none" />
      ) : (
        <input value={value || ""} onChange={(event) => onChange(event.target.value)} placeholder={placeholder} className="theme-input mt-2 w-full rounded-2xl px-4 py-3 text-sm outline-none" />
      )}
    </label>
  );
}

function NumberField({ label, value, onChange, suffix, step = "1" }: { label: string; value: number; onChange: (value: number) => void; suffix: string; step?: string }) {
  return (
    <label>
      <span className="theme-title text-sm font-bold">{label}</span>
      <div className="theme-input mt-2 flex items-center rounded-2xl px-4 py-3">
        <input type="number" step={step} value={value ?? 0} onChange={(event) => onChange(Number(event.target.value))} className="w-full bg-transparent text-sm outline-none" />
        <span className="theme-muted text-xs font-black uppercase">{suffix}</span>
      </div>
    </label>
  );
}

function SelectField({ label, value, onChange, options }: { label: string; value: string; onChange: (value: string) => void; options: string[] }) {
  return (
    <label>
      <span className="theme-title text-sm font-bold">{label}</span>
      <select value={value} onChange={(event) => onChange(event.target.value)} className="theme-input mt-2 w-full rounded-2xl px-4 py-3 text-sm outline-none">
        {options.map((option) => (
          <option key={option} value={option}>{option.replaceAll("_", " ")}</option>
        ))}
      </select>
    </label>
  );
}

function Toggle({ label, body, checked, onChange }: { label: string; body: string; checked: boolean; onChange: (checked: boolean) => void }) {
  return (
    <button type="button" onClick={() => onChange(!checked)} className={`rounded-3xl border p-5 text-left transition ${checked ? "border-sky-400/60 bg-sky-500/10" : "theme-panel-soft border-slate-300 dark:border-white/10"}`}>
      <div className="flex items-start justify-between gap-4">
        <div>
          <p className="theme-title font-black">{label}</p>
          <p className="theme-muted mt-2 text-sm leading-6">{body}</p>
        </div>
        <span className={`mt-1 h-7 w-12 rounded-full p-1 transition ${checked ? "bg-sky-500" : "bg-slate-400/40"}`}>
          <span className={`block h-5 w-5 rounded-full bg-white transition ${checked ? "translate-x-5" : ""}`} />
        </span>
      </div>
    </button>
  );
}

function MetricCard({ label, value }: { label: string; value: string }) {
  return (
    <div className="theme-panel-soft rounded-2xl p-4">
      <p className="theme-soft text-[11px] font-black uppercase tracking-[0.2em]">{label}</p>
      <p className="theme-title mt-2 text-xl font-black">{value}</p>
    </div>
  );
}

function AccountCard({ account, translateText }: { account: OwnerSettingsResponse | null; translateText: (value: string) => string }) {
  return (
    <div className="theme-panel-soft rounded-3xl p-5">
      <h3 className="theme-title text-base font-black">{translateText("Account status")}</h3>
      <div className="mt-4 space-y-3">
        <AccountRow label={translateText("Email")} value={account?.ownerEmail || "-"} />
        <AccountRow label={translateText("Phone")} value={account?.ownerPhone || "-"} />
        <AccountRow label={translateText("Status")} value={account?.accountStatus || "-"} />
      </div>
    </div>
  );
}

function AccountRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="theme-card flex items-center justify-between gap-4 rounded-2xl px-4 py-3">
      <span className="theme-muted text-xs font-bold uppercase tracking-[0.18em]">{label}</span>
      <span className="theme-title break-words text-right text-sm font-bold">{value}</span>
    </div>
  );
}

function StatusBanner({ tone, text }: { tone: "success" | "error"; text: string }) {
  return (
    <div className={`rounded-2xl border px-4 py-3 text-sm font-semibold ${tone === "success" ? "border-emerald-400/40 bg-emerald-500/10 text-emerald-700 dark:text-emerald-200" : "border-red-400/40 bg-red-500/10 text-red-700 dark:text-red-200"}`}>
      {text}
    </div>
  );
}

function readApiError(error: unknown, fallback: string) {
  const apiError = error as { response?: { data?: { message?: string; error?: string } } };
  return apiError.response?.data?.message || apiError.response?.data?.error || fallback;
}
