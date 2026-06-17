import type {
  AdminBookingCancellation,
  AdminBookingDispute,
  AdminBookingEscalation,
  AdminBookingOpsDashboard,
  AdminBookingRecord,
  AdminBookingTimeline,
  AdminCommissionRule,
  AdminCommunityModeration,
  AdminConfigurationAudit,
  AdminFeatureFlag,
  AdminFinanceAudit,
  AdminFinanceDashboard,
  AdminFinanceDispute,
  AdminFinanceFraudAlert,
  AdminFinanceRefund,
  AdminFinanceTransaction,
  AdminManagedTeam,
  AdminManagedUser,
  AdminMarketingAnalytics,
  AdminMarketingBanner,
  AdminMarketingCampaign,
  AdminMarketingCoupon,
  AdminMarketingDashboard,
  AdminOwnerSettlement,
  AdminPermission,
  AdminPermissionGroup,
  AdminPlatformSetting,
  AdminReleaseVersion,
  AdminSafetyAppeal,
  AdminSafetyBan,
  AdminSafetyDashboard,
  AdminSafetyFraudProfile,
  AdminSafetyMedia,
  AdminSafetyReport,
  AdminSafetyReview,
  AdminSafetySuspension,
  AdminSafetyTeam,
  AdminSafetyWarning,
  AdminSlotConflict,
  AdminSupportAgent,
  AdminSupportDashboard,
  AdminSupportTicket,
  AdminTaxReport,
  AdminTrustAdjustment,
  AdminUserActivity,
  AdminUserCenterSummary,
  AdminWalletSummary,
  PermissionAudit,
} from "@/modules/admin/services/adminService";

export const withDemo = <T,>(items: T[] | undefined, demoItems: T[]) => (items && items.length > 0 ? items : demoItems);

export const demoUserSummary: AdminUserCenterSummary = {
  totalPlayers: 1,
  totalOwners: 1,
  totalTeams: 1,
  pendingVerifications: 1,
  suspendedAccounts: 1,
  highRiskUsers: 1,
  newRegistrations: 3,
};

export const demoManagedUsers: AdminManagedUser[] = [
  { id: 101, name: "Rahul Sharma", email: "rahul.player@demo.com", phone: "9876500001", status: "ACTIVE", roles: ["USER"], city: "Ahmedabad", trustScore: 92, riskLevel: "LOW", reports: 0, bookings: 8, teams: 1 },
  { id: 102, name: "Amit Owner", email: "amit.owner@demo.com", phone: "9876500002", status: "ACTIVE", roles: ["OWNER"], city: "Ahmedabad", trustScore: 88, riskLevel: "MEDIUM", reports: 1, bookings: 24, teams: 0 },
  { id: 103, name: "Flagged Player", email: "risk.player@demo.com", phone: "9876500003", status: "BLOCKED", roles: ["USER"], city: "Surat", trustScore: 38, riskLevel: "HIGH", reports: 4, bookings: 2, teams: 0 },
];

export const demoTeams: AdminManagedTeam[] = [
  { id: 11, name: "Night Strikers", sport: "Box Cricket", city: "Ahmedabad", captainId: 101, captainName: "Rahul Sharma", members: 9, visibility: "PUBLIC" },
];

export const demoActivity: AdminUserActivity[] = [
  { id: 1, userId: 101, activityType: "BOOKING_CREATED", entityId: 501, summary: "Rahul booked Box Cricket Arena for 7 PM." },
];

export const demoOwnerVerifications = [
  { id: 1, ownerId: 102, documents: "GST, PAN, business license uploaded", status: "PENDING", verifiedBy: "" },
];

export const demoTurfs = [
  { id: 301, name: "Green Box Arena", location: "SG Highway", city: "Ahmedabad", pricePerHour: 1500, pitchType: "TURF", boxType: "6v6", status: "ACTIVE", rating: 4.6, totalReviews: 28 },
  { id: 302, name: "Night Lights Cricket", location: "Vesu", city: "Surat", pricePerHour: 2200, pitchType: "TURF", boxType: "7v7", status: "MAINTENANCE", rating: 4.2, totalReviews: 11 },
] as const;

export const demoTurfModerations = [
  { id: 1, turfId: 302, status: "PENDING", reason: "Photo and pricing verification required" },
] as const;

export const demoBookingDashboard: AdminBookingOpsDashboard = {
  todaysBookings: 4,
  activeMatches: 1,
  upcomingSlots: 3,
  delayedCheckIns: 1,
  pendingCancellations: 1,
  activeDisputes: 1,
  noShowCases: 1,
  criticalEscalations: 1,
  cancellationRate: 8,
  disputeRate: 4,
  noShowRate: 3,
  liveFeed: [{ id: 1, bookingId: 501, eventType: "CREATED", performedBy: "Player", details: "Booking created for Green Box Arena." }],
  insight: "Sample flow: booking created, payment pending, dispute can be resolved by admin.",
};

export const demoBookings: AdminBookingRecord[] = [
  { id: 501, playerId: 101, player: "Rahul Sharma", playerEmail: "rahul.player@demo.com", ownerId: 102, owner: "Amit Owner", turfId: 301, turf: "Green Box Arena", slotId: 47, slot: "19:00 - 21:00", bookingDate: "2026-06-01", paymentStatus: "PAID", amount: 3000, status: "CONFIRMED" },
];

export const demoCancellations: AdminBookingCancellation[] = [
  { id: 1, bookingId: 501, player: "Rahul Sharma", turf: "Green Box Arena", requestedBy: "PLAYER", cancellationType: "PLAYER_CANCELLATION", reason: "Team unavailable", status: "PENDING" },
];

export const demoDisputes: AdminBookingDispute[] = [
  { id: 1, bookingId: 501, disputeType: "PAYMENT", status: "OPEN", summary: "Player claims amount deducted twice.", assignedTo: "support-admin" },
];

export const demoConflicts: AdminSlotConflict[] = [
  { id: 1, slotId: 47, bookingA: 501, bookingB: 502, status: "OPEN", notes: "Overlapping 7 PM slot detected." },
];

export const demoNoShows = [{ id: 1, bookingId: 501, userId: 103, userName: "Flagged Player", noShowType: "PLAYER", status: "OPEN", penalty: 250, notes: "Repeated no-show sample." }];
export const demoWeatherCases = [{ id: 1, bookingId: 501, refundType: "WALLET_CREDIT", amount: 1500, status: "PENDING", reason: "Heavy rain alert." }];
export const demoTimeline: AdminBookingTimeline[] = [{ id: 1, bookingId: 501, eventType: "PAYMENT_CAPTURED", performedBy: "System", details: "₹3000 payment captured." }];
export const demoEscalations: AdminBookingEscalation[] = [{ id: 1, bookingId: 501, ticketId: 701, priority: "HIGH", assignedTo: "ops-lead", status: "OPEN", summary: "Resolve slot/payment conflict before match time." }];

export const demoFinanceDashboard: AdminFinanceDashboard = {
  gmv: 17500,
  netRevenue: 15750,
  commissionRevenue: 1750,
  refundAmount: 1500,
  pendingSettlements: 2,
  transactions: 3,
  refunds: 1,
  openDisputes: 1,
  fraudAlerts: 1,
  dailyRevenue: [{ date: "2026-05-28", revenue: 4500 }, { date: "2026-05-29", revenue: 7500 }, { date: "2026-05-30", revenue: 5500 }],
  insight: "Sample finance flow: transaction paid, refund queued, settlement pending.",
};

export const demoTransactions: AdminFinanceTransaction[] = [{ id: 1, transactionId: "pay_demo_001", orderId: "order_demo_001", bookingId: 501, player: "Rahul Sharma", owner: "Amit Owner", amount: 3000, gateway: "Razorpay", status: "SUCCESS" }];
export const demoRefunds: AdminFinanceRefund[] = [{ id: 1, paymentId: 1, bookingId: 501, amount: 1500, status: "PENDING", reason: "Weather disruption" }];
export const demoSettlements: AdminOwnerSettlement[] = [{ id: 1, ownerId: 102, ownerName: "Amit Owner", bookingId: 501, grossAmount: 3000, commission: 300, netAmount: 2700, status: "PENDING" }];
export const demoCommissionRules: AdminCommissionRule[] = [{ id: 1, type: "PERCENT", value: 10, active: true, description: "Default platform commission" }];
export const demoWallets: AdminWalletSummary[] = [{ id: 1, userId: 101, userName: "Rahul Sharma", balance: 750, transactions: [{ id: 1, amount: 750, type: "WEATHER_CREDIT", status: "SUCCESS", description: "Sample wallet credit" }] }];
export const demoFinanceDisputes: AdminFinanceDispute[] = [{ id: 1, userId: 101, bookingId: 501, type: "DOUBLE_DEBIT", status: "OPEN", amount: 3000, summary: "Customer reports duplicate debit." }];
export const demoReconciliation = [{ id: 1, transactionId: "pay_demo_001", gatewayAmount: 3000, systemAmount: 3000, status: "MATCHED", notes: "Sample matched transaction" }];
export const demoTaxReports: AdminTaxReport[] = [{ id: 1, reportType: "GST_SUMMARY", period: "2026-05", taxableAmount: 14830, gstAmount: 2670 }];
export const demoFinanceAudits: AdminFinanceAudit[] = [{ id: 1, action: "REFUND_REVIEWED", actor: "admin@cricbook.com", userId: 101, entity: "Booking #501", details: "Sample refund reviewed by finance admin." }];
export const demoFinanceFraud: AdminFinanceFraudAlert[] = [{ id: 1, userId: 103, severity: "HIGH", reason: "Multiple refund requests in short window." }];

export const demoSupportDashboard: AdminSupportDashboard = {
  openTickets: 1,
  resolvedTickets: 2,
  averageResponseHours: 1.2,
  averageResolutionHours: 6,
  slaBreaches: 0,
  liveChats: 1,
  escalations: 1,
  csatScore: 4.5,
  agentAvailability: 80,
  ticketTrends: [{ date: "2026-05-30", tickets: 3 }],
  aiInsights: [{ title: "Refund tickets rising", message: "Most sample support demand is refund-related.", severity: "MEDIUM" }],
};

export const demoSupportTickets: AdminSupportTicket[] = [{ id: 701, ticketNumber: "TCK-701", userId: 101, category: "Payment Issue", priority: "HIGH", status: "OPEN", assignedTo: "support-agent", channel: "IN_APP", issue: "Payment deducted but booking still pending.", slaBreached: false }];
export const demoAgents: AdminSupportAgent[] = [{ id: 1, userId: 9001, name: "Support Agent", role: "L1 Support", status: "ONLINE" }];

export const demoMarketingDashboard: AdminMarketingDashboard = { activeCampaigns: 1, couponUsage: 12, referralRevenue: 4500, pushDeliveryRate: 94, emailOpenRate: 42, campaignRoi: 3.2, aiInsights: [{ title: "Weekend offer works", message: "Sample users respond better to weekend turf offers.", severity: "LOW" }] };
export const demoBanners: AdminMarketingBanner[] = [{ id: 1, title: "Weekend Cricket Offer", location: "Homepage", bannerType: "Image", imageUrl: "/demo-banner.jpg", targetUrl: "/search", status: "ACTIVE" }];
export const demoCoupons: AdminMarketingCoupon[] = [{ id: 1, code: "WELCOME100", discountType: "FLAT", discountValue: 100, usageCount: 12, active: true }];
export const demoCampaigns: AdminMarketingCampaign[] = [{ id: 1, name: "Weekend Push", title: "Book cricket this weekend", message: "Save on prime slots.", channel: "PUSH", status: "DRAFT", sentCount: 0 }];
export const demoMarketingAnalytics: AdminMarketingAnalytics = { couponUsage: 12, referralConversion: 8, campaignRoi: 3.2, pushOpenRate: 28, emailOpenRate: 42 };

export const demoSystemSettings: AdminPlatformSetting[] = [
  { id: 1, key: "GLOBAL_PLATFORM_NAME", value: "Cricbook" },
  { id: 2, key: "PAYMENT_PLATFORM_COMMISSION", value: "10" },
  { id: 3, key: "BOOKING_ADVANCE_DAYS", value: "30" },
];
export const demoFeatureFlags: AdminFeatureFlag[] = [{ id: 1, featureKey: "ENABLE_TOURNAMENTS", enabled: true, rolloutPercentage: 100, targetAudience: "ALL", description: "Sample feature rollout" }];
export const demoReleases: AdminReleaseVersion[] = [{ id: 1, version: "2.5-demo", notes: "Demo release for admin control center.", status: "ROLLOUT", rolloutPercentage: 25 }];
export const demoConfigAudits: AdminConfigurationAudit[] = [{ id: 1, configKey: "PAYMENT_PLATFORM_COMMISSION", oldValue: "8", newValue: "10", changedBy: "admin@cricbook.com" }];

export const demoSafetyDashboard: AdminSafetyDashboard = { openReports: 1, pendingAppeals: 1, suspendedUsers: 1, flaggedPhotos: 1, fraudCases: 1, trustViolations: 1, warnings: 1, bans: 1, safetyHealthScore: 86, reportsPerDay: [{ date: "2026-05-30", reports: 2 }], insight: "Sample safety flow: report, warning, suspension, appeal, and media review." };
export const demoSafetyReports: AdminSafetyReport[] = [{ id: 1, reporter: "Rahul Sharma", reportedUserId: 103, reportedUser: "Flagged Player", reason: "No Show", description: "Repeatedly missed confirmed match.", status: "OPEN" }];
export const demoWarnings: AdminSafetyWarning[] = [{ id: 1, userId: 103, userName: "Flagged Player", warningLevel: "LEVEL_2", reason: "No-show pattern", issuedBy: "admin@cricbook.com" }];
export const demoSuspensions: AdminSafetySuspension[] = [{ id: 1, entityType: "USER", entityId: 103, status: "ACTIVE", reason: "Repeated no-shows" }];
export const demoBans: AdminSafetyBan[] = [{ id: 1, type: "PHONE", value: "9876500003", reason: "Fraud risk sample", active: true }];
export const demoAppeals: AdminSafetyAppeal[] = [{ id: 1, userId: 103, userName: "Flagged Player", caseId: 1, caseType: "SUSPENSION", message: "I want to explain the missed game.", status: "PENDING" }];
export const demoMedia: AdminSafetyMedia[] = [{ id: 1, ownerType: "USER", ownerId: 103, mediaType: "PROFILE_PHOTO", imageUrl: "", moderationStatus: "PENDING", active: true }];
export const demoFraudProfiles: AdminSafetyFraudProfile[] = [{ id: 1, userId: 103, userName: "Flagged Player", riskLevel: "HIGH", reason: "Same device used by multiple accounts", signals: "DEVICE_MATCH,REFUND_SPIKE", status: "OPEN" }];
export const demoSafetyTeams: AdminSafetyTeam[] = [{ id: 11, name: "Night Strikers", sport: "Box Cricket", city: "Ahmedabad", visibility: "PUBLIC", captain: "Rahul Sharma" }];
export const demoSafetyReviews: AdminSafetyReview[] = [{ id: 1, userId: 101, turfId: 301, rating: 2, comment: "Owner cancelled last minute.", reports: 1 }];
export const demoTrustAdjustments: AdminTrustAdjustment[] = [{ id: 1, userId: 103, userName: "Flagged Player", oldScore: 55, newScore: 45, reason: "No-show penalty", adminId: "admin@cricbook.com" }];
export const demoCommunityModeration: AdminCommunityModeration[] = [{ id: 1, contentType: "POST", contentId: 9001, action: "HIDE", reason: "Spam recruitment post", moderatorId: "admin@cricbook.com" }];

export const demoPermissions: AdminPermission[] = [{ id: 1, code: "USER_VIEW", name: "User View", module: "USER", description: "View users" }];
export const demoPermissionGroups: AdminPermissionGroup[] = [{ id: 1, name: "Sample User Controls", description: "Demo permission group", permissions: demoPermissions }];
export const demoPermissionAudit: PermissionAudit[] = [{ id: 1, actor: "admin@cricbook.com", action: "ROLE_ASSIGNED", entity: "Admin User", details: "Assigned sample support role" }];
