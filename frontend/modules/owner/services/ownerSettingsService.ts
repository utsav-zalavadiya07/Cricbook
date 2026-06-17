import api from "@/services/api";

export type OwnerSettingsPayload = {
  businessName: string;
  supportPhone: string;
  businessAddress: string;
  gstNumber: string;
  businessLogoUrl: string;
  supportEmail: string;
  businessType: "SINGLE_TURF" | "MULTI_TURF" | "FRANCHISE";
  operatingHours: string;
  defaultSlotDurationMinutes: number;
  openingTime: string;
  closingTime: string;
  indoorOutdoor: "INDOOR" | "OUTDOOR" | "SEMI_INDOOR";
  amenities: string;
  advanceBookingDays: number;
  minimumBookingMinutes: number;
  allowInstantBooking: boolean;
  approvalRequired: boolean;
  trustedPlayersOnly: boolean;
  cancellationPolicy: "FLEXIBLE" | "MODERATE" | "STRICT";
  dynamicPricingEnabled: boolean;
  weekendPricingEnabled: boolean;
  peakPricingEnabled: boolean;
  pricingMode: "MANUAL" | "AI_SUGGESTED" | "AUTOMATIC";
  peakMultiplier: number;
  weekendMultiplier: number;
  staffInviteEmail: string;
  staffRole: "MANAGER" | "RECEPTIONIST" | "GROUND_STAFF" | "FINANCE_STAFF" | "TOURNAMENT_ADMIN";
  staffPermissions: string;
  bankAccountName: string;
  bankAccountNumber: string;
  bankIfsc: string;
  upiId: string;
  settlementSchedule: "DAILY" | "WEEKLY" | "MONTHLY";
  bookingAlerts: boolean;
  paymentAlerts: boolean;
  reviewAlerts: boolean;
  marketingAlerts: boolean;
  promotionWhatsappEnabled: boolean;
  reminderLeadMinutes: number;
  partialPaymentEnabled: boolean;
  minimumPaymentPercent: number;
  refundMode: "FULL" | "PARTIAL" | "WALLET_CREDIT" | "RESCHEDULE_ONLY";
  rainRefundAllowed: boolean;
  freeWeatherReschedule: boolean;
  weatherRiskAlerts: boolean;
  rainThresholdPercent: number;
  resaleEnabled: boolean;
  transferEnabled: boolean;
  maxResaleMarkupPercent: number;
  restrictSuspiciousAccounts: boolean;
  allowPublicTournaments: boolean;
  communityVisibility: "PUBLIC" | "INVITE_ONLY" | "PRIVATE";
  teamRegistrationRule: "OPEN" | "APPROVAL_REQUIRED" | "INVITE_ONLY";
  brandPrimaryColor: string;
  brandTheme: "light" | "dark" | "system";
  compactDashboard: boolean;
  twoFactorEnabled: boolean;
  sessionTimeoutMinutes: number;
  ipRestrictions: string;
  autoPricingEnabled: boolean;
  autoRescheduleEnabled: boolean;
  aiCoachEnabled: boolean;
  smartPromotionsEnabled: boolean;
  revenueExportEnabled: boolean;
  gstReportsEnabled: boolean;
  exportFormat: "CSV" | "EXCEL" | "PDF";
  dataExportRequested: boolean;
};

export type OwnerSettingsResponse = OwnerSettingsPayload & {
  ownerId: number;
  ownerName: string;
  ownerEmail: string;
  ownerPhone: string;
  accountStatus: string;
  preferredLanguage: string;
  preferredTheme: string;
  readinessScore: number;
  lastExportRequestedAt?: string;
  updatedAt?: string;
};

export const getOwnerSettings = () => api.get<OwnerSettingsResponse>("/owner/settings");

export const updateOwnerSettings = (payload: OwnerSettingsPayload) =>
  api.put<OwnerSettingsResponse>("/owner/settings", payload, {
    notification: {
      category: "system",
      success: {
        title: "Owner Settings Saved",
        message: "Your owner portal preferences have been updated.",
      },
    },
  });

export const requestOwnerDataExport = () =>
  api.post<OwnerSettingsResponse>("/owner/settings/data-export", undefined, {
    notification: {
      category: "system",
      success: {
        title: "Owner Export Requested",
        message: "We will prepare your owner account data export.",
      },
    },
  });
