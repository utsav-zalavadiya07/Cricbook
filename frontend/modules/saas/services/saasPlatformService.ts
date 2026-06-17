import api from "@/services/api";

export type SaasMap = Record<string, any>;

export type TenantPayload = {
  name: string;
  domain: string;
  tenantType?: string;
  countryCode?: string;
  regionCode?: string;
  defaultLocale?: string;
  currency?: string;
  timezone?: string;
  lifecycleStage?: string;
  status?: string;
};

export type BrandingPayload = {
  tenantId: number;
  appName?: string;
  logoUrl?: string;
  faviconUrl?: string;
  primaryColor?: string;
  secondaryColor?: string;
  fontFamily?: string;
  published?: boolean;
};

export type CustomDomainPayload = {
  tenantId: number;
  domain: string;
  routingMode?: string;
};

export type FeatureTogglePayload = {
  tenantId: number;
  featureKey: string;
  enabled: boolean;
  rolloutStrategy?: string;
  notes?: string;
};

export type SubscriptionPayload = {
  tenantId: number;
  planCode: string;
  billingCycle?: string;
  price: number;
  currency?: string;
  autoRenew?: boolean;
  startDate?: string;
  endDate?: string;
};

export type InvoicePayload = {
  tenantId: number;
  subscriptionId?: number;
  amount: number;
  taxAmount?: number;
  currency?: string;
  dueDate?: string;
};

export type InvoicePaymentPayload = {
  amount: number;
  paymentReference?: string;
};

export type MarketplaceModulePayload = {
  moduleCode: string;
  name: string;
  category?: string;
  pricingModel?: string;
  price?: number;
  currency?: string;
  certificationStatus?: string;
};

export type ModuleInstallationPayload = {
  tenantId: number;
  moduleId: number;
  licenseType?: string;
};

export type AppBuildPayload = {
  tenantId: number;
  appType?: string;
  platform?: string;
  version?: string;
};

export type CompliancePayload = {
  tenantId: number;
  regulation?: string;
  requestType?: string;
  subjectEmail?: string;
  notes?: string;
};

export type ResellerPayload = {
  name: string;
  email: string;
  regionCode?: string;
  commissionPercent?: number;
  parentResellerId?: number;
};

export const getSaasTenantContext = (tenantId?: number) => api.get<SaasMap>("/saas/tenant-context", { params: tenantId ? { tenantId } : undefined });
export const getSaasMarketplaceModules = () => api.get<SaasMap[]>("/saas/marketplace/modules");
export const getSaasRegions = () => api.get<SaasMap[]>("/saas/regions");

export const getAdminSaasDashboard = () => api.get<SaasMap>("/admin/saas/dashboard");
export const getAdminSaasTenants = () => api.get<SaasMap[]>("/admin/saas/tenants");
export const createAdminSaasTenant = (payload: TenantPayload) => api.post<SaasMap>("/admin/saas/tenants", payload);
export const updateAdminSaasTenantStatus = (tenantId: number, payload: { status?: string; lifecycleStage?: string; reason?: string }) => api.post<SaasMap>(`/admin/saas/tenants/${tenantId}/status`, payload);
export const saveAdminSaasBranding = (payload: BrandingPayload) => api.post<SaasMap>("/admin/saas/branding", payload);
export const addAdminSaasDomain = (payload: CustomDomainPayload) => api.post<SaasMap>("/admin/saas/domains", payload);
export const verifyAdminSaasDomain = (domainId: number) => api.post<SaasMap>(`/admin/saas/domains/${domainId}/verify`);
export const activateAdminSaasDomain = (domainId: number) => api.post<SaasMap>(`/admin/saas/domains/${domainId}/activate`);
export const setAdminSaasFeature = (payload: FeatureTogglePayload) => api.post<SaasMap>("/admin/saas/features", payload);
export const createAdminSaasSubscription = (payload: SubscriptionPayload) => api.post<SaasMap>("/admin/saas/subscriptions", payload);
export const renewAdminSaasSubscription = (subscriptionId: number) => api.post<SaasMap>(`/admin/saas/subscriptions/${subscriptionId}/renew`);
export const generateAdminSaasInvoice = (payload: InvoicePayload) => api.post<SaasMap>("/admin/saas/invoices", payload);
export const payAdminSaasInvoice = (invoiceId: number, payload: InvoicePaymentPayload) => api.post<SaasMap>(`/admin/saas/invoices/${invoiceId}/pay`, payload);
export const publishAdminSaasMarketplaceModule = (payload: MarketplaceModulePayload) => api.post<SaasMap>("/admin/saas/marketplace/modules", payload);
export const installAdminSaasModule = (payload: ModuleInstallationPayload) => api.post<SaasMap>("/admin/saas/marketplace/installations", payload);
export const requestAdminSaasAppBuild = (payload: AppBuildPayload) => api.post<SaasMap>("/admin/saas/app-builds", payload);
export const createAdminSaasComplianceRequest = (payload: CompliancePayload) => api.post<SaasMap>("/admin/saas/compliance", payload);
export const registerAdminSaasReseller = (payload: ResellerPayload) => api.post<SaasMap>("/admin/saas/resellers", payload);
export const getAdminSaasAudit = () => api.get<SaasMap[]>("/admin/saas/audit");
