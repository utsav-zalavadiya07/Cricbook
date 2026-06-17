import api from "@/services/api";

export type PlayerSettingsResponse = {
  profile: {
    userId: number;
    displayName: string;
    email: string;
    phone: string;
    bio?: string | null;
    photoUrl?: string | null;
    city?: string | null;
    position?: string | null;
    skillLevel?: string | null;
    visibility: string;
    completionPercentage?: number | null;
  };
  security: {
    twoFactorEnabled: boolean;
    loginMethod: string;
    emailVerified: boolean;
    phoneVerified: boolean;
  };
  notifications: {
    push: boolean;
    email: boolean;
    sms: boolean;
    whatsapp: boolean;
    frequency: string;
    channels: Record<string, boolean>;
  };
  appearance: {
    theme: string;
    accentColor: string;
    compactMode: boolean;
    reduceMotion: boolean;
  };
  privacy: {
    profileVisibility: string;
    showTrustScore: boolean;
    showStats: boolean;
    showAvailability: boolean;
    showLocation: boolean;
  };
  booking: {
    preferredDay?: string | null;
    preferredTime?: string | null;
    preferredDurationMinutes?: number | null;
    preferredCity?: string | null;
    preferredTeamId?: number | null;
    autoWaitlistEnabled: boolean;
    quickRebookEnabled: boolean;
  };
  matchmaking: {
    competitionLevel: string;
    preferredSkillLevel: string;
    trustedOnly: boolean;
    nearbyOnly: boolean;
  };
  community: {
    allowTeamInvites: boolean;
    allowFriendRequests: boolean;
    allowMessages: boolean;
  };
  payments: {
    defaultPaymentMethod?: string | null;
    savedUpiId?: string | null;
    autoPaySplits: boolean;
  };
  localization: {
    language: string;
    regionCity: string;
    currency: string;
    timezone: string;
  };
  devices: {
    activeSessions: number;
    currentDevice: string;
    lastActivity: string;
  };
  data: {
    downloadDataRequested: boolean;
    lastExportRequestedAt?: string | null;
    exportFormats: string[];
  };
  smartSuggestions: string[];
};

export type PlayerSettingsPayload = Partial<{
  profile: Partial<PlayerSettingsResponse["profile"]>;
  security: Partial<PlayerSettingsResponse["security"]>;
  notifications: Partial<PlayerSettingsResponse["notifications"]>;
  privacy: Partial<PlayerSettingsResponse["privacy"]>;
  booking: Partial<PlayerSettingsResponse["booking"]>;
  matchmaking: Partial<PlayerSettingsResponse["matchmaking"]>;
  community: Partial<PlayerSettingsResponse["community"]>;
  payments: Partial<PlayerSettingsResponse["payments"]>;
  localization: Partial<PlayerSettingsResponse["localization"]>;
  data: Partial<PlayerSettingsResponse["data"]>;
}>;

export const getPlayerSettings = () => api.get<PlayerSettingsResponse>("/settings");
export const updatePlayerSettings = (payload: PlayerSettingsPayload) =>
  api.put<PlayerSettingsResponse>("/settings", payload, {
    notification: {
      category: "system",
      success: {
        title: "Settings Saved",
        message: "Your account preferences have been updated.",
      },
    },
  });
export const requestDataExport = () =>
  api.post<PlayerSettingsResponse>("/settings/data-export", undefined, {
    notification: {
      category: "system",
      success: {
        title: "Export Requested",
        message: "We will prepare your account data export.",
      },
    },
  });
