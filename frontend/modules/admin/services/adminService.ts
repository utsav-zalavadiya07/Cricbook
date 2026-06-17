import api from "@/services/api";

export type AdminTrendPoint = {
  date: string;
  revenue?: number;
  users?: number;
  bookings?: number;
  teams?: number;
  activity?: number;
};

export type AdminFunnelStep = {
  label: string;
  value: number;
};

export type AdminInsight = {
  title: string;
  message: string;
  severity: string;
};

export type AdminExecutiveAnalytics = {
  totalUsers: number;
  totalOwners: number;
  totalRevenue: number;
  monthlyRevenue: number;
  commissionRevenue: number;
  refunds: number;
  activePlayers: number;
  activeOwners: number;
  bookingsToday: number;
  occupancyPercent: number;
  retentionPercent: number;
  growthPercent: number;
  communityHealth: number;
  safetyScore: number;
  flaggedRiskCount: number;
  revenueTrend: AdminTrendPoint[];
  userGrowth: AdminTrendPoint[];
  bookingTrend: AdminTrendPoint[];
  ownerGrowth: AdminTrendPoint[];
  revenueAnalytics: {
    dailyRevenue: AdminTrendPoint[];
    weeklyRevenue: number;
    monthlyRevenue: number;
    yearlyRevenue: number;
    commissionRevenue: number;
    byCity: Record<string, number>;
    byTurf: Record<string, number>;
    byOwner: Record<string, number>;
    bySport: Record<string, number>;
    refundRatio: number;
  };
  userAnalytics: {
    newRegistrations: number;
    activeUsers: number;
    dailyActiveUsers: number;
    monthlyActiveUsers: number;
    funnel: AdminFunnelStep[];
  };
  ownerAnalytics: {
    topOwners: Array<Record<string, string | number>>;
    topRevenue: Array<Record<string, string | number>>;
    topGrowth: Array<Record<string, string | number>>;
    averageOwnerBookings: number;
  };
  turfAnalytics: {
    occupancyPercent: number;
    peakHours: Array<{ hour: string; bookings: number }>;
    revenuePerTurf: Array<Record<string, string | number>>;
    cancellationRate: number;
    heatmap: Array<{ day: string; bookings: number }>;
    insight: string;
  };
  bookingFunnel: {
    steps: AdminFunnelStep[];
    conversionPercent: number;
    dropOffPercent: number;
    insight: string;
  };
  communityAnalytics: {
    posts: number;
    comments: number;
    teams: number;
    invites: number;
    connections: number;
    engagementRate: number;
    activeTeams: number;
    networkGrowth: AdminTrendPoint[];
    activity: AdminTrendPoint[];
  };
  tournamentAnalytics: {
    registrations: number;
    participation: number;
    revenue: number;
    completionRate: number;
    topEvents: Array<Record<string, string | number>>;
  };
  retentionAnalytics: {
    day1: number;
    day7: number;
    day30: number;
    day90: number;
    cohorts: Array<Record<string, string | number>>;
    insight: string;
  };
  geographicAnalytics: {
    cities: Array<Record<string, string | number>>;
    topCity: string;
  };
  forecasting: {
    expectedRevenueNextMonth: number;
    expectedBookingsNextMonth: number;
    expectedOccupancy: number;
    expectedGrowth: number;
    model: string;
  };
  aiInsights: AdminInsight[];
};

export type AdminBiWorkspace = {
  generatedAt?: string;
  executive: AdminExecutiveAnalytics;
  scorecard: {
    analyticsConfidenceScore: number;
    dataQualityScore: number;
    forecastConfidence: number;
    decisionReadiness: string;
    kpiCoverage: string[];
    singleScreenAccuracy: Record<string, string | number>;
  };
  dashboardBuilder: {
    savedLayout: string;
    templates: Array<{ key: string; name: string; description: string }>;
    widgets: Array<{ id: string; title: string; type: string; dataKey: string }>;
    permissions: Record<string, string>;
  };
  reportCatalog: Array<{ type: string; title: string; description: string; formats: string[]; permissions: string[] }>;
  forecastAccuracy: Array<{ metric: string; forecast: number; actual: number; deviation: number; confidence: number; status: string }>;
  reconciliation: AdminBiReconciliation;
  dataQuality: {
    score: number;
    missingBookingDate: number;
    missingPaymentAmount: number;
    missingTurfCity: number;
    delayedAnalyticsEvents: number;
    duplicateBookingSignals: number;
    lineage: string[];
  };
  insights: AdminInsight[];
};

export type AdminBiReconciliation = {
  generatedAt?: string;
  score: number;
  status: string;
  checks: Array<{ name: string; status: string; records: number; mismatches: number }>;
  mismatches: Array<{ name: string; status: string; records: number; mismatches: number }>;
};

export type AdminBiReport = {
  type: string;
  period: string;
  generatedAt?: string;
  accuracyStatus: string;
  rows: Array<Record<string, string | number>>;
  totals: Record<string, string | number>;
  insights: AdminInsight[];
};

export type AdminBiReportExport = {
  fileName: string;
  format: string;
  generatedAt?: string;
  report: AdminBiReport;
  rows: Array<Record<string, string | number>>;
};

export const getAdminAnalytics = () => api.get<AdminExecutiveAnalytics>("/admin/analytics");

export const getAdminExecutiveAnalytics = () => api.get<AdminExecutiveAnalytics>("/admin/analytics/executive");

export const getAdminBiWorkspace = () => api.get<AdminBiWorkspace>("/admin/analytics/bi");

export const getAdminBiForecasts = () => api.get("/admin/analytics/forecasts");

export const getAdminBiReconciliation = () => api.get<AdminBiReconciliation>("/admin/analytics/reconciliation");

export const getAdminBiReport = (params?: { type?: string; period?: string }) => api.get<AdminBiReport>("/admin/analytics/reports", { params });

export const exportAdminBiReport = (params?: { type?: string; period?: string; format?: string }) => api.get<AdminBiReportExport>("/admin/analytics/reports/export", { params });

export const getAdminBiDashboards = () => api.get<AdminBiWorkspace["dashboardBuilder"]>("/admin/analytics/dashboards");

export const saveAdminBiDashboard = (payload: { name: string; audience: string; widgets: string[] }) => api.post<AdminBiWorkspace["dashboardBuilder"]>("/admin/analytics/dashboards", payload);

export type AdminMarketingInsight = {
  title: string;
  message: string;
  severity: string;
};

export type AdminMarketingDashboard = {
  activeCampaigns: number;
  couponUsage: number;
  referralRevenue: number;
  pushDeliveryRate: number;
  emailOpenRate: number;
  campaignRoi: number;
  aiInsights: AdminMarketingInsight[];
};

export type AdminCmsPage = {
  id?: number;
  slug: string;
  title: string;
  content: string;
  status: string;
  versionNumber?: number;
  updatedAt?: string;
};

export type AdminMarketingBanner = {
  id?: number;
  title: string;
  location: string;
  bannerType: string;
  imageUrl: string;
  targetUrl?: string;
  status: string;
  createdAt?: string;
};

export type AdminMarketingCoupon = {
  id?: number;
  code: string;
  discountType: string;
  discountValue: number;
  maxDiscount?: number;
  minBookingAmount?: number;
  validFrom?: string;
  validUntil?: string;
  active?: boolean;
  usageCount?: number;
};

export type AdminReferralCampaign = {
  id?: number;
  name: string;
  reward: number;
  eligibility: string;
  status: string;
  expiresAt?: string;
};

export type AdminMarketingCampaign = {
  id?: number;
  name: string;
  title: string;
  message: string;
  channel: "EMAIL" | "PUSH" | "SMS" | "WHATSAPP";
  targetRole?: string;
  deepLink?: string;
  status?: string;
  sentCount?: number;
  scheduledAt?: string;
  createdAt?: string;
};

export type AdminNotificationTemplate = {
  id?: number;
  name: string;
  subject?: string;
  body: string;
  channel: "EMAIL" | "PUSH" | "SMS" | "WHATSAPP";
};

export type AdminAnnouncement = {
  id?: number;
  title: string;
  message: string;
  priority: string;
  displayLocation: string;
  status: string;
};

export type AdminLandingPage = {
  id?: number;
  slug: string;
  title: string;
  templateName: string;
  contentJson: string;
  status: string;
};

export type AdminSeoSetting = {
  id?: number;
  pageSlug: string;
  metaTitle: string;
  metaDescription: string;
  keywords?: string;
  openGraphImage?: string;
};

export type AdminAudienceSegment = {
  id?: number;
  name: string;
  ruleType: string;
  ruleValue: string;
};

export type AdminMarketingAnalytics = {
  couponUsage: number;
  referralConversion: number;
  campaignRoi: number;
  pushOpenRate: number;
  emailOpenRate: number;
};

export type AdminGrowthInsight = {
  title: string;
  message: string;
  severity: string;
};

export type AdminGrowthSegment = {
  name: string;
  description: string;
  customers: number;
  rule: string;
  recommendedAction: string;
};

export type AdminGrowthDashboard = {
  generatedAt?: string;
  scorecard: {
    totalCustomers: number;
    activeCustomers: number;
    repeatCustomers: number;
    vipCustomers: number;
    inactiveCustomers: number;
    customerLifetimeValue: number;
    averageClv: number;
  };
  loyalty: Record<string, number>;
  memberships: Record<string, number>;
  referrals: Record<string, number>;
  coupons: Record<string, number>;
  wallet: Record<string, number>;
  campaigns: Record<string, number>;
  retention: Record<string, number>;
  segments: AdminGrowthSegment[];
  insights: AdminGrowthInsight[];
};

export type AdminGrowthAutomation = {
  key: string;
  trigger: string;
  audience: number;
  action: string;
  status: string;
};

export type AdminGrowthReconciliation = {
  generatedAt?: string;
  status: string;
  score: number;
  checks: Array<{ name: string; records: number; mismatches: number; status: string }>;
};

export const getAdminMarketingDashboard = () => api.get<AdminMarketingDashboard>("/admin/marketing/dashboard");
export const getAdminCmsPages = () => api.get<AdminCmsPage[]>("/admin/marketing/cms-pages");
export const saveAdminCmsPage = (payload: AdminCmsPage) => api.post<AdminCmsPage>("/admin/marketing/cms-pages", payload);
export const getAdminMarketingBanners = () => api.get<AdminMarketingBanner[]>("/admin/marketing/banners");
export const saveAdminMarketingBanner = (payload: AdminMarketingBanner) => api.post<AdminMarketingBanner>("/admin/marketing/banners", payload);
export const getAdminMarketingCoupons = () => api.get<AdminMarketingCoupon[]>("/admin/marketing/coupons");
export const saveAdminMarketingCoupon = (payload: AdminMarketingCoupon) => api.post<AdminMarketingCoupon>("/admin/marketing/coupons", payload);
export const getAdminReferralCampaigns = () => api.get<AdminReferralCampaign[]>("/admin/marketing/referral-campaigns");
export const saveAdminReferralCampaign = (payload: AdminReferralCampaign) => api.post<AdminReferralCampaign>("/admin/marketing/referral-campaigns", payload);
export const getAdminMarketingCampaigns = () => api.get<AdminMarketingCampaign[]>("/admin/marketing/campaigns");
export const createAdminMarketingCampaign = (payload: AdminMarketingCampaign) => api.post<AdminMarketingCampaign>("/admin/marketing/campaigns", payload);
export const sendAdminMarketingCampaign = (id: number) => api.post<AdminMarketingCampaign>(`/admin/marketing/campaigns/${id}/send`);
export const getAdminMarketingTemplates = () => api.get<AdminNotificationTemplate[]>("/admin/marketing/templates");
export const saveAdminMarketingTemplate = (payload: AdminNotificationTemplate) => api.post<AdminNotificationTemplate>("/admin/marketing/templates", payload);
export const getAdminAnnouncements = () => api.get<AdminAnnouncement[]>("/admin/marketing/announcements");
export const saveAdminAnnouncement = (payload: AdminAnnouncement) => api.post<AdminAnnouncement>("/admin/marketing/announcements", payload);
export const getAdminLandingPages = () => api.get<AdminLandingPage[]>("/admin/marketing/landing-pages");
export const saveAdminLandingPage = (payload: AdminLandingPage) => api.post<AdminLandingPage>("/admin/marketing/landing-pages", payload);
export const getAdminSeoSettings = () => api.get<AdminSeoSetting[]>("/admin/marketing/seo");
export const saveAdminSeoSetting = (payload: AdminSeoSetting) => api.post<AdminSeoSetting>("/admin/marketing/seo", payload);
export const getAdminAudienceSegments = () => api.get<AdminAudienceSegment[]>("/admin/marketing/segments");
export const saveAdminAudienceSegment = (payload: AdminAudienceSegment) => api.post<AdminAudienceSegment>("/admin/marketing/segments", payload);
export const getAdminMarketingAnalytics = () => api.get<AdminMarketingAnalytics>("/admin/marketing/analytics");
export const getAdminGrowthDashboard = () => api.get<AdminGrowthDashboard>("/admin/growth/dashboard");
export const getAdminGrowthSegments = () => api.get<AdminGrowthSegment[]>("/admin/growth/segments");
export const getAdminGrowthAutomation = () => api.get<AdminGrowthAutomation[]>("/admin/growth/automation");
export const getAdminGrowthReconciliation = () => api.get<AdminGrowthReconciliation>("/admin/growth/reconciliation");

export type AdminEnterpriseMetricMap = Record<string, string | number | boolean | null | undefined>;

export type AdminEnterpriseDashboard = {
  generatedAt?: string;
  businesses: AdminEnterpriseMetricMap[];
  kpis: {
    totalBusinesses: number;
    totalCenters: number;
    totalFranchises: number;
    totalTurfs: number;
    activeTurfs: number;
    totalStaff: number;
    totalBookings: number;
    totalRevenue: number;
    occupancyPercent: number;
    customerCount: number;
  };
  drillDown: {
    platformToCity: Record<string, number>;
    cityToCenter: AdminEnterpriseMetricMap[];
    centerToTurf: Record<string, AdminEnterpriseMetricMap[]>;
  };
  centerRankings: AdminEnterpriseMetricMap[];
  crm: AdminEnterpriseMetricMap;
  governance: AdminEnterpriseGovernance;
  insights: Array<{ title: string; message: string; severity: string }>;
};

export type AdminEnterpriseCenter = AdminEnterpriseMetricMap & {
  id: number | string;
  title: string;
  city: string;
  status: string;
  turfs: number;
  bookings: number;
  revenue: number;
  occupancyPercent: number;
  managerName?: string;
};

export type AdminEnterpriseStaff = AdminEnterpriseMetricMap & {
  id: number | string;
  title: string;
  role: string;
  status: string;
  centerName?: string;
  permissions?: string;
  kpiScore?: number;
};

export type AdminEnterpriseFranchise = AdminEnterpriseMetricMap & {
  id: number | string;
  title: string;
  status: string;
  city?: string;
  revenue?: number;
  revenueSharePercent?: number;
};

export type AdminEnterpriseReports = {
  generatedAt?: string;
  revenueReport: Record<string, number>;
  occupancyReport: AdminEnterpriseMetricMap[];
  staffPerformanceReport: AdminEnterpriseMetricMap[];
  customerGrowthReport: AdminEnterpriseMetricMap;
  exports: AdminEnterpriseMetricMap[];
};

export type AdminEnterpriseGovernance = {
  score: number;
  status: string;
  controls: Array<{ name: string; status: string; detail: string }>;
  settings: Array<{ key: string; description: string; precedence: string }>;
};

export const getAdminEnterpriseDashboard = () => api.get<AdminEnterpriseDashboard>("/admin/enterprise/dashboard");
export const getAdminEnterpriseCenters = () => api.get<AdminEnterpriseCenter[]>("/admin/enterprise/centers");
export const getAdminEnterpriseStaff = () => api.get<AdminEnterpriseStaff[]>("/admin/enterprise/staff");
export const getAdminEnterpriseFranchises = () => api.get<AdminEnterpriseFranchise[]>("/admin/enterprise/franchises");
export const getAdminEnterpriseReports = () => api.get<AdminEnterpriseReports>("/admin/enterprise/reports");
export const getAdminEnterpriseGovernance = () => api.get<AdminEnterpriseGovernance>("/admin/enterprise/governance");

export type AdminWorkforceMap = Record<string, string | number | boolean | null | undefined>;

export type AdminWorkforceDashboard = {
  generatedAt?: string;
  staff?: AdminWorkforceMap | null;
  centers: AdminWorkforceMap[];
  kpis: {
    todayBookings: number;
    pendingCheckIns: number;
    openTasks: number;
    maintenanceIssues: number;
    emergencyAlerts: number;
    equipmentDamaged: number;
  };
  schedule: AdminWorkforceMap[];
  tasks: AdminWorkforceMap[];
  maintenance: AdminWorkforceMap[];
  announcements: AdminWorkforceMap[];
  analytics: AdminWorkforceAnalytics;
};

export type AdminWorkforceAnalytics = {
  attendancePercent: number;
  taskCompletionPercent: number;
  issueResolutionPercent: number;
  utilizationPercent: number;
  performanceScore: number;
  topPerformers: AdminWorkforceMap[];
};

export type AdminWorkforceReports = {
  generatedAt?: string;
  attendanceReport: AdminWorkforceMap[];
  taskReport: AdminWorkforceMap[];
  maintenanceReport: AdminWorkforceMap[];
  staffPerformanceReport: AdminWorkforceMap[];
  exports: AdminWorkforceMap[];
};

export const getAdminWorkforceDashboard = () => api.get<AdminWorkforceDashboard>("/admin/workforce/dashboard");
export const getAdminWorkforceTasks = () => api.get<AdminWorkforceMap[]>("/admin/workforce/tasks");
export const getAdminWorkforceMaintenance = () => api.get<AdminWorkforceMap[]>("/admin/workforce/maintenance");
export const getAdminWorkforceAttendance = () => api.get<AdminWorkforceMap[]>("/admin/workforce/attendance");
export const getAdminWorkforceEquipment = () => api.get<AdminWorkforceMap[]>("/admin/workforce/equipment");
export const getAdminWorkforceEmergencies = () => api.get<AdminWorkforceMap[]>("/admin/workforce/emergencies");
export const getAdminWorkforceAnalytics = () => api.get<AdminWorkforceAnalytics>("/admin/workforce/analytics");
export const getAdminWorkforceReports = () => api.get<AdminWorkforceReports>("/admin/workforce/reports");

export type AdminSportsMap = Record<string, string | number | boolean | null | undefined | string[] | Record<string, unknown> | Array<Record<string, unknown>>>;

export type AdminSportsDashboard = {
  generatedAt?: string;
  kpis: Record<string, string | number>;
  catalog: AdminSportsMap[];
  activeSports: AdminSportsMap[];
  sportTemplates: AdminSportsMap[];
  turfCoverage: AdminSportsMap[];
  crossModuleStatus: AdminSportsMap[];
  futureSportReadiness: AdminSportsMap;
  governanceControls: AdminSportsMap[];
  priorityActions: AdminSportsMap[];
};

export type AdminSportConfigPayload = {
  name: string;
  code?: string;
  icon?: string;
  rules?: string;
  active?: boolean;
  displayOrder?: number;
  minPlayers?: number;
  maxPlayers?: number;
  scoringType?: string;
  matchType?: string;
  rankingType?: string;
  teamBased?: boolean;
  tournamentEnabled?: boolean;
  statisticsEnabled?: boolean;
  configurationJson?: string;
  metadataJson?: string;
};

export const getSportsCatalog = (activeOnly = false) => api.get<AdminSportsMap[]>("/sports/catalog", { params: { activeOnly } });
export const getAdminSportsDashboard = () => api.get<AdminSportsDashboard>("/admin/sports/dashboard");
export const getAdminSportsCatalog = () => api.get<AdminSportsMap[]>("/admin/sports/catalog");
export const createAdminSport = (payload: AdminSportConfigPayload) => api.post<AdminSportsMap>("/admin/sports", payload);
export const updateAdminSport = (id: number, payload: AdminSportConfigPayload) => api.put<AdminSportsMap>(`/admin/sports/${id}`, payload);
export const setAdminSportStatus = (id: number, payload: { active: boolean; reason?: string }) => api.patch<AdminSportsMap>(`/admin/sports/${id}/status`, payload);
export const mapAdminSportToTurf = (payload: { sportId?: number; sportCode?: string; turfId: number }) => api.post<AdminSportsMap>("/admin/sports/turfs", payload);
export const getAdminSportsAnalytics = () => api.get<AdminSportsMap>("/admin/sports/analytics");
export const getAdminSportsAudit = () => api.get<AdminSportsMap[]>("/admin/sports/audit");
export const seedAdminSportsDefaults = () => api.post<AdminSportsMap>("/admin/sports/seed-defaults");
export const simulateSportRules = (payload: { sportCode: string; matchType?: string; playersPerSide?: number; payload?: Record<string, unknown> }) => api.post<AdminSportsMap>("/sports/rules/simulate", payload);

export type AdminDeveloperMap = Record<string, string | number | boolean | null | undefined | string[] | Record<string, unknown> | Array<Record<string, unknown>>>;

export type AdminDeveloperDashboard = {
  generatedAt?: string;
  kpis: Record<string, string | number>;
  developers: AdminDeveloperMap[];
  applications: AdminDeveloperMap[];
  apiKeys: AdminDeveloperMap[];
  webhooks: AdminDeveloperMap[];
  usageAnalytics: AdminDeveloperMap;
  ratePlans: AdminDeveloperMap[];
  apiProducts: AdminDeveloperMap[];
  docs: AdminDeveloperMap[];
  marketplace: AdminDeveloperMap[];
  securityControls: AdminDeveloperMap[];
  sandbox: AdminDeveloperMap;
  priorityActions: AdminDeveloperMap[];
};

export type DeveloperRegistrationPayload = {
  name: string;
  email: string;
  companyName?: string;
  website?: string;
  plan?: string;
};

export type DeveloperApplicationPayload = {
  developerId: number;
  name: string;
  environment?: string;
  callbackUrl?: string;
  scopes?: string[];
};

export type DeveloperApiKeyPayload = {
  applicationId: number;
  environment?: string;
  expiresAt?: string;
};

export type DeveloperWebhookPayload = {
  applicationId: number;
  url: string;
  eventTypes?: string[];
  secret?: string;
  status?: string;
};

export type DeveloperMarketplacePayload = {
  developerId: number;
  title: string;
  category?: string;
  description?: string;
  website?: string;
  pricingModel?: string;
  revenueSharePercent?: number;
};

export const getDeveloperPlatformDocs = () => api.get<AdminDeveloperMap>("/developer-platform/docs");
export const getDeveloperMarketplace = () => api.get<AdminDeveloperMap[]>("/developer-platform/marketplace");
export const registerDeveloper = (payload: DeveloperRegistrationPayload) => api.post<AdminDeveloperMap>("/developers/register", payload);
export const getAdminDeveloperPlatformDashboard = () => api.get<AdminDeveloperDashboard>("/admin/developer-platform/dashboard");
export const getAdminDevelopers = () => api.get<AdminDeveloperMap[]>("/admin/developer-platform/developers");
export const createAdminDeveloper = (payload: DeveloperRegistrationPayload) => api.post<AdminDeveloperMap>("/admin/developer-platform/developers", payload);
export const updateAdminDeveloperStatus = (id: number, payload: { status: string; reason?: string }) => api.patch<AdminDeveloperMap>(`/admin/developer-platform/developers/${id}/status`, payload);
export const getAdminDeveloperApplications = () => api.get<AdminDeveloperMap[]>("/admin/developer-platform/applications");
export const createAdminDeveloperApplication = (payload: DeveloperApplicationPayload) => api.post<AdminDeveloperMap>("/admin/developer-platform/applications", payload);
export const createAdminDeveloperSandbox = (developerId: number) => api.post<AdminDeveloperMap>("/admin/developer-platform/applications/sandbox", { developerId });
export const getAdminDeveloperApiKeys = () => api.get<AdminDeveloperMap[]>("/admin/developer-platform/api-keys");
export const createAdminDeveloperApiKey = (payload: DeveloperApiKeyPayload) => api.post<AdminDeveloperMap>("/admin/developer-platform/api-keys", payload);
export const rotateAdminDeveloperApiKey = (id: number) => api.post<AdminDeveloperMap>(`/admin/developer-platform/api-keys/${id}/rotate`);
export const revokeAdminDeveloperApiKey = (id: number) => api.post<AdminDeveloperMap>(`/admin/developer-platform/api-keys/${id}/revoke`);
export const getAdminDeveloperWebhooks = () => api.get<AdminDeveloperMap[]>("/admin/developer-platform/webhooks");
export const createAdminDeveloperWebhook = (payload: DeveloperWebhookPayload) => api.post<AdminDeveloperMap>("/admin/developer-platform/webhooks", payload);
export const testAdminDeveloperWebhook = (id: number, eventType = "booking.created") => api.post<AdminDeveloperMap>(`/admin/developer-platform/webhooks/${id}/test`, null, { params: { eventType } });
export const getAdminDeveloperAnalytics = () => api.get<AdminDeveloperMap>("/admin/developer-platform/analytics");
export const getAdminDeveloperBilling = () => api.get<AdminDeveloperMap>("/admin/developer-platform/billing");
export const getAdminDeveloperAudit = () => api.get<AdminDeveloperMap[]>("/admin/developer-platform/audit");
export const getAdminDeveloperMarketplace = () => api.get<AdminDeveloperMap[]>("/admin/developer-platform/marketplace");
export const createAdminDeveloperMarketplaceListing = (payload: DeveloperMarketplacePayload) => api.post<AdminDeveloperMap>("/admin/developer-platform/marketplace", payload);
export const updateAdminDeveloperMarketplaceStatus = (id: number, payload: { status: string; notes?: string }) => api.post<AdminDeveloperMap>(`/admin/developer-platform/marketplace/${id}/status`, payload);

export type AdminAcademyMap = Record<string, string | number | boolean | null | undefined | string[] | Record<string, unknown> | Array<Record<string, unknown>>>;

export type AdminAcademyDashboard = {
  generatedAt?: string;
  kpis: Record<string, string | number>;
  academies: AdminAcademyMap[];
  coaches: AdminAcademyMap[];
  programs: AdminAcademyMap[];
  batches: AdminAcademyMap[];
  enrollments: AdminAcademyMap[];
  talentLeaderboard: AdminAcademyMap[];
  academyCrm: AdminAcademyMap;
  revenue: AdminAcademyMap;
  skillProgression: AdminAcademyMap;
  certification: AdminAcademyMap;
  governanceControls: AdminAcademyMap[];
  priorityActions: AdminAcademyMap[];
};

export type AcademyRegistrationPayload = {
  name: string;
  description?: string;
  sportsOffered: string;
  city?: string;
  address?: string;
  website?: string;
  email?: string;
  phone?: string;
  logoUrl?: string;
  documentUrl?: string;
};

export type CoachRegistrationPayload = {
  academyId?: number;
  name: string;
  email: string;
  phone?: string;
  sport?: string;
  city?: string;
  experienceYears?: number;
  pricePerSession?: number;
  certifications?: string;
  achievements?: string;
  profilePhotoUrl?: string;
};

export type TrainingProgramPayload = {
  academyId: number;
  coachId: number;
  title: string;
  sport?: string;
  level?: string;
  description?: string;
  durationWeeks?: number;
  capacity?: number;
  price?: number;
  status?: string;
};

export const getAcademyMarketplace = (params?: { city?: string; sport?: string }) => api.get<AdminAcademyMap[]>("/academies/marketplace", { params });
export const getCoachMarketplace = (params?: { city?: string; sport?: string; minRating?: number }) => api.get<AdminAcademyMap[]>("/coaches/marketplace", { params });
export const getTrainingPrograms = (params?: { city?: string; sport?: string }) => api.get<AdminAcademyMap[]>("/training/programs", { params });
export const getTalentDiscovery = (params?: { city?: string; sport?: string; ageGroup?: string }) => api.get<AdminAcademyMap[]>("/talent/discovery", { params });
export const registerAcademy = (payload: AcademyRegistrationPayload) => api.post<AdminAcademyMap>("/academies/register", payload);
export const registerCoach = (payload: CoachRegistrationPayload) => api.post<AdminAcademyMap>("/coaches/register", payload);
export const getAdminAcademyDashboard = () => api.get<AdminAcademyDashboard>("/admin/academy/dashboard");
export const getAdminAcademies = () => api.get<AdminAcademyMap[]>("/admin/academy/academies");
export const createAdminAcademy = (payload: AcademyRegistrationPayload) => api.post<AdminAcademyMap>("/admin/academy/academies", payload);
export const updateAdminAcademyStatus = (id: number, payload: { status: string; notes?: string }) => api.post<AdminAcademyMap>(`/admin/academy/academies/${id}/status`, payload);
export const getAdminCoaches = () => api.get<AdminAcademyMap[]>("/admin/academy/coaches");
export const createAdminCoach = (payload: CoachRegistrationPayload) => api.post<AdminAcademyMap>("/admin/academy/coaches", payload);
export const updateAdminCoachStatus = (id: number, payload: { status: string; notes?: string }) => api.post<AdminAcademyMap>(`/admin/academy/coaches/${id}/status`, payload);
export const getAdminTrainingPrograms = () => api.get<AdminAcademyMap[]>("/admin/academy/programs");
export const createAdminTrainingProgram = (payload: TrainingProgramPayload) => api.post<AdminAcademyMap>("/admin/academy/programs", payload);
export const updateAdminTrainingProgramStatus = (id: number, payload: { status: string; notes?: string }) => api.post<AdminAcademyMap>(`/admin/academy/programs/${id}/status`, payload);
export const getAdminTrainingBatches = () => api.get<AdminAcademyMap[]>("/admin/academy/batches");
export const getAdminAcademyEnrollments = () => api.get<AdminAcademyMap[]>("/admin/academy/enrollments");
export const getAdminAcademyAttendance = () => api.get<AdminAcademyMap[]>("/admin/academy/attendance");
export const getAdminAcademyAssessments = () => api.get<AdminAcademyMap[]>("/admin/academy/assessments");
export const getAdminAcademyVideos = () => api.get<AdminAcademyMap[]>("/admin/academy/videos");
export const getAdminTalentProfiles = () => api.get<AdminAcademyMap[]>("/admin/academy/talent");
export const getAdminAcademyCertificates = () => api.get<AdminAcademyMap[]>("/admin/academy/certificates");
export const getAdminAcademyAnalytics = () => api.get<AdminAcademyMap>("/admin/academy/analytics");
export const getAdminAcademyRevenue = () => api.get<AdminAcademyMap>("/admin/academy/revenue");
export const getAdminAcademyAudit = () => api.get<AdminAcademyMap[]>("/admin/academy/audit");

export type AdminCareerMap = Record<string, string | number | boolean | null | undefined | string[] | Record<string, unknown> | Array<Record<string, unknown>>>;

export type AdminCareerDashboard = {
  generatedAt?: string;
  kpis: Record<string, string | number>;
  profileLeaderboard: AdminCareerMap[];
  scouts: AdminCareerMap[];
  opportunities: AdminCareerMap[];
  applications: AdminCareerMap[];
  trials: AdminCareerMap[];
  scholarships: AdminCareerMap[];
  pendingVerification: AdminCareerMap[];
  recommendations: AdminCareerMap[];
  careerAnalytics: AdminCareerMap;
  pipeline: AdminCareerMap;
  ranking: AdminCareerMap[];
  governanceControls: AdminCareerMap[];
  priorityActions: AdminCareerMap[];
  audits: AdminCareerMap[];
};

export type AdminCareerOpportunityPayload = {
  opportunityType: string;
  title: string;
  organizationName: string;
  sport?: string;
  category?: string;
  location?: string;
  description?: string;
  eligibility?: string;
  capacity?: number;
  deadline?: string;
  startsAt?: string;
  scholarshipAmount?: number;
  status?: string;
};

export const getAdminCareerDashboard = () => api.get<AdminCareerDashboard>("/admin/career/dashboard");
export const getAdminCareerProfiles = () => api.get<AdminCareerMap[]>("/admin/career/profiles");
export const getAdminCareerScouts = () => api.get<AdminCareerMap[]>("/admin/career/scouts");
export const updateAdminCareerScoutStatus = (id: number, payload: { status: string; notes?: string }) => api.post<AdminCareerMap>(`/admin/career/scouts/${id}/status`, payload);
export const getAdminCareerOpportunities = (params?: { type?: string }) => api.get<AdminCareerMap[]>("/admin/career/opportunities", { params });
export const createAdminCareerOpportunity = (payload: AdminCareerOpportunityPayload) => api.post<AdminCareerMap>("/admin/career/opportunities", payload);
export const getAdminCareerApplications = () => api.get<AdminCareerMap[]>("/admin/career/applications");
export const updateAdminCareerApplicationStatus = (id: number, payload: { status: string; notes?: string; score?: number }) => api.post<AdminCareerMap>(`/admin/career/applications/${id}/status`, payload);
export const evaluateAdminCareerApplication = (id: number, payload: { evaluatorName?: string; techniqueScore?: number; fitnessScore?: number; disciplineScore?: number; awarenessScore?: number; decision?: string; notes?: string }) => api.post<AdminCareerMap>(`/admin/career/applications/${id}/evaluations`, payload);
export const getAdminCareerEvaluations = () => api.get<AdminCareerMap[]>("/admin/career/evaluations");
export const verifyAdminCareerAchievement = (id: number, payload: { status: string; notes?: string }) => api.post<AdminCareerMap>(`/admin/career/achievements/${id}/verify`, payload);
export const getAdminCareerAudit = () => api.get<AdminCareerMap[]>("/admin/career/audit");

export type AdminFederationGovernanceMap = Record<string, any>;

export type AdminFederationGovernanceDashboard = {
  generatedAt?: string;
  kpis: Record<string, string | number>;
  federations: AdminFederationGovernanceMap[];
  associations: AdminFederationGovernanceMap[];
  franchises: AdminFederationGovernanceMap[];
  playerRegistry: AdminFederationGovernanceMap[];
  drafts: AdminFederationGovernanceMap[];
  auctions: AdminFederationGovernanceMap[];
  transfers: AdminFederationGovernanceMap[];
  licenses: AdminFederationGovernanceMap[];
  officials: AdminFederationGovernanceMap[];
  disciplinaryCases: AdminFederationGovernanceMap[];
  elections: AdminFederationGovernanceMap[];
  rankings: AdminFederationGovernanceMap[];
  integrityAlerts: AdminFederationGovernanceMap[];
  analytics: AdminFederationGovernanceMap;
  compliance: AdminFederationGovernanceMap[];
  priorityActions: AdminFederationGovernanceMap[];
  crossEpicSignals: AdminFederationGovernanceMap;
  audits: AdminFederationGovernanceMap[];
};

export type GovernanceBodyPayload = {
  parentBodyId?: number;
  name: string;
  bodyType?: string;
  sport?: string;
  region?: string;
  registrationNumber?: string;
  website?: string;
  contactEmail?: string;
  governanceNotes?: string;
};

export type GovernanceFranchisePayload = {
  federationId: number;
  name: string;
  ownerName?: string;
  city?: string;
  homeVenue?: string;
  salaryCap?: number;
};

export type GovernancePlayerPayload = {
  federationId?: number;
  careerProfileId?: number;
  playerName: string;
  email: string;
  phone?: string;
  sport?: string;
  city?: string;
  ageGroup?: string;
  dateOfBirth?: string;
  identityDocumentUrl?: string;
};

export type GovernanceAuctionPayload = {
  federationId: number;
  playerRegistrationId: number;
  auctionName: string;
  basePrice?: number;
  startsAt?: string;
  endsAt?: string;
};

export type GovernanceElectionPayload = {
  federationId: number;
  title: string;
  positionName: string;
  candidateNames: string;
  votingOpensAt?: string;
  votingClosesAt?: string;
};

export const getFederationGovernanceFederations = (params?: { sport?: string; region?: string }) => api.get<AdminFederationGovernanceMap[]>("/governance/federations", { params });
export const getFederationGovernanceRankings = (params?: { sport?: string; type?: string }) => api.get<AdminFederationGovernanceMap[]>("/governance/rankings", { params });
export const getAdminFederationGovernanceDashboard = () => api.get<AdminFederationGovernanceDashboard>("/admin/governance/dashboard");
export const getAdminFederationGovernanceBodies = () => api.get<AdminFederationGovernanceMap[]>("/admin/governance/bodies");
export const createAdminFederationGovernanceBody = (payload: GovernanceBodyPayload) => api.post<AdminFederationGovernanceMap>("/admin/governance/bodies", payload);
export const updateAdminFederationGovernanceBodyStatus = (id: number, payload: { status: string; notes?: string }) => api.post<AdminFederationGovernanceMap>(`/admin/governance/bodies/${id}/status`, payload);
export const getAdminFederationGovernanceFranchises = () => api.get<AdminFederationGovernanceMap[]>("/admin/governance/franchises");
export const createAdminFederationGovernanceFranchise = (payload: GovernanceFranchisePayload) => api.post<AdminFederationGovernanceMap>("/admin/governance/franchises", payload);
export const getAdminFederationGovernancePlayers = () => api.get<AdminFederationGovernanceMap[]>("/admin/governance/players");
export const createAdminFederationGovernancePlayer = (payload: GovernancePlayerPayload) => api.post<AdminFederationGovernanceMap>("/admin/governance/players", payload);
export const updateAdminFederationGovernancePlayerVerification = (id: number, payload: { status: string; notes?: string }) => api.post<AdminFederationGovernanceMap>(`/admin/governance/players/${id}/verification`, payload);
export const getAdminFederationGovernanceDrafts = () => api.get<AdminFederationGovernanceMap[]>("/admin/governance/drafts");
export const createAdminFederationGovernanceDraft = (payload: AdminFederationGovernanceMap) => api.post<AdminFederationGovernanceMap>("/admin/governance/drafts", payload);
export const selectAdminFederationGovernanceDraftPlayer = (id: number, payload: { franchiseId: number; playerRegistrationId: number; selectionType?: string }) => api.post<AdminFederationGovernanceMap>(`/admin/governance/drafts/${id}/selections`, payload);
export const getAdminFederationGovernanceAuctions = () => api.get<AdminFederationGovernanceMap[]>("/admin/governance/auctions");
export const createAdminFederationGovernanceAuction = (payload: GovernanceAuctionPayload) => api.post<AdminFederationGovernanceMap>("/admin/governance/auctions", payload);
export const placeAdminFederationGovernanceBid = (id: number, payload: { franchiseId: number; bidAmount: number; bidderName?: string }) => api.post<AdminFederationGovernanceMap>(`/admin/governance/auctions/${id}/bids`, payload);
export const completeAdminFederationGovernanceAuction = (id: number, payload: { status: string; notes?: string }) => api.post<AdminFederationGovernanceMap>(`/admin/governance/auctions/${id}/complete`, payload);
export const getAdminFederationGovernanceTransfers = () => api.get<AdminFederationGovernanceMap[]>("/admin/governance/transfers");
export const createAdminFederationGovernanceTransfer = (payload: AdminFederationGovernanceMap) => api.post<AdminFederationGovernanceMap>("/admin/governance/transfers", payload);
export const updateAdminFederationGovernanceTransferStatus = (id: number, payload: { status: string; notes?: string }) => api.post<AdminFederationGovernanceMap>(`/admin/governance/transfers/${id}/status`, payload);
export const getAdminFederationGovernanceLicenses = () => api.get<AdminFederationGovernanceMap[]>("/admin/governance/licenses");
export const issueAdminFederationGovernanceLicense = (payload: AdminFederationGovernanceMap) => api.post<AdminFederationGovernanceMap>("/admin/governance/licenses", payload);
export const getAdminFederationGovernanceOfficials = () => api.get<AdminFederationGovernanceMap[]>("/admin/governance/officials");
export const createAdminFederationGovernanceOfficial = (payload: AdminFederationGovernanceMap) => api.post<AdminFederationGovernanceMap>("/admin/governance/officials", payload);
export const updateAdminFederationGovernanceOfficialStatus = (id: number, payload: { status: string; notes?: string }) => api.post<AdminFederationGovernanceMap>(`/admin/governance/officials/${id}/status`, payload);
export const getAdminFederationGovernanceCases = () => api.get<AdminFederationGovernanceMap[]>("/admin/governance/cases");
export const createAdminFederationGovernanceCase = (payload: AdminFederationGovernanceMap) => api.post<AdminFederationGovernanceMap>("/admin/governance/cases", payload);
export const resolveAdminFederationGovernanceCase = (id: number, payload: { status: string; penalty?: string; notes?: string }) => api.post<AdminFederationGovernanceMap>(`/admin/governance/cases/${id}/resolve`, payload);
export const getAdminFederationGovernanceElections = () => api.get<AdminFederationGovernanceMap[]>("/admin/governance/elections");
export const createAdminFederationGovernanceElection = (payload: GovernanceElectionPayload) => api.post<AdminFederationGovernanceMap>("/admin/governance/elections", payload);
export const updateAdminFederationGovernanceElectionStatus = (id: number, payload: { status: string; notes?: string }) => api.post<AdminFederationGovernanceMap>(`/admin/governance/elections/${id}/status`, payload);
export const publishAdminFederationGovernanceElectionResults = (id: number) => api.post<AdminFederationGovernanceMap>(`/admin/governance/elections/${id}/publish-results`);
export const getAdminFederationGovernanceRankings = () => api.get<AdminFederationGovernanceMap[]>("/admin/governance/rankings");
export const upsertAdminFederationGovernanceRanking = (payload: AdminFederationGovernanceMap) => api.post<AdminFederationGovernanceMap>("/admin/governance/rankings", payload);
export const getAdminFederationGovernanceIntegrityAlerts = () => api.get<AdminFederationGovernanceMap[]>("/admin/governance/integrity-alerts");
export const createAdminFederationGovernanceIntegrityAlert = (payload: AdminFederationGovernanceMap) => api.post<AdminFederationGovernanceMap>("/admin/governance/integrity-alerts", payload);
export const updateAdminFederationGovernanceIntegrityAlertStatus = (id: number, payload: { status: string; notes?: string }) => api.post<AdminFederationGovernanceMap>(`/admin/governance/integrity-alerts/${id}/status`, payload);
export const getAdminFederationGovernanceAudit = () => api.get<AdminFederationGovernanceMap[]>("/admin/governance/audit");

export type AdminCorporateMap = Record<string, any>;

export type AdminCorporateDashboard = {
  kpis: AdminCorporateMap;
  companies: AdminCorporateMap[];
  employees: AdminCorporateMap[];
  teams: AdminCorporateMap[];
  bookingPlans: AdminCorporateMap[];
  tournaments: AdminCorporateMap[];
  wellnessPrograms: AdminCorporateMap[];
  billing: AdminCorporateMap;
  analytics: AdminCorporateMap;
  crm: AdminCorporateMap;
  leaderboards: AdminCorporateMap;
  governanceControls: AdminCorporateMap[];
  priorityActions: AdminCorporateMap[];
  generatedAt?: string;
};

export type CorporateCompanyPayload = {
  name: string;
  gstNumber?: string;
  industry?: string;
  city?: string;
  locations?: string;
  hrContactName?: string;
  hrEmail?: string;
  hrPhone?: string;
  adminEmail?: string;
  logoUrl?: string;
  documentUrl?: string;
  annualContractValue?: number;
  monthlyBudget?: number;
};

export type CorporateEmployeePayload = {
  companyId: number;
  locationId?: number;
  departmentId?: number;
  name: string;
  email: string;
  phone?: string;
  employeeCode?: string;
  role?: string;
};

export type CorporateTeamPayload = {
  companyId: number;
  departmentId?: number;
  name: string;
  sport: string;
  captainEmployeeId?: number;
};

export const getCorporateMarketplace = (params?: { city?: string }) => api.get<AdminCorporateMap[]>("/corporates/marketplace", { params });
export const registerCorporateCompany = (payload: CorporateCompanyPayload) => api.post<AdminCorporateMap>("/corporates/register", payload);
export const getAdminCorporateDashboard = () => api.get<AdminCorporateDashboard>("/admin/corporate/dashboard");
export const getAdminCorporateCompanies = () => api.get<AdminCorporateMap[]>("/admin/corporate/companies");
export const createAdminCorporateCompany = (payload: CorporateCompanyPayload) => api.post<AdminCorporateMap>("/admin/corporate/companies", payload);
export const updateAdminCorporateCompanyStatus = (id: number, payload: { status: string; notes?: string }) => api.post<AdminCorporateMap>(`/admin/corporate/companies/${id}/status`, payload);
export const getAdminCorporateLocations = () => api.get<AdminCorporateMap[]>("/admin/corporate/locations");
export const createAdminCorporateLocation = (payload: AdminCorporateMap) => api.post<AdminCorporateMap>("/admin/corporate/locations", payload);
export const getAdminCorporateDepartments = () => api.get<AdminCorporateMap[]>("/admin/corporate/departments");
export const createAdminCorporateDepartment = (payload: AdminCorporateMap) => api.post<AdminCorporateMap>("/admin/corporate/departments", payload);
export const getAdminCorporateEmployees = () => api.get<AdminCorporateMap[]>("/admin/corporate/employees");
export const createAdminCorporateEmployee = (payload: CorporateEmployeePayload) => api.post<AdminCorporateMap>("/admin/corporate/employees", payload);
export const getAdminCorporateTeams = () => api.get<AdminCorporateMap[]>("/admin/corporate/teams");
export const createAdminCorporateTeam = (payload: CorporateTeamPayload) => api.post<AdminCorporateMap>("/admin/corporate/teams", payload);
export const getAdminCorporateTeamMemberships = () => api.get<AdminCorporateMap[]>("/admin/corporate/team-memberships");
export const createAdminCorporateTeamMembership = (payload: AdminCorporateMap) => api.post<AdminCorporateMap>("/admin/corporate/team-memberships", payload);
export const getAdminCorporateBookingPlans = () => api.get<AdminCorporateMap[]>("/admin/corporate/booking-plans");
export const createAdminCorporateBookingPlan = (payload: AdminCorporateMap) => api.post<AdminCorporateMap>("/admin/corporate/booking-plans", payload);
export const getAdminCorporateTournaments = () => api.get<AdminCorporateMap[]>("/admin/corporate/tournaments");
export const createAdminCorporateTournament = (payload: AdminCorporateMap) => api.post<AdminCorporateMap>("/admin/corporate/tournaments", payload);
export const getAdminCorporateWellnessPrograms = () => api.get<AdminCorporateMap[]>("/admin/corporate/wellness-programs");
export const createAdminCorporateWellnessProgram = (payload: AdminCorporateMap) => api.post<AdminCorporateMap>("/admin/corporate/wellness-programs", payload);
export const getAdminCorporateWellnessActivities = () => api.get<AdminCorporateMap[]>("/admin/corporate/wellness-activities");
export const createAdminCorporateWellnessActivity = (payload: AdminCorporateMap) => api.post<AdminCorporateMap>("/admin/corporate/wellness-activities", payload);
export const getAdminCorporateRewards = () => api.get<AdminCorporateMap[]>("/admin/corporate/rewards");
export const createAdminCorporateReward = (payload: AdminCorporateMap) => api.post<AdminCorporateMap>("/admin/corporate/rewards", payload);
export const redeemAdminCorporateReward = (payload: AdminCorporateMap) => api.post<AdminCorporateMap>("/admin/corporate/rewards/redeem", payload);
export const getAdminCorporateInvoices = () => api.get<AdminCorporateMap[]>("/admin/corporate/invoices");
export const createAdminCorporateInvoice = (payload: AdminCorporateMap) => api.post<AdminCorporateMap>("/admin/corporate/invoices", payload);
export const updateAdminCorporateInvoicePayment = (id: number, payload: { status: string }) => api.post<AdminCorporateMap>(`/admin/corporate/invoices/${id}/payment`, payload);
export const getAdminCorporateContracts = () => api.get<AdminCorporateMap[]>("/admin/corporate/contracts");
export const createAdminCorporateContract = (payload: AdminCorporateMap) => api.post<AdminCorporateMap>("/admin/corporate/contracts", payload);
export const renewAdminCorporateContract = (id: number, payload?: AdminCorporateMap) => api.post<AdminCorporateMap>(`/admin/corporate/contracts/${id}/renew`, payload || {});
export const getAdminCorporateLeagues = () => api.get<AdminCorporateMap[]>("/admin/corporate/leagues");
export const createAdminCorporateLeague = (payload: AdminCorporateMap) => api.post<AdminCorporateMap>("/admin/corporate/leagues", payload);
export const getAdminCorporateAnalytics = () => api.get<AdminCorporateMap>("/admin/corporate/analytics");
export const getAdminCorporateBilling = () => api.get<AdminCorporateMap>("/admin/corporate/billing");
export const getAdminCorporateAudit = () => api.get<AdminCorporateMap[]>("/admin/corporate/audit");

export type AdminMonetizationMap = Record<string, string | number | boolean | null | undefined>;

export type AdminMonetizationDashboard = {
  generatedAt?: string;
  kpis: Record<string, string | number>;
  revenueBreakdown: Record<string, number>;
  marketplace: AdminMonetizationMap[];
  brandPipeline: AdminMonetizationMap[];
  proposalBoard: AdminMonetizationMap[];
  campaignPerformance: AdminMonetizationMap[];
  leadFunnel: Record<string, number>;
  contracts: AdminMonetizationMap[];
  invoices: AdminMonetizationMap[];
  priorityActions: AdminMonetizationMap[];
};

export type AdminMonetizationReports = {
  revenue: Record<string, number>;
  adFunnel: Record<string, number>;
  leadFunnel: Record<string, number>;
  brandSafety: Record<string, number>;
};

export type AdminBrandPartnerPayload = {
  companyName: string;
  brandType?: string;
  gstNumber?: string;
  website?: string;
  email: string;
  phone?: string;
  city?: string;
  logoUrl?: string;
  documentUrl?: string;
};

export type AdminSponsorshipOpportunityPayload = {
  title: string;
  opportunityType?: string;
  description?: string;
  city?: string;
  sportCategory?: string;
  audienceSize?: number;
  amount?: number;
  benefits?: string;
  ownerReference?: string;
  expiresAt?: string;
};

export type AdminSponsoredCampaignPayload = {
  brandId: number;
  name: string;
  campaignType?: string;
  placement?: string;
  targetAudience?: string;
  city?: string;
  creativeUrl?: string;
  destinationUrl?: string;
  dailyBudget?: number;
  lifetimeBudget?: number;
  startsAt?: string;
  endsAt?: string;
};

export const getAdminMonetizationDashboard = () => api.get<AdminMonetizationDashboard>("/admin/monetization/dashboard");
export const getAdminMonetizationBrands = () => api.get<AdminMonetizationMap[]>("/admin/monetization/brands");
export const createAdminMonetizationBrand = (payload: AdminBrandPartnerPayload) => api.post<AdminMonetizationMap>("/admin/monetization/brands", payload);
export const getAdminMonetizationOpportunities = () => api.get<AdminMonetizationMap[]>("/admin/monetization/opportunities");
export const createAdminMonetizationOpportunity = (payload: AdminSponsorshipOpportunityPayload) => api.post<AdminMonetizationMap>("/admin/monetization/opportunities", payload);
export const getAdminMonetizationProposals = () => api.get<AdminMonetizationMap[]>("/admin/monetization/proposals");
export const updateAdminMonetizationProposalStatus = (id: number, payload: { status: string; notes?: string }) => api.post<AdminMonetizationMap>(`/admin/monetization/proposals/${id}/status`, payload);
export const getAdminMonetizationCampaigns = () => api.get<AdminMonetizationMap[]>("/admin/monetization/campaigns");
export const createAdminMonetizationCampaign = (payload: AdminSponsoredCampaignPayload) => api.post<AdminMonetizationMap>("/admin/monetization/campaigns", payload);
export const getAdminMonetizationLeads = () => api.get<AdminMonetizationMap[]>("/admin/monetization/leads");
export const getAdminMonetizationContracts = () => api.get<AdminMonetizationMap[]>("/admin/monetization/contracts");
export const getAdminMonetizationInvoices = () => api.get<AdminMonetizationMap[]>("/admin/monetization/invoices");
export const getAdminMonetizationReports = () => api.get<AdminMonetizationReports>("/admin/monetization/reports");

export type AdminAiMap = Record<string, string | number | boolean | null | undefined | string[] | number[] | Record<string, string | number | boolean | null | undefined> | Array<Record<string, unknown>>>;

export type AdminAiDashboard = {
  generatedAt?: string;
  modelVersion: string;
  kpis: Record<string, number | string>;
  assistant: AdminAiMap;
  recommendations: AdminAiMap;
  forecasts: AdminAiMap[];
  pricingAdvisor: { suggestions?: AdminAiMap[]; approvalMode?: string; rollbackReady?: boolean };
  reviewInsights: AdminAiMap;
  fraud: AdminAiMap;
  churn: AdminAiMap;
  operations: AdminAiMap;
  copilots: AdminAiMap[];
  governance: AdminAiGovernance;
  audit: AdminAiMap;
  priorityActions: AdminAiMap[];
};

export type AdminAiGovernance = {
  modelVersion: string;
  modelFamily: string;
  driftStatus: string;
  accuracyMonitoring: Record<string, number>;
  biasControls: string[];
  guardrails: Record<string, string>;
  humanReview: Record<string, boolean>;
  fallbacks: string[];
  observability: Record<string, number>;
  responsibleAiControls: string[];
};

export type AdminAiCopilotResponse = {
  modelVersion: string;
  query: string;
  intent: string;
  answer: string;
  confidence: number;
  sources: string[];
  recommendedActions: string[];
};

export const getAdminAiDashboard = () => api.get<AdminAiDashboard>("/admin/ai/dashboard");
export const getAdminAiGovernance = () => api.get<AdminAiGovernance>("/admin/ai/governance");
export const getAdminAiForecasts = () => api.get<AdminAiMap[]>("/admin/ai/forecasts");
export const getAdminAiReviewInsights = () => api.get<AdminAiMap>("/admin/ai/review-insights");
export const askAdminAiCopilot = (query: string) => api.post<AdminAiCopilotResponse>("/admin/ai/copilot/query", { query });

export type AdminSupportInsight = {
  title: string;
  message: string;
  severity: string;
};

export type AdminSupportDashboard = {
  openTickets: number;
  resolvedTickets: number;
  averageResponseHours: number;
  averageResolutionHours: number;
  slaBreaches: number;
  liveChats: number;
  escalations: number;
  csatScore: number;
  agentAvailability: number;
  ticketTrends: Array<{ date: string; tickets: number }>;
  aiInsights: AdminSupportInsight[];
};

export type AdminSupportTicket = {
  id: number;
  ticketNumber: string;
  userId: number;
  category: string;
  priority: string;
  status: string;
  assignedTo?: string;
  channel: string;
  issue: string;
  internalNotes?: string;
  chatLog?: string;
  slaBreached: boolean;
  createdAt?: string;
  updatedAt?: string;
};

export type AdminSupportChat = {
  id: number;
  ticketId: number;
  senderId?: number;
  senderType: string;
  message: string;
  createdAt?: string;
};

export type AdminSlaPolicy = {
  id?: number;
  priority: string;
  responseTimeHours: number;
  resolutionTimeHours: number;
};

export type AdminTicketEscalation = {
  id?: number;
  ticketId: number;
  level: string;
  assignedTo: string;
  status?: string;
  reason?: string;
  createdAt?: string;
};

export type AdminSupportAgent = {
  id?: number;
  userId?: number;
  name: string;
  role: string;
  status: string;
};

export type AdminKnowledgeArticle = {
  id?: number;
  title: string;
  content: string;
  category: string;
  status: string;
};

export type AdminFaq = {
  id?: number;
  question: string;
  answer: string;
  category: string;
  status: string;
};

export type AdminCsatFeedback = {
  id?: number;
  ticketId: number;
  rating: number;
  feedback?: string;
  createdAt?: string;
};

export type AdminTicketNote = {
  id?: number;
  ticketId: number;
  agentId?: string;
  note: string;
  createdAt?: string;
};

export type AdminSupportAutomation = {
  id?: number;
  ruleName: string;
  condition: string;
  action: string;
  active?: boolean;
};

export type AdminSupportChannelMessage = {
  id?: number;
  channel: string;
  externalId?: string;
  ticketId: number;
  message: string;
  createdAt?: string;
};

export const getAdminSupportDashboard = () => api.get<AdminSupportDashboard>("/admin/support/dashboard");
export const getAdminSupportTickets = () => api.get<AdminSupportTicket[]>("/admin/support/tickets");
export const createAdminSupportTicket = (payload: { userId?: number; category: string; priority: string; assignedTo?: string; channel: string; issue: string }) => api.post<AdminSupportTicket>("/admin/support/tickets", payload);
export const assignAdminSupportTicket = (id: number, assignedTo: string) => api.post<AdminSupportTicket>(`/admin/support/tickets/${id}/assign`, { assignedTo });
export const replyAdminSupportTicket = (id: number, payload: { reply: string; status: string; internalNotes?: string }) => api.post<AdminSupportTicket>(`/admin/support/tickets/${id}/reply`, payload);
export const getAdminSupportChat = () => api.get<AdminSupportChat[]>("/admin/support/chat");
export const sendAdminSupportChat = (payload: { ticketId: number; senderId?: number; senderType: string; message: string }) => api.post<AdminSupportChat>("/admin/support/chat", payload);
export const getAdminSlaPolicies = () => api.get<AdminSlaPolicy[]>("/admin/support/sla");
export const saveAdminSlaPolicy = (payload: AdminSlaPolicy) => api.post<AdminSlaPolicy>("/admin/support/sla", payload);
export const getAdminTicketEscalations = () => api.get<AdminTicketEscalation[]>("/admin/support/escalations");
export const createAdminTicketEscalation = (payload: AdminTicketEscalation) => api.post<AdminTicketEscalation>("/admin/support/escalations", payload);
export const getAdminSupportAgents = () => api.get<AdminSupportAgent[]>("/admin/support/agents");
export const saveAdminSupportAgent = (payload: AdminSupportAgent) => api.post<AdminSupportAgent>("/admin/support/agents", payload);
export const getAdminKnowledgeBase = () => api.get<AdminKnowledgeArticle[]>("/admin/support/knowledge-base");
export const saveAdminKnowledgeArticle = (payload: AdminKnowledgeArticle) => api.post<AdminKnowledgeArticle>("/admin/support/knowledge-base", payload);
export const getAdminFaqs = () => api.get<AdminFaq[]>("/admin/support/faqs");
export const saveAdminFaq = (payload: AdminFaq) => api.post<AdminFaq>("/admin/support/faqs", payload);
export const getAdminCsat = () => api.get<AdminCsatFeedback[]>("/admin/support/csat");
export const saveAdminCsat = (payload: AdminCsatFeedback) => api.post<AdminCsatFeedback>("/admin/support/csat", payload);
export const getAdminTicketNotes = () => api.get<AdminTicketNote[]>("/admin/support/notes");
export const saveAdminTicketNote = (payload: AdminTicketNote) => api.post<AdminTicketNote>("/admin/support/notes", payload);
export const getAdminSupportAutomations = () => api.get<AdminSupportAutomation[]>("/admin/support/automations");
export const saveAdminSupportAutomation = (payload: AdminSupportAutomation) => api.post<AdminSupportAutomation>("/admin/support/automations", payload);
export const getAdminSupportChannels = () => api.get<AdminSupportChannelMessage[]>("/admin/support/channels");
export const saveAdminSupportChannel = (payload: AdminSupportChannelMessage) => api.post<AdminSupportChannelMessage>("/admin/support/channels", payload);

export type AdminSystemDashboard = {
  activeFeatures: number;
  totalFeatures: number;
  maintenanceStatus: boolean;
  currentVersion: string;
  recentConfigChanges: AdminConfigurationAudit[];
  pendingReleases: number;
};

export type AdminPlatformSetting = {
  id?: number;
  key: string;
  value: string;
};

export type AdminFeatureFlag = {
  id?: number;
  featureKey: string;
  enabled: boolean;
  rolloutPercentage: number;
  targetAudience: string;
  description?: string;
  updatedAt?: string;
};

export type AdminMaintenanceConfig = {
  id?: number;
  enabled: boolean;
  message: string;
  startTime?: string;
  endTime?: string;
  allowAdminAccess: boolean;
};

export type AdminReleaseVersion = {
  id?: number;
  version: string;
  releaseDate?: string;
  notes: string;
  status: string;
  rolloutPercentage: number;
};

export type AdminConfigurationAudit = {
  id: number;
  configKey: string;
  oldValue?: string;
  newValue: string;
  changedBy: string;
  changedAt?: string;
};

export const getAdminSystemDashboard = () => api.get<AdminSystemDashboard>("/admin/system/dashboard");
export const getAdminSystemSettings = () => api.get<AdminPlatformSetting[]>("/admin/system/settings");
export const saveAdminSystemSetting = (payload: { key: string; value: string }) => api.post<AdminPlatformSetting>("/admin/system/settings", payload);
export const getAdminSystemSettingsByDomain = (domain: string) => api.get<AdminPlatformSetting[]>(`/admin/system/settings/${domain}`);
export const getAdminFeatureFlags = () => api.get<AdminFeatureFlag[]>("/admin/system/feature-flags");
export const saveAdminFeatureFlag = (payload: AdminFeatureFlag) => api.post<AdminFeatureFlag>("/admin/system/feature-flags", payload);
export const getAdminMaintenanceConfig = () => api.get<AdminMaintenanceConfig>("/admin/system/maintenance");
export const saveAdminMaintenanceConfig = (payload: AdminMaintenanceConfig) => api.post<AdminMaintenanceConfig>("/admin/system/maintenance", payload);
export const getAdminReleaseVersions = () => api.get<AdminReleaseVersion[]>("/admin/system/releases");
export const saveAdminReleaseVersion = (payload: AdminReleaseVersion) => api.post<AdminReleaseVersion>("/admin/system/releases", payload);
export const getAdminConfigurationAudit = () => api.get<AdminConfigurationAudit[]>("/admin/system/audit");
export const seedAdminSystemDefaults = () => api.post<{ settings: number; featureFlags: number }>("/admin/system/seed-defaults");

export type PlatformEpicSurface = {
  surface: string;
  status: string;
  owner: string;
  capabilities: string[];
};

export type PlatformEpicChecklist = {
  key: string;
  label: string;
  status: string;
};

export type PlatformEpic = {
  epicId: string;
  name: string;
  objective: string;
  releaseStage: string;
  defaultTheme: string;
  sharedDomains: string[];
  surfaces: PlatformEpicSurface[];
  readiness: PlatformEpicChecklist[];
  notificationEvents: string[];
  analyticsEvents: string[];
};

export const getPlatformEpics = () => api.get<PlatformEpic[]>("/platform/epics");

export const getAdminOwners = () => api.get("/admin/owners");

export const updateAdminUserStatus = (userId: number, status: "ACTIVE" | "BLOCKED" | "UNVERIFIED") =>
  api.patch(`/admin/users/${userId}/status`, { status });

export const getAdminTurfs = () => api.get("/admin/turfs");

export const updateAdminTurfStatus = (turfId: number, status: "ACTIVE" | "INACTIVE" | "MAINTENANCE") =>
  api.patch(`/admin/turfs/${turfId}/status`, { status });

export const getAdminReviews = () => api.get("/admin/reviews");

export const deleteAdminReview = (reviewId: number) => api.delete(`/admin/reviews/${reviewId}`);

export const getCampaigns = () => api.get("/campaigns");

export const createCampaign = (payload: {
  name: string;
  title: string;
  message: string;
  channel: "EMAIL" | "PUSH" | "SMS" | "WHATSAPP";
  targetRole?: string;
  deepLink?: string;
}) => api.post("/campaigns", payload);

export const sendCampaign = (campaignId: number) => api.post(`/campaigns/${campaignId}/send`);

// --- Epic 17 Platform Operations Endpoints ---

export const adminLogin = (payload: { email: string; password: string }) => api.post("/admin/auth/login", payload);

export const adminVerifyOtp = (payload: { email: string; otp: string }) => api.post("/admin/auth/verify-otp", payload);

export const getAdminOpsUsers = () => api.get("/admin/ops/users");

export type AdminUserCenterSummary = {
  totalPlayers: number;
  totalOwners: number;
  totalTeams: number;
  pendingVerifications: number;
  suspendedAccounts: number;
  highRiskUsers: number;
  newRegistrations: number;
};

export type AdminManagedUser = {
  id: number;
  name: string;
  email: string;
  phone: string;
  status: string;
  roles: string[];
  city?: string;
  trustScore: number;
  riskLevel: "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";
  reports: number;
  bookings: number;
  teams: number;
  createdAt?: string;
};

export type AdminManagedTeam = {
  id: number;
  name: string;
  sport: string;
  city: string;
  captainId: number;
  captainName: string;
  members: number;
  visibility: string;
  createdAt?: string;
};

export type AdminUserActivity = {
  id: number;
  userId: number;
  activityType: string;
  entityId?: number;
  summary: string;
  createdAt?: string;
};

export const getAdminUserCenterSummary = () => api.get<AdminUserCenterSummary>("/admin/ops/user-center/summary");

export const searchAdminManagedUsers = (params?: { query?: string; role?: string; status?: string; risk?: string }) =>
  api.get<AdminManagedUser[]>("/admin/ops/user-center/users", { params });

export const getAdminManagedUserProfile = (userId: number) => api.get(`/admin/ops/user-center/users/${userId}`);

export const getAdminOwnerProfile = (ownerId: number) => api.get(`/admin/ops/user-center/owners/${ownerId}`);

export const getAdminManagedTeams = (query?: string) => api.get<AdminManagedTeam[]>("/admin/ops/user-center/teams", { params: { query } });

export const getAdminUserActivity = () => api.get<AdminUserActivity[]>("/admin/ops/user-center/activity");

export const resetAdminUserTrustScore = (userId: number) => api.post(`/admin/ops/user-center/users/${userId}/reset-trust`);

export const sendAdminUserCommunication = (userId: number, payload: { channel: string; message: string }) =>
  api.post(`/admin/ops/user-center/users/${userId}/communicate`, payload);

export const warnUser = (userId: number, reason: string) =>
  api.post(`/admin/ops/users/${userId}/warn`, { reason });

export const banUser = (userId: number, reason: string) =>
  api.post(`/admin/ops/users/${userId}/ban`, { reason });

export const unbanUser = (userId: number) =>
  api.post(`/admin/ops/users/${userId}/unban`);

export const getOwnerVerifications = () => api.get("/admin/ops/owner-verifications");

export const approveOwner = (id: number) => api.post(`/admin/ops/owner-verifications/${id}/approve`);

export const rejectOwner = (id: number) => api.post(`/admin/ops/owner-verifications/${id}/reject`);

export const getTurfModerations = () => api.get("/admin/ops/turf-moderations");

export const approveTurf = (id: number) => api.post(`/admin/ops/turf-moderations/${id}/approve`);

export const rejectTurf = (id: number, reason: string) => api.post(`/admin/ops/turf-moderations/${id}/reject`, { reason });

export const getOpsBookings = () => api.get("/admin/ops/bookings");

export const overrideBookingStatus = (bookingId: number, status: string) =>
  api.post(`/admin/ops/bookings/${bookingId}/override`, { status });

export const triggerManualRefund = (bookingId: number) =>
  api.post(`/admin/ops/bookings/${bookingId}/refund`);

export const getOpsFinance = () => api.get("/admin/ops/finance");

export const getOpsSettings = () => api.get("/admin/ops/settings");

export const saveOpsSetting = (key: string, value: string) =>
  api.post("/admin/ops/settings", { key, value });

export const getFraudAlerts = () => api.get("/admin/ops/fraud-alerts");

export const dismissFraudAlert = (id: number) => api.post(`/admin/ops/fraud-alerts/${id}/dismiss`);

export const getOpsCmsContent = () => api.get("/admin/ops/cms");

export const addOpsCmsContent = (payload: { type: string; title: string; imageUrl?: string; status?: string }) =>
  api.post("/admin/ops/cms", payload);

export const deleteOpsCmsContent = (id: number) => api.delete(`/admin/ops/cms/${id}`);

export const getOpsAuditLogs = () => api.get("/admin/ops/audit-logs");

export type AdminGovernanceControl = {
  name: string;
  status: "PASSING" | "ACTION_REQUIRED";
  detail: string;
};

export type AdminGovernancePriorityAction = {
  label: string;
  count: number;
  owner: string;
  recommendation: string;
};

export type AdminGovernanceDashboard = {
  generatedAt?: string;
  governanceScore: number;
  riskLevel: "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";
  kpis: {
    users: number;
    adminUsers: number;
    activeAdmins: number;
    roles: number;
    permissions: number;
    bookings: number;
    payments: number;
    refunds: number;
    auditEvents: number;
  };
  queues: {
    ownerVerifications: number;
    turfModerations: number;
    refunds: number;
    supportTickets: number;
    incidents: number;
    fraudAlerts: number;
    highFraudAlerts: number;
    highRiskUsers: number;
  };
  controlHealth: AdminGovernanceControl[];
  priorityActions: AdminGovernancePriorityAction[];
  recentAudits: AdminAuditLog[];
  rbacSummary: {
    roles: number;
    permissions: number;
    inactiveRoles: number;
    rolesWithoutPermissions: number;
    catalogPages: number;
  };
};

export type AdminAuditLog = {
  id: number;
  adminId: string;
  action: string;
  entity: string;
  timestamp?: string;
};

export type AdminAuditExport = {
  fileName: string;
  format: string;
  generatedAt?: string;
  totalRows: number;
  rows: AdminAuditLog[];
};

export type AdminPermission = {
  id: number;
  code: string;
  name: string;
  module: string;
  description: string;
};

export type AdminRole = {
  id: number;
  name: string;
  description: string;
  systemRole?: boolean;
  active?: boolean;
  createdAt?: string;
  permissions: AdminPermission[];
};

export type AdminOpsUser = {
  id: number;
  email: string;
  role: string;
  status: string;
  createdAt?: string;
};

export type AdminPermissionGroup = {
  id: number;
  name: string;
  description: string;
  permissions: AdminPermission[];
};

export type AdminAccessPolicy = {
  id: number;
  roleId: number;
  roleName: string;
  policyType: string;
  policyValue: string;
  createdAt?: string;
};

export type AdminResourcePermission = {
  id: number;
  userId: number;
  resourceType: string;
  resourceId: number;
  permission: string;
  createdAt?: string;
};

export type AdminTemporaryPermission = {
  id: number;
  userId: number;
  permissionCode: string;
  permissionName: string;
  expiresAt: string;
  createdAt?: string;
};

export type AdminOwnerStaffRole = {
  id: number;
  ownerId: number;
  staffId: number;
  roleId: number;
  roleName: string;
  active: boolean;
  createdAt?: string;
};

export type PermissionAudit = {
  id: number;
  actor: string;
  action: string;
  entity: string;
  details: string;
  timestamp?: string;
};

export type AdminAccessContext = {
  id: number;
  email: string;
  role: string;
  permissions: string[];
  visibleMenus: string[];
};

export type AdminPagePermissionCatalogItem = {
  portal: "PLAYER" | "OWNER" | "ADMIN";
  route: string;
  label: string;
  description: string;
  key: string;
  permissions: {
    read: string;
    write: string;
    full: string;
  };
};

export type AdminRbacMatrixRole = {
  roleId: number;
  roleName: string;
  description: string;
  active?: boolean;
  systemRole?: boolean;
  permissionCount: number;
  permissions: string[];
  pageAccess: Array<{
    route: string;
    label: string;
    portal: string;
    level: "NONE" | "READ" | "WRITE" | "FULL";
  }>;
};

export type AdminRbacMatrix = {
  generatedAt?: string;
  roles: AdminRbacMatrixRole[];
  permissionsByModule: Record<string, number>;
  catalog: AdminPagePermissionCatalogItem[];
  summary: {
    roleCount: number;
    permissionCount: number;
    pageCount: number;
    rolesWithoutPermissions: number;
    inactiveRoles: number;
  };
};

export const getAdminGovernanceDashboard = () => api.get<AdminGovernanceDashboard>("/admin/ops/governance/dashboard");

export const searchOpsAuditLogs = (params?: { adminId?: string; action?: string; entity?: string; limit?: number }) =>
  api.get<AdminAuditLog[]>("/admin/ops/audit-logs/search", { params });

export const exportOpsAuditLogs = (params?: { adminId?: string; action?: string; entity?: string }) =>
  api.get<AdminAuditExport>("/admin/ops/audit-logs/export", { params });

export const getAdminPermissions = () => api.get<AdminPermission[]>("/admin/ops/rbac/permissions");

export const getAdminPagePermissions = () => api.get<AdminPagePermissionCatalogItem[]>("/admin/ops/rbac/page-permissions");

export const getAdminRoles = () => api.get<AdminRole[]>("/admin/ops/rbac/roles");

export const createAdminRole = (payload: { name: string; description: string; permissionCodes: string[] }) =>
  api.post<AdminRole>("/admin/ops/rbac/roles", payload);

export const getAdminRbacUsers = () => api.get<AdminOpsUser[]>("/admin/ops/rbac/admin-users");

export const assignAdminRole = (adminId: number, role: string) =>
  api.patch<AdminOpsUser>(`/admin/ops/rbac/admin-users/${adminId}/role`, { role });

export const updateAdminOpsUserStatus = (adminId: number, status: string, reason: string) =>
  api.patch<AdminOpsUser>(`/admin/ops/rbac/admin-users/${adminId}/status`, { status, reason });

export const getAdminAccessContext = () => api.get<AdminAccessContext>("/admin/ops/rbac/me");

export const getAdminRbacMatrix = () => api.get<AdminRbacMatrix>("/admin/ops/rbac/matrix");

export const getPermissionGroups = () => api.get<AdminPermissionGroup[]>("/admin/ops/rbac/groups");

export const savePermissionGroup = (payload: { name: string; description: string; permissionCodes: string[] }) =>
  api.post<AdminPermissionGroup>("/admin/ops/rbac/groups", payload);

export const getAccessPolicies = () => api.get<AdminAccessPolicy[]>("/admin/ops/rbac/policies");

export const saveAccessPolicy = (payload: { roleId: number; policyType: string; policyValue: string }) =>
  api.post<AdminAccessPolicy>("/admin/ops/rbac/policies", payload);

export const getResourcePermissions = () => api.get<AdminResourcePermission[]>("/admin/ops/rbac/resource-permissions");

export const grantResourcePermission = (payload: { userId: number; resourceType: string; resourceId: number; permission: string }) =>
  api.post<AdminResourcePermission>("/admin/ops/rbac/resource-permissions", payload);

export const getTemporaryPermissions = () => api.get<AdminTemporaryPermission[]>("/admin/ops/rbac/temporary-permissions");

export const grantTemporaryPermission = (payload: { userId: number; permissionCode: string; expiresAt: string }) =>
  api.post<AdminTemporaryPermission>("/admin/ops/rbac/temporary-permissions", payload);

export const getOwnerStaffRoles = () => api.get<AdminOwnerStaffRole[]>("/admin/ops/rbac/owner-staff-roles");

export const assignOwnerStaffRole = (payload: { ownerId: number; staffId: number; roleId: number }) =>
  api.post<AdminOwnerStaffRole>("/admin/ops/rbac/owner-staff-roles", payload);

export const getPermissionAudit = () => api.get<PermissionAudit[]>("/admin/ops/rbac/audit");

export type AdminFinanceDashboard = {
  gmv: number;
  netRevenue: number;
  commissionRevenue: number;
  refundAmount: number;
  pendingSettlements: number;
  transactions: number;
  refunds: number;
  openDisputes: number;
  fraudAlerts: number;
  dailyRevenue: { date: string; revenue: number }[];
  insight: string;
};

export type AdminFinanceTransaction = {
  id: number;
  transactionId?: string;
  orderId?: string;
  bookingId?: number;
  player: string;
  owner: string;
  amount: number;
  gateway: string;
  status: string;
  createdAt?: string;
};

export type AdminFinanceRefund = {
  id: number;
  paymentId: number;
  bookingId: number;
  amount: number;
  status: string;
  reason: string;
  gatewayRefundId?: string;
  createdAt?: string;
};

export type AdminOwnerSettlement = {
  id: number;
  ownerId: number;
  ownerName: string;
  bookingId?: number;
  grossAmount: number;
  commission: number;
  netAmount: number;
  status: string;
  reference?: string;
  createdAt?: string;
};

export type AdminCommissionRule = {
  id: number;
  type: string;
  value: number;
  ownerId?: number;
  active: boolean;
  description?: string;
  createdAt?: string;
};

export type AdminWalletSummary = {
  id: number;
  userId: number;
  userName: string;
  balance: number;
  transactions: Array<{ id: number; amount: number; type: string; status: string; description?: string; createdAt?: string }>;
  updatedAt?: string;
};

export type AdminFinanceDispute = {
  id: number;
  userId: number;
  bookingId?: number;
  type: string;
  status: string;
  amount: number;
  summary?: string;
  resolution?: string;
  createdAt?: string;
};

export type AdminReconciliationLog = {
  id: number;
  transactionId: string;
  gatewayAmount: number;
  systemAmount: number;
  status: string;
  notes?: string;
  createdAt?: string;
};

export type AdminTaxReport = {
  id: number;
  reportType: string;
  period: string;
  taxableAmount: number;
  gstAmount: number;
  generatedAt?: string;
};

export type AdminFinanceAudit = {
  id: number;
  action: string;
  actor: string;
  userId?: number;
  entity: string;
  details: string;
  timestamp?: string;
};

export type AdminFinanceFraudAlert = {
  id: number;
  userId: number;
  severity: string;
  reason: string;
  createdAt?: string;
};

export const getAdminFinanceDashboard = () => api.get<AdminFinanceDashboard>("/admin/finance/dashboard");
export const getAdminFinanceTransactions = (status?: string) => api.get<AdminFinanceTransaction[]>("/admin/finance/transactions", { params: { status } });
export const getAdminFinanceRefunds = () => api.get<AdminFinanceRefund[]>("/admin/finance/refunds");
export const updateAdminRefundStatus = (refundId: number, status: string) => api.post<AdminFinanceRefund>(`/admin/finance/refunds/${refundId}/status`, { status });
export const forceAdminRefund = (payload: { paymentId: number; amount: number; reason: string }) => api.post<AdminFinanceRefund>("/admin/finance/refunds/force", payload);
export const getAdminOwnerSettlements = () => api.get<AdminOwnerSettlement[]>("/admin/finance/settlements");
export const generateAdminSettlements = () => api.post<{ created: number; commissionRate: number; settlements: AdminOwnerSettlement[] }>("/admin/finance/settlements/generate");
export const updateAdminSettlementStatus = (settlementId: number, status: string) => api.post<AdminOwnerSettlement>(`/admin/finance/settlements/${settlementId}/status`, { status });
export const getAdminCommissionRules = () => api.get<AdminCommissionRule[]>("/admin/finance/commission-rules");
export const saveAdminCommissionRule = (payload: { type: string; value: number; ownerId?: number; description: string }) => api.post<AdminCommissionRule>("/admin/finance/commission-rules", payload);
export const getAdminWallets = () => api.get<AdminWalletSummary[]>("/admin/finance/wallets");
export const adjustAdminWallet = (payload: { userId: number; amount: number; type: string; reason: string }) => api.post<AdminWalletSummary>("/admin/finance/wallets/adjust", payload);
export const getAdminFinanceDisputes = () => api.get<AdminFinanceDispute[]>("/admin/finance/disputes");
export const createAdminFinanceDispute = (payload: { userId: number; bookingId?: number; type: string; amount: number; summary: string }) => api.post<AdminFinanceDispute>("/admin/finance/disputes", payload);
export const resolveAdminFinanceDispute = (disputeId: number, resolution: string) => api.post<AdminFinanceDispute>(`/admin/finance/disputes/${disputeId}/resolve`, { resolution });
export const getAdminReconciliationLogs = () => api.get<AdminReconciliationLog[]>("/admin/finance/reconciliation");
export const runAdminReconciliation = () => api.post<{ checked: number; mismatches: number }>("/admin/finance/reconciliation/run");
export const getAdminTaxReports = () => api.get<AdminTaxReport[]>("/admin/finance/tax-reports");
export const generateAdminTaxReport = (payload: { reportType: string; period: string }) => api.post<AdminTaxReport>("/admin/finance/tax-reports/generate", payload);
export const getAdminFinanceAudits = () => api.get<AdminFinanceAudit[]>("/admin/finance/audits");
export const getAdminFinanceFraudAlerts = () => api.get<AdminFinanceFraudAlert[]>("/admin/finance/fraud");

export type AdminBookingOpsDashboard = {
  todaysBookings: number;
  activeMatches: number;
  upcomingSlots: number;
  delayedCheckIns: number;
  pendingCancellations: number;
  activeDisputes: number;
  noShowCases: number;
  criticalEscalations: number;
  cancellationRate: number;
  disputeRate: number;
  noShowRate: number;
  liveFeed: AdminBookingTimeline[];
  insight: string;
};

export type AdminBookingRecord = {
  id: number;
  playerId: number;
  player: string;
  playerEmail: string;
  ownerId: number;
  owner: string;
  turfId: number;
  turf: string;
  slotId: number;
  slot: string;
  bookingDate: string;
  paymentStatus: string;
  amount: number;
  status: string;
  createdAt?: string;
};

export type AdminBookingCancellation = {
  id: number;
  bookingId: number;
  player: string;
  turf: string;
  requestedBy: string;
  cancellationType: string;
  reason: string;
  status: string;
  decisionNotes?: string;
  createdAt?: string;
};

export type AdminBookingDispute = {
  id: number;
  bookingId: number;
  disputeType: string;
  status: string;
  summary?: string;
  resolution?: string;
  assignedTo?: string;
  createdAt?: string;
};

export type AdminSlotConflict = {
  id: number;
  slotId: number;
  bookingA: number;
  bookingB?: number;
  status: string;
  notes?: string;
  createdAt?: string;
};

export type AdminNoShowCase = {
  id: number;
  bookingId: number;
  userId: number;
  userName: string;
  noShowType: string;
  status: string;
  penalty: number;
  notes?: string;
  createdAt?: string;
};

export type AdminWeatherBookingCase = {
  id: number;
  bookingId: number;
  refundType: string;
  amount: number;
  status: string;
  reason?: string;
  createdAt?: string;
};

export type AdminBookingTimeline = {
  id: number;
  bookingId: number;
  eventType: string;
  performedBy: string;
  details: string;
  createdAt?: string;
};

export type AdminBookingEscalation = {
  id: number;
  bookingId: number;
  ticketId?: number;
  priority: string;
  assignedTo: string;
  status: string;
  summary: string;
  createdAt?: string;
};

export const getAdminBookingOpsDashboard = () => api.get<AdminBookingOpsDashboard>("/admin/bookings/dashboard");
export const getAdminBookingRecords = (params?: { status?: string; query?: string }) => api.get<AdminBookingRecord[]>("/admin/bookings", { params });
export const getAdminBookingDetails = (bookingId: number) => api.get(`/admin/bookings/${bookingId}`);
export const updateAdminBookingStatus = (bookingId: number, payload: { status: string; reason: string }) => api.post<AdminBookingRecord>(`/admin/bookings/${bookingId}/status`, payload);
export const modifyAdminBooking = (bookingId: number, payload: { bookingDate?: string; slotId?: number; reason: string }) => api.post<AdminBookingRecord>(`/admin/bookings/${bookingId}/modify`, payload);
export const createAdminCancellation = (payload: { bookingId: number; requestedBy: string; cancellationType: string; reason: string }) => api.post<AdminBookingCancellation>("/admin/bookings/cancellations", payload);
export const getAdminCancellations = () => api.get<AdminBookingCancellation[]>("/admin/bookings/cancellations");
export const decideAdminCancellation = (id: number, payload: { decision: string; reason: string }) => api.post<AdminBookingCancellation>(`/admin/bookings/cancellations/${id}/decision`, payload);
export const getAdminBookingDisputes = () => api.get<AdminBookingDispute[]>("/admin/bookings/disputes");
export const createAdminBookingDispute = (payload: { bookingId: number; disputeType: string; summary: string; assignedTo: string }) => api.post<AdminBookingDispute>("/admin/bookings/disputes", payload);
export const resolveAdminBookingDispute = (id: number, reason: string) => api.post<AdminBookingDispute>(`/admin/bookings/disputes/${id}/resolve`, { reason });
export const getAdminSlotConflicts = () => api.get<AdminSlotConflict[]>("/admin/bookings/conflicts");
export const scanAdminSlotConflicts = () => api.post<{ created: number; conflicts: AdminSlotConflict[] }>("/admin/bookings/conflicts/scan");
export const resolveAdminSlotConflict = (id: number, reason: string) => api.post<AdminSlotConflict>(`/admin/bookings/conflicts/${id}/resolve`, { reason });
export const getAdminNoShowCases = () => api.get<AdminNoShowCase[]>("/admin/bookings/no-shows");
export const createAdminNoShowCase = (payload: { bookingId: number; userId?: number; noShowType: string; penalty: number; notes: string }) => api.post<AdminNoShowCase>("/admin/bookings/no-shows", payload);
export const resolveAdminNoShowCase = (id: number, reason: string) => api.post<AdminNoShowCase>(`/admin/bookings/no-shows/${id}/resolve`, { reason });
export const getAdminWeatherBookingCases = () => api.get<AdminWeatherBookingCase[]>("/admin/bookings/weather-cases");
export const decideAdminWeatherBookingCase = (id: number, payload: { decision: string; reason: string }) => api.post<AdminWeatherBookingCase>(`/admin/bookings/weather-cases/${id}/decision`, payload);
export const getAdminBookingTimeline = () => api.get<AdminBookingTimeline[]>("/admin/bookings/timeline");
export const getAdminBookingEscalations = () => api.get<AdminBookingEscalation[]>("/admin/bookings/escalations");
export const createAdminBookingEscalation = (payload: { bookingId: number; ticketId?: number; priority: string; assignedTo: string; summary: string }) => api.post<AdminBookingEscalation>("/admin/bookings/escalations", payload);
export const resolveAdminBookingEscalation = (id: number, reason: string) => api.post<AdminBookingEscalation>(`/admin/bookings/escalations/${id}/resolve`, { reason });

export type AdminSafetyDashboard = {
  openReports: number;
  pendingAppeals: number;
  suspendedUsers: number;
  flaggedPhotos: number;
  fraudCases: number;
  trustViolations: number;
  warnings: number;
  bans: number;
  safetyHealthScore: number;
  reportsPerDay: Array<{ date: string; reports: number }>;
  insight: string;
};

export type AdminSafetyReport = {
  id: number;
  reporter: string;
  reportedUserId?: number;
  reportedUser: string;
  reason: string;
  description?: string;
  status: string;
  createdAt?: string;
};

export type AdminSafetyWarning = {
  id: number;
  userId: number;
  userName: string;
  warningLevel: string;
  reason: string;
  issuedBy: string;
  createdAt?: string;
};

export type AdminSafetySuspension = {
  id: number;
  entityType: string;
  entityId: number;
  status: string;
  reason: string;
  startDate?: string;
  endDate?: string;
};

export type AdminSafetyBan = {
  id: number;
  type: string;
  value: string;
  reason: string;
  active: boolean;
  createdAt?: string;
};

export type AdminSafetyAppeal = {
  id: number;
  userId: number;
  userName: string;
  caseId: number;
  caseType: string;
  message: string;
  status: string;
  decisionReason?: string;
  createdAt?: string;
};

export type AdminSafetyMedia = {
  id: number;
  ownerType: string;
  ownerId: number;
  mediaType: string;
  imageUrl: string;
  moderationStatus: string;
  moderationReason?: string;
  active: boolean;
  uploadedAt?: string;
};

export type AdminSafetyFraudProfile = {
  id: number;
  userId: number;
  userName: string;
  riskLevel: string;
  reason: string;
  signals?: string;
  status: string;
  createdAt?: string;
};

export type AdminSafetyTeam = {
  id: number;
  name: string;
  sport: string;
  city: string;
  visibility: string;
  captain: string;
  createdAt?: string;
};

export type AdminSafetyReview = {
  id: number;
  userId: number;
  turfId: number;
  rating: number;
  comment?: string;
  reports: number;
  createdAt?: string;
};

export type AdminTrustAdjustment = {
  id: number;
  userId: number;
  userName: string;
  oldScore: number;
  newScore: number;
  reason: string;
  adminId: string;
  createdAt?: string;
};

export type AdminCommunityModeration = {
  id: number;
  contentType: string;
  contentId: number;
  action: string;
  reason: string;
  moderatorId: string;
  createdAt?: string;
};

export const getAdminSafetyDashboard = () => api.get<AdminSafetyDashboard>("/admin/safety/dashboard");
export const getAdminSafetyReports = () => api.get<AdminSafetyReport[]>("/admin/safety/reports");
export const resolveAdminSafetyReport = (id: number, payload: { status: string; trustScore?: number; reason: string }) => api.post<AdminSafetyReport>(`/admin/safety/reports/${id}/resolve`, payload);
export const getAdminSafetyWarnings = () => api.get<AdminSafetyWarning[]>("/admin/safety/warnings");
export const createAdminSafetyWarning = (payload: { userId: number; warningLevel: string; reason: string }) => api.post<AdminSafetyWarning>("/admin/safety/warnings", payload);
export const getAdminSafetySuspensions = () => api.get<AdminSafetySuspension[]>("/admin/safety/suspensions");
export const createAdminSafetySuspension = (payload: { entityType: string; entityId: number; hours?: number; reason: string }) => api.post<AdminSafetySuspension>("/admin/safety/suspensions", payload);
export const resolveAdminSafetySuspension = (id: number) => api.post<AdminSafetySuspension>(`/admin/safety/suspensions/${id}/resolve`);
export const getAdminSafetyBans = () => api.get<AdminSafetyBan[]>("/admin/safety/bans");
export const createAdminSafetyBan = (payload: { type: string; value: string; reason: string }) => api.post<AdminSafetyBan>("/admin/safety/bans", payload);
export const unbanAdminSafetyEntity = (id: number) => api.post<AdminSafetyBan>(`/admin/safety/bans/${id}/unban`);
export const getAdminSafetyAppeals = () => api.get<AdminSafetyAppeal[]>("/admin/safety/appeals");
export const createAdminSafetyAppeal = (payload: { userId: number; caseId: number; caseType: string; message: string }) => api.post<AdminSafetyAppeal>("/admin/safety/appeals", payload);
export const decideAdminSafetyAppeal = (id: number, payload: { decision: string; reason: string }) => api.post<AdminSafetyAppeal>(`/admin/safety/appeals/${id}/decision`, payload);
export const getAdminSafetyMedia = () => api.get<AdminSafetyMedia[]>("/admin/safety/media");
export const decideAdminSafetyMedia = (id: number, payload: { decision: string; reason: string }) => api.post<AdminSafetyMedia>(`/admin/safety/media/${id}/decision`, payload);
export const getAdminSafetyFraudProfiles = () => api.get<AdminSafetyFraudProfile[]>("/admin/safety/fraud-profiles");
export const scanAdminSafetyFraudProfiles = () => api.post<{ created: number; fraudProfiles: AdminSafetyFraudProfile[] }>("/admin/safety/fraud-profiles/scan");
export const getAdminSafetyTeams = () => api.get<AdminSafetyTeam[]>("/admin/safety/teams");
export const moderateAdminSafetyTeam = (id: number, payload: { decision: string; reason: string }) => api.post<AdminSafetyTeam>(`/admin/safety/teams/${id}/moderate`, payload);
export const getAdminSafetyReviews = () => api.get<AdminSafetyReview[]>("/admin/safety/reviews");
export const moderateAdminSafetyReview = (id: number, payload: { decision: string; reason: string }) => api.post<AdminSafetyReview>(`/admin/safety/reviews/${id}/moderate`, payload);
export const adjustAdminSafetyTrust = (userId: number, payload: { newScore: number; reason: string }) => api.post(`/admin/safety/trust/${userId}/adjust`, payload);
export const getAdminSafetyTrustAdjustments = () => api.get<AdminTrustAdjustment[]>("/admin/safety/trust/adjustments");
export const getAdminCommunityModeration = () => api.get<AdminCommunityModeration[]>("/admin/safety/community/moderation");
export const createAdminCommunityModeration = (payload: { contentType: string; contentId: number; action: string; reason: string }) => api.post<AdminCommunityModeration>("/admin/safety/community/moderation", payload);

export const getSupportTickets = () => api.get("/admin/ops/support-tickets");

export const replySupportTicket = (ticketId: number, payload: { reply: string; status: string; internalNotes?: string }) =>
  api.post(`/admin/ops/support-tickets/${ticketId}/reply`, payload);

export const getIncidents = () => api.get("/admin/ops/incidents");

export const createIncident = (payload: { name: string; status?: string; severity: string; message: string }) =>
  api.post("/admin/ops/incidents", payload);

export const resolveIncident = (id: number) => api.post(`/admin/ops/incidents/${id}/resolve`);

export const getCampaignAnalytics = (campaignId: number) => api.get(`/campaigns/analytics/${campaignId}`);

export const getTemplates = () => api.get("/admin/templates");
export const createTemplate = (payload: Record<string, unknown>) => api.post("/admin/templates", payload);
export const deleteTemplate = (id: number) => api.delete(`/admin/templates/${id}`);

export const getSegments = () => api.get("/admin/segments");
export const createSegment = (payload: Record<string, unknown>) => api.post("/admin/segments", payload);
export const deleteSegment = (id: number) => api.delete(`/admin/segments/${id}`);
